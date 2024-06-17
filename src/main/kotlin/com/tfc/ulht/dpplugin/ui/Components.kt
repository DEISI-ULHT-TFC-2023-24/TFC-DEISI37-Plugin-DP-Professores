package com.tfc.ulht.dpplugin.ui

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UI
import com.tfc.ulht.dpplugin.State
import com.tfc.ulht.dpplugin.dplib.*
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.font.TextAttribute
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.Box.Filler
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.Document

enum class FilterType {
    NUMBER,
    TEXT,
    BOOLEAN,
}

fun <T : DPComponent> stringComparator(argsFun: (T) -> String?): Comparator<T> =
    Comparator { arg1, arg2 ->
        val strings = Pair(argsFun(arg1), argsFun(arg2))

        strings.first?.compareTo(strings.second ?: "") ?: 0
    }

fun <T : DPComponent> intComparator(argsFun: (T) -> Int?): Comparator<T> =
    Comparator { arg1, arg2 ->
        val ints = Pair(argsFun(arg1), argsFun(arg2))

        (ints.first ?: 0) - (ints.second ?: 0)
    }

class DashboardItemComponent(id: Int, text: String, icon: Icon?, listener: (Int) -> Unit) : JLabel(text, icon, LEFT) {
    init {
        this.foreground = JBColor.BLUE

        this.font = JBFont.h2().deriveFont(JBFont.h2().attributes.toMutableMap().apply {
            this[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
        })

        this.cursor = Cursor(Cursor.HAND_CURSOR)

        this.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) = listener(id)

            override fun mousePressed(e: MouseEvent?) {}

            override fun mouseReleased(e: MouseEvent?) {}

            override fun mouseEntered(e: MouseEvent?) {}

            override fun mouseExited(e: MouseEvent?) {}
        })
    }
}

abstract class DPComponent(val padding: Int = 0) : JComponent() {

    private val cols = mutableSetOf<String>()
    private val endCols = mutableSetOf<String>()
    private val bindings = mutableMapOf<String, Component?>()
    private val colSorters = mutableMapOf<String, Comparator<DPComponent>>()
    private val colFilters = mutableMapOf<String, Pair<FilterType, (Any?) -> Boolean>>()

    private var endFiller: Filler? = null

    init {
        this.layout = BoxLayout(this, BoxLayout.X_AXIS)
        this.border = JBUI.Borders.empty(2, 0)
        this.alignmentX = 0.0f
    }

    protected fun initCols(cols: List<String>) = cols.forEach {
        this.cols.add(it)
        bindings.putIfAbsent(it, null)
    }

    protected fun initEndCols(cols: List<String>) {
        endCols.addAll(cols)
    }

    protected fun initColSorters(sorters: Map<String, Comparator<DPComponent>>) {
        colSorters.putAll(sorters)
    }

    protected fun initColFilters(sorters: Map<String, Pair<FilterType, (Any?) -> Boolean>>) {
        colFilters.putAll(sorters)
    }

    fun getBindings(): Map<String, Component?> = bindings
    fun getCols(): Set<String> = cols
    fun getEndCols(): Set<String> = endCols
    fun getColSorters(): Map<String, Comparator<DPComponent>> = colSorters
    fun getColFilters(): Map<String, Pair<FilterType, (Any?) -> Boolean>> = colFilters

    protected fun addComponent(key: String, component: Component) {
        bindings.putIfAbsent(key, component)

        add(component)
    }

    // protected fun addComponentEnd(component: Component) { add(component, -1) }

    fun updateFillers(colStartPositions: Map<String, Int>) {
        components.filter { it is Filler && it != endFiller }.forEach { this.remove(it) }

        val componentList = components.filter { it !is Filler }

        val indexedStartPositions = colStartPositions.map {
            val index = componentList.indexOf(bindings[it.key])

            Pair(index, it.value)
        }.filter { it.first > 0 }.sortedBy { it.first }

        indexedStartPositions.forEachIndexed { i, it ->
            val (index, start) = it

            val fillerWidth = componentList[index - 1].let { comp ->
                val endCoord = (if (i == 0) 0 else indexedStartPositions[i - 1].second) + comp.preferredSize.width
                start - endCoord
            }

            val realIndex = components.indexOf(componentList[index])

            add(Box.createRigidArea(Dimension(fillerWidth, 0)), realIndex)
        }

        if (endCols.isNotEmpty() && endFiller == null) {
            Box.createHorizontalGlue().let {
                endFiller = it as Filler?
                add(it, components.indexOf(bindings[endCols.first()]))
            }
        }
    }
}

