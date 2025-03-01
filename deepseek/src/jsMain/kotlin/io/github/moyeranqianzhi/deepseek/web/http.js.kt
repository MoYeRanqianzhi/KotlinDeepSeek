package io.github.moyeranqianzhi.deepseek.web

import io.ktor.client.*
import io.ktor.client.engine.js.*


actual fun httpClient(userConfig: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Js, userConfig)
