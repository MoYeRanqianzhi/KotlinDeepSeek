package io.github.moyeranqianzhi.deepseek

import io.github.moyeranqianzhi.deepseek.api.Message
import io.github.moyeranqianzhi.deepseek.api.Messages
import io.github.moyeranqianzhi.deepseek.api.Model
import io.github.moyeranqianzhi.deepseek.api.toMessages
import kotlinx.coroutines.flow.Flow


class SimpleRequest(
    private val apiKey: String,
    private val model: Model = Model.Chat,
    private val messages: Messages
) {
    constructor(
        apiKey: String,
        model: Model = Model.Chat,
        messages: List<Message>
    ) : this(apiKey, model, messages.toMessages())

    private val client = SimpleClient(apiKey)

    suspend fun chatCompletion(): Pair<String?, String?> = client.chatCompletion(model, messages)
    fun chatCompletions(): Flow<Pair<String?, String?>> = client.chatCompletions(model, messages)
}