package ar.edu.uade.miti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ar.edu.uade.miti.presentacion.MitiApp
import ar.edu.uade.miti.presentacion.tema.MitiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MitiTheme {
                MitiApp()
            }
        }
    }
}
