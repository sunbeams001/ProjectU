package com.projectu

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.projectu.ui.screens.home.HomeScreen
import com.projectu.ui.theme.AppTheme
import org.koin.compose.KoinContext

@Composable
fun App() {
    KoinContext {
        AppTheme {
            Navigator(HomeScreen()) { navigator ->
                SlideTransition(navigator)
            }
        }
    }
}

