package com.loe159.rekordbot.mobile.domain.soundcharts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundchartsConfigurationValidatorTest {
    @Test
    fun `disabled enrichment accepts empty credentials`() {
        assertTrue(
            SoundchartsConfigurationValidator.localErrors(SoundchartsConfiguration()).isEmpty(),
        )
    }

    @Test
    fun `enabled enrichment requires both legacy credentials`() {
        val errors = SoundchartsConfigurationValidator.localErrors(
            SoundchartsConfiguration(enabled = true),
        )

        assertEquals(2, errors.size)
    }
}
