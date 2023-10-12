package com.tfc.ulht.dpplugin.dplib

import kotlinx.serialization.Serializable

interface DPData

class Null : DPData

@Serializable
data class Instructions(val body: String, val format: String) : DPData

@Serializable
data class Assignment(
    val active: Boolean,
    val dueDate: String?,
    val id: String,
    val instructions: Instructions?,
    val language: String,
    val name: String,
    val packageName: String,
    val submissionMethod: String
) : DPData

@Serializable
data class Author(
    val id: Int,
    val name: String
) : DPData

@Serializable
data class ProjectGroup(
    val id: Int,
    val authors: List<Author>
) : DPData

@Serializable
data class TestResult(
    val methodName: String,
    val fullMethodName: String,
    val type: String,
    val failureType: String?,
    val failureErrorLine: String?,
    val failureDetail: String?
) : DPData

@Serializable
data class Submission(
    val id: Int,
    val submissionDate: String,
    val status: String,
    val statusDate: String,
    val testResults: List<TestResult>? = null
) : DPData

@Serializable
data class SubmissionsResponse(
    val projectGroup: ProjectGroup,
    val allSubmissions: List<Submission>
) : DPData