package com.fixmateai.data.repository

import android.content.Context
import com.fixmateai.data.model.DiagnosisResult
import com.fixmateai.data.model.RepairMessage
import com.fixmateai.data.model.RepairReport
import com.fixmateai.utils.Constants
import com.fixmateai.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists repair reports: saves the damage image to the app's private internal
 * storage (free, no Firebase Storage / billing required), writes the report
 * document (with the AI diagnosis + local image path) to Firestore, and reads
 * them back for the History screen. Also stores generated outreach messages.
 *
 * Note on image storage: the image lives in internal storage on this device, and
 * its file path is saved in Firestore. This keeps everything on the free tier.
 * Images therefore appear only on the device that created the report.
 */
@Singleton
class ReportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private fun uid(): String? = auth.currentUser?.uid

    /**
     * Saves the compressed image bytes to the app's private internal storage and
     * returns the absolute file path (which Glide can load directly). Runs the
     * file I/O off the main thread.
     */
    suspend fun saveImageLocally(imageBytes: ByteArray): Resource<String> {
        val userId = uid() ?: return Resource.Error("Not signed in.")
        return try {
            withContext(Dispatchers.IO) {
                // /data/data/<app>/files/damage_images/<userId>/
                val dir = File(context.filesDir, "${Constants.STORAGE_DAMAGE_IMAGES}/$userId")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "${System.currentTimeMillis()}.jpg")
                file.writeBytes(imageBytes)
                Resource.Success(file.absolutePath)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save image locally.", e)
        }
    }

    /** Saves a completed report and returns its generated document id. */
    suspend fun saveReport(imageUrl: String, diagnosis: DiagnosisResult): Resource<String> {
        val userId = uid() ?: return Resource.Error("Not signed in.")
        return try {
            val docRef = firestore.collection(Constants.COLLECTION_REPORTS).document()
            val report = RepairReport(
                id = docRef.id,
                userId = userId,
                imageUrl = imageUrl,
                diagnosis = diagnosis,
                status = RepairReport.STATUS_PENDING,
                timestamp = System.currentTimeMillis()
            )
            docRef.set(report).await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save report.", e)
        }
    }

    /** Returns the signed-in user's reports, newest first. */
    suspend fun getReports(): Resource<List<RepairReport>> {
        val userId = uid() ?: return Resource.Error("Not signed in.")
        return try {
            // Note: we filter by userId on the server and sort client-side.
            // This avoids requiring a composite Firestore index (userId + timestamp).
            val snapshot = firestore.collection(Constants.COLLECTION_REPORTS)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val reports = snapshot.toObjects(RepairReport::class.java)
                .sortedByDescending { it.timestamp }
            Resource.Success(reports)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to load reports.", e)
        }
    }

    /** Updates the repair status (Pending / In Progress / Resolved). */
    suspend fun updateStatus(reportId: String, status: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_REPORTS)
                .document(reportId)
                .update("status", status)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update status.", e)
        }
    }

    /** Deletes a report document. */
    suspend fun deleteReport(reportId: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_REPORTS)
                .document(reportId)
                .delete()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete report.", e)
        }
    }

    /** Stores a generated outreach message in the `messages` collection. */
    suspend fun saveMessage(reportId: String, tradesperson: String, text: String): Resource<Unit> {
        val userId = uid() ?: return Resource.Error("Not signed in.")
        return try {
            val docRef = firestore.collection(Constants.COLLECTION_MESSAGES).document()
            val message = RepairMessage(
                id = docRef.id,
                userId = userId,
                reportId = reportId,
                tradesperson = tradesperson,
                messageText = text,
                timestamp = System.currentTimeMillis()
            )
            docRef.set(message).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save message.", e)
        }
    }
}
