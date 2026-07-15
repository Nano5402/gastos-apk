package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FinanceDashboardScreen
import com.example.ui.FinanceViewModel
import com.example.ui.FinanceViewModelFactory
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: FinanceViewModel = viewModel(
        factory = FinanceViewModelFactory(application)
      )
      val isDarkModeState = viewModel.isDarkMode.collectAsState()
      val systemInDark = isSystemInDarkTheme()
      val useDarkTheme = isDarkModeState.value ?: systemInDark

      MyApplicationTheme(darkTheme = useDarkTheme) {
        FinanceDashboardScreen(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}

