package io.github.moyeranqianzhi.deepseek.api

class ChatCompletionConfig {
    lateinit var model: Model
    lateinit var messages: Messages
    var stream = false

    val settings = Settings

    object Settings {
        val model = ModelSettings

        object ModelSettings {
            var type: ModelType = ModelType.Auto
        }
    }
}