package com.fixmateai.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * The structured diagnosis returned by the AI model.
 *
 * Fields are **nullable**: the AI may omit some keys (e.g. for a "no damage"
 * image), and Gson bypasses Kotlin constructors so missing keys arrive as null.
 * Use the `*OrDefault` helpers below for safe display.
 */
@Parcelize
data class DiagnosisResult(
    @SerializedName("damageType") val damageType: String? = null,
    @SerializedName("severity") val severity: String? = null,
    @SerializedName("possibleCauses") val possibleCauses: List<String>? = null,
    @SerializedName("recommendedRepair") val recommendedRepair: String? = null,
    @SerializedName("urgency") val urgency: String? = null,
    @SerializedName("requiredTools") val requiredTools: List<String>? = null,
    @SerializedName("safetyWarnings") val safetyWarnings: List<String>? = null,
    @SerializedName("suggestedTradesperson") val suggestedTradesperson: String? = null,
    @SerializedName("summary") val summary: String? = null,
    /** Ordered DIY repair steps, rendered as a checklist on the diagnosis screen. */
    @SerializedName("steps") val steps: List<String>? = null,
    /** Whether a confident DIYer could reasonably tackle this themselves. */
    @SerializedName("canDiy") val canDiy: Boolean? = null
) : Parcelable {

    // --- Null-safe accessors for the UI ---
    val damageTypeOrDefault: String get() = damageType?.takeIf { it.isNotBlank() } ?: "Unknown"
    val stepsList: List<String> get() = steps?.filter { it.isNotBlank() } ?: emptyList()
    /** True when the AI says DIY is reasonable AND severity isn't critical/high. */
    val isDiyFriendly: Boolean
        get() = (canDiy == true) && severity?.lowercase() !in listOf("critical", "high")
    val severityOrDefault: String get() = severity?.takeIf { it.isNotBlank() } ?: "Unknown"
    val urgencyOrDefault: String get() = urgency?.takeIf { it.isNotBlank() } ?: "Unknown"
    val summaryOrDefault: String get() = summary?.takeIf { it.isNotBlank() } ?: "No summary provided."
    val recommendedRepairOrDefault: String
        get() = recommendedRepair?.takeIf { it.isNotBlank() } ?: "No repair suggestion available."
    val tradespersonOrDefault: String
        get() = suggestedTradesperson?.takeIf { it.isNotBlank() } ?: "handyman"

    val causesText: String get() = bulletList(possibleCauses, "•")
    val toolsText: String get() = bulletList(requiredTools, "•")
    val safetyText: String get() = bulletList(safetyWarnings, "⚠")

    private fun bulletList(items: List<String>?, bullet: String): String {
        if (items.isNullOrEmpty()) return "—"
        return items.joinToString("\n") { "$bullet $it" }
    }
}
