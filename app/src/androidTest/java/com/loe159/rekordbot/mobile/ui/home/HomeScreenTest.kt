package com.loe159.rekordbot.mobile.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.loe159.rekordbot.mobile.ui.theme.RekordbotTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryActionsAreVisibleAndClickable() {
        var configurationOpened = false
        var queueOpened = false
        var shazamOpened = false
        composeRule.setContent {
            RekordbotTheme {
                HomeScreen(
                    state = HomeUiState(
                        isAirtableConfigured = true,
                        pendingTracks = 2,
                        pendingShazams = 3,
                    ),
                    onConfigureAirtable = { configurationOpened = true },
                    onOpenQueue = { queueOpened = true },
                    onOpenShazam = { shazamOpened = true },
                )
            }
        }

        composeRule.onNodeWithText("File d’attente · 2")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Configurer Airtable")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Ouvrir Shazam · 3")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertTrue(queueOpened)
            assertTrue(configurationOpened)
            assertTrue(shazamOpened)
        }
    }
}
