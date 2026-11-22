package ar.edu.uade.miti.data.remote

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.data.local.datastore.SesionGuardada
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProveedorDeTokenTest {

    private val servidor = MockWebServer()
    private val ahora = 1_800_000_000_000L
    private val reloj = Clock.fixed(Instant.ofEpochMilli(ahora), ZoneOffset.UTC)

    @Before fun arrancar() = servidor.start()
    @After fun parar() = servidor.close()

    private fun sesion(venceEn: Long) = SesionGuardada("u-ana", "ana@mail.com", "access-viejo", "refresh-viejo", venceEn)

    @Test
    fun `si el token sigue vigente no llama al servidor`() = runTest {
        val local = FakeSesionLocal(sesion(venceEn = ahora + 10 * 60_000))

        val token = ProveedorDeToken(authApiContra(servidor), local, reloj).tokenVigente()

        assertEquals(Resource.Success("access-viejo"), token)
        assertEquals(0, servidor.requestCount)
    }

    @Test
    fun `si esta por vencer lo renueva y guarda los tokens nuevos`() = runTest {
        val local = FakeSesionLocal(sesion(venceEn = ahora + 30_000))
        servidor.enqueue(MockResponse(code = 200, body = sesionJson(access = "access-nuevo", refresh = "refresh-nuevo")))

        val token = ProveedorDeToken(authApiContra(servidor), local, reloj).tokenVigente()

        assertEquals(Resource.Success("access-nuevo"), token)
        val pedido = servidor.takeRequest()
        assertEquals("/auth/v1/token?grant_type=refresh_token", pedido.url.encodedPath + "?" + pedido.url.encodedQuery)
        assertTrue(pedido.body!!.utf8().contains("\"refresh_token\":\"refresh-viejo\""))
        assertEquals(CLAVE_DE_PRUEBA, pedido.headers["apikey"])
        val guardada = local.sesion.value!!
        assertEquals("refresh-nuevo", guardada.refreshToken)
        assertEquals(ahora + 3_600_000, guardada.venceEn)
    }

    @Test
    fun `si el refresh token ya no sirve, cierra la sesion local`() = runTest {
        val local = FakeSesionLocal(sesion(venceEn = ahora - 1))
        servidor.enqueue(MockResponse(code = 400, body = """{"code":400,"error_code":"refresh_token_not_found","msg":"Invalid Refresh Token"}"""))

        val token = ProveedorDeToken(authApiContra(servidor), local, reloj).tokenVigente()

        assertEquals(Resource.Error(SESION_VENCIDA), token)
        assertNull(local.sesion.value)
    }

    @Test
    fun `sin conexion no pierde la sesion`() = runTest {
        val local = FakeSesionLocal(sesion(venceEn = ahora - 1))
        val api = authApiContra(servidor)
        servidor.close()

        val token = ProveedorDeToken(api, local, reloj).tokenVigente()

        assertEquals(SIN_CONEXION, (token as Resource.Error).message)
        assertEquals("refresh-viejo", local.sesion.value!!.refreshToken)
    }

    @Test
    fun `sin sesion pide volver a ingresar`() = runTest {
        assertEquals(Resource.Error(SESION_VENCIDA), ProveedorDeToken(authApiContra(servidor), FakeSesionLocal(), reloj).tokenVigente())
    }
}
