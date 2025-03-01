package io.github.moyeranqianzhi.deepseek.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private const val CHAT = "deepseek-chat"
private const val REASONER = "deepseek-reasoner"

@Serializable(with = Model.Serializer::class)
sealed class Model {
    data object Chat : Model()

    data object Reasoner : Model()

    data class Custom(val id: String) : Model()

    internal object Serializer : KSerializer<Model> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Model", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Model) {
            when (value) {
                is Chat -> encoder.encodeString(CHAT)
                is Reasoner -> encoder.encodeString(REASONER)
                is Custom -> encoder.encodeString(value.id)
            }
        }

        override fun deserialize(decoder: Decoder) = decoder.decodeString().let { model ->
            when (model) {
                CHAT -> Chat
                REASONER -> Reasoner
                else -> Custom(model)
            }
        }
    }
}