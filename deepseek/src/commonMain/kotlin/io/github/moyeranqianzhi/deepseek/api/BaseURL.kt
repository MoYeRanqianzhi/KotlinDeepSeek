package io.github.moyeranqianzhi.deepseek.api

import io.github.moyeranqianzhi.deepseek.api.BaseURL.Serializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private const val BASEURL = "https://api.deepseek.com"
private const val SUFFIX = "/chat/completions"


@Serializable(with = Serializer::class)
sealed class BaseURL {
    data object DeepSeek : BaseURL()

    data class Custom(val baseurl: String) : BaseURL()

    open val suffix = SUFFIX
    val url: String
        get() = when (this) {
            DeepSeek -> BASEURL + suffix
            is Custom -> baseurl + suffix
        }

    internal object Serializer : KSerializer<BaseURL> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BaseURL", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: BaseURL) {
            when (value) {
                is DeepSeek -> encoder.encodeString(BASEURL)
                is Custom -> encoder.encodeString(value.baseurl)
            }
        }

        override fun deserialize(decoder: Decoder) = decoder.decodeString().let { url ->
            when (url) {
                BASEURL -> DeepSeek
                else -> Custom(url)
            }
        }
    }
}