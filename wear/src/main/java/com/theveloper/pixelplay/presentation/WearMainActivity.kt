package com.theveloper.pixelplay.presentation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.ambient.AmbientLifecycleObserver
import com.theveloper.pixelplay.presentation.theme.WearPixelPlayTheme
import com.theveloper.pixelplay.presentation.viewmodel.WearPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WearMainActivity : FragmentActivity() {

    companion object {
        @Volatile
        var isForeground: Boolean = false
            private set
    }

    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        AmbientLifecycleObserver(this, ambientCallback).also {
            lifecycle.addObserver(it)
        }

        setContent {
            val playerViewModel: WearPlayerViewModel = hiltViewModel()
            val albumArt by playerViewModel.albumArt.collectAsState()
            val paletteSeedArgb by playerViewModel.paletteSeedArgb.collectAsState()
            val themePalette by playerViewModel.themePalette.collectAsState()

            WearPixelPlayTheme(
                albumArt = albumArt,
                seedColorArgb = paletteSeedArgb,
                themePalette = themePalette,
            ) {
                WearNavigation()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isForeground = true
    }

    override fun onStop() {
        isForeground = false
        super.onStop()
    }
}
