package com.fixmateai.domain

import com.fixmateai.data.model.DiagnosisResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Domain-layer use case that turns an AI [DiagnosisResult] into a polite,
 * professional repair-request message the user can send to a tradesperson.
 *
 * Kept free of Android/UI dependencies so it is easy to unit-test.
 */
@Singleton
class MessageGenerator @Inject constructor() {

    fun generate(diagnosis: DiagnosisResult, userName: String = ""): String {
        val builder = StringBuilder()

        builder.append("Hello, ")
        builder.append("I need help with a home repair issue. ")

        if (!diagnosis.summary.isNullOrBlank()) {
            builder.append(diagnosis.summary).append(" ")
        } else if (!diagnosis.damageType.isNullOrBlank()) {
            builder.append("The issue appears to be: ${diagnosis.damageType}. ")
        }

        if (!diagnosis.severity.isNullOrBlank()) {
            builder.append("An AI assessment rates the severity as ${diagnosis.severity}")
            if (!diagnosis.urgency.isNullOrBlank()) {
                builder.append(" with ${diagnosis.urgency.lowercase()} urgency")
            }
            builder.append(". ")
        }

        if (!diagnosis.recommendedRepair.isNullOrBlank()) {
            builder.append("Suggested repair: ${diagnosis.recommendedRepair}. ")
        }

        builder.append(
            "Could you please let me know your availability and an estimate to inspect " +
                "and repair this? "
        )

        if (userName.isNotBlank()) {
            builder.append("\n\nThank you,\n$userName")
        } else {
            builder.append("\n\nThank you.")
        }

        return builder.toString()
    }
}
