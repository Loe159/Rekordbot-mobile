package com.loe159.rekordbot.mobile.ui

import androidx.compose.runtime.Composable
import com.loe159.rekordbot.mobile.ui.home.HomeRoute
import com.loe159.rekordbot.mobile.ui.theme.RekordbotTheme

@Composable
fun RekordbotApp() {
    RekordbotTheme {
        HomeRoute()
    }
}

