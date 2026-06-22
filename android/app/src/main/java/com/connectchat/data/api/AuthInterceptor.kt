package com.connectchat.data.api

import com.connectchat.BuildConfig
import com.connectchat.data.preferences.UserPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val userPreferences: UserPreferences
) : Interceptor {

    private val refreshClient = OkHttpClient()
    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { userPreferences.accessToken.first() }
        val response = chain.proceed(withAuth(chain.request(), token))

        if (response.code == 401) {
            response.close()
            val newToken = tryRefresh() ?: return chain.proceed(withAuth(chain.request(), null))
            return chain.proceed(withAuth(chain.request(), newToken))
        }
        return response
    }

    private fun withAuth(request: Request, token: String?): Request =
        if (token != null) request.newBuilder().header("Authorization", "Bearer $token").build()
        else request

    @Suppress("UNCHECKED_CAST")
    private fun tryRefresh(): String? {
        val refreshToken = runBlocking { userPreferences.refreshToken.first() } ?: return null
        val body = """{"refreshToken":"$refreshToken"}"""
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${BuildConfig.BASE_URL}/api/auth/refresh")
            .post(body)
            .build()
        return try {
            val response = refreshClient.newCall(request).execute()
            if (!response.isSuccessful) {
                runBlocking { userPreferences.clearAll() }
                return null
            }
            val json = response.body?.string() ?: return null
            val mapType = object : TypeToken<Map<String, Any?>>() {}.type
            val map: Map<String, Any?> = gson.fromJson(json, mapType)
            val data = map["data"] as? Map<*, *> ?: return null
            val newAccess = data["accessToken"] as? String ?: return null
            val newRefresh = data["refreshToken"] as? String ?: refreshToken
            runBlocking { userPreferences.saveTokens(newAccess, newRefresh) }
            newAccess
        } catch (e: Exception) {
            null
        }
    }
}
