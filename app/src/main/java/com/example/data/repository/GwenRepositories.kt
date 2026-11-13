package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AssistantTask
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.MemoryItem
import com.example.data.model.TaskStage
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GwenFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- USER PROFILE ---

    suspend fun ensureUserProfile(
        displayName: String,
        preferredLanguage: String = "en-IN",
        preferredModel: String = "gemini-3.8-flash"
    ): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val snap = try {
            docRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            throw e
        }
        if (snap.exists()) {
            snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(id = uid, userId = uid, displayName = displayName)
        } else {
            val safeName = displayName.ifBlank { "Boss" }.take(100)
            val payload = mapOf(
                "userId" to uid,
                "displayName" to safeName,
                "preferredLanguage" to preferredLanguage,
                "preferredModel" to preferredModel,
                "memoryEnabled" to true,
                "voiceEnrolled" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.set(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
                throw e
            }
            UserProfile(
                id = uid,
                userId = uid,
                displayName = safeName,
                preferredLanguage = preferredLanguage,
                preferredModel = preferredModel,
                memoryEnabled = true,
                voiceEnrolled = false
            )
        }
    }

    suspend fun updateUserProfileSettings(
        preferredLanguage: String? = null,
        preferredModel: String? = null,
        memoryEnabled: Boolean? = null,
        voiceEnrolled: Boolean? = null
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        preferredLanguage?.let { updates["preferredLanguage"] = it }
        preferredModel?.let { updates["preferredModel"] = it }
        memoryEnabled?.let { updates["memoryEnabled"] = it }
        voiceEnrolled?.let { updates["voiceEnrolled"] = it }

        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    // --- PERSONAL MEMORY ---

    fun observeMemories(userId: String): Flow<List<MemoryItem>> = flow {
        val path = "users/$userId/memories"
        emitAll(
            db.collection("users").document(userId).collection("memories")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MemoryItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun createMemory(
        title: String,
        content: String,
        category: String = "note",
        isApproved: Boolean = true
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val memoryId = "mem_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("memories").document(memoryId)
        val payload = mapOf(
            "userId" to uid,
            "title" to title.trim().take(120).ifBlank { "Memory" },
            "content" to content.trim().take(4000).ifBlank { "Empty note" },
            "category" to category.takeIf { it in setOf("preference", "context", "note", "gaming") }.orEmpty().ifBlank { "note" },
            "isApproved" to isApproved,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            memoryId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateMemory(
        memoryId: String,
        title: String,
        content: String,
        category: String,
        isApproved: Boolean = true
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(memoryId.isNotBlank()) { "Memory ID cannot be blank" }
        val docRef = db.collection("users").document(uid).collection("memories").document(memoryId)
        val payload = mapOf(
            "title" to title.trim().take(120).ifBlank { "Memory" },
            "content" to content.trim().take(4000).ifBlank { "Empty note" },
            "category" to category.takeIf { it in setOf("preference", "context", "note", "gaming") }.orEmpty().ifBlank { "note" },
            "isApproved" to isApproved,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteMemory(memoryId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("memories").document(memoryId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    suspend fun clearAllMemories(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("memories")
        try {
            val docs = colRef.get().await().documents
            val batch = db.batch()
            docs.forEach { batch.delete(it.reference) }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colRef.path, auth)
            throw e
        }
    }

    // --- CONVERSATIONS & MESSAGES ---

    fun observeConversations(userId: String): Flow<List<Conversation>> = flow {
        val path = "users/$userId/conversations"
        emitAll(
            db.collection("users").document(userId).collection("conversations")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Conversation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeMessages(userId: String, conversationId: String): Flow<List<ChatMessage>> = flow {
        val path = "users/$userId/conversations/$conversationId/messages"
        emitAll(
            db.collection("users").document(userId)
                .collection("conversations").document(conversationId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun createConversation(
        title: String,
        mode: String = "general"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val convId = "conv_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("conversations").document(convId)
        val payload = mapOf(
            "userId" to uid,
            "title" to title.trim().take(120).ifBlank { "New Conversation" },
            "mode" to mode,
            "lastMessagePreview" to "",
            "messageCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            convId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun appendMessage(
        conversationId: String,
        role: String,
        text: String,
        expression: String,
        hasImage: Boolean = false,
        mode: String = "general"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val convRef = db.collection("users").document(uid)
            .collection("conversations").document(conversationId)
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val msgRef = convRef.collection("messages").document(msgId)

        val safeText = text.trim().take(20000).ifBlank { "..." }
        val preview = safeText.take(160)

        try {
            val convSnap = convRef.get().await()
            val batch = db.batch()
            if (!convSnap.exists()) {
                batch.set(
                    convRef,
                    mapOf(
                        "userId" to uid,
                        "title" to preview.take(48).ifBlank { "Gwen Session" },
                        "mode" to mode,
                        "lastMessagePreview" to preview,
                        "messageCount" to 1L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    convRef,
                    mapOf(
                        "mode" to mode,
                        "lastMessagePreview" to preview,
                        "messageCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                msgRef,
                mapOf(
                    "userId" to uid,
                    "role" to role,
                    "text" to safeText,
                    "expression" to expression.take(32),
                    "hasImage" to hasImage,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            msgId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, msgRef.path, auth)
            throw e
        }
    }

    // --- ASSISTANT TASKS ---

    fun observeTasks(userId: String): Flow<List<AssistantTask>> = flow {
        val path = "users/$userId/tasks"
        emitAll(
            db.collection("users").document(userId).collection("tasks")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AssistantTask::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun createTask(
        command: String,
        category: String,
        planSummary: String,
        requiresInternet: Boolean
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val taskId = "task_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
        val payload = mapOf(
            "userId" to uid,
            "command" to command.trim().take(500).ifBlank { "Task" },
            "category" to category.takeIf { it in setOf("phone", "ai", "media", "screen", "memory") }.orEmpty().ifBlank { "ai" },
            "stage" to TaskStage.UNDERSTANDING.key,
            "planSummary" to planSummary.take(2000),
            "verificationResult" to "Pending verification",
            "requiresInternet" to requiresInternet,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            taskId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateTaskStage(
        taskId: String,
        stage: TaskStage,
        planSummary: String,
        verificationResult: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
        val payload = mapOf(
            "stage" to stage.key,
            "planSummary" to planSummary.take(2000),
            "verificationResult" to verificationResult.take(1000),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }
}