interface SearchableComponent {
    fun match(queries: List<String>): Boolean
}

class StudentComponent(private val student: StudentListResponse) : DPComponent() {
    private val nameLabel: JLabel = CustomLabel(student.text + ": ")
    private val idLabel: JLabel = JLabel(student.value)

    init {
        initCols(
            listOf(
                "Name",
                "ID"
            )
        )

        this.addComponent("Name", nameLabel)
        this.addComponent("ID", idLabel)
        this.add(Box.createHorizontalGlue())
    }

    fun addOnClickListener(callback: (StudentListResponse) -> Unit) {
        this.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) = callback(student)

            override fun mousePressed(e: MouseEvent?) {}

            override fun mouseReleased(e: MouseEvent?) {}

            override fun mouseEntered(e: MouseEvent?) {
                this@StudentComponent.background = Color(53, 132, 228)
            }

            override fun mouseExited(e: MouseEvent?) {
                this@StudentComponent.background = null
            }
        })
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)

        this.background?.let {
            g?.color = it
            (g as Graphics2D?)?.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g?.fillRoundRect(0, 0, this.width, this.height - 2, 5, 3)
        }
    }
}

class AssignmentComponent(val assignment: Assignment) : DPComponent(padding = 10), SearchableComponent {
    private val idLabel: JLabel
    private val submissionsLabel: JLabel
    private val activeCheckbox: JCheckBox

    init {
        initCols(
            listOf(
                "ID",
                "Due",
                "Sub. nº",
                "Active",
                "Submissions"
            )
        )

        initEndCols(
            listOf(
                "Submissions"
            )
        )

        @Suppress("UNCHECKED_CAST")
        initColSorters(
            mapOf(
                Pair(
                    "ID",
                    stringComparator<AssignmentComponent> {
                        it.assignment.id
                    } as Comparator<DPComponent>
                )
            )
        )

        initColFilters(
            mapOf(
                Pair(
                    "ID",
                    Pair(
                        FilterType.TEXT
                    ) { arg -> assignment.id.contains((arg as String?) ?: "") }
                ),
                Pair(
                    "TestFilter",
                    Pair(
                        FilterType.BOOLEAN,
                    ) { arg -> (arg as Boolean?) ?: true }
                ),
            )
        )

        val idHolder = JPanel().apply {
            this.layout = BoxLayout(this, BoxLayout.X_AXIS)
        }

        idLabel = CustomLabel(assignment.id)
        idHolder.add(idLabel)

        this.addComponent("ID", idHolder)

        assignment.dueDate?.let {
            this.addComponent("Due", JLabel(it))
        }

        this.addComponent("Sub. nº", JLabel(assignment.numSubmissions.toString()))

        activeCheckbox = JCheckBox().apply {
            isSelected = assignment.active
            action = object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent?) {
                    this@apply.isSelected = assignment.active

                    State.client.toggleAssignmentState(assignment.id) {
                        this@apply.isSelected = if (it == true) !assignment.active else assignment.active
                        assignment.active = this@apply.isSelected
                    }
                }
            }
        }
        this.addComponent("Active", activeCheckbox)

        submissionsLabel = JLabel("Submissions").apply {
            this.foreground = JBColor.BLUE

            val style = this.font.attributes.toMutableMap()
            style[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
            this.font = this.font.deriveFont(style)

            this.cursor = Cursor(Cursor.HAND_CURSOR)
        }

        this.addComponent("Submissions", submissionsLabel)
    }

    fun addSubmissionClickListener(listener: (MouseEvent?) -> Unit) =
        submissionsLabel.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) = listener(e)

            override fun mousePressed(e: MouseEvent?) {}

            override fun mouseReleased(e: MouseEvent?) {}

            override fun mouseEntered(e: MouseEvent?) {}

            override fun mouseExited(e: MouseEvent?) {}
        })

    override fun match(queries: List<String>): Boolean {
        if (queries.isEmpty()) return false

        for (query in queries) {
            if (!assignment.id.lowercase().contains(query.lowercase()) && !assignment.name.lowercase()
                    .contains(query.lowercase())
            ) {
                return false
            }
        }

        return true
    }
}

