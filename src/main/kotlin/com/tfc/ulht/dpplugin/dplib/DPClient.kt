package com.tfc.ulht.dpplugin.dplib

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import kotlinx.serialization.json.Json
import okhttp3.*
import java.io.IOException
import kotlinx.serialization.decodeFromString

class DPClient {
    /* private val authenticator = object : Authenticator {
        var username: String? = null
        var token: String? = null

        override fun authenticate(route: Route?, response: Response): Request? {
            if (username == null || token == null || response.request.header("Authorization") != null) {
                return null
            }

            return response.request.newBuilder()
                .header("Authorization", Credentials.basic(username!!, token!!))
                .build()
        }
    }*/

    companion object {
        private val client = OkHttpClient()
    }

    private val json = Json { ignoreUnknownKeys = true }

    private var authString: String? = null

    private val loggedIn: Boolean
        get() = authString != null

    fun login(token: String, callback: ((Boolean) -> Unit)?) {
        val request = Request.Builder()
            .url(BASE_URL + "api/teacher/assignments/current")
            .header("Authorization", token)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (callback != null) callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                authString = if (response.isSuccessful) {
                    if (callback != null) callback(true)

                    PasswordSafe.instance.set(
                        CredentialAttributes("DP", "dp"),
                        com.intellij.credentialStore.Credentials(token))

                    authString = token

                    token
                } else {
                    if (callback != null) callback(false)

                    PasswordSafe.instance.set(
                        CredentialAttributes("DP", "dp"),
                        null)

                    authString = null

                    null
                }
            }
        })
    }

    fun login(username: String, token: String, callback: ((Boolean) -> Unit)?) {
        val credentials = Credentials.basic(username, token)
        login(credentials, callback)
    }

    fun getAssignments(callback: (List<Assignment>?) -> Unit) {
        if (!loggedIn) {
            callback(null)
            return
        }

        val request = Request.Builder()
            .url(BASE_URL + "api/teacher/assignments/current")
            .header("Authorization", authString!!)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback(null)
                    return
                }

                try {
                    val assignment = json.decodeFromString<List<Assignment>>(response.body!!.string())
                    callback(assignment)
                } catch (_: Exception) {
                    callback(null)
                }
            }
        })
    }

    fun getSubmissions(assignmentId: String, callback: ((List<SubmissionsResponse>?) -> Unit)) {
        if (!loggedIn) {
            callback(null)
            return
        }

        val request = Request.Builder()
            .url(BASE_URL + "api/teacher/assignments/$assignmentId/submissions")
            .header("Authorization", authString!!)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback(null)
                    return
                }

                try {
                    val submissions = json.decodeFromString<List<SubmissionsResponse>>(response.body!!.string())
                    callback(submissions)
                } catch (_: Exception) {
                    callback(null)
                }
            }
        })
    }
}