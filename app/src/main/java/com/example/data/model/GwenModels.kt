package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class GwenExpression(val label: String, val accentTag: String) {
    HAPPY("Happy", "Warm Cyan"),
    THINKING("Thinking", "Violet Pulse"),
    LISTENING("Listening", "Electric Blue"),
    SPEAKING("Speaking", "Cyan Wave"),
    SERIOUS("Serious", "Deep Indigo"),
    ANGRY("Angry", "Crimson Flare"),
    LAUGHING("Laughing", "Radiant Gold"),
    SURPRISED("Surprised", "Neon Aqua"),
    CONFUSED("Confused", "Amber Violet"),
    WORRIED("Worried", "Soft Magenta")
}

enum class AvatarMode(val label: String, val description: String) {
    UPPER_BODY("Upper Body", "Focused expressive bust portrait with live HUD ring"),
    FULL_BODY("Full Body", "Complete animated Gwen character stance with aura"),
    HOLOGRAM("3D Hologram", "Translucent cyan wireframe projection with emitter base")
}

enum class AiChatMode(val key: String, val label: String, val systemInstruction: String) {
    GENERAL(
        "general",
        "General",
        "You are Gwen AI (Maximum Edition), an intelligent, confident, warm, and resourceful personal AI assistant inspired by a brilliant tech hero. Address the user respectfully as Boss when natural. Keep responses crisp, accurate, and helpful."
    ),
    CODING(
        "coding",
        "Coding",
        "You are Gwen AI in Coding Architect mode. Provide clean, production-grade code, explain edge cases clearly, and diagnose bugs systematically."
    ),
    RESEARCH(
        "research",
        "Research",
        "You are Gwen AI in Research Analyst mode. Break down complex topics with structured facts, pros/cons, and clear actionable takeaways."
    ),
    STORY(
        "story",
        "Story & Script",
        "You are Gwen AI in Creative Writer mode. Craft vivid stories, scripts, dialogues, and narrative concepts with rich pacing."
    ),
    TRANSLATION(
        "translation",
        "Translate",
        "You are Gwen AI in Multilingual Translator mode, fluent in English, Hindi, Hinglish, and major world languages. Provide natural translations plus pronunciation hints."
    ),
    GAMING(
        "gaming",
        "Gaming",
        "You are Gwen AI in Tactical Gaming Assistant mode. Give sharp strategy tips, HUD/sensitivity advice, loadout analysis, and performance optimization notes."
    )
}

enum class TaskStage(val key: String, val label: String) {
    UNDERSTANDING("understanding", "Understanding Request"),
    ANALYZING("analyzing", "Analyzing Intent"),
    PLANNING("planning", "Planning Steps"),
    CHECKING_PERMISSIONS("checking_permissions", "Checking Permissions"),
    EXECUTING("executing", "Executing Action"),
    VERIFYING("verifying", "Verifying Outcome"),
    COMPLETED("completed", "Completed & Verified"),
    FAILED("failed", "Failed")
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Boss",
    val preferredLanguage: String = "en-IN",
    val preferredModel: String = "gemini-3.8-flash",
    val memoryEnabled: Boolean = true,
    val voiceEnrolled: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MemoryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "note",
    val isApproved: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "New Conversation",
    val mode: String = "general",
    val lastMessagePreview: String = "",
    val messageCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val text: String = "",
    val expression: String = GwenExpression.HAPPY.label,
    val hasImage: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AssistantTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val command: String = "",
    val category: String = "ai",
    val stage: String = TaskStage.UNDERSTANDING.key,
    val planSummary: String = "",
    val verificationResult: String = "Pending",
    val requiresInternet: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
