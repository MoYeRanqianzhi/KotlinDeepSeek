package io.github.moyeranqianzhi.deepseek.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
internal sealed class DeepSeekResponse {
    @Serializable
    internal sealed class Delta : DeepSeekResponse() {
        @Serializable
        internal data class Reasoner(
            @SerialName("id")
            val id: String,
            @SerialName("object")
            val `object`: Object,
            @SerialName("created")
            val created: Int,
            @SerialName("model")
            val model: String,
            @SerialName("choices")
            val choices: List<ReasonerChoice> = listOf(),
            @SerialName("usage")
            val usage: ReasonerUsage = ReasonerUsage(),
            @SerialName("system_fingerprint")
            val systemFingerprint: String
        ) : Delta()

        @Serializable
        internal data class Chat(
            @SerialName("id")
            val id: String,
            @SerialName("object")
            val `object`: Object,
            @SerialName("created")
            val created: Int,
            @SerialName("model")
            val model: String,
            @SerialName("choices")
            val choices: List<ChatChoice> = listOf(),
            @SerialName("usage")
            val usage: ChatUsage = ChatUsage(),
            @SerialName("system_fingerprint")
            val systemFingerprint: String
        ) : Delta()

        @Serializable
        internal data class ReasonerChoice(
            @SerialName("index")
            val index: Int,
            @SerialName("delta")
            val message: ReasonerMessage,
            @SerialName("logprobs")
            val logprobs: Logprobs? = null,
            @SerialName("finish_reason")
            val finishReason: FinishReason? = null
        )

        @Serializable
        internal data class ChatChoice(
            @SerialName("index")
            val index: Int,
            @SerialName("delta")
            val message: ChatMessage,
            @SerialName("logprobs")
            val logprobs: Logprobs? = null,
            @SerialName("finish_reason")
            val finishReason: FinishReason? = null
        )
    }

    @Serializable
    internal sealed class Common : DeepSeekResponse() {
        @Serializable
        data class Reasoner(
            @SerialName("id")
            val id: String,
            @SerialName("object")
            val `object`: Object,
            @SerialName("created")
            val created: Int,
            @SerialName("model")
            val model: String,
            @SerialName("choices")
            val choices: List<ReasonerChoice> = listOf(),
            @SerialName("usage")
            val usage: ReasonerUsage = ReasonerUsage(),
            @SerialName("system_fingerprint")
            val systemFingerprint: String
        ) : Common()

        @Serializable
        internal data class Chat(
            @SerialName("id")
            val id: String,
            @SerialName("object")
            val `object`: Object,
            @SerialName("created")
            val created: Int,
            @SerialName("model")
            val model: String,
            @SerialName("choices")
            val choices: List<ChatChoice> = listOf(),
            @SerialName("usage")
            val usage: ChatUsage = ChatUsage(),
            @SerialName("system_fingerprint")
            val systemFingerprint: String
        ) : Common()

        @Serializable
        internal data class ReasonerChoice(
            @SerialName("index")
            val index: Int,
            @SerialName("message")
            val message: ReasonerMessage,
            @SerialName("logprobs")
            val logprobs: Logprobs? = null,
            @SerialName("finish_reason")
            val finishReason: FinishReason? = null
        )

        @Serializable
        internal data class ChatChoice(
            @SerialName("index")
            val index: Int,
            @SerialName("message")
            val message: ChatMessage,
            @SerialName("logprobs")
            val logprobs: Logprobs? = null,
            @SerialName("finish_reason")
            val finishReason: FinishReason? = null
        )
    }

    internal enum class Object {
        @SerialName("chat.completion")
        ChatCompletion,

        @SerialName("chat.completion.chunk")
        ChatCompletionChunk,
    }

    @Serializable
    internal data class ReasonerMessage(
        @SerialName("role")
        val role: Role = Role.Assistant,
        @SerialName("content")
        val content: String?,
        @SerialName("reasoning_content")
        val reasoningContent: String?,
        @SerialName("tool_calls")
        val toolCalls: List<ToolCall> = listOf()
    )

    @Serializable
    internal data class ChatMessage(
        @SerialName("role")
        val role: Role = Role.Assistant,
        @SerialName("content")
        val content: String?,
        @SerialName("tool_calls")
        val toolCalls: List<ToolCall> = listOf()
    )

    internal enum class Role {
        @SerialName("assistant")
        Assistant
    }

    @Serializable
    internal data class ToolCall(
        @SerialName("id")
        val id: String,
        @SerialName("type")
        val type: ToolType,
        @SerialName("function")
        val function: ToolFunction,
    )

    @Serializable
    internal enum class ToolType {
        @SerialName("function")
        Function
    }

    @Serializable
    internal data class ToolFunction(
        @SerialName("name")
        val name: String,
        @SerialName("arguments")
        val arguments: String
    )

    @Serializable
    internal data class Logprobs(
        @SerialName("content")
        val content: List<LogprobsContent>
    )

    @Serializable
    internal data class LogprobsContent(
        @SerialName("token")
        val token: String,
        @SerialName("logprob")
        val logprob: Double,
        @SerialName("bytes")
        val bytes: List<Int>? = null,
        @SerialName("top_logprobs")
        val topLogprobs: List<TopLogprobsContent>? = null
    )

    @Serializable
    internal data class TopLogprobsContent(
        @SerialName("token")
        val token: String,
        @SerialName("logprob")
        val logprob: Double,
        @SerialName("bytes")
        val bytes: List<Int>? = null
    )


    @Serializable
    internal enum class FinishReason {
        @SerialName("stop")
        Stop,

        @SerialName("length")
        Length,

        @SerialName("content_filter")
        ContentFilter,

        @SerialName("tool_calls")
        ToolCalls,

        @SerialName("insufficient_system_resource")
        InsufficientSystemResource
    }

    @Serializable
    internal data class ReasonerUsage(
        @SerialName("prompt_tokens")
        val promptTokens: Int? = null,
        @SerialName("completion_tokens")
        val completionTokens: Int? = null,
        @SerialName("total_tokens")
        val totalTokens: Int? = null,
        @SerialName("prompt_tokens_details")
        val promptTokensDetails: PromptTokensDetails = PromptTokensDetails(),
        @SerialName("completion_tokens_details")
        val completionTokensDetails: CompletionTokensDetails = CompletionTokensDetails(),
        @SerialName("prompt_cache_hit_tokens")
        val promptCacheHitTokens: Int? = null,
        @SerialName("prompt_cache_miss_tokens")
        val promptCacheMissTokens: Int? = null
    )

    @Serializable
    internal data class ChatUsage(
        @SerialName("prompt_tokens")
        val promptTokens: Int? = null,
        @SerialName("completion_tokens")
        val completionTokens: Int? = null,
        @SerialName("total_tokens")
        val totalTokens: Int? = null,
        @SerialName("prompt_tokens_details")
        val promptTokensDetails: PromptTokensDetails = PromptTokensDetails(),
        @SerialName("prompt_cache_hit_tokens")
        val promptCacheHitTokens: Int? = null,
        @SerialName("prompt_cache_miss_tokens")
        val promptCacheMissTokens: Int? = null
    )

    @Serializable
    internal data class PromptTokensDetails(
        @SerialName("cached_tokens")
        val cachedTokens: Int? = null
    )

    @Serializable
    internal data class CompletionTokensDetails(
        @SerialName("reasoning_tokens")
        val reasoningTokens: Int? = null
    )
}
