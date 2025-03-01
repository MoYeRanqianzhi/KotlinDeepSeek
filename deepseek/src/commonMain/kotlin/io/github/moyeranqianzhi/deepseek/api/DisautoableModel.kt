package io.github.moyeranqianzhi.deepseek.api

class DisautoableModel(model: Model.Custom) : Exception(
    "This model [${model.id}] can not auto.\n" +
            "Please set config.settings.model.type!"
)