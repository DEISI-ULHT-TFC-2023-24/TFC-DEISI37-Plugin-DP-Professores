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
    val submissionMethod: String,
    val numSubmissions: Int,
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
    val testResults: List<TestResult>? = null,
    val teacherTests: JUnitSummary? = null,
    val studentTests: JUnitSummary? = null,
    val hiddenTests: JUnitSummary? = null,
    val markedAsFinal: Boolean,
    val group: ProjectGroup? = null
) : DPData

@Serializable
data class JUnitSummary(
    val numTests: Int,
    val numFailures: Int,
    val numErrors: Int,
    val numSkipped: Int,
    val ellapsed: Float,
    val numMandatoryOK: Int,
    val numMandatoryNOK: Int
)

@Serializable
data class SubmissionsResponse(
    val projectGroup: ProjectGroup,
    val lastSubmission: Submission,
    val numSubmissions: Int = 0,
    // TODO: Remove this
    val allSubmissions: List<Submission> = listOf()
) : DPData

data class AssignmentSubmissions(
    val assignmentId: String,
    val submissionsResponse: List<SubmissionsResponse>
) : DPData

data class GroupSubmissions(
    val assignmentId: String,
    val groupId: Int,
    val allSubmissions: List<Submission>
) : DPData

@Serializable
data class FullBuildReport(
    var numSubmissions: Long? = null,
    var submission: Submission? = null,
    var error: String? = null,
    var summary: List<SubmissionReport>? = null,
    var structureErrors: List<String>? = null,
    var buildReport: BuildReport? = null
) : DPData

@Serializable
data class SubmissionReport(
    val reportKey: String,
    val reportValue: String,
    val reportProgress: Int? = null,
    val reportGoal: Int? = null
)

@Serializable
data class BuildReport(
    val compilationErrors: List<String>? = null,
    val checkstyleErrors: List<String>? = null,
    val junitSummaryStudent: String? = null,
    val junitErrorsStudent: String? = null,
    val junitSummaryTeacher: String? = null,
    val junitSummaryTeacherExtraDescription: String? = null,
    val junitErrorsTeacher: String? = null
)

@Serializable
data class StudentHistory(
    val history: List<StudentHistoryEntry>
)

@Serializable
data class StudentHistoryEntry(
    val assignment: Assignment,
    val sortedSubmissions: List<Submission>
) : DPData

@Serializable
data class StudentListResponse(val value: String, val text: String)