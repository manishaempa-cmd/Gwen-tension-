package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.TaskStage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GwenRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createMemory_authenticatedOwner_succeedsAndEmitsRealtime() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = GwenFirestoreRepository(firestore, auth)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createMemory(
                title = "Voice Preference",
                content = "Use Hinglish for casual chats.",
                category = "preference"
            )
        }
        assertTrue(createResult.isSuccess)
        val memId = createResult.getOrThrow()
        assertTrue(memId.isNotEmpty())

        val memories = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMemories(uid).first { list -> list.any { it.id == memId } }
        }
        assertTrue(memories.any { it.id == memId && it.title == "Voice Preference" })
    }

    @Test
    fun observeMemories_crossUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = GwenFirestoreRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createMemory("Private Note", "Secret context", "note").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = GwenFirestoreRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeMemories(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull("Expected PERMISSION_DENIED on cross-user access", caught)
        val firestoreEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeMemories_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = GwenFirestoreRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeMemories("some_user_123").first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull("Expected PERMISSION_DENIED when signed out", caught)
        val firestoreEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun createAndCompleteTask_terminalStateLocksFurtherUpdate() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = GwenFirestoreRepository(firestore, auth)

        val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(
                command = "Open YouTube",
                category = "media",
                planSummary = "Launch YouTube search intent",
                requiresInternet = true
            ).getOrThrow()
        }

        val completeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateTaskStage(
                taskId = taskId,
                stage = TaskStage.COMPLETED,
                planSummary = "Launched YouTube",
                verificationResult = "Verified intent activity resolved"
            )
        }
        assertTrue(completeResult.isSuccess)

        val postCompleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateTaskStage(
                taskId = taskId,
                stage = TaskStage.EXECUTING,
                planSummary = "Tampered",
                verificationResult = "Should fail"
            )
        }
        assertTrue(postCompleteResult.isFailure)
        val ex = postCompleteResult.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
    }

    @Test
    fun appendMessage_createsParentAndMessageAtomically() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = GwenFirestoreRepository(firestore, auth)

        val convId = "conv_test_${System.currentTimeMillis()}"
        val msgResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.appendMessage(
                conversationId = convId,
                role = "user",
                text = "Hello Gwen, check system telemetry.",
                expression = "Happy",
                hasImage = false,
                mode = "general"
            )
        }
        assertTrue(msgResult.isSuccess)

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMessages(uid, convId).first { it.isNotEmpty() }
        }
        assertEquals(1, messages.size)
        assertEquals("Hello Gwen, check system telemetry.", messages.first().text)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_gwen@test.com"
        const val BOB_EMAIL = "bob_gwen@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
