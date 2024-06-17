// ApiService.kt
package com.gandalf.connect.api

import com.apollographql.apollo3.ApolloClient
import com.gandalf.connect.GetAppByPublicKeyQuery
import com.gandalf.connect.GetSupportedServicesQuery
import com.gandalf.connect.types.SupportedServicesAndTraits
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import com.gandalf.connect.lib.WATSON_URL

class ApiService {
    private val apolloClient = ApolloClient.Builder()
        .serverUrl(WATSON_URL)
        .build()

    fun verifyPublicKey(publicKey: String): Single<Boolean> {
        val query = GetAppByPublicKeyQuery(publicKey)
        return Single.fromCallable {
            runBlocking {
                apolloClient.query(query)
                    .toFlow()
                    .map { response ->
                        val gandalfID = response.data?.getAppByPublicKey?.gandalfID
                        when (gandalfID) {
                            is String -> gandalfID.toIntOrNull()?.let { it > 0 } ?: false
                            is Int -> gandalfID > 0
                            else -> false
                        }
                    }
                    .single()
            }
        }.onErrorReturnItem(false)
    }

    fun getSupportedServicesAndTraits(): Single<SupportedServicesAndTraits> {
        val query = GetSupportedServicesQuery()
        return Single.fromCallable {
            runBlocking {
                apolloClient.query(query)
                    .toFlow()
                    .map { response ->
                        val result = SupportedServicesAndTraits(
                            services = mutableListOf(),
                            traits = mutableListOf(),
                            activities = mutableListOf()
                        )
                        response.data?.let { data ->
                            data.__sourceType?.enumValues?.forEach { enumValue ->
                                result.services.add(enumValue.name.lowercase())
                            }
                            data.__traitType?.enumValues?.forEach { enumValue ->
                                result.traits.add(enumValue.name.lowercase())
                            }
                            data.__activityType?.enumValues?.forEach { enumValue ->
                                result.activities.add(enumValue.name.lowercase())
                            }
                        }
                        result
                    }
                    .single()
            }
        }
    }
}