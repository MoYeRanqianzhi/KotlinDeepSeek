package io.github.moyeranqianzhi.deepseek.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DeepSeekRequest(
    @SerialName("model")
    val model: Model,
    @SerialName("messages")
    val messages: Messages,
    @SerialName("stream")
    val stream: Boolean = false,
//    @SerialName("stream_options")
//    val streamOptions: StreamOptions = StreamOptions(),
//    @SerialName("stop")
//    val stop: String? = null,
//    @SerialName("temperature")
//    val temperature: Int = 1,
//    @SerialName("top_p")
//    val topP: Int = 1,
//    @SerialName("frequency_penalty")
//    val frequencyPenalty: Int = 0,
//    @SerialName("max_tokens")
//    val maxTokens: Int = 4096,
//    @SerialName("presence_penalty")
//    val presencePenalty: Int = 0,
//    @SerialName("response_format")
//    val responseFormat: ResponseFormat = ResponseFormat(),
//    @SerialName("tools")
//    val tools: Tool? = null,
//    @SerialName("tool_choice")
//    val toolChoice: String? = null,
//    @SerialName("logprobs")
//    val logprobs: Boolean? = null,
//    @SerialName("top_logprobs")
//    val topLogprobs: Int? = null,
)


//@Serializable
//data class ResponseFormat(
//    @SerialName("type")
//    val type: String = "text",
//)
//
//@Serializable
//data class StreamOptions(
//    @SerialName("include_usage")
//    val includeUsage: Boolean? = null,
//)
//
//@Serializable
//data class Tool(
//    val type: String = "function",
//    val function: ToolFunction,
//)
//
//@Serializable
//data class ToolFunction(
//    val name: String = "",
//    val description: String = "",
//    val parameters: String = "",
//)