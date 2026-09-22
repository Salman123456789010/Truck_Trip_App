package com.dadabarbie.TruckTrip.Utils

import com.vasyerp.freshvegetables.util.NetworkResult
import retrofit2.Response

abstract class BaseApiResponse {
    suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): NetworkResult<T> {
        try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    return NetworkResult.Success(body)
                }
            }
            val errorBodyString = try {
                response.errorBody()?.string()
            } catch (_: Exception) {
                null
            }
            val parsedErrorMessage = try {
                if (!errorBodyString.isNullOrEmpty()) {
                    val json = org.json.JSONObject(errorBodyString)
                    json.optString("message", "").ifEmpty {
                        json.optString("error", errorBodyString)
                    }
                } else null
            } catch (_: Exception) {
                errorBodyString
            }
            val finalMessage = parsedErrorMessage?.ifBlank { null } ?: "${response.code()} ${response.message()}"
            return error(finalMessage)
        } catch (e: Exception) {
            return error(e.localizedMessage ?: e.message ?: e.toString())
        }
    }

    private fun <T> error(errorMessage: String): NetworkResult<T> =
        NetworkResult.Error(errorMessage)
}