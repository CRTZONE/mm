package com.example

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.EmailDeckScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private var isPipMode by mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          EmailDeckScreen(
            isInPipMode = isPipMode,
            onEnterPip = { enterPip() }
          )
        }
      }
    }
  }

  private fun enterPip() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      try {
        val aspectRatio = Rational(9, 16)
        val params = PictureInPictureParams.Builder()
          .setAspectRatio(aspectRatio)
          .build()
        enterPictureInPictureMode(params)
      } catch (_: Exception) {
      }
    }
  }

  override fun onPictureInPictureModeChanged(
    isInPictureInPictureMode: Boolean,
    newConfig: Configuration
  ) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    isPipMode = isInPictureInPictureMode
  }

  override fun onUserLeaveHint() {
    super.onUserLeaveHint()
    // When leaving to home screen, auto-enter PiP if desired
  }
}
