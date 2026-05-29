package com.example.nervoscompanion

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey
@Serializable data object News : NavKey
@Serializable data object Apps : NavKey
@Serializable data object Tools : NavKey
@Serializable data object Settings : NavKey
@Serializable
data class WebBrowser(val url: String, val title: String) : NavKey
@Serializable data object CkbConsole : NavKey

