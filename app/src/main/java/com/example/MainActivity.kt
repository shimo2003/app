package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.CameraScannerScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DocumentViewerScreen
import com.example.ui.screens.ExportConvertScreen
import com.example.ui.screens.SubscriptionScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.ScannerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DocScanApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DocScanApp(viewModel: ScannerViewModel) {
    val currentScreen by viewModel.screen.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
            when (screen) {
                AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                AppScreen.CAMERA_SCANNER -> CameraScannerScreen(viewModel = viewModel)
                AppScreen.DOCUMENT_VIEWER -> DocumentViewerScreen(viewModel = viewModel)
                AppScreen.EXPORT_CONVERT -> ExportConvertScreen(viewModel = viewModel)
                AppScreen.SUBSCRIPTION -> SubscriptionScreen(viewModel = viewModel)
            }
        }
    }
}
