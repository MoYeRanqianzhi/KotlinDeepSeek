package io.github.moyeranqianzhi.deepseek.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ApiKey.Serializer::class)
sealed class ApiKey {
    data class Key(override val key: String) : ApiKey()

    abstract val key: String

    internal object Serializer : KSerializer<ApiKey> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ApiKey", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: ApiKey) {
            encoder.encodeString(value.key)
        }

        override fun deserialize(decoder: Decoder) = Key(decoder.decodeString())
    }
}