class TestResultsComponent(results: JUnitSummary?) : JLabel() {
    init {
        if (results == null) {
            this.text = "Structure"
            this.foreground = JBColor.RED
        } else {
            val progress = results.numTests - (results.numErrors + results.numFailures)
            this.text = progress.toString() + "/" + results.numTests

            (if (progress >= results.numTests) JBColor.GREEN else JBColor.RED).run {
                this@TestResultsComponent.foreground = this
            }
        }
    }
}

class GroupSubmissionsComponent(private val submissions: SubmissionsResponse) : DPComponent(padding = 10),
    SearchableComponent {

    private val idLabel: JLabel
    private val allSubmissions: NumberBox
    private val submissionDownloadLabel: JLabel
    private val buildReportLabel: JLabel

    init {
        initCols(
            listOf(
                "ID",
                "Last submission",
                "Last status",
                "Teacher Tests",
                "Student Tests",
                "Hidden Tests",
                "Build Report",
                "Download",

                )
        )

        initEndCols(
            listOf(
                "Build Report",
                "Download"
            )
        )

        @Suppress("UNCHECKED_CAST")
        initColSorters(
            mapOf(
                Pair(
                    "ID",
                    intComparator<GroupSubmissionsComponent> {
                        it.submissions.projectGroup.authors.first().id
                    } as Comparator<DPComponent>
                )
            )
        )

        idLabel = CustomLabel(submissions.projectGroup.authors.joinToString(separator = ",") { "${it.id}-${it.name}" })
        this.addComponent("ID", idLabel)

        this.addComponent("Last submission", JLabel(submissions.lastSubmission.submissionDate))

        val statusPanel = JPanel().apply {
            this.layout = BoxLayout(this, BoxLayout.X_AXIS)
        }

        statusPanel.add(JLabel(submissions.lastSubmission.status))

        allSubmissions = NumberBox(submissions.numSubmissions)
        statusPanel.add(Box.createRigidArea(Dimension(5, 0)))
        statusPanel.add(allSubmissions)

        this.addComponent("Last status", statusPanel)

        this.addComponent("Teacher Tests", TestResultsComponent(submissions.lastSubmission.teacherTests))

        submissions.lastSubmission.studentTests?.let {
            this.addComponent("Student Tests", TestResultsComponent(it))
        }

        submissions.lastSubmission.hiddenTests?.let {
            this.addComponent("Hidden Tests", TestResultsComponent(it))
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

        this.addComponent("Build Report", buildReportLabel)
        this.addComponent("Download", submissionDownloadLabel)
    }

    fun addSubmissionDownloadClickListener(listener: (MouseEvent?) -> Unit) =
        submissionDownloadLabel.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) = listener(e)

            override fun mousePressed(e: MouseEvent?) {}

            override fun mouseReleased(e: MouseEvent?) {}

            override fun mouseEntered(e: MouseEvent?) {}

            override fun mouseExited(e: MouseEvent?) {}
        })

    fun addBuildReportClickListener(listener: (MouseEvent?) -> Unit) =
        buildReportLabel.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) = listener(e)

            override fun mousePressed(e: MouseEvent?) {}

            override fun mouseReleased(e: MouseEvent?) {}

            override fun mouseEntered(e: MouseEvent?) {}

            override fun mouseExited(e: MouseEvent?) {}
        })

    fun addAllSubmissionsClickListener(listener: () -> Unit) {
        allSubmissions.clickListener = listener
    }

    override fun match(queries: List<String>): Boolean {
        if (queries.isEmpty()) return false

        for (query in queries) {
            if (submissions.projectGroup.authors.none {
                    it.name.lowercase().contains(query.lowercase()) || it.id.toString().contains(query)
                }) {
                return false
            }
        }

        return true
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

class SubmissionComponent(var submission: Submission) : DPComponent(padding = 10), SearchableComponent {
    private val idHolder: JPanel = JPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.X_AXIS)
    }

    private val idLabel: JLabel
    private val submissionDownloadLabel: JLabel
    private val markAsFinalLabel: JLabel
    private val buildReportLabel: JLabel

    init {
        initCols(
            listOf(
                "ID",
                "Teacher Tests",
                "Student Tests",
                "Hidden Tests",
                "Build Report",
                "Mark as Final",
                "Download"
            )
        )

        initEndCols(
            listOf(
                "Build Report",
                "Mark as Final",
                "Download"
            )
        )

        val date = submission.getParsedDate()

        idLabel = CustomLabel("${submission.id}: ${date.format(DateTimeFormatter.ofPattern("dd/LLL HH:mm"))}")
        idHolder.add(idLabel)

        if (submission.markedAsFinal) idHolder.add(JLabel(ImageIcon(SubmissionComponent::class.java.getResource("/icons/final.png"))))

        this.addComponent("ID", idHolder)

        submissionDownloadLabel = JLabel("Download").apply {
            this.foreground = JBColor.BLUE

            val style = this.font.attributes.toMutableMap()
            style[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
            this.font = this.font.deriveFont(style)

            this.cursor = Cursor(Cursor.HAND_CURSOR)
        }

        markAsFinalLabel = JLabel("Mark as Final").apply {
            this.foreground = if (!submission.markedAsFinal) JBColor.BLUE else JBColor.DARK_GRAY

            val style = this.font.attributes.toMutableMap()
            style[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
            this.font = this.font.deriveFont(style)

            this.cursor = if (!submission.markedAsFinal) Cursor(Cursor.HAND_CURSOR) else null
        }

        this.addComponent("Teacher Tests", TestResultsComponent(submission.teacherTests))

        submission.studentTests?.let {
            this.addComponent("Student Tests", TestResultsComponent(it))
        }

        submission.hiddenTests?.let {
            this.addComponent("Hidden Tests", TestResultsComponent(it))
        }

        buildReportLabel = JLabel("Build Report").apply {
            this.foreground = JBColor.BLUE

            this.font = this.font.deriveFont(this.font.attributes.toMutableMap().apply {
                this[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
            })

            this.cursor = Cursor(Cursor.HAND_CURSOR)
        }

        this.addComponent("Build Report", buildReportLabel)
        this.addComponent("Mark as Final", markAsFinalLabel)
        this.addComponent("Download", submissionDownloadLabel)
    }

    fun addSubmissionDownloadClickListener(listener: (MouseEvent?) -> Unit) =
        submissionDownloadLabel.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) = listener(e)

            override fun mousePressed(e: MouseEvent?) {}

            override fun mouseReleased(e: MouseEvent?) {}

            override fun mouseEntered(e: MouseEvent?) {}

            override fun mouseExited(e: MouseEvent?) {}
        })

    fun addMarkAsFinalClickListener(listener: (MouseEvent?) -> Unit) =
        markAsFinalLabel.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) {
                if (!submission.markedAsFinal) {
                    listener(e)
                }
            }

            override fun mousePressed(e: MouseEvent?) {}

            override fun mouseReleased(e: MouseEvent?) {}

            override fun mouseEntered(e: MouseEvent?) {}

            override fun mouseExited(e: MouseEvent?) {}
        })

    fun addBuildReportClickListener(listener: (MouseEvent?) -> Unit) =
        buildReportLabel.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) = listener(e)

            override fun mousePressed(e: MouseEvent?) {}

            override fun mouseReleased(e: MouseEvent?) {}

            override fun mouseEntered(e: MouseEvent?) {}

            override fun mouseExited(e: MouseEvent?) {}
        })

    fun markedAsFinal() {
        this.idHolder.add(JLabel(ImageIcon(SubmissionComponent::class.java.getResource("/icons/final.png"))))

        this.submission = Submission(
            submission.id,
            submission.statusDate,
            submission.status,
            submission.statusDate,
            submission.testResults,
            submission.teacherTests,
            submission.studentTests,
            submission.hiddenTests,
            true,
            submission.group
        )

        this.markAsFinalLabel.apply {
            this.foreground = JBColor.DARK_GRAY

            this.cursor = null
        }
    }

    fun unmarkFinal() {
        if (idHolder.components.size > 1)
            idHolder.remove(idHolder.components.last())

        this.submission = Submission(
            submission.id,
            submission.statusDate,
            submission.status,
            submission.statusDate,
            submission.testResults,
            submission.teacherTests,
            submission.studentTests,
            submission.hiddenTests,
            false,
            submission.group
        )

        this.markAsFinalLabel.apply {
            this.foreground = JBColor.BLUE

            this.cursor = null
        }
    }

    override fun match(queries: List<String>): Boolean {
        if (queries.isEmpty()) return false

        for (query in queries) {
            if (!submission.id.toString().lowercase().contains(query.lowercase())) {
                return false
            }
        }

        return true
    }
}

