package com.fixmateai.data.model

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

/**
 * A saved diagnosis report stored in the Firestore `repair_reports` collection.
 *
 * Holds the uploaded image URL, the AI [DiagnosisResult], a repair status and a
 * timestamp. `@DocumentId` lets Firestore auto-populate [id] with the document id.
 */
@Parcelize
data class RepairReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val diagnosis: DiagnosisResult = DiagnosisResult(),
    val status: String = STATUS_PENDING,
    val timestamp: Long = 0L
) : Parcelable {

    companion object {
        const val STATUS_PENDING = "Pending"
        const val STATUS_IN_PROGRESS = "In Progress"
        const val STATUS_RESOLVED = "Resolved"
    }
}
