package com.gandalf.connect

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.rx3.rxQuery
import com.google.gson.Gson
import com.gandalf.connect.api.ApiService
import com.gandalf.connect.lib.*
import com.gandalf.connect.types.*
import io.reactivex.rxjava3.core.Single
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URL
import java.util.*
import kotlin.collections.HashMap

class Connect(input: ConnectInput) {

    var publicKey: String = input.publicKey
    var redirectURL: String = input.redirectURL
    var data: InputData = input.services
    var platform: Platform = input.platform ?: Platform.IOS
    var verificationComplete: Boolean = false

    init {
        if (redirectURL.endsWith("/")) {
            redirectURL = redirectURL.dropLast(1)
        }
    }

    suspend fun generateURL(): String {
        allValidations(publicKey, redirectURL, data)
        val dataJson = Gson().toJson(data)
        return encodeComponents(dataJson, redirectURL, publicKey)
    }

    companion object {
        suspend fun getSupportedServicesAndTraits(): SupportedServicesAndTraits {
            val apiService = ApiService()
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

        private suspend fun validatePublicKey(publicKey: String) {
            val apiService = ApiService()
            val isValidPublicKey = apiService.verifyPublicKey(publicKey).blockingGet()
            if (isValidPublicKey) {
                throw GandalfError(
                    "Public key does not exist",
                    GandalfErrorCode.InvalidPublicKey
                )
            }
        }

        private suspend fun validateInputData(input: InputData): InputData {
            val apiService = ApiService()
            val supportedServicesAndTraits = apiService.getSupportedServicesAndTraits().blockingGet()
            val cleanServices = mutableMapOf<String, Service>()

            val unsupportedServices = mutableListOf<String>()
            val keys = input.keys
            val lkeys = keys.map { it.lowercase() }

            if (lkeys.size > 2 || (lkeys.size == 2 && !lkeys.contains("gandalf"))) {
                throw GandalfError(
                    "Only one non Gandalf service is supported per Connect URL",
                    GandalfErrorCode.InvalidService
                )
            }

            for (key in keys) {
                if (!supportedServicesAndTraits.services.contains(key.lowercase())) {
                    unsupportedServices.add(key)
                    continue
                }

                val service = input[key]
                validateInputService(service!!, supportedServicesAndTraits)
                cleanServices[key.lowercase()] = input[key]!!
            }

            if (unsupportedServices.isNotEmpty()) {
                throw GandalfError(
                    "These services [ ${unsupportedServices.joinToString(", ")} ] are unsupported",
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
        val baseUrl = when (platform) {
            Platform.ANDROID -> ANDROID_APP_CLIP_BASE_URL
            Platform.UNIVERSAL -> UNIVERSAL_APP_CLIP_BASE_URL
            Platform.IOS -> IOS_APP_CLIP_BASE_URL
        }

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

    private suspend fun allValidations(publicKey: String, redirectURL: String, data: InputData) {
        if (!verificationComplete) {
            validatePublicKey(publicKey)
            validateRedirectURL(redirectURL)
            val cleanServices = validateInputData(data)
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