class SubmissionReportComponent(report: SubmissionReport) : DPComponent() {
    init {
        initCols(
            listOf("Report", "Value")
        )

        initEndCols(
            listOf("Value")
        )

        this.addComponent("Report", JLabel(report.reportKey))
        this.addComponent("Value", JLabel(report.reportValue))
    }
}

class BuildReportComponent(report: BuildReport) : DPComponent() {
    init {
        initCols(
            listOf("Summary")
        )

        this.addComponent("Summary", JLabel(report.junitSummaryTeacher))
    }
}

class SearchBar(hint: String, description: String) : JComponent() {
    private val searchBar = JBTextField().apply {
        this.emptyText.text = hint
        this.maximumSize = Dimension(this.maximumSize.width, this.preferredSize.height)
    }

    val text: String
        get() = searchBar.text

    init {
        this.layout = BorderLayout()
        this.alignmentX = 0F

        val panel = UI.PanelFactory.panel(searchBar).withComment(description).createPanel()
        this.add(panel)

        this.maximumSize = Dimension(this.maximumSize.width, panel.preferredSize.height)
    }

    fun addActionListener(listener: (ActionEvent) -> Unit) = searchBar.addActionListener(listener)
    fun addDocumentListener(listener: (Document) -> Unit) =
        searchBar.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = listener(searchBar.document)

