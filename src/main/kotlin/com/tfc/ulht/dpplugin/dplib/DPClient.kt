package com.tfc.ulht.dpplugin.dplib

import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

const val TIMEOUT = 30L

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
        private val client = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .build()
    }
    
    private val logger = Logger.getInstance(this::class.java)

    private val json = Json { ignoreUnknownKeys = true }

    private var authString: String? = null

    val loggedIn: Boolean
        get() = authString != null

    var loggingIn: Boolean = false
        private set

    private fun logRequestStart(requestName: String) = logger.warn("${requestName}: Making request...")
    private fun logRequestSuccess(requestName: String) = logger.warn("${requestName}: done")
    private fun logRequestError(requestName: String, response: Response) = logger.error("${requestName}: ${response.code}")
    private fun logException(requestName: String, e: java.lang.Exception) = logger.error("${requestName}: ${e.message}")

    fun loginBlocking(token: String): Boolean {
        val request = Request.Builder()
            .url(BASE_URL + "api/teacher/assignments/current")
            .header("Authorization", token)
            .build()
        
        logRequestStart("loginBlocking")

        return client.newCall(request).execute().let { response ->
            authString = if (response.isSuccessful) {
                logRequestSuccess("loginBlocking")
                token
            } else {
                logRequestError("loginBlocking", response)
                null
            }

            response.isSuccessful.also {
                response.close()
            }
        }
    }

    fun login(token: String, callback: ((Boolean, String?) -> Unit)?) {
        loggingIn = true

        val request = Request.Builder()
            .url(BASE_URL + "api/teacher/assignments/current")
            .header("Authorization", token)
            .build()

        logRequestStart("login")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logException("login", e)
                loggingIn = false
                if (callback != null) callback(false, if (e.message == "timeout") "Error: Timeout" else null)
            }

            override fun onResponse(call: Call, response: Response) {
                loggingIn = false
                authString = if (response.isSuccessful) {
                    logRequestSuccess("login")
                    token
                } else {
                    logRequestError("login", response)
                    null
                }

                if (callback != null) {
                    callback(response.isSuccessful, "${response.code} ${response.message}")
                }

                response.close()
            }
        })
    }

    fun login(username: String, token: String, callback: ((Boolean, String?) -> Unit)?) {
        val credentials = Credentials.basic(username, token)
        login(credentials, callback)
    }

    fun getAssigmentsBlocking(): List<Assignment>? {
        if (!loggedIn) return null

        val request = Request.Builder()
            .url(BASE_URL + "api/teacher/assignments/current")
            .header("Authorization", authString!!)
            .build()

        logRequestStart("getAssignmentsBlocking")

        return client.newCall(request).execute().let { response ->
            try {
                val assignment = json.decodeFromString<List<Assignment>>(response.body!!.string())
                logRequestSuccess("getAssignmentsBlocking")
                response.close()
                assignment
            } catch (e: Exception) {
                logException("getAssignmentsBlocking", e)
                response.close()
                null
            }
        }
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

        logRequestStart("getAssignments")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logException("getAssignments", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    logRequestError("getAssignments", response)
                    callback(null)
                    response.close()
                    return
                }

                try {
                    val assignment = json.decodeFromString<List<Assignment>>(response.body!!.string())
                    logRequestSuccess("getAssignments")
                    callback(assignment)
                    response.close()
                } catch (e: Exception) {
                    logException("getAssignments", e)
                    callback(null)
                    response.close()
                }
            }
        })
    }

    fun getSubmissionsBlocking(assignmentId: String): List<SubmissionsResponse>? {
        if (!loggedIn) return null

        val request = Request.Builder()
            .url(BASE_URL + "api/teacher/assignments/$assignmentId/submissions")
            .header("Authorization", authString!!)
            .build()

        logRequestStart("getSubmissionsBlocking")

        return client.newCall(request).execute().let { response ->
            try {
                val submissions = json.decodeFromString<List<SubmissionsResponse>>(response.body!!.string())
                logRequestSuccess("getSubmissionsBlocking")
                response.close()
                submissions
            } catch (e: Exception) {
                logException("getSubmissionsBlocking", e)
                response.close()
                null
            }
        }
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

        logRequestStart("getSubmissions")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logException("getSubmissions", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    logRequestError("getSubmissions", response)
                    callback(null)
                    response.close()
                    return
                }

                try {
                    val submissions = json.decodeFromString<List<SubmissionsResponse>>(response.body!!.string())
                    logRequestSuccess("getSubmissions")
                    callback(submissions)
                    response.close()
                } catch (e: Exception) {
                    logException("getSubmissions", e)
                    callback(null)
                    response.close()
                }
            }
        })
    }

    fun getGroupSubmissionsBlocking(assignmentId: String, groupId: Int): List<Submission>? {
        if (!loggedIn) return null

        val request = Request.Builder()
            .url(BASE_URL + "api/teacher/assignments/$assignmentId/submissions/$groupId")
            .header("Authorization", authString!!)
            .build()

        logRequestStart("getGroupSubmissionsBlocking")

        return client.newCall(request).execute().let { response ->
            try {
                val submissions = json.decodeFromString<List<Submission>>(response.body!!.string())
                logRequestSuccess("getGroupSubmissionsBlocking")
                response.close()
                submissions
            } catch (e: Exception) {
                logException("getGroupSubmissionsBlocking", e)
                response.close()
                null
            }
        }
    }

    fun getGroupSubmissions(assignmentId: String, groupId: Int, callback: ((List<Submission>?) -> Unit)) {
        if (!loggedIn) {
            callback(null)
            return
        }

        val request = Request.Builder()
            .url(BASE_URL + "api/teacher/assignments/$assignmentId/submissions/$groupId")
            .header("Authorization", authString!!)
            .build()

        logRequestStart("getGroupSubmissions")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logException("getGroupSubmissions", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    logRequestError("getGroupSubmissions", response)
                    callback(null)
                    response.close()
                    return
                }

                try {
                    val submissions = json.decodeFromString<List<Submission>>(response.body!!.string())
                    logRequestSuccess("getGroupSubmissions")
                    callback(submissions)
                    response.close()
                } catch (e: Exception) {
                    logException("getGroupSubmissions", e)
                    callback(null)
                    response.close()
                }
            }
        })
    }

    fun downloadSubmissionBlocking(submissionId: String): InputStream? {
        if (!loggedIn) return null

        val request = Request.Builder()
            .url(BASE_URL + "api/teacher/download/$submissionId")
            .header("Authorization", authString!!)
            .build()

        return client.newCall(request).execute().let { response ->
            if (!response.isSuccessful) return@let null

            response.body?.byteStream().also {
                response.close()
            }
        }
    }

    fun downloadSubmission(submissionId: String, callback: ((InputStream?) -> Unit)) {
        if (!loggedIn) {
            callback(null)
            return
        }

        val request = Request.Builder()
            .url(BASE_URL + "api/teacher/download/$submissionId")
            .header("Authorization", authString!!)
            .build()

        logRequestStart("downloadSubmission")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logException("downloadSubmission", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    logRequestError("downloadSubmission", response)
                    callback(null)
                }

                logRequestSuccess("downloadSubmission")
                callback(response.body?.byteStream())
                response.close()
            }
        })
    }

    fun getBuildReport(submissionId: String, callback: ((FullBuildReport?) -> Unit)) {
        if (!loggedIn) {
            callback(null)
            return
        }

        val request = Request.Builder()
            .url(BASE_URL + "api/teacher/submissions/$submissionId")
            .header("Authorization", authString!!)
            .build()

        logRequestStart("getBuildReport")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logException("getBuildReport", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback(null)
                    response.close()
                    return
                }

                try {
                    val buildReport = json.decodeFromString<FullBuildReport>(response.body!!.string())
                    logRequestSuccess("getBuildReport")
                    callback(buildReport)
                    response.close()
                } catch (e: Exception) {
                    logException("getBuildReport", e)
                    callback(null)
                    response.close()
                }
            }
        })
    }

    fun getStudentHistory(studentId: String, callback: ((StudentHistory?) -> Unit)) {
        if (!loggedIn) {
            callback(null)
            return
        }

        val request = Request.Builder()
            .url(BASE_URL + "api/teacher/studentHistory/$studentId")
            .header("Authorization", authString!!)
            .build()

        logRequestStart("getStudentHistory")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logException("getStudentHistory", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    logRequestError("getStudentHistory", response)
                    callback(null)
                    response.close()
                    return
                }

                try {
                    val studentHistory = json.decodeFromString<StudentHistory>(response.body!!.string())
                    logRequestSuccess("getStudentHistory")
                    callback(studentHistory)
                    response.close()
                } catch (e: Exception) {
                    logException("getStudentHistory", e)
                    callback(null)
                    response.close()
                }
            }
        })
    }

    fun searchStudents(query: String, callback: ((List<StudentListResponse>?) -> Unit)) {
        if (!loggedIn) {
            callback(null)
            return
        }

        val request = Request.Builder()
            .url(BASE_URL + "api/teacher/studentSearch/$query")
            .header("Authorization", authString!!)
            .build()

        logRequestStart("searchStudents")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logException("searchStudents", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    logRequestError("searchStudents", response)
                    callback(null)
                    response.close()
                    return
                }

                try {
                    val studentHistory = json.decodeFromString<List<StudentListResponse>>(response.body!!.string())
                    logRequestSuccess("searchStudents")
                    callback(studentHistory)
                    response.close()
                } catch (e: Exception) {
                    logException("searchStudents", e)
                    callback(null)
                    response.close()
                }
            }
        })
    }

    fun searchAssignments(query: String, callback: ((List<StudentListResponse>?) -> Unit)) {
        if (!loggedIn) {
            callback(null)
            return
        }

        val request = Request.Builder()
            .url(BASE_URL + "api/teacher/assignmentSearch/$query")
            .header("Authorization", authString!!)
            .build()

        logRequestStart("searchAssignments")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logException("searchAssignments", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    logRequestError("searchAssignments", response)
                    callback(null)
                    response.close()
                    return
                }

                try {
                    val assignments = json.decodeFromString<List<StudentListResponse>>(response.body!!.string())
                    logRequestSuccess("searchAssignments")
                    callback(assignments)
                    response.close()
                } catch (e: Exception) {
                    logException("searchAssignments", e)
                    callback(null)
                    response.close()
                }
            }
        })
    }

    fun markAsFinal(submissionId: Int, callback: ((Boolean?) -> Unit)) {
        if (!loggedIn) {
            callback(null)
            return
        }

        val request = Request.Builder()
            .url(BASE_URL + "api/teacher/submissions/$submissionId/markAsFinal")
            .header("Authorization", authString!!)
            .build()

        logRequestStart("markAsFinal")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logException("markAsFinal", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    logRequestError("markAsFinal", response)
                    callback(null)
                    response.close()
                    return
                }

                try {
                    val result = response.body!!.string()
                    logRequestSuccess("markAsFinal")
                    callback(result == "true")
                    response.close()
                } catch (e: Exception) {
                    logException("markAsFinal", e)
                    callback(null)
                    response.close()
                }
            }
        })
    }

    fun previewMarkBestSubmissions(assignmentId: String, callback: (List<Submission>?) -> Unit) {
        if (!loggedIn) {
            callback(null)
            return
        }

        val request = Request.Builder()
            .url(BASE_URL + "api/teacher/assignments/$assignmentId/previewMarkBestSubmissions")
            .header("Authorization", authString!!)
            .build()

        logRequestStart("previewMarkBestSubmissions")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logException("previewMarkBestSubmissions", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    logRequestError("previewMarkBestSubmissions", response)
                    callback(null)
                    response.close()
                    return
                }

                try {
                    val submissions = json.decodeFromString<List<Submission>>(response.body!!.string())
                    logRequestSuccess("previewMarkBestSubmissions")
                    callback(submissions)
                    response.close()
                } catch (e: Exception) {
                    logException("previewMarkBestSubmissions", e)
                    callback(null)
                    response.close()
                }
            }
        })
    }

    fun markMultipleAsFinal(submissionIds: List<Int>, callback: ((Boolean?) -> Unit)) {
        if (!loggedIn) {
            callback(null)
            return
        }

        val request = Request.Builder()
            .url(BASE_URL + "api/teacher/markMultipleAsFinal")
            .post(
                Json.encodeToString(submissionIds).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            )
            .header("Authorization", authString!!)
            .build()

        logRequestStart("markMultipleAsFinal")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logException("markMultipleAsFinal", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    logRequestError("markMultipleAsFinal", response)
                    callback(null)
                    response.close()
                    return
                }

                try {
                    val result = response.body!!.string()
                    logRequestSuccess("markMultipleAsFinal")
                    callback(result == "true")
                    response.close()
                } catch (e: Exception) {
                    logException("markMultipleAsFinal", e)
                    callback(null)
                    response.close()
                }
            }
        })
    }

    fun toggleAssignmentState(assignmentId: String, callback: ((Boolean?) -> Unit)) {
        if (!loggedIn) {
            callback(null)
            return
        }

        val request = Request.Builder()
            .url(BASE_URL + "api/teacher/assignments/$assignmentId/toggleState")
            .header("Authorization", authString!!)
            .build()

        logRequestStart("toggleAssignmentState")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logException("toggleAssignmentState", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    logRequestError("toggleAssignmentState", response)
                    callback(null)
                    response.close()
                    return
                }

                try {
                    val result = response.body!!.string()
                    logRequestSuccess("toggleAssignmentState")
                    callback(result == "true")
                    response.close()
                } catch (e: Exception) {
                    logException("toggleAssignmentState", e)
                    callback(null)
                    response.close()
                }
            }
        })
    }
}