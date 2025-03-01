package io.github.moyeranqianzhi.deepseek.api

import io.github.moyeranqianzhi.deepseek.web.httpClient
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class Client(private val config: ClientConfig) {
    constructor(block: ClientConfig.() -> Unit = {}) : this(ClientConfig().apply(block))
    constructor(
        apiKey: ApiKey,
        baseURL: BaseURL = BaseURL.DeepSeek,
        httpConfig: HttpClientConfig<*>.() -> Unit
    ) : this(
        block = {
            this.apiKey = apiKey
            this.baseurl = baseURL
            http {
                httpConfig()
            }
        }
    )

    private val client: HttpClient = httpClient(config.httpConfig)
    private val key get() = config.apiKey.key

    suspend fun chatCompletion(
        block: ChatCompletionConfig.() -> Unit
    ): ClientMessage = chatCompletion(
        config = ChatCompletionConfig().apply(block)
    )

    suspend fun chatCompletion(
        config: ChatCompletionConfig
    ): ClientMessage = requestChatCompletion(
        reasoning = when (config.settings.model.type) {
            ModelType.Chat -> false
            ModelType.Reasoner -> true
            ModelType.Auto -> when (config.model) {
                is Model.Chat -> false
                is Model.Reasoner -> true
                is Model.Custom -> throw DisautoableModel(config.model as Model.Custom)
            }
        },
        config = config
    )

    fun chatCompletionChunk(
        block: ChatCompletionConfig.() -> Unit
    ): Flow<ClientMessage> = chatCompletionChunk(
        config = ChatCompletionConfig().apply(block)
    )

    fun chatCompletionChunk(
        config: ChatCompletionConfig
    ): Flow<ClientMessage> = requestChatCompletionChunk(
        reasoning = when (config.settings.model.type) {
            ModelType.Chat -> false
            ModelType.Reasoner -> true
            ModelType.Auto -> when (config.model) {
                is Model.Chat -> false
                is Model.Reasoner -> true
                is Model.Custom -> throw DisautoableModel(config.model as Model.Custom)
            }
        },
        config = config
    )

    private suspend fun requestChatCompletion(
        config: ChatCompletionConfig,
        reasoning: Boolean
    ): ClientMessage =
        try {
            client.post(this.config.baseurl.url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $key")
                setBody(
                    Json.encodeToString(
                        DeepSeekRequest(
                            model = config.model,
                            messages = config.messages,
                            stream = config.stream
                        )
                    )
                )
            }.let { response ->
                if (response.status == HttpStatusCode.OK) {
                    if (reasoning) parseReasonerChatCompletion(response.bodyAsText())
                    else parseChatChatCompletion(response.bodyAsText())
                } else ClientMessage.Error
            }
        } catch (e: Exception) {
            ClientMessage.Error
        }

    private inline fun parseReasonerChatCompletion(
        data: String
    ) = Json.decodeFromString<DeepSeekResponse.Common.Reasoner>(data).let { response ->
        ClientMessage.Success(
            id = response.id,
            `object` = when (response.`object`) {
                DeepSeekResponse.Object.ChatCompletion -> ClientMessage.Object.ChatCompletion
                DeepSeekResponse.Object.ChatCompletionChunk -> ClientMessage.Object.ChatCompletionChunk
            },
            created = response.created,
            model = response.model,
            choices = response.choices.map {
                ClientMessage.Choice(
                    index = it.index,
                    message = ClientMessage.ChatMessage(
                        role = when (it.message.role) {
                            DeepSeekResponse.Role.Assistant -> ClientMessage.ChatRole.Assistant
                        },
                        content = it.message.content,
                        reasoningContent = it.message.reasoningContent,
                        toolCalls = it.message.toolCalls.map { toolCall ->
                            ClientMessage.ToolCall(
                                id = toolCall.id,
                                type = when (toolCall.type) {
                                    DeepSeekResponse.ToolType.Function -> ClientMessage.ToolType.Function
                                },
                                function = ClientMessage.ToolFunction(
                                    name = toolCall.function.name,
                                    arguments = toolCall.function.arguments
                                )
                            )
                        }
                    ),
                    logprobs = it.logprobs?.let { logprobs ->
                        ClientMessage.Logprobs(
                            content = logprobs.content.map { logprobsContent ->
                                ClientMessage.LogprobsContent(
                                    token = logprobsContent.token,
                                    logprob = logprobsContent.logprob,
                                    bytes = logprobsContent.bytes,
                                    topLogprobs = logprobsContent.topLogprobs?.map { topLogprobsContent ->
                                        ClientMessage.TopLogprobsContent(
                                            token = topLogprobsContent.token,
                                            logprob = topLogprobsContent.logprob,
                                        )
                                    }
                                )
                            }
                        )
                    },
                    finishReason = when (it.finishReason) {
                        DeepSeekResponse.FinishReason.Stop -> ClientMessage.FinishReason.Stop
                        DeepSeekResponse.FinishReason.Length -> ClientMessage.FinishReason.Length
                        DeepSeekResponse.FinishReason.ContentFilter -> ClientMessage.FinishReason.ContentFilter
                        DeepSeekResponse.FinishReason.ToolCalls -> ClientMessage.FinishReason.ToolCalls
                        DeepSeekResponse.FinishReason.InsufficientSystemResource -> ClientMessage.FinishReason.InsufficientSystemResource
                        null -> null
                    }
                )
            },
            usage = ClientMessage.Usage(
                promptTokens = response.usage.promptTokens,
                completionTokens = response.usage.completionTokens,
                totalTokens = response.usage.totalTokens,
                promptTokensDetails = ClientMessage.PromptTokensDetails(
                    cachedTokens = response.usage.promptTokensDetails.cachedTokens
                ),
                completionTokensDetails = response.usage.completionTokensDetails.let {
                    ClientMessage.CompletionTokensDetails(
                        reasoningTokens = it.reasoningTokens
                    )
                },
                promptCacheHitTokens = response.usage.promptCacheHitTokens,
                promptCacheMissTokens = response.usage.promptCacheMissTokens
            ),
            systemFingerprint = response.systemFingerprint
        )
    }

    private inline fun parseChatChatCompletion(
        data: String
    ) = Json.decodeFromString<DeepSeekResponse.Common.Chat>(data).let { response ->
        ClientMessage.Success(
            id = response.id,
            `object` = when (response.`object`) {
                DeepSeekResponse.Object.ChatCompletion -> ClientMessage.Object.ChatCompletion
                DeepSeekResponse.Object.ChatCompletionChunk -> ClientMessage.Object.ChatCompletionChunk
            },
            created = response.created,
            model = response.model,
            choices = response.choices.map {
                ClientMessage.Choice(
                    index = it.index,
                    message = ClientMessage.ChatMessage(
                        role = when (it.message.role) {
                            DeepSeekResponse.Role.Assistant -> ClientMessage.ChatRole.Assistant
                        },
                        content = it.message.content,
                        reasoningContent = null,
                        toolCalls = it.message.toolCalls.map { toolCall ->
                            ClientMessage.ToolCall(
                                id = toolCall.id,
                                type = when (toolCall.type) {
                                    DeepSeekResponse.ToolType.Function -> ClientMessage.ToolType.Function
                                },
                                function = ClientMessage.ToolFunction(
                                    name = toolCall.function.name,
                                    arguments = toolCall.function.arguments
                                )
                            )
                        }
                    ),
                    logprobs = it.logprobs?.let { logprobs ->
                        ClientMessage.Logprobs(
                            content = logprobs.content.map { logprobsContent ->
                                ClientMessage.LogprobsContent(
                                    token = logprobsContent.token,
                                    logprob = logprobsContent.logprob,
                                    bytes = logprobsContent.bytes,
                                    topLogprobs = logprobsContent.topLogprobs?.map { topLogprobsContent ->
                                        ClientMessage.TopLogprobsContent(
                                            token = topLogprobsContent.token,
                                            logprob = topLogprobsContent.logprob,
                                        )
                                    }
                                )
                            }
                        )
                    },
                    finishReason = when (it.finishReason) {
                        DeepSeekResponse.FinishReason.Stop -> ClientMessage.FinishReason.Stop
                        DeepSeekResponse.FinishReason.Length -> ClientMessage.FinishReason.Length
                        DeepSeekResponse.FinishReason.ContentFilter -> ClientMessage.FinishReason.ContentFilter
                        DeepSeekResponse.FinishReason.ToolCalls -> ClientMessage.FinishReason.ToolCalls
                        DeepSeekResponse.FinishReason.InsufficientSystemResource -> ClientMessage.FinishReason.InsufficientSystemResource
                        null -> null
                    }
                )
            },
            usage = ClientMessage.Usage(
                promptTokens = response.usage.promptTokens,
                completionTokens = response.usage.completionTokens,
                totalTokens = response.usage.totalTokens,
                promptTokensDetails = ClientMessage.PromptTokensDetails(
                    cachedTokens = response.usage.promptTokensDetails.cachedTokens
                ),
                completionTokensDetails = null,
                promptCacheHitTokens = response.usage.promptCacheHitTokens,
                promptCacheMissTokens = response.usage.promptCacheMissTokens
            ),
            systemFingerprint = response.systemFingerprint
        )
    }

    private fun requestChatCompletionChunk(
        config: ChatCompletionConfig,
        reasoning: Boolean
    ) = flow {
        config.stream = true
        try {
            client.preparePost(this@Client.config.baseurl.url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $key")
                setBody(
                    Json.encodeToString(
                        DeepSeekRequest(
                            model = config.model,
                            messages = config.messages,
                            stream = config.stream
                        )
                    )
                )
            }.execute { response ->
                if (response.status == HttpStatusCode.OK) {
                    response.bodyAsChannel().let { channel ->
                        while (!channel.isClosedForRead) {
                            channel.readUTF8Line()?.let { line ->
                                if (line.startsWith("data: ")) {
                                    line.removePrefix("data: ").trim().let {
                                        if (it != "[DONE]") {
                                            if (reasoning) emit(parseReasonerChatCompletionChunk(it))
                                            else emit(parseChatChatCompletionChunk(it))
                                        } else emit(ClientMessage.Finish)
                                    }
                                }
                            }
                        }
                    }
                } else emit(ClientMessage.Error)
            }
        } catch (e: Exception) {
            emit(ClientMessage.Error)
        }
    }

    private inline fun parseReasonerChatCompletionChunk(
        data: String
    ) = Json.decodeFromString<DeepSeekResponse.Delta.Reasoner>(data).let { response ->
        ClientMessage.Success(
            id = response.id,
            `object` = when (response.`object`) {
                DeepSeekResponse.Object.ChatCompletion -> ClientMessage.Object.ChatCompletion
                DeepSeekResponse.Object.ChatCompletionChunk -> ClientMessage.Object.ChatCompletionChunk
            },
            created = response.created,
            model = response.model,
            choices = response.choices.map {
                ClientMessage.Choice(
                    index = it.index,
                    message = ClientMessage.ChatMessage(
                        role = when (it.message.role) {
                            DeepSeekResponse.Role.Assistant -> ClientMessage.ChatRole.Assistant
                        },
                        content = it.message.content,
                        reasoningContent = it.message.reasoningContent,
                        toolCalls = it.message.toolCalls.map { toolCall ->
                            ClientMessage.ToolCall(
                                id = toolCall.id,
                                type = when (toolCall.type) {
                                    DeepSeekResponse.ToolType.Function -> ClientMessage.ToolType.Function
                                },
                                function = ClientMessage.ToolFunction(
                                    name = toolCall.function.name,
                                    arguments = toolCall.function.arguments
                                )
                            )
                        }
                    ),
                    logprobs = it.logprobs?.let { logprobs ->
                        ClientMessage.Logprobs(
                            content = logprobs.content.map { logprobsContent ->
                                ClientMessage.LogprobsContent(
                                    token = logprobsContent.token,
                                    logprob = logprobsContent.logprob,
                                    bytes = logprobsContent.bytes,
                                    topLogprobs = logprobsContent.topLogprobs?.map { topLogprobsContent ->
                                        ClientMessage.TopLogprobsContent(
                                            token = topLogprobsContent.token,
                                            logprob = topLogprobsContent.logprob,
                                        )
                                    }
                                )
                            }
                        )
                    },
                    finishReason = when (it.finishReason) {
                        DeepSeekResponse.FinishReason.Stop -> ClientMessage.FinishReason.Stop
                        DeepSeekResponse.FinishReason.Length -> ClientMessage.FinishReason.Length
                        DeepSeekResponse.FinishReason.ContentFilter -> ClientMessage.FinishReason.ContentFilter
                        DeepSeekResponse.FinishReason.ToolCalls -> ClientMessage.FinishReason.ToolCalls
                        DeepSeekResponse.FinishReason.InsufficientSystemResource -> ClientMessage.FinishReason.InsufficientSystemResource
                        null -> null
                    }
                )
            },
            usage = ClientMessage.Usage(
                promptTokens = response.usage.promptTokens,
                completionTokens = response.usage.completionTokens,
                totalTokens = response.usage.totalTokens,
                promptTokensDetails = ClientMessage.PromptTokensDetails(
                    cachedTokens = response.usage.promptTokensDetails.cachedTokens
                ),
                completionTokensDetails = response.usage.completionTokensDetails.let {
                    ClientMessage.CompletionTokensDetails(
                        reasoningTokens = it.reasoningTokens
                    )
                },
                promptCacheHitTokens = response.usage.promptCacheHitTokens,
                promptCacheMissTokens = response.usage.promptCacheMissTokens
            ),
            systemFingerprint = response.systemFingerprint
        )
    }

    private inline fun parseChatChatCompletionChunk(
        data: String
    ) = Json.decodeFromString<DeepSeekResponse.Delta.Chat>(data).let { response ->
        ClientMessage.Success(
            id = response.id,
            `object` = when (response.`object`) {
                DeepSeekResponse.Object.ChatCompletion -> ClientMessage.Object.ChatCompletion
                DeepSeekResponse.Object.ChatCompletionChunk -> ClientMessage.Object.ChatCompletionChunk
            },
            created = response.created,
            model = response.model,
            choices = response.choices.map {
                ClientMessage.Choice(
                    index = it.index,
                    message = ClientMessage.ChatMessage(
                        role = when (it.message.role) {
                            DeepSeekResponse.Role.Assistant -> ClientMessage.ChatRole.Assistant
                        },
                        content = it.message.content,
                        reasoningContent = null,
                        toolCalls = it.message.toolCalls.map { toolCall ->
                            ClientMessage.ToolCall(
                                id = toolCall.id,
                                type = when (toolCall.type) {
                                    DeepSeekResponse.ToolType.Function -> ClientMessage.ToolType.Function
                                },
                                function = ClientMessage.ToolFunction(
                                    name = toolCall.function.name,
                                    arguments = toolCall.function.arguments
                                )
                            )
                        }
                    ),
                    logprobs = it.logprobs?.let { logprobs ->
                        ClientMessage.Logprobs(
                            content = logprobs.content.map { logprobsContent ->
                                ClientMessage.LogprobsContent(
                                    token = logprobsContent.token,
                                    logprob = logprobsContent.logprob,
                                    bytes = logprobsContent.bytes,
                                    topLogprobs = logprobsContent.topLogprobs?.map { topLogprobsContent ->
                                        ClientMessage.TopLogprobsContent(
                                            token = topLogprobsContent.token,
                                            logprob = topLogprobsContent.logprob,
                                        )
                                    }
                                )
                            }
                        )
                    },
                    finishReason = when (it.finishReason) {
                        DeepSeekResponse.FinishReason.Stop -> ClientMessage.FinishReason.Stop
                        DeepSeekResponse.FinishReason.Length -> ClientMessage.FinishReason.Length
                        DeepSeekResponse.FinishReason.ContentFilter -> ClientMessage.FinishReason.ContentFilter
                        DeepSeekResponse.FinishReason.ToolCalls -> ClientMessage.FinishReason.ToolCalls
                        DeepSeekResponse.FinishReason.InsufficientSystemResource -> ClientMessage.FinishReason.InsufficientSystemResource
                        null -> null
                    }
                )
            },
            usage = ClientMessage.Usage(
                promptTokens = response.usage.promptTokens,
                completionTokens = response.usage.completionTokens,
                totalTokens = response.usage.totalTokens,
                promptTokensDetails = ClientMessage.PromptTokensDetails(
                    cachedTokens = response.usage.promptTokensDetails.cachedTokens
                ),
                completionTokensDetails = null,
                promptCacheHitTokens = response.usage.promptCacheHitTokens,
                promptCacheMissTokens = response.usage.promptCacheMissTokens
            ),
            systemFingerprint = response.systemFingerprint
        )
    }
}
