package com.tfc.ulht.dpplugin.dplib

import okhttp3.Credentials
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

const val SUBMISSION_ID = "106" // Replace with a valid submission ID

class DPClientTest {
    private val token = ResourceBundle.getBundle("client").getString("token").split(":").let {
        Credentials.basic(it[0], it[1])
    }

    @Test
    fun assignmentListTest() {
        val client = DPClient()

        assertTrue(client.loginBlocking(token))

        assertTrue((client.getAssigmentsBlocking()?.size ?: 0) > 0)
    }

    @Test
    fun assignmentSubmissionsTest() {
        val client = DPClient()

        assertTrue(client.loginBlocking(token))

        val id = client.getAssigmentsBlocking()?.getOrNull(0)?.id
        assertNotNull(id)

        assertTrue((client.getSubmissionsBlocking(id!!)?.size ?: 0) > 0)
    }

    @Test
    fun downloadSubmissionTest() {
        val client = DPClient()

        assertTrue(client.loginBlocking(token))

        assertNotNull(client.downloadSubmissionBlocking(SUBMISSION_ID))
    }
}