            override fun removeUpdate(e: DocumentEvent?) = listener(searchBar.document)

            override fun changedUpdate(e: DocumentEvent?) = listener(searchBar.document)
        })
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
                                    comment("<icon src='AllIcons.General.InspectionsOK'>&nbsp;")
                                } else {
                                    comment("<icon src='AllIcons.Actions.Suspend'>&nbsp;")
                                }
                            }

                            "C" -> {
                                label("Compilation")
                                if (summary.reportValue == "OK") {
                                    comment("<icon src='AllIcons.General.InspectionsOK'>&nbsp;")
                                } else {
                                    comment("<icon src='AllIcons.Actions.Suspend'>&nbsp;")
                                }
                            }

                            "TT" -> {
                                label("Teacher Unit Tests")
                                if (summary.reportValue == "OK") {
                                    comment("<icon src='AllIcons.General.InspectionsOK'>&nbsp;<b>${summary.reportProgress}/${summary.reportGoal}</b>")
                                } else { //OK NOK ..?.. NULL??...CHECK THIS
                                    comment("<icon src='AllIcons.Actions.Suspend'>&nbsp;<b>${summary.reportProgress}/${summary.reportGoal}</b>")
                                }
                            }

                            "CS" -> {
                                label("Code Quality")
                                if (summary.reportValue == "OK") {
                                    comment("<icon src='AllIcons.General.InspectionsOK'>&nbsp;")
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
