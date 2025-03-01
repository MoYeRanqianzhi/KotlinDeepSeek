package io.github.moyeranqianzhi.deepseek.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = Messages.Serializer::class)
class Messages(vararg messages: Message) : List<Message> by messages.toList() {
    constructor(block: MessagesBuilder.() -> Unit) : this(*MessagesBuilder().apply(block).messageArray)

    internal object Serializer : KSerializer<Messages> {
        private val serializer = ListSerializer(Message.serializer())
        override val descriptor: SerialDescriptor = serializer.descriptor

        override fun serialize(encoder: Encoder, value: Messages) {
            serializer.serialize(encoder, value)
        }

        override fun deserialize(decoder: Decoder) = Messages(*serializer.deserialize(decoder).toTypedArray())
    }

    fun append(message: Message) = (this + message).toMessages()
    fun append(role: Role, content: String) = (this + Message.Custom(role, content)).toMessages()
    fun add(message: Message) = (this + message).toMessages()
    fun add(index: Int, message: Message): Messages {
        val tmp = toMutableList()
        tmp.add(index, message)
        return tmp.toMessages()
    }

    fun system(content: String) = (this + Message.System(content)).toMessages()
    fun user(content: String) = (this + Message.User(content)).toMessages()
    fun assistant(content: String) = (this + Message.Assistant(content)).toMessages()
    fun custom(role: Role, content: String) = (this + Message.Custom(role, content)).toMessages()
}

class MessagesBuilder {
    private val _messages = mutableListOf<Message>()
    val messageList get() = _messages.toList()
    val messageArray get() = _messages.toTypedArray()
    val messages: Messages get() = _messages.toMessages()

    fun append(message: Message) = _messages.add(message)
    fun append(role: Role, content: String) = _messages.add(Message.Custom(role, content))
    fun add(message: Message) = _messages.add(message)
    fun add(index: Int, message: Message) = _messages.add(index, message)
    fun system(content: String) = _messages.add(Message.System(content))
    fun user(content: String) = _messages.add(Message.User(content))
    fun assistant(content: String) = _messages.add(Message.Assistant(content))
    fun custom(role: Role, content: String) = _messages.add(Message.Custom(role, content))
}

fun List<Message>.toMessages() = Messages(*this.toTypedArray())
fun Array<Message>.toMessages() = Messages(*this)
fun Iterable<Message>.toMessages() = Messages(*this.toList().toTypedArray())

