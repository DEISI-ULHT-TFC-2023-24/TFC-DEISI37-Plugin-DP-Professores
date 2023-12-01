package com.tfc.ulht.dpplugin.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.tfc.ulht.dpplugin.dplib.*
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.font.TextAttribute
import javax.swing.*
import javax.swing.Box.Filler

class DashboardItemComponent(id: Int, text: String, icon: Icon?, listener: (Int) -> Unit) : JLabel(text, icon, LEFT) {
    init {
        this.foreground = JBColor.BLUE

        this.font = JBFont.h2().deriveFont(JBFont.h2().attributes.toMutableMap().apply {
            this[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
        })

        this.cursor = Cursor(Cursor.HAND_CURSOR)

        this.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) = listener(id)

            override fun mousePressed(e: MouseEvent?) {  }

            override fun mouseReleased(e: MouseEvent?) {  }

            override fun mouseEntered(e: MouseEvent?) {  }

            override fun mouseExited(e: MouseEvent?) {  }
        })
    }
}

open class DPComponent : JComponent() {
    val fillers: MutableList<Component> = mutableListOf()

    init {
        this.layout = BoxLayout(this, BoxLayout.X_AXIS)
        this.border = JBUI.Borders.empty(2, 0)
        this.alignmentX = 0.0f

        this.add(Box.createHorizontalGlue())
    }

    fun addComponent(component: Component) {
        if (component is Filler) {
            fillers.add(component)
        }

        for (i in components.indices) {
            if (components[i] is Filler && !fillers.contains(components[i])) {
                add(component, i)
                return
            }
        }

        add(component, -1)
    }

    protected fun addComponentEnd(component: Component) = add(component, -1)
}

class AssignmentComponent(val assignment: Assignment) : DPComponent() {
    val idLabel: JLabel
    val submissionsLabel: JLabel

    init {
        idLabel = CustomLabel(assignment.id)
        this.addComponent(idLabel)

        assignment.dueDate?.let {
            this.addComponent(Box.createRigidArea(Dimension(10, 0)))
            this.addComponent(LabelWithDescription(it, "due"))
        }

        this.addComponent(Box.createRigidArea(Dimension(10, 0)))
        this.addComponent(LabelWithDescription(assignment.numSubmissions.toString(), "submissions"))

        submissionsLabel = JLabel("Submissions").apply {
            this.foreground = JBColor.BLUE

            val style = this.font.attributes.toMutableMap()
            style[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
            this.font = this.font.deriveFont(style)

            this.cursor = Cursor(Cursor.HAND_CURSOR)
        }

        this.addComponentEnd(submissionsLabel)
    }

    fun addSubmissionClickListener(listener: (MouseEvent?) -> Unit) = submissionsLabel.addMouseListener(object : MouseListener {
        override fun mouseClicked(e: MouseEvent?) = listener(e)

        override fun mousePressed(e: MouseEvent?) {  }

        override fun mouseReleased(e: MouseEvent?) {  }

        override fun mouseEntered(e: MouseEvent?) {  }

        override fun mouseExited(e: MouseEvent?) {  }
    })
}

class TestResultsComponent(results: JUnitSummary, description: String) : LabelWithDescription("", description) {
    init {
        val progress = results.numTests - (results.numErrors + results.numFailures)
        this.contentLabel.text = progress.toString() + "/" + results.numTests

        (if (progress >= results.numTests) JBColor.GREEN else JBColor.RED).run {
            this@TestResultsComponent.contentLabel.foreground = this
        }
    }
}

class GroupSubmissionsComponent(submissions: SubmissionsResponse) : DPComponent() {
    private val idLabel: JLabel
    private val allSubmissions: NumberBox
    private val submissionDownloadLabel: JLabel
    private val buildReportLabel: JLabel

