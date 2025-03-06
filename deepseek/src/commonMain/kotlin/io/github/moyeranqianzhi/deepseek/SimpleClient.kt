package io.github.moyeranqianzhi.deepseek

import io.github.moyeranqianzhi.deepseek.api.ApiKey
import io.github.moyeranqianzhi.deepseek.api.Client
import io.github.moyeranqianzhi.deepseek.api.ClientMessage
import io.github.moyeranqianzhi.deepseek.api.Messages
import io.github.moyeranqianzhi.deepseek.api.Model
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SimpleClient(apiKey: String) {
    private val client = Client {
        this.apiKey = ApiKey.Key(apiKey)
    }

    suspend fun chatCompletion(
        model: Model = Model.Chat, messages: Messages
    ): Pair<String?, String?> {
        client.chatCompletion {
            this.model = model
            this.messages = messages
        }.let { res ->
            when (res) {
                is ClientMessage.Success -> {
                    return Pair(res.choices.first().message.reasoningContent, res.choices.first().message.content)
                }

                else -> {
                    return Pair("<|ERROR|>", null)
                }
            }
        }
    }

    fun chatCompletions(
        model: Model = Model.Chat,
        messages: Messages
    ): Flow<Pair<String?, String?>> = flow {
        client.chatCompletionChunk {
            this.model = model
            this.messages = messages
        }.collect { res ->
            when (res) {
                is ClientMessage.Success -> {
                    emit(Pair(res.choices.first().message.reasoningContent, res.choices.first().message.content))
                }

                is ClientMessage.Finish -> {
                    emit(Pair("<|FINISH|>", null))
                }

                is ClientMessage.Error -> {
                    emit(Pair("<|ERROR|>", null))
                }
            }
        }
    }
}