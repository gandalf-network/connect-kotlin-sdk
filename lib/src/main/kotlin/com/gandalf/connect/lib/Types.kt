package com.gandalf.connect.types

data class StylingOptions(
    val primaryColor: String?,
    val backgroundColor: String?,
    val foregroundColor: String?,
    val accentColor: String?,
)

data class ConnectOptions(
  val style: StylingOptions,
)

data class ConnectInput(
    var publicKey: String,
    var redirectURL: String,
    var services: InputData,
    var options: ConnectOptions?,
)

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
