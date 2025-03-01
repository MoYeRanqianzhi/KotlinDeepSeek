package io.github.moyeranqianzhi.deepseek.web

import io.ktor.client.*

expect fun httpClient(userConfig: HttpClientConfig<*>.() -> Unit = {}): HttpClient