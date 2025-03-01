package io.github.moyeranqianzhi.deepseek.api

import io.github.moyeranqianzhi.deepseek.api.Message.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = Serializer::class)
sealed class Message {
    abstract val content: String

    data class System(override val content: String) : Message()
    data class User(override val content: String) : Message()
    data class Assistant(override val content: String) : Message()
    data class Custom(val role: Role, override val content: String) : Message()

    internal object Serializer : KSerializer<Message> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Message", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Message) {
            encoder.encodeSerializableValue(
                ChatMessage.serializer(),
                ChatMessage(
                    role = when (value) {
                        is System -> Role.System
                        is User -> Role.User
                        is Assistant -> Role.Assistant
                        is Custom -> value.role
                    },
                    content = value.content
                )
            )
        }

        override fun deserialize(decoder: Decoder) = decoder.decodeSerializableValue(
            ChatMessage.serializer()
        ).let { message ->
            when (message.role) {
                Role.System -> System(message.content)
                Role.User -> User(message.content)
                Role.Assistant -> Assistant(message.content)
                is Role.Custom -> Custom(message.role, message.content)
            }
        }
    }
}

private const val SYSTEM = "system"
private const val USER = "user"
private const val ASSISTANT = "assistant"

@Serializable(with = Role.Serializer::class)
sealed class Role {
    data object System : Role()
    data object User : Role()
    data object Assistant : Role()
    data class Custom(val value: String) : Role()

    internal object Serializer : KSerializer<Role> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Role", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Role) {
            when (value) {
                is System -> encoder.encodeString(SYSTEM)
                is User -> encoder.encodeString(USER)
                is Assistant -> encoder.encodeString(ASSISTANT)
                is Custom -> encoder.encodeString(value.value)
            }
        }

        override fun deserialize(decoder: Decoder) = decoder.decodeString().let { role ->
            when (role) {
                SYSTEM -> System
                USER -> User
                ASSISTANT -> Assistant
                else -> Custom(role)
            }
        }
    }
}

@Serializable
private data class ChatMessage(
    val role: Role,
    val content: String
)