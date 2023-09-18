package com.tfc.ulht.dpplugin.dplib

import kotlinx.serialization.Serializable

@Serializable
data class Instructions(val body: String, val format: String)

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
)

@Serializable
data class Author(
    val id: Int,
    val name: String
)

@Serializable
data class ProjectGroup(
    val id: Int,
    val authors: List<Author>
)

@Serializable
data class TestResult(
    val methodName: String,
    val fullMethodName: String,
    val type: String,
    val failureType: String?,
    val failureErrorLine: String?,
    val failureDetail: String?
)

@Serializable
data class Submission(
    val submissionDate: String,
    val status: String,
    val statusDate: String,
    val testResults: List<TestResult>?
)

@Serializable
data class SubmissionsResponse(
    val projectGroup: ProjectGroup,
    val allSubmissions: List<Submission>
)