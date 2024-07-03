package com.gandalf.connect.types

data class StylingOptions(
    val primaryColor: String? = null,
    val backgroundColor: String? = null,
    val foregroundColor: String? = null,
    val accentColor: String? = null
)

data class ConnectOptions(
  val style: StylingOptions,
)

data class ConnectInput(
    var publicKey: String,
    var redirectURL: String,
    var services: InputData,
    var options: ConnectOptions? = null
)

typealias InputData = MutableMap<String, Service>

data class Service(
    val traits: List<String>? = emptyList(),
    val activities: List<String>? = emptyList(),
    val required: Boolean? = true
)

data class SupportedServicesAndTraits(
    val services: MutableList<String> = mutableListOf(),
    val traits: MutableList<String> = mutableListOf(),
    val activities: MutableList<String> = mutableListOf()
)
