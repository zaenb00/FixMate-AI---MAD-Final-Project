package com.fixmateai

import com.fixmateai.data.model.DiagnosisResult
import com.fixmateai.domain.MessageGenerator
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [MessageGenerator]. This is a pure-Kotlin class with no Android
 * dependencies, so it runs fast on the JVM (no emulator required).
 *
 * Run from the terminal with:  ./gradlew test
 */
class MessageGeneratorTest {

    private lateinit var generator: MessageGenerator

    @Before
    fun setUp() {
        generator = MessageGenerator()
    }

    @Test
    fun `generated message mentions the damage summary`() {
        val diagnosis = DiagnosisResult(
            damageType = "Ceiling water leak",
            severity = "High",
            recommendedRepair = "Seal the pipe joint and replace the drywall.",
            urgency = "High",
            suggestedTradesperson = "plumber",
            summary = "There is water leakage damage in the kitchen ceiling."
        )

        val message = generator.generate(diagnosis, userName = "Zainab")

        assertTrue(message.contains("water leakage", ignoreCase = true))
        assertTrue(message.contains("High", ignoreCase = true))
        assertTrue(message.contains("Seal the pipe joint", ignoreCase = true))
        assertTrue(message.endsWith("Zainab"))
    }

    @Test
    fun `message is non-empty even with sparse diagnosis`() {
        val message = generator.generate(DiagnosisResult())
        assertTrue(message.isNotBlank())
    }
}
