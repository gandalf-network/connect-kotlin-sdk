package com.gandalf.connect

import com.apollographql.apollo3.ApolloClient
import com.google.gson.Gson
import com.gandalf.connect.api.ApiService
import com.gandalf.connect.lib.*
import com.gandalf.connect.types.*
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URL
import java.util.*

class Connect(input: ConnectInput) {

    var publicKey: String = input.publicKey
    var redirectURL: String = input.redirectURL
    var data: InputData = input.services
    var options: ConnectOptions? = input.options
    var verificationComplete: Boolean = false
    private var apiService: ApiService = ApiService()

    init {
        if (redirectURL.endsWith("/")) {
            redirectURL = redirectURL.dropLast(1)
        }
    }

    suspend fun generateURL(): String {
        allValidations(publicKey, redirectURL, data, this.apiService)
        var dataJson = Gson().toJson(data)
        val safeOptions = options
        if (safeOptions != null) {
            val optionsJSON = Gson().toJson(safeOptions.style)
            dataJson = dataJson.trim().removeSuffix("}") + "," +
                    "\"options\":" + optionsJSON + "}"
        }
        return encodeComponents(dataJson, redirectURL, publicKey)
    }

    fun setApiService(apiService: ApiService) {
        this.apiService = apiService
    }

    companion object {
        suspend fun getSupportedServicesAndTraits(apiService: ApiService = ApiService()): SupportedServicesAndTraits {
            return apiService.getSupportedServicesAndTraits().blockingGet()
        }

        fun getDataKeyFromURL(redirectURL: String): String {
            validateRedirectURL(redirectURL)
            val url = URL(redirectURL)
            val dataKey = url.getQueryParam("dataKey")
            return dataKey ?: throw GandalfError(
                "Datakey not found in the URL $redirectURL",
                GandalfErrorCode.DataKeyNotFound
            )
        }

        private suspend fun validatePublicKey(publicKey: String, apiService: ApiService = ApiService()) {
            val isValidPublicKey = apiService.verifyPublicKey(publicKey).blockingGet()
            if (!isValidPublicKey) {
                throw GandalfError(
                    "Public key does not exist",
                    GandalfErrorCode.InvalidPublicKey
                )
            }
        }

        private suspend fun validateInputData(input: InputData, apiService: ApiService = ApiService()): InputData {
            val supportedServicesAndTraits = apiService.getSupportedServicesAndTraits().blockingGet()
            val cleanServices = mutableMapOf<String, Service>()

            val unsupportedServices = mutableListOf<String>()
            val keys = input.keys
            val lkeys = keys.map { it.lowercase() }

            if (lkeys.size == 1 && lkeys.contains("gandalf")) {
                throw GandalfError(
                    "Another non Gandalf service is required",
                    GandalfErrorCode.InvalidService
                )
            }

            var atLeastOneServiceRequired = false

            for (key in keys) {
                if (!supportedServicesAndTraits.services.contains(key.lowercase())) {
                    unsupportedServices.add(key)
                    continue
                }

                val service = input[key]!!.copy(required = input[key]!!.required ?: true)
                validateInputService(service, supportedServicesAndTraits)
                cleanServices[key.lowercase()] = service

                if (service.required == true) {
                    atLeastOneServiceRequired = true
                }
            }

            if (unsupportedServices.isNotEmpty()) {
                throw GandalfError(
                    "These services [ ${unsupportedServices.joinToString(", ")} ] are unsupported",
                    GandalfErrorCode.InvalidService
                )
            }

            if (!atLeastOneServiceRequired) {
                throw GandalfError(
                    "At least one service must have the 'required' property set to true",
                    GandalfErrorCode.InvalidService
                )
            }

            return cleanServices
        }

        private fun validateInputService(
            input: Service,
            supportedServicesAndTraits: SupportedServicesAndTraits
        ) {
            if ((input.activities?.size ?: 0) < 1 && (input.traits?.size ?: 0) < 1) {
                throw GandalfError(
                    "At least one trait or activity is required",
                    GandalfErrorCode.InvalidService
                )
            }

            val unsupportedActivities = mutableListOf<String>()
            val unsupportedTraits = mutableListOf<String>()

            input.activities?.forEach { key ->
                if (!supportedServicesAndTraits.activities.contains(key.lowercase())) {
                    unsupportedActivities.add(key)
                }
            }

            input.traits?.forEach { key ->
                if (!supportedServicesAndTraits.traits.contains(key.lowercase())) {
                    unsupportedTraits.add(key)
                }
            }

            if (unsupportedActivities.isNotEmpty()) {
                throw GandalfError(
                    "These activities [ ${unsupportedActivities.joinToString(", ")} ] are unsupported",
                    GandalfErrorCode.InvalidService
                )
            }

            if (unsupportedTraits.isNotEmpty()) {
                throw GandalfError(
                    "These traits [ ${unsupportedTraits.joinToString(", ")} ] are unsupported",
                    GandalfErrorCode.InvalidService
                )
            }
        }

        private fun validateRedirectURL(url: String) {
            try {
                URL(url)
            } catch (e: Throwable) {
                throw GandalfError(
                    "Invalid redirectURL",
                    GandalfErrorCode.InvalidRedirectURL
                )
            }
        }
    }

    private fun encodeComponents(data: String, redirectUrl: String, publicKey: String): String {
        val baseUrl = ANDROID_APP_CLIP_BASE_URL

        val base64Data = Base64.getEncoder().encodeToString(data.toByteArray())

        val urlBuilder = baseUrl.toHttpUrlOrNull()?.newBuilder() ?: throw GandalfError(
            "Invalid base URL",
            GandalfErrorCode.InvalidBaseURL
        )

        urlBuilder.addQueryParameter("publicKey", publicKey)
        urlBuilder.addQueryParameter("redirectUrl", redirectUrl)
        urlBuilder.addQueryParameter("data", base64Data)

        return urlBuilder.build().toString()
    }

    private fun ensureRequiredProperty(data: InputData): InputData {
        val cleanServices = mutableMapOf<String, Service>()
        data.forEach { (key, service) ->
            cleanServices[key] = service.copy(required = service.required ?: true)
        }
        return cleanServices
    }

    private suspend fun allValidations(
        publicKey: String, 
        redirectURL: String, 
        data: InputData,
        apiService: ApiService,
        ) {
        if (!verificationComplete) {
            validatePublicKey(publicKey, apiService)
            validateRedirectURL(redirectURL)
            val cleanServices = validateInputData(ensureRequiredProperty(data), apiService)
            this.data = cleanServices
        }

        verificationComplete = true
    }
}

// Extension function to get query parameters
fun URL.getQueryParam(param: String): String? {
    return this.query?.split("&")
        ?.map { it.split("=") }
        ?.firstOrNull { it.size > 1 && it[0] == param }
        ?.get(1)
}