    init {
        idLabel = CustomLabel(submissions.projectGroup.authors.joinToString(separator = ",") { "${it.id}-${it.name}" })
        this.addComponent(idLabel)

        this.addComponent(Box.createRigidArea(Dimension(10, 0)))
        this.addComponent(LabelWithDescription(submissions.allSubmissions.first().submissionDate, "last submission"))

        this.addComponent(Box.createRigidArea(Dimension(10, 0)))
        this.addComponent(LabelWithDescription(submissions.allSubmissions.first().status, "last status"))

        this.addComponent(Box.createRigidArea(Dimension(10, 0)))
        allSubmissions = NumberBox(submissions.allSubmissions.size)
        this.addComponent(allSubmissions)

        submissions.allSubmissions.first().teacherTests?.let {
            this.addComponent(Box.createRigidArea(Dimension(10, 0)))
            this.addComponent(TestResultsComponent(it, "Teacher Tests"))
        }

        submissions.allSubmissions.first().studentTests?.let {
            this.addComponent(Box.createRigidArea(Dimension(10, 0)))
            this.addComponent(TestResultsComponent(it, "Teacher Tests"))
        }

        submissions.allSubmissions.first().hiddenTests?.let {
            this.addComponent(Box.createRigidArea(Dimension(10, 0)))
            this.addComponent(TestResultsComponent(it, "Teacher Tests"))
        }

        buildReportLabel = JLabel("Build Report").apply {
            this.foreground = JBColor.BLUE

            this.font = this.font.deriveFont(this.font.attributes.toMutableMap().apply {
                this[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
            })

            this.cursor = Cursor(Cursor.HAND_CURSOR)
        }
        
        submissionDownloadLabel = JLabel("Download").apply {
            this.foreground = JBColor.BLUE

            this.font = this.font.deriveFont(this.font.attributes.toMutableMap().apply {
                this[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
            })

            this.cursor = Cursor(Cursor.HAND_CURSOR)
        }
        
        this.addComponentEnd(buildReportLabel)
        this.addComponentEnd(Box.createRigidArea(Dimension(10, 0)))
        this.addComponentEnd(submissionDownloadLabel)
    }

    fun addSubmissionDownloadClickListener(listener: (MouseEvent?) -> Unit) = submissionDownloadLabel.addMouseListener(object : MouseListener {
        override fun mouseClicked(e: MouseEvent?) = listener(e)

        override fun mousePressed(e: MouseEvent?) {  }

        override fun mouseReleased(e: MouseEvent?) {  }

        override fun mouseEntered(e: MouseEvent?) {  }

        override fun mouseExited(e: MouseEvent?) {  }
    })

    fun addBuildReportClickListener(listener: (MouseEvent?) -> Unit) = buildReportLabel.addMouseListener(object : MouseListener {
        override fun mouseClicked(e: MouseEvent?) = listener(e)

        override fun mousePressed(e: MouseEvent?) {  }

        override fun mouseReleased(e: MouseEvent?) {  }

        override fun mouseEntered(e: MouseEvent?) {  }

        override fun mouseExited(e: MouseEvent?) {  }
    })

    fun addAllSubmissionsClickListener(listener: () -> Unit) {
        allSubmissions.clickListener = listener
    }
}

open class LabelWithDescription(text: String, description: String) : JComponent() {
    protected val descriptionLabel: JLabel
    protected val contentLabel: JLabel

    init {
        this.layout = BoxLayout(this, BoxLayout.X_AXIS)

        descriptionLabel = JLabel("$description:").apply {
            this.font = JBFont.small().asBold()
            this.alignmentY = 0.4f
        }

        this.add(descriptionLabel)

        contentLabel = JLabel(text)
        this.add(contentLabel)
    }
}

class NumberBox(number: Int) : JComponent() {
    private val numString = number.toString()
    var mouseOver = false

    var clickListener: (() -> Unit)? = null

    init {
        this.minimumSize = Dimension(5, 10)
        cursor = Cursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent) {
                clickListener?.invoke()
            }
            override fun mousePressed(e: MouseEvent) {}
            override fun mouseReleased(e: MouseEvent) {}
            override fun mouseEntered(e: MouseEvent) {
                mouseOver = true
                repaint()
            }

            override fun mouseExited(e: MouseEvent) {
                mouseOver = false
                repaint()
            }
        })
    }

    override fun getMaximumSize(): Dimension {
        return Dimension(25, 25)
    }

