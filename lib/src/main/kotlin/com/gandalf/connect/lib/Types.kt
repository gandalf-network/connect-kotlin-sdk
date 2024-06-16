package com.gandalf.connect.types

data class ConnectInput(
    var publicKey: String,
    var redirectURL: String,
    var services: InputData,
    var platform: Platform? = Platform.IOS
)

enum class Platform {
    IOS, ANDROID, UNIVERSAL
}

typealias InputData = MutableMap<String, Service>

data class Service(
    val traits: List<String>? = emptyList(),
    val activities: List<String>? = emptyList()
)

data class SupportedServicesAndTraits(
    val services: MutableList<String> = mutableListOf(),
    val traits: MutableList<String> = mutableListOf(),
    val activities: MutableList<String> = mutableListOf()
)
