package io.github.moyeranqianzhi.deepseek.api

import kotlinx.serialization.Serializable

sealed class ClientMessage {
    data class Success(
        val id: String,
        val `object`: Object,
        val created: Int,
        val model: String,
        val choices: List<Choice> = listOf(),
        val usage: Usage = Usage(),
        val systemFingerprint: String
    ) : ClientMessage()

    data object Finish : ClientMessage()

    data object Error : ClientMessage()

    enum class Object {
        ChatCompletion,
        ChatCompletionChunk,
    }

    data class Choice(
        val index: Int,
        val message: ChatMessage,
        val logprobs: Logprobs? = null,
        val finishReason: FinishReason? = null
    )

    data class ChatMessage(
        val role: ChatRole,
        val content: String?,
        val reasoningContent: String?,
        val toolCalls: List<ToolCall> = listOf()
    )

    enum class ChatRole {
        System, User, Assistant,
    }

    data class ToolCall(
        val id: String,
        val type: ToolType,
        val function: ToolFunction,
    )

    enum class ToolType {
        Function
    }

    data class ToolFunction(
        val name: String, val arguments: String
    )

    data class Logprobs(
        val content: List<LogprobsContent>
    )

    data class LogprobsContent(
        val token: String,
        val logprob: Double,
        val bytes: List<Int>? = null,
        val topLogprobs: List<TopLogprobsContent>? = null
    )

    data class TopLogprobsContent(
        val token: String,
        val logprob: Double,
        val bytes: List<Int>? = null
    )

    @Serializable
    enum class FinishReason {
        Stop, Length, ContentFilter, ToolCalls, InsufficientSystemResource
    }

    @Serializable
    data class Usage(
        val promptTokens: Int? = null,
        val completionTokens: Int? = null,
        val totalTokens: Int? = null,
        val promptTokensDetails: PromptTokensDetails = PromptTokensDetails(),
        val completionTokensDetails: CompletionTokensDetails? = CompletionTokensDetails(),
        val promptCacheHitTokens: Int? = null,
        val promptCacheMissTokens: Int? = null
    )

    @Serializable
    data class PromptTokensDetails(
        val cachedTokens: Int? = null
    )

    @Serializable
    data class CompletionTokensDetails(
        val reasoningTokens: Int? = null
    )
}


