package ar.edu.uade.miti.data.repository

import ar.edu.uade.miti.core.Resource
import ar.edu.uade.miti.data.local.datastore.SesionGuardada
import ar.edu.uade.miti.data.remote.CLAVE_DE_PRUEBA
import ar.edu.uade.miti.data.remote.ConfiguracionSupabase
import ar.edu.uade.miti.data.remote.FakeHogarActivo
import ar.edu.uade.miti.data.remote.FakeSesionLocal
import ar.edu.uade.miti.data.remote.ProveedorDeToken
import ar.edu.uade.miti.data.remote.RESPUESTA_INESPERADA
import ar.edu.uade.miti.data.remote.SIN_CONEXION
import ar.edu.uade.miti.data.remote.SIN_CONFIGURAR
import ar.edu.uade.miti.data.remote.authApiContra
import ar.edu.uade.miti.data.remote.sesionJson
import ar.edu.uade.miti.domain.model.Usuario
import com.squareup.moshi.Moshi
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SesionRepositoryImplTest {

    private val servidor = MockWebServer()
    private val ahora = 1_800_000_000_000L
    private val reloj = Clock.fixed(Instant.ofEpochMilli(ahora), ZoneOffset.UTC)
    private val moshi = Moshi.Builder().build()
    private val sesionLocal = FakeSesionLocal()
    private val hogarActivo = FakeHogarActivo()
    private var datosBorrados = false

    @Before fun arrancar() = servidor.start()
    @After fun parar() = servidor.close()

    private fun repositorio(configurado: Boolean = true): SesionRepositoryImpl {
        val api = authApiContra(servidor, moshi)
        val config = ConfiguracionSupabase(if (configurado) servidor.url("/").toString() else "", CLAVE_DE_PRUEBA)
        return SesionRepositoryImpl(
            api, config, sesionLocal, ProveedorDeToken(api, sesionLocal, reloj), hogarActivo,
            { datosBorrados = true }, moshi,
        )
    }

    @Test
    fun `ingresar guarda la sesion pero nunca la contrasenia`() = runTest {
        servidor.enqueue(MockResponse(code = 200, body = sesionJson()))

        val resultado = repositorio().iniciarSesion("ana@mail.com", "secreta1")

        assertEquals(Resource.Success(Usuario("u-ana", "ana@mail.com")), resultado)
        val pedido = servidor.takeRequest()
        assertEquals("/auth/v1/token", pedido.url.encodedPath)
        assertEquals("password", pedido.url.queryParameter("grant_type"))
        assertEquals(CLAVE_DE_PRUEBA, pedido.headers["apikey"])
        assertEquals(
            SesionGuardada("u-ana", "ana@mail.com", "access-1", "refresh-1", ahora + 3_600_000),
            sesionLocal.sesion.value,
        )
        assertFalse(sesionLocal.sesion.value.toString().contains("secreta1"))
        assertEquals(Usuario("u-ana", "ana@mail.com"), repositorio().observarUsuario().first())
    }

    @Test
    fun `credenciales incorrectas dan el mensaje de la preentrega`() = runTest {
        servidor.enqueue(MockResponse(code = 400, body = """{"code":400,"error_code":"invalid_credentials","msg":"Invalid login credentials"}"""))

        val resultado = repositorio().iniciarSesion("ana@mail.com", "otra")

        assertEquals(Resource.Error("Email o contraseña incorrectos"), resultado)
        assertNull(sesionLocal.sesion.value)
    }

    @Test
    fun `el formato viejo de error de Supabase tambien se entiende`() = runTest {
        servidor.enqueue(MockResponse(code = 400, body = """{"error":"invalid_grant","error_description":"Invalid login credentials"}"""))

        assertEquals(Resource.Error("Email o contraseña incorrectos"), repositorio().iniciarSesion("ana@mail.com", "otra"))
    }

    @Test
    fun `sin conexion pide conectarse`() = runTest {
        val repo = repositorio()
        servidor.close()

        assertEquals(SIN_CONEXION, (repo.iniciarSesion("ana@mail.com", "secreta1") as Resource.Error).message)
    }

    @Test
    fun `una respuesta rota no se confunde con falta de conexion`() = runTest {
        servidor.enqueue(MockResponse(code = 200, body = "{esto no es json"))

        assertEquals(RESPUESTA_INESPERADA, (repositorio().iniciarSesion("ana@mail.com", "secreta1") as Resource.Error).message)
    }

    @Test
    fun `crear cuenta manda el nombre y deja la sesion iniciada`() = runTest {
        servidor.enqueue(MockResponse(code = 200, body = sesionJson()))

        val resultado = repositorio().crearCuenta("Ana", "ana@mail.com", "secreta1")

        assertTrue(resultado is Resource.Success)
        val pedido = servidor.takeRequest()
        assertEquals("/auth/v1/signup", pedido.url.encodedPath)
        assertTrue(pedido.body!!.utf8().contains("\"data\":{\"nombre\":\"Ana\"}"))
        assertEquals("access-1", sesionLocal.sesion.value?.accessToken)
    }

    @Test
    fun `un email ya registrado da un error claro`() = runTest {
        servidor.enqueue(MockResponse(code = 422, body = """{"code":422,"error_code":"user_already_exists","msg":"User already registered"}"""))

        assertEquals(
            Resource.Error("Ya existe una cuenta con ese email. Probá ingresar."),
            repositorio().crearCuenta("Ana", "ana@mail.com", "secreta1"),
        )
    }

    @Test
    fun `si el proyecto exige confirmar el email, avisa y no inicia sesion`() = runTest {
        servidor.enqueue(MockResponse(code = 200, body = """{"id":"u-ana","email":"ana@mail.com","aud":"authenticated"}"""))

        val resultado = repositorio().crearCuenta("Ana", "ana@mail.com", "secreta1")

        assertTrue((resultado as Resource.Error).message.contains("confirmar"))
        assertNull(sesionLocal.sesion.value)
    }

    @Test
    fun `sin configurar no intenta conectarse`() = runTest {
        assertEquals(Resource.Error(SIN_CONFIGURAR), repositorio(configurado = false).iniciarSesion("ana@mail.com", "secreta1"))
        assertEquals(0, servidor.requestCount)
    }

    @Test
    fun `cerrar sesion revoca el token y borra todo lo local`() = runTest {
        sesionLocal.guardar(SesionGuardada("u-ana", "ana@mail.com", "access-1", "refresh-1", ahora + 3_600_000))
        servidor.enqueue(MockResponse(code = 204))

        assertEquals(Resource.Success(Unit), repositorio().cerrarSesion())

        assertEquals("Bearer access-1", servidor.takeRequest().headers["Authorization"])
        assertNull(sesionLocal.sesion.value)
        assertNull(hogarActivo.id.value)
        assertTrue(datosBorrados)
    }

    @Test
    fun `cerrar sesion sin conexion igual borra todo lo local`() = runTest {
        sesionLocal.guardar(SesionGuardada("u-ana", "ana@mail.com", "access-1", "refresh-1", ahora + 3_600_000))
        val repo = repositorio()
        servidor.close()

        assertEquals(Resource.Success(Unit), repo.cerrarSesion())
        assertNull(sesionLocal.sesion.value)
        assertTrue(datosBorrados)
    }
}
