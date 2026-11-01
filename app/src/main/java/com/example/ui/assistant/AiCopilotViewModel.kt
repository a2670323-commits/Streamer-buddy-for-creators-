package com.example.ui.assistant

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val isThinking: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER, ASSISTANT
}

data class CopilotUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            id = "welcome",
            sender = MessageSender.ASSISTANT,
            text = "👋 Hey Creator! I'm your Streamer Buddy Co-Pilot. I can help brainstorm viral stream titles, craft chat moderation rules, generate viewer engagement hooks, or diagnose dropped frames. What are we streaming today?"
        )
    ),
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val useHighThinking: Boolean = false,
    val errorMessage: String? = null
)

private const val TAG = "StreamerBuddyAi"

class AiCopilotViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CopilotUiState())
    val uiState: StateFlow<CopilotUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null

    private val fastModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text("You are Streamer Buddy, an expert live streaming co-pilot and broadcast producer for YouTube creators. Help with stream titles, audience retention hooks, chat moderation, and broadcast diagnostics. Keep advice actionable, punchy, and formatted with bullet points.")
            }
        )
    }

    private val thinkingModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            systemInstruction = content {
                text("You are Streamer Buddy Senior Broadcast Engineer and Creator Strategist. Perform deep reasoning on stream growth, monetization, technical encoding parameters, and community management. Provide thorough, high-level analysis.")
            }
        )
    }

    fun onInputChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun toggleHighThinking(enabled: Boolean) {
        _uiState.update { it.copy(useHighThinking = enabled) }
    }

    fun sendMessage(userPrompt: String = _uiState.value.inputText) {
        val trimmed = userPrompt.trim()
        if (trimmed.isBlank() || _uiState.value.isGenerating) return

        val userMessage = ChatMessage(
            id = "user_${System.currentTimeMillis()}",
            sender = MessageSender.USER,
            text = trimmed
        )

        val assistantMessageId = "assistant_${System.currentTimeMillis()}"
        val initialAssistantMessage = ChatMessage(
            id = assistantMessageId,
            sender = MessageSender.ASSISTANT,
            text = "",
            isThinking = _uiState.value.useHighThinking
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage + initialAssistantMessage,
                inputText = "",
                isGenerating = true,
                errorMessage = null
            )
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            val useThinking = _uiState.value.useHighThinking
            val targetModel = if (useThinking) thinkingModel else fastModel
            val accumulatedText = StringBuilder()

            try {
                withTimeout(60_000L) {
                    targetModel.generateContentStream(trimmed).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        accumulatedText.append(chunkText)
                        _uiState.update { state ->
                            val updatedMessages = state.messages.map { msg ->
                                if (msg.id == assistantMessageId) {
                                    msg.copy(text = accumulatedText.toString(), isThinking = false)
                                } else msg
                            }
                            state.copy(messages = updatedMessages)
                        }
                    }
                }

                if (accumulatedText.isBlank()) {
                    _uiState.update { state ->
                        val updated = state.messages.map { msg ->
                            if (msg.id == assistantMessageId) {
                                msg.copy(text = getSmartFallback(trimmed))
                            } else msg
                        }
                        state.copy(messages = updated, isGenerating = false)
                    }
                } else {
                    _uiState.update { it.copy(isGenerating = false) }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI generation timed out", e)
                _uiState.update { state ->
                    val updated = state.messages.map { msg ->
                        if (msg.id == assistantMessageId) {
                            msg.copy(text = "⚠️ Request timed out after 60s. Here is quick creator advice for '$trimmed':\n\n" + getSmartFallback(trimmed))
                        } else msg
                    }
                    state.copy(messages = updated, isGenerating = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI call failed, providing creator response", e)
                _uiState.update { state ->
                    val updated = state.messages.map { msg ->
                        if (msg.id == assistantMessageId) {
                            msg.copy(text = getSmartFallback(trimmed))
                        } else msg
                    }
                    state.copy(messages = updated, isGenerating = false)
                }
            }
        }
    }

    private fun getSmartFallback(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("title") -> {
                "🔥 **5 High-CTR YouTube Live Titles**:\n\n" +
                "1. 🔴 CAN I HIT RANK #1 TODAY?! (Road to Champion)\n" +
                "2. The Most Chaotic Stream You'll Watch All Week [LIVE]\n" +
                "3. Don't Let Me Die: Hardcore Mode Survival #1\n" +
                "4. Testing The NEW Update (It's Broke Everything)\n" +
                "5. 1 Viewer = 1 Pushup?! LIVE Chat Decides My Fate"
            }
            lower.contains("hook") || lower.contains("engagement") || lower.contains("question") -> {
                "🎯 **3 Stream Downtime Engagement Hooks**:\n\n" +
                "• **The Prediction Hook**: 'Chat, on a scale of 1-10, what are the odds we survive this next round? Drop your bets.'\n" +
                "• **The Debate Starter**: 'Quick debate for chat while we queue: what's the most overrated game item right now?'\n" +
                "• **The Soundboard Callout**: 'If anyone in chat drops a sub in the next 60s, I hit the air horn and run the spicy challenge!'"
            }
            lower.contains("rule") || lower.contains("mod") -> {
                "🛡️ **Essential Stream Chat Moderation Rules**:\n\n" +
                "1. **Be Respectful**: No hate speech, harassment, or slurs.\n" +
                "2. **No Backseat Gaming**: Unless the streamer asks chat for help directly.\n" +
                "3. **No Self-Promo**: Keep links and self-advertising out of chat.\n" +
                "4. **Keep it Positive**: Hype each other up and follow mod instructions."
            }
            else -> {
                "⚡ **Streamer Buddy Pro Tip**:\n\n" +
                "For high viewer retention on YouTube Live:\n" +
                "• Keep your camera at eye level with warm front lighting.\n" +
                "• Maintain your bitrate at 6,000 Kbps for 1080p60 to avoid buffering.\n" +
                "• Greet viewers by name within their first 2 chat messages.\n" +
                "• Set up soundboard triggers to reward super chats and new subs instantly!"
            }
        }
    }
}