    override fun getPreferredSize(): Dimension {
        return maximumSize
    }

    public override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.color = JBColor.namedColor("Separator.separatorColor")

        g2.drawRoundRect(0, 0, width - 1, height - 1, width / 4, height / 4)

        if (mouseOver) {
            g2.color = Color(JBColor.namedColor("Panel.background").rgb and (0xFF shl 4 * 8), true)
            g2.fillRoundRect(1, 1, width - 2, height - 2, width / 4, height / 4)
            g2.color = JBColor.BLACK
        }

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        val rec = g.getFont().getStringBounds(numString, g2.fontRenderContext)
        g.drawString(numString, (width / 2 - rec.width / 2).toInt() + 1, (height / 2 - rec.y / 2).toInt())

        super.paintComponent(g)
    }
}

class SubmissionComponent(val submission: Submission) : DPComponent() {
    private val idLabel: JLabel
    private val submissionDownloadLabel: JLabel
    private val buildReportLabel: JLabel

    init {
        val idHolder = JPanel().apply {
            this.layout = BoxLayout(this, BoxLayout.X_AXIS)
        }

        idLabel = CustomLabel("${submission.id}: ${submission.submissionDate}")
        idHolder.add(idLabel)

        if (submission.markedAsFinal) idHolder.add(JLabel(AllIcons.Nodes.Function))

        this.addComponent(idHolder)

        submissionDownloadLabel = JLabel("Download").apply {
            this.foreground = JBColor.BLUE

            val style = this.font.attributes.toMutableMap()
            style[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
            this.font = this.font.deriveFont(style)

            this.cursor = Cursor(Cursor.HAND_CURSOR)
        }

        submission.teacherTests?.let {
            this.addComponent(Box.createRigidArea(Dimension(10, 0)))
            this.addComponent(TestResultsComponent(it, "Teacher Tests"))
        }

        submission.studentTests?.let {
            this.addComponent(Box.createRigidArea(Dimension(10, 0)))
            this.addComponent(TestResultsComponent(it, "Teacher Tests"))
        }

        submission.hiddenTests?.let {
            this.addComponent(Box.createRigidArea(Dimension(10, 0)))
            this.addComponent(TestResultsComponent(it, "Teacher Tests"))
        }

        buildReportLabel = JLabel("Build Report").apply {
            this.foreground = JBColor.BLUE

            this.font = this.font.deriveFont(this.font.attributes.toMutableMap().apply {
                this[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
            })

            this.cursor = Cursor(Cursor.HAND_CURSOR)
        }

        this.addComponentEnd(buildReportLabel)
        this.addComponentEnd(Box.createRigidArea(Dimension(10, 0)))
        this.addComponentEnd(submissionDownloadLabel)
    }

    fun addSubmissionDownloadClickListener(listener: (MouseEvent?) -> Unit) = submissionDownloadLabel.addMouseListener(object : MouseListener {
        override fun mouseClicked(e: MouseEvent?) = listener(e)

        override fun mousePressed(e: MouseEvent?) {  }

        override fun mouseReleased(e: MouseEvent?) {  }

        override fun mouseEntered(e: MouseEvent?) {  }

        override fun mouseExited(e: MouseEvent?) {  }
    })

    fun addBuildReportClickListener(listener: (MouseEvent?) -> Unit) = buildReportLabel.addMouseListener(object : MouseListener {
        override fun mouseClicked(e: MouseEvent?) = listener(e)

        override fun mousePressed(e: MouseEvent?) {  }

        override fun mouseReleased(e: MouseEvent?) {  }

        override fun mouseEntered(e: MouseEvent?) {  }

        override fun mouseExited(e: MouseEvent?) {  }
    })
}

class SubmissionReportComponent(report: SubmissionReport) : DPComponent() {
    init {
        this.addComponent(JLabel(report.reportKey))
        this.addComponentEnd(JLabel(report.reportValue))
    }
}

class BuildReportComponent(report: BuildReport) : DPComponent() {
    init {
        this.addComponent(JLabel(report.junitSummaryTeacher))
    }
}

class UIBuildReport {
    @Suppress("DialogTitleCapitalization")
    fun buildComponents(buildReport: FullBuildReport, submissionNumber: Int?): JComponent {
        val panel = panel {
            row {
                label("Build Report").bold()
                submissionNumber?.let {
                    comment("Submission: $submissionNumber")
                }
            }
            /*row {
                comment("Assignment: ${buildReport.assignment!!.name}")
            }*/
            row {
                val fullDate = buildReport.submission!!.submissionDate.split(".")[0].split("T")
                comment("Submitted: ${fullDate[0]} | ${fullDate[1]}")
            }
            buildReport.summary?.let { self ->
                self.forEach { summary ->
                    row {
                        when (summary.reportKey) {
                            "PS" -> {
                                label("Project Structure")
                                if (summary.reportValue == "OK") {
                                    comment("<icon src='AllIcons.Actions.Commit'>&nbsp;")
                                } else {
                                    comment("<icon src='AllIcons.Actions.Suspend'>&nbsp;")
                                }
                            }

                            "C" -> {
                                label("Compilation")
                                if (summary.reportValue == "OK") {
                                    comment("<icon src='AllIcons.Actions.Commit'>&nbsp;")
                                } else {
                                    comment("<icon src='AllIcons.Actions.Suspend'>&nbsp;")
                                }
                            }

                            "TT" -> {
                                label("Teacher Unit Tests")
                                if (summary.reportValue == "OK") {
                                    comment("<icon src='AllIcons.Actions.Commit'>&nbsp;<b>${summary.reportProgress}/${summary.reportGoal}</b>")
                                } else { //OK NOK ..?.. NULL??...CHECK THIS
                                    comment("<icon src='AllIcons.Actions.Suspend'>&nbsp;<b>${summary.reportProgress}/${summary.reportGoal}</b>")
                                }
                            }

                            "CS" -> {
                                label("Code Quality")
                                if (summary.reportValue == "OK") {
                                    comment("<icon src='AllIcons.Actions.Commit'>&nbsp;")
                                } else {
                                    comment("<icon src='AllIcons.Actions.Suspend'>&nbsp;")
                                }
                            }
                        }
                    }.layout(RowLayout.PARENT_GRID)
                }
            }
            //PROJECT STRUCTURE
            buildReport.structureErrors?.let { psErr ->
                collapsibleGroup("Project Structure Errors (${psErr.size})") {
                    psErr.forEachIndexed { index, error ->
                        group("Error $index") {
                            row {
                                text(error)
                            }
                        }
                    }
                }
            }
            //COMPILATION
            buildReport.buildReport?.compilationErrors?.let { cErr ->
                collapsibleGroup(
                    "Compilation Errors (${
                        cErr.filter { it.contains("[TEST]") }.size
                    })"
                ) {
                    cErr.joinToString("<br>").trim().split("[TEST]").forEach { error ->
                        if (error.isNotBlank()) {
                            group {
                                row {
                                    text(error)
                                }
                            }
                        }
                    }
                }
            }
            //CHECKSTYLE
            buildReport.buildReport?.checkstyleErrors?.let { csErr ->
                collapsibleGroup("Code Quality Errors (${csErr.size})") {
                    csErr.forEach { error ->
                        group {
                            row {
                                text(error)
                            }
                        }
                    }
                }
            }
            //TEACHER UNIT TESTS
            buildReport.buildReport?.junitErrorsTeacher?.let { ttErr ->
                collapsibleGroup("Teacher Units Tests Errors") {
                    ttErr.split("ERROR: |FAILURE:".toRegex()).forEach { error ->
                        if (error.isNotBlank()) {
                            group {
                                row {
                                    text(
                                        error.replace("<", "&lt;").replace("\n", "<br>")
                                    )
                                }
                            }
                        }
                    }
                }
            }
            //SUMMARY
            buildReport.buildReport?.junitSummaryTeacher?.let { summary ->
                group {
                    row {
                        text("$summary<br>")
                    }
                }
            }


        }

        panel.alignmentX = Component.LEFT_ALIGNMENT

        return panel
    }
}
