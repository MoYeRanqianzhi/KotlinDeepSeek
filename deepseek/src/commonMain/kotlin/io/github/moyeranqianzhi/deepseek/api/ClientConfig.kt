package io.github.moyeranqianzhi.deepseek.api

import io.ktor.client.*

class ClientConfig {
    lateinit var apiKey: ApiKey
    var baseurl: BaseURL = BaseURL.DeepSeek

    internal var httpConfig: HttpClientConfig<*>.() -> Unit = {}
    fun http(
        block: HttpClientConfig<*>.() -> Unit
    ) {
        val old = httpConfig
        httpConfig = {
            old()
            block()
        }
    }

    fun build(): Client = Client(this)
    val client get() = build()
}
