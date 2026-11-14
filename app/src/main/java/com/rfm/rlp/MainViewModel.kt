package com.rfm.rlp

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rfm.rlp.analysis.AnalysisReport
import com.rfm.rlp.analysis.ImageAnalyzer
import com.rfm.rlp.analysis.ImageLoader
import com.rfm.rlp.analysis.LoadedImage
import kotlinx.coroutines.launch

enum class Screen { HOME, RESULT, DEVELOPER }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    var screen by mutableStateOf(Screen.HOME)
        private set

    var image by mutableStateOf<LoadedImage?>(null)
        private set

    var report by mutableStateOf<AnalysisReport?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isAnalyzing by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            isLoading = true
            error = null
            report = null
            try {
                image = ImageLoader.load(getApplication(), uri)
            } catch (e: OutOfMemoryError) {
                error = "الصورة كبيرة جداً ولا يمكن تحميلها. اختر صورة أصغر."
            } catch (e: Exception) {
                error = e.message ?: "تعذّر تحميل الصورة."
            } finally {
                isLoading = false
            }
        }
    }

    fun startAnalysis() {
        val current = image ?: return
        if (isAnalyzing) return
        viewModelScope.launch {
            isAnalyzing = true
            error = null
            try {
                report = ImageAnalyzer.analyze(current)
                screen = Screen.RESULT
            } catch (e: OutOfMemoryError) {
                error = "نفدت الذاكرة أثناء الفحص. جرّب صورة أصغر."
            } catch (e: Exception) {
                error = "حدث خطأ أثناء الفحص: ${e.message ?: "غير معروف"}"
            } finally {
                isAnalyzing = false
            }
        }
    }

    fun goHome() {
        screen = Screen.HOME
    }

    fun openDeveloper() {
        screen = Screen.DEVELOPER
    }

    fun clearError() {
        error = null
    }
}
