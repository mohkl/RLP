package com.rfm.rlp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rfm.rlp.ui.DeveloperScreen
import com.rfm.rlp.ui.HomeScreen
import com.rfm.rlp.ui.ResultScreen
import com.rfm.rlp.ui.theme.RlpTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RlpTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    RlpApp()
                }
            }
        }
    }
}

@Composable
fun RlpApp(viewModel: MainViewModel = viewModel()) {
    BackHandler(enabled = viewModel.screen != Screen.HOME) {
        viewModel.goHome()
    }

    when (viewModel.screen) {
        Screen.HOME -> HomeScreen(
            image = viewModel.image,
            isLoading = viewModel.isLoading,
            isAnalyzing = viewModel.isAnalyzing,
            error = viewModel.error,
            onPickImage = viewModel::onImagePicked,
            onStart = viewModel::startAnalysis,
            onDeveloper = viewModel::openDeveloper,
            onDismissError = viewModel::clearError
        )

        Screen.RESULT -> ResultScreen(
            image = viewModel.image,
            report = viewModel.report,
            onBack = viewModel::goHome,
            onNewImage = viewModel::goHome
        )

        Screen.DEVELOPER -> DeveloperScreen(
            onBack = viewModel::goHome
        )
    }
}
