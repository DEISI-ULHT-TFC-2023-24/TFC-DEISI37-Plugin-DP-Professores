package com.tfc.ulht.dpplugin.dplib

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.ResourceBundle

class DPClientTest {
    private val token = ResourceBundle.getBundle("client").getString("token")

    @Test
    fun assignmentListTest() {
        val client = DPClient()

        client.login(token) {
            assertTrue(it)

            client.getAssignments { assignments ->
                assertTrue((assignments?.size ?: 0) > 0)
            }
        }
    }

    @Test
    fun assignmentSubmissionsTest() {
        val client = DPClient()

        client.login(token) {
            assertTrue(it)

            client.getAssignments { assignments ->
                assertTrue((assignments?.size ?: 0) > 0)

                client.getSubmissions(assignments!![0].id) { sub ->
                    assertTrue((sub?.size ?: 0) > 0)
                }
            }
        }
    }
}