package com.aistudio.docscanpro.zqwk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.aistudio.docscanpro.zqwk.ui.components.AppDrawerContent
import com.aistudio.docscanpro.zqwk.ui.screens.CameraScannerScreen
import com.aistudio.docscanpro.zqwk.ui.screens.DashboardScreen
import com.aistudio.docscanpro.zqwk.ui.screens.DocumentViewerScreen
import com.aistudio.docscanpro.zqwk.ui.screens.ExportConvertScreen
import com.aistudio.docscanpro.zqwk.ui.screens.SerialExcelViewerScreen
import com.aistudio.docscanpro.zqwk.ui.screens.SerialScannerScreen
import com.aistudio.docscanpro.zqwk.ui.screens.SubscriptionScreen
import com.aistudio.docscanpro.zqwk.ui.theme.MyApplicationTheme
import com.aistudio.docscanpro.zqwk.viewmodel.AppScreen
import com.aistudio.docscanpro.zqwk.viewmodel.ScannerViewModel
import kotlinx.coroutines.launch

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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentScreen == AppScreen.DASHBOARD,
        drawerContent = {
            AppDrawerContent(
                viewModel = viewModel,
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    AppScreen.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onOpenDrawer = {
                            coroutineScope.launch { drawerState.open() }
                        }
                    )
                    AppScreen.CAMERA_SCANNER -> CameraScannerScreen(viewModel = viewModel)
                    AppScreen.DOCUMENT_VIEWER -> DocumentViewerScreen(viewModel = viewModel)
                    AppScreen.EXPORT_CONVERT -> ExportConvertScreen(viewModel = viewModel)
                    AppScreen.SUBSCRIPTION -> SubscriptionScreen(viewModel = viewModel)
                    AppScreen.SERIAL_SCANNER -> SerialScannerScreen(viewModel = viewModel)
                    AppScreen.SERIAL_EXCEL_VIEWER -> SerialExcelViewerScreen(viewModel = viewModel)
                }
            }
        }
    }
}
