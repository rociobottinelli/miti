package ar.edu.uade.miti.data.remote

/** URL y clave publicable del proyecto (de local.properties vía BuildConfig; ver supabase/README.md). */
data class ConfiguracionSupabase(val url: String, val clavePublicable: String) {
    val configurado: Boolean get() = url.isNotBlank() && clavePublicable.isNotBlank()

    /** Retrofit necesita una URL válida terminada en "/" aunque la app no esté configurada. */
    val urlBase: String get() = if (configurado) url.trimEnd('/') + "/" else "https://supabase.invalid/"
}
