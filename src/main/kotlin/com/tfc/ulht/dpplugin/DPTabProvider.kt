package com.tfc.ulht.dpplugin

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.WindowManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.tfc.ulht.dpplugin.dplib.*
import com.tfc.ulht.dpplugin.ui.*
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.*
import java.awt.font.TextAttribute
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.event.DocumentEvent

class DPTabProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file is com.tfc.ulht.dpplugin.VirtualFile

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        DPTabHolder(project, (file as com.tfc.ulht.dpplugin.VirtualFile).data)

    override fun getEditorTypeId(): String = "dp-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

open class DPTab(addReloadButton: Boolean = false) :
    JScrollPane(VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED) {
    lateinit var holder: DPTabHolder
    private var parent: DPTab? = null
    var next: DPTab? = null

    val toolPanel: JPanel

    private val backButton: JButton
    private val forwardButton: JButton
    private val assignmentsButton: JButton
    private val dashboardButton: JButton
    val reloadButton: JButton?

    var reloadFunction: (() -> Unit)? = null
    var reloadCheckFunction: (() -> Boolean)? = null

    private var hidden = false

    val rootPanel = JPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    init {
        this.layout = ScrollPaneLayout()

        this.setViewportView(rootPanel)

        toolPanel = JPanel().apply {
            this.layout = BoxLayout(this, BoxLayout.X_AXIS)
            this.alignmentX = 0F
        }

        backButton = JButton(AllIcons.Actions.Back).apply {
            this.addActionListener {
                navigateBackwards()
            }
        }

        forwardButton = JButton(AllIcons.Actions.Forward).apply {
            this.addActionListener {
                navigateForward()
            }
        }

        assignmentsButton = JButton(AllIcons.Actions.Annotate).apply {
            toolTipText = "Open the assignments menu"
            this.addActionListener {
                navigateToAssignments()
            }
        }

        dashboardButton = JButton(AllIcons.Actions.GroupBy).apply {
            toolTipText = "Open the dashboard"
            this.addActionListener {
                navigateToDashboard()
            }
        }

        updateNavButtons()

        toolPanel.add(backButton)
        toolPanel.add(forwardButton)
        toolPanel.add(assignmentsButton)
        toolPanel.add(dashboardButton)

        rootPanel.add(toolPanel)

        if (addReloadButton) {
            reloadButton = JButton(AllIcons.Actions.Refresh).apply {
                this.isEnabled = false
                this.addActionListener {
                    reloadFunction?.let { fn -> fn() }
                }
            }

            toolPanel.add(reloadButton)
        } else {
            reloadButton = null
        }
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)

        if (hidden) {
            hidden = false
            if (reloadCheckFunction != null) {
                startRefreshThread()
            }
        }
    }

    private fun updateNavButtons() {
        backButton.isEnabled = parent != null
        forwardButton.isEnabled = next != null
    }

    fun navigateForward(tab: DPTab) {
        next = tab
        navigateForward()
    }

    private fun navigateForward() = next?.let {
        it.parent = this

        SwingUtilities.invokeLater {
            holder.panel.removeAll()
            holder.panel.add(it)
            it.updateNavButtons()

            holder.panel.revalidate()
            holder.panel.repaint()
        }
    }

    private fun navigateBackwards() = parent?.let {
        SwingUtilities.invokeLater {
            holder.panel.removeAll()
            holder.panel.add(it)
            it.updateNavButtons()

            holder.panel.revalidate()
            holder.panel.repaint()
        }
    }

    /* private fun navigateHome() = parent?.let {
        SwingUtilities.invokeLater {
            var parentTab = it

            while (parentTab.parent != null) {
                parentTab = parentTab.parent!!
            }

            holder.panel.removeAll()
            holder.panel.add(parentTab)
            parentTab.updateNavButtons()

            holder.panel.revalidate()
            holder.panel.repaint()
        }
    } */

    private fun navigateToAssignments() {
        val loadingPanel = startLoading()

        State.client.getAssignments { assignments ->
            if (assignments == null) {
                JOptionPane.showMessageDialog(
                    WindowManager.getInstance().findVisibleFrame(),
                    "Couldn't load assignments.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )

                stopLoading(loadingPanel.component1(), loadingPanel.component2())
            }

            assignments?.let { data ->
                stopLoading(loadingPanel.component1(), loadingPanel.component2())
                holder.data = data
                navigateForward((holder.getTab(data.first().javaClass.name) as DPTab))
            }
        }
    }

    private fun navigateToDashboard() {
        SwingUtilities.invokeLater {
            navigateForward(holder.getTab(Null::class.java.name) as DPTab)
        }
    }

    fun startRefreshThread() =
        Thread {
            while (this@DPTab.isShowing) {
                Thread.sleep(REFRESH_INTERVAL)

                if (reloadCheckFunction?.let { it() } == true) {
                    SwingUtilities.invokeLater {
                        // last check before enabling the button
                        if (this@DPTab.isShowing) {
                            this@DPTab.reloadButton?.isEnabled = true
                        }
                    }
                }
            }

            this@DPTab.hidden = true
        }.start()
}

open class DPListTab<T : DPComponent>(
    title: String, addReloadButton: Boolean, addSearchBar: Boolean = false,
    private val addFilterSection: Boolean = false, searchBarHint: String = "", searchBarDescription: String = ""
) :
    DPTab(addReloadButton) {

    enum class ColSortState {
        ASC,
        DESC,
        NONE
    }

    class HeaderComponent(
        cols: Set<String>,
        sortableCols: Set<String> = setOf(),
        sortedColState: Pair<String, ColSortState>? = null,
        sortListener: ((String, ColSortState) -> Unit)? = null,
    ) : DPComponent() {

        init {
            initCols(cols.toList())

            cols.forEach {
                this.addComponent(it, JLabel(it).apply {
                    font = JBFont.regular().asBold()

                    if (sortedColState?.first == it) {
                        icon = when (sortedColState.second) {
                            ColSortState.ASC -> AllIcons.Actions.MoveUp
                            ColSortState.DESC -> AllIcons.Actions.MoveDown
                            ColSortState.NONE -> null
                        }
                    }

                    if (sortableCols.contains(it)) {
                        font = font.deriveFont(
                            font.attributes.plus(
                                Pair(
                                    TextAttribute.UNDERLINE,
                                    TextAttribute.UNDERLINE_ON
                                )
                            )
                        )

                        addMouseListener(object : MouseAdapter() {
                            override fun mouseClicked(e: MouseEvent?) {
                                super.mouseClicked(e)

                                sortListener?.let { fn ->
                                    fn(
                                        it,
                                        if (sortedColState?.first == it &&
                                            sortedColState.second == ColSortState.ASC
                                        ) ColSortState.DESC else ColSortState.ASC
                                    )
                                }
                            }
                        })
                    }
                })
            }
        }
    }

    private val allItems: MutableList<T> = mutableListOf()
    private val items: MutableList<T> = mutableListOf()
    private val unfilteredItems: MutableList<T> = mutableListOf()
    val itemsPanel: JPanel = JPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.border = JBUI.Borders.empty(0, 10)
    }

    private val colAssoc: MutableMap<String, MutableList<Component>> = mutableMapOf()
    private var currentColSortState: Pair<String, ColSortState>? = null

    private var header: HeaderComponent? = null

    private var filterSection: JPanel? = null
    private lateinit var filterSectionLabel: JLabel
    private val filterArgs: MutableMap<String, Any?> = mutableMapOf()

    init {
        rootPanel.border = JBUI.Borders.empty(0, 20)

        rootPanel.add(JLabel("<html><h1>$title</h1></html>").apply { alignmentX = 0.0f })

        if (addSearchBar) {
            rootPanel.add(SearchBar(searchBarHint, searchBarDescription).apply {
                this.addActionListener {
                    if (allItems.isNotEmpty() && SearchableComponent::class.java in allItems.first()::class.java.interfaces) {
                        this@DPListTab.clear()

                        for (item in allItems) {
                            if (this.text.isBlank() || (item as SearchableComponent).match(listOf(this.text))) {
                                this@DPListTab.addItem(item)
                            }
                        }

                        redraw()
                    }
                }
            })
        }

        rootPanel.add(itemsPanel)
    }

    private fun createFilterSection(filterableRows: Map<String, DPComponent.ColumnFilter>) {

        filterSection = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = 0.0f
        }

        filterSectionLabel = JLabel("Filters").apply {
            icon = AllIcons.General.ChevronRight

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    val isExpanded = icon == AllIcons.General.ChevronDown

                    icon = if (isExpanded) AllIcons.General.ChevronRight else AllIcons.General.ChevronDown

                    filterSection!!.components.drop(1).forEach {
                        it.isVisible = !isExpanded
                    }

                    filterSection!!.revalidate()
                    filterSection!!.repaint()
                }
            })
        }

        filterSection!!.add(filterSectionLabel)

        for (row in filterableRows.entries) {
            val panel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                isVisible = false
                alignmentX = 0.0f
            }

            panel.add(JLabel(row.key + ": "))

            val component = when (row.value.type) {
                FilterType.NUMBER -> JBIntSpinner(0, 0, Int.MAX_VALUE).apply {
                    addChangeListener {
                        filterArgs[row.key] = number
                    }
                }

                FilterType.TEXT -> JBTextField().apply {
                    this.document.addDocumentListener(object : DocumentAdapter() {
                        override fun textChanged(e: DocumentEvent) {
                            filterArgs[row.key] = this@apply.text
                        }

                    })

                    this.emptyText.setText(row.value.hintText ?: "")
                }

                FilterType.BOOLEAN -> JBCheckBox().apply {
                    addItemListener {
                        filterArgs[row.key] = when (it.stateChange) {
                            ItemEvent.SELECTED -> true
                            ItemEvent.DESELECTED -> false
                            else -> filterArgs[row.key] ?: false
                        }
                    }
                }
            }.apply {
                maximumSize = Dimension(maximumSize.width, preferredSize.height)
                minimumSize = Dimension(preferredSize.width, preferredSize.height)
            }

            panel.add(component)

            filterSection!!.add(panel)
        }

        val applyButton = JButton("Apply").apply {
            isVisible = false

            addActionListener {
                if (unfilteredItems.isEmpty())
                    unfilteredItems.addAll(items)

                clear()

                unfilteredItems.filter { item ->
                    item.getColFilters().map { it.value.filterFunc(filterArgs[it.key]) }.all { it }
                }.forEach {
                    this@DPListTab.addItem(it)
                }

                redraw()
            }
        }

        filterSection!!.add(applyButton)

        rootPanel.add(filterSection, 2)
    }

    fun getItems(): List<T> = items.toList()

    fun redraw() {
        colAssoc.clear()

        header?.let {
            itemsPanel.remove(it)
            header = null
        }

        items.forEach {
            it.getBindings().forEach { (k, v) ->
                colAssoc.putIfAbsent(k, mutableListOf())

                v?.let {
                    colAssoc[k]?.add(v)
                }
            }
        }

        this.itemsPanel.revalidate()
        this.itemsPanel.repaint()

        val colStartPositions = mutableMapOf<String, Int>()
        val cols = items.firstOrNull()?.getCols() ?: return
        val endCols = items.first().getEndCols()
        val colSorters = items.first().getColSorters()

        if (addFilterSection && filterSection == null)
            createFilterSection(items.first().getColFilters())

        header = HeaderComponent(
            cols.filter { it !in endCols && colAssoc[it]?.isNotEmpty() == true }.toSet(),
            colSorters.keys,
            currentColSortState
        ) { col, state ->
            val sortedItems = items.sortedWith(colSorters[col]!!).toMutableList()

            currentColSortState = Pair(col, state)

            if (state == ColSortState.ASC) {
                sortedItems.reverse()
            }

            SwingUtilities.invokeLater {
                this.clear()

                sortedItems.forEach { item ->
                    this.addItem(item)
                }

                this.redraw()
            }
        }

        header!!.getBindings().forEach { (k, v) ->
            colAssoc[k]?.add(v!!)
        }

        itemsPanel.add(header!!, itemsPanel.components.indexOf(items.first()))

        var acc = 0

        for (i in 0 until cols.size - 1) {
            val it = cols.elementAt(i)

            colStartPositions.putIfAbsent(it, 0)

            val colItems = colAssoc[it]!!
            val value =
                colItems.maxOfOrNull { item -> item.preferredSize.width + (item.parent as DPComponent).padding } ?: 0

            colStartPositions[cols.elementAt(i + 1)] = acc + value
            acc += value
        }

        items.forEach {
            it.updateFillers(colStartPositions)
        }

        header!!.updateFillers(colStartPositions)

        this.itemsPanel.revalidate()
        this.itemsPanel.repaint()
    }

    fun addItem(component: T) {
        if (items.isNotEmpty()) {
            itemsPanel.add(JSeparator(SwingConstants.HORIZONTAL).apply {
                maximumSize = Dimension(maximumSize.width, 2)
            })
        }

        if (component !in allItems)
            allItems.add(component)

        items.add(component)

        itemsPanel.add(component)
    }

    fun clear() {
        items.clear()

        for (i in itemsPanel.components) {
            itemsPanel.remove(i)
        }
    }
}

fun DPTab.startLoading(): Pair<JBLoadingPanel, Disposable> {
    val disposable = Disposer.newDisposable()
    val loadingPanel = JBLoadingPanel(null, disposable)

    this.parent.parent.add(loadingPanel)

    loadingPanel.startLoading()

    return Pair(loadingPanel, disposable)
}

fun DPTab.stopLoading(loadingPanel: JBLoadingPanel, disposable: Disposable) {
    Disposer.dispose(disposable)
    this.parent.parent.remove(loadingPanel)
}

const val MIN_SEARCH_CHARACTERS = 3

class SubmissionsTab(
    title: String, addReloadButton: Boolean, addSearchBar: Boolean = false,
    addFilterSection: Boolean = false, searchBarHint: String = "", searchBarDescription: String = "",
    addMarkFinalButton: Boolean = false
) : DPListTab<SubmissionComponent>(
    title,
    addReloadButton,
    addSearchBar,
    addFilterSection,
    searchBarHint,
    searchBarDescription
) {

    constructor(title: String) : this(title, false)

    private val markAsFinalButton: DPOptionButton? =
        if (addMarkFinalButton) DPOptionButton("Mark as Final...") else null

    init {
        if (addMarkFinalButton)
            toolPanel.add(markAsFinalButton)
    }

    fun setMarkAsFinalButtonOptions(options: Array<Action>) {
        markAsFinalButton?.options = options
    }

    fun setSubmissions(submissions: List<Submission>) {
        submissions.forEach { s ->
            addItem(SubmissionComponent(s).apply {
                this.addBuildReportClickListener { _ ->
                    val loadingPanel = startLoading()

                    State.client.getBuildReport(s.id.toString()) { report ->
                        if (report == null) {
                            stopLoading(loadingPanel.component1(), loadingPanel.component2())
                        }

                        report?.let { data ->
                            stopLoading(loadingPanel.component1(), loadingPanel.component2())
                            holder.data = listOf(data)
                            navigateForward((holder.getTab(data.javaClass.name) as DPTab))
                        }
                    }
                }
                this.addSubmissionDownloadClickListener {
                    SubmissionsAction.openSubmission(this.submission.id.toString())
                }
                this.addMarkAsFinalClickListener {
                    State.client.markAsFinal(this.submission.id) { result ->
                        if (result == true) {
                            this.markedAsFinal()

                            this.revalidate()
                            this.repaint()
                        }
                    }
                }
            })
        }
    }
}

@Suppress("UNUSED_PARAMETER")
private fun dashboardTabProvider(data: List<DPData>): DPTab {
    val panel = DPTab().apply {
        this.rootPanel.border = JBUI.Borders.empty(0, 20)
    }

    val root = panel.rootPanel

    root.add(JLabel("<html><h1>Dashboard</h1></html>").apply { alignmentX = 0.0f })

    val content = JPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.border = JBUI.Borders.empty(0, 10)
    }

    root.add(content)

    val studentHistoryContainer = JPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.border = JBUI.Borders.empty(0, 250)
    }

    val assignmentSearchContainer = JPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.border = JBUI.Borders.empty(0, 250)
    }


    val studentHistoryBarContainer = JPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.X_AXIS)
        this.alignmentX = 0F
    }

    val assignmentSearchBarContainer = JPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.X_AXIS)
        this.alignmentX = 0F
    }

    content.add(studentHistoryContainer)
    content.add(assignmentSearchContainer)

    val studentSearchField =
        SearchBar("ex: Student, 22111333", "Searches for all submissions made by a student").apply {
            fun search() {
                State.client.searchStudents(this.text) {
                    SwingUtilities.invokeLater {
                        while (studentHistoryContainer.componentCount > 1) {
                            studentHistoryContainer.remove(1)
                        }

                        studentHistoryContainer.revalidate()
                        studentHistoryContainer.repaint()

                        it?.forEach { student ->
                            studentHistoryContainer.add(StudentComponent(student).apply {
                                this.addOnClickListener { r ->
                                    val (loadingPanel, disposable) = panel.startLoading()

                                    State.client.getStudentHistory(r.value) { sh, isException ->
                                        panel.stopLoading(loadingPanel, disposable)
                                        if (isException)
                                            JOptionPane.showMessageDialog(
                                                null,
                                                "Couldn't get this student's history.\nFor more information, use the web UI.",
                                                "Error",
                                                JOptionPane.ERROR_MESSAGE
                                            )

                                        sh?.let {
                                            panel.holder.data = listOf(StudentHistoryPage.from(it.history))
                                            panel.navigateForward(panel.holder.getTab(StudentHistoryPage::class.java.name) as DPTab)
                                        }
                                    }
                                }
                            })

                            studentHistoryContainer.revalidate()
                            studentHistoryContainer.repaint()
                        }
                    }
                }
            }

            this.addActionListener { search() }
            this.addDocumentListener { document ->
                if (document.length >= MIN_SEARCH_CHARACTERS) {
                    search()
                }
            }

            this.alignmentY = Component.TOP_ALIGNMENT + 0.1f
        }

    val assignmentSearchField =
        SearchBar("ex: sampleJavaAssignment, tagName", "Searches for an assignment with matching name or tag").apply {
            this.addActionListener {
                State.client.searchAssignments(this.text) {
                    SwingUtilities.invokeLater {
                        while (assignmentSearchContainer.componentCount > 1) {
                            assignmentSearchContainer.remove(1)
                        }

                        assignmentSearchContainer.revalidate()
                        assignmentSearchContainer.repaint()

                        it?.let {
                            for (student in it) {
                                assignmentSearchContainer.add(StudentComponent(student).apply {
                                    this.addOnClickListener { r ->
                                        State.client.getSubmissions(r.value) { subs ->
                                            subs?.let {
                                                panel.holder.data = listOf(AssignmentSubmissions(r.value, subs))
                                                panel.navigateForward(panel.holder.getTab(AssignmentSubmissions::class.java.name) as DPTab)
                                            }
                                        }
                                    }
                                })

                                assignmentSearchContainer.revalidate()
                                assignmentSearchContainer.repaint()
                            }
                        }
                    }
                }
            }

            this.alignmentY = Component.TOP_ALIGNMENT + 0.1f
        }

    val studentHistoryLabel = JLabel("Student History: ").apply { alignmentY = Component.TOP_ALIGNMENT }
    val assignmentSearchLabel = JLabel("Assignments: ").apply { alignmentY = Component.TOP_ALIGNMENT }

    maxOf(studentHistoryLabel.preferredSize.width, assignmentSearchLabel.preferredSize.width).run {
        studentHistoryLabel.preferredSize = Dimension(this, studentHistoryLabel.preferredSize.height)
        assignmentSearchLabel.preferredSize = Dimension(this, assignmentSearchLabel.preferredSize.height)
    }

    studentHistoryBarContainer.add(studentHistoryLabel)
    studentHistoryBarContainer.add(studentSearchField)

    assignmentSearchBarContainer.add(assignmentSearchLabel)
    assignmentSearchBarContainer.add(assignmentSearchField)

    studentHistoryContainer.add(studentHistoryBarContainer)
    assignmentSearchContainer.add(assignmentSearchBarContainer)

    val buttonPanel = JPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.X_AXIS)
        this.alignmentX = 0F
    }

    buttonPanel.add(Box.createHorizontalGlue())

    buttonPanel.add(
        JButton("All assignments").apply {
            this.addActionListener {
                val loadingPanel = panel.startLoading()

                State.client.getAssignments { assignments ->
                    if (assignments == null) {
                        JOptionPane.showMessageDialog(
                            WindowManager.getInstance().findVisibleFrame(),
                            "Couldn't load assignments.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )

                        panel.stopLoading(loadingPanel.component1(), loadingPanel.component2())
                    }

                    assignments?.let { data ->
                        panel.stopLoading(loadingPanel.component1(), loadingPanel.component2())
                        panel.holder.data = data
                        panel.navigateForward((panel.holder.getTab(data.first().javaClass.name) as DPTab))
                    }
                }
            }
        }
    )

    buttonPanel.add(Box.createHorizontalGlue())

    content.add(buttonPanel)
    content.add(JSeparator(JSeparator.HORIZONTAL))

    return panel
}

data class StudentHistoryPage(
    val previous: StudentHistoryPage?,
    var next: StudentHistoryPage?,
    val content: List<StudentHistoryEntry>
) : DPData {
    companion object {
        private const val MAX_ENTRIES = 50

        fun from(list: List<StudentHistoryEntry>, previous: StudentHistoryPage? = null): StudentHistoryPage {
            val sortedList = list.sortedByDescending { it.sortedSubmissions.first().getParsedDate() }
            var count = 0
            val content = mutableListOf<StudentHistoryEntry>()

            for (entry in sortedList) {
                val entryCount = entry.sortedSubmissions.size

                if (count + entryCount > MAX_ENTRIES) {
                    val diff = count + entryCount - MAX_ENTRIES
                    val newEntry = StudentHistoryEntry(entry.assignment, entry.sortedSubmissions.dropLast(diff))

                    if (newEntry.sortedSubmissions.isNotEmpty())
                        content.add(newEntry)

                    val nextEntry = StudentHistoryEntry(entry.assignment, entry.sortedSubmissions.takeLast(diff))

                    return StudentHistoryPage(previous, null, content).also {
                        it.next = from(listOf(nextEntry) + list.drop(list.indexOf(entry) + 1), it)
                    }
                }

                count += entryCount
                content.add(entry)
            }

            return StudentHistoryPage(previous, null, content)
        }
    }

    val length: Int
        get() {
            var prev = previous
            var nex = next
            var result = 1

            while (prev != null) {
                result += 1
                prev = prev.previous
            }
            while (nex != null) {
                result += 1
                nex = nex.next
            }

            return result
        }

    val currentIndex: Int
        get() {
            var prev = previous
            var result = 0

            while (prev != null) {
                result += 1
                prev = prev.previous
            }

            return result + 1
        }
}

@Suppress("UNCHECKED_CAST")
private fun studentHistoryTabProvider(data: List<DPData>): DPListTab<SubmissionComponent> {
    val root = SubmissionsTab("Student History")
    lateinit var nextButton: JButton
    lateinit var previousButton: JButton

    var page = (data as List<StudentHistoryPage>).first()
    var currentGroup: Pair<Assignment, ProjectGroup?>?

    fun refresh() {
        root.clear()
        currentGroup = null

        page.content.forEach { entry ->
            if (Pair(entry.assignment, entry.sortedSubmissions.first().group) != currentGroup) {
                root.itemsPanel.add(JLabel("<html><h2>${entry.assignment.name}: ${entry.sortedSubmissions[0].group?.authors?.joinToString { it.name }}</h2></html>"))
                currentGroup = Pair(entry.assignment, entry.sortedSubmissions.first().group)
            }

            root.setSubmissions(entry.sortedSubmissions)

            root.redraw()
        }

        previousButton.isEnabled = page.previous != null
        nextButton.isEnabled = page.next != null
    }

    val buttonPanel = JPanel().apply {
        layout = BoxLayout(this@apply, BoxLayout.X_AXIS)
        alignmentX = 0F
    }

    buttonPanel.add(Box.createHorizontalGlue())

    previousButton = JButton(AllIcons.General.ArrowLeft).apply {
        addActionListener {
            page = page.previous ?: page
            refresh()
        }
    }
    nextButton = JButton(AllIcons.General.ArrowRight).apply {
        addActionListener {
            page = page.next ?: page
            refresh()
        }
    }

    buttonPanel.add(previousButton)
    buttonPanel.add(nextButton)

    buttonPanel.add(Box.createHorizontalGlue())
    root.rootPanel.add(buttonPanel)

    root.rootPanel.add(JPanel().apply {
        layout = BoxLayout(this@apply, BoxLayout.X_AXIS)
        alignmentX = 0F

        add(Box.createHorizontalGlue())
        add(JLabel("${page.currentIndex}/${page.length}"))
        add(Box.createHorizontalGlue())
    })

    refresh()

    return root
}

@Suppress("UNCHECKED_CAST")
private fun assignmentTabProvider(data: List<DPData>): DPListTab<AssignmentComponent> {
    val root = DPListTab<AssignmentComponent>(
        "Assignments", addReloadButton = false, addSearchBar = true, addFilterSection = true,
        searchBarHint = "ex: sampleJavaAssignment, tagName",
        searchBarDescription = "Searches for an assignment with matching name or tag"
    )

    val assignments = data as List<Assignment>

    assignments.forEach { assignment ->
        root.addItem(AssignmentComponent(assignment).apply {
            this.addSubmissionClickListener {
                val loadingPanel = root.startLoading()

                State.client.getSubmissions(this.assignment.id) { subs ->
                    if (subs == null) {
                        JOptionPane.showMessageDialog(
                            WindowManager.getInstance().findVisibleFrame(),
                            "Couldn't load submissions.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )

                        root.stopLoading(loadingPanel.component1(), loadingPanel.component2())
                    }

                    subs?.let { data ->
                        root.stopLoading(loadingPanel.component1(), loadingPanel.component2())
                        root.holder.data = listOf(AssignmentSubmissions(assignment.id, data))
                        root.navigateForward((root.holder.getTab(AssignmentSubmissions::class.java.name) as DPTab))
                    }
                }
            }
        })
    }

    root.redraw()

    return root
}

@Suppress("UNCHECKED_CAST")
fun groupSubmissionsTabProvider(data: List<DPData>): DPListTab<GroupSubmissionsComponent> {
    val root = DPListTab<GroupSubmissionsComponent>(
        "Submissions", addReloadButton = true, addSearchBar = true,
        searchBarHint = "ex: Student, 22111333", searchBarDescription = "Searches for a matching student/group"
    )

    var submissions = (data as List<AssignmentSubmissions>)[0].submissionsResponse
    var submissionsCache = submissions

    root.reloadCheckFunction = {
        var ret = false
        val response = State.client.getSubmissionsBlocking(data[0].assignmentId)

        if (response != null && response != submissions) {
            ret = true
            submissionsCache = response
        }

        ret
    }

    val populateRoot = {
        submissions.forEach {
            root.addItem(GroupSubmissionsComponent(it).apply {
                this.addBuildReportClickListener { _ ->
                    val loadingPanel = root.startLoading()

                    State.client.getBuildReport(it.lastSubmission.id.toString()) { report ->
                        root.stopLoading(loadingPanel.component1(), loadingPanel.component2())

                        report?.let { data ->
                            root.holder.data = listOf(data)
                            root.navigateForward((root.holder.getTab(data.javaClass.name) as DPTab))
                        }
                    }
                }
                this.addSubmissionDownloadClickListener { _ ->
                    SubmissionsAction.openSubmission(it.lastSubmission.id.toString())
                }
                this.addAllSubmissionsClickListener {
                    val loadingPanel = root.startLoading()

                    State.client.getGroupSubmissions(data[0].assignmentId, it.projectGroup.id) { subs ->
                        root.stopLoading(loadingPanel.component1(), loadingPanel.component2())

                        subs?.let { s ->
                            root.holder.data = listOf(GroupSubmissions(data[0].assignmentId, it.projectGroup.id, s))
                            root.navigateForward((root.holder.getTab(GroupSubmissions::class.java.name) as DPTab))
                        }
                    }
                }
            })
        }

        root.redraw()
    }

    root.reloadFunction = {
        submissions = submissionsCache
        root.clear()
        populateRoot()

        root.revalidate()
        root.repaint()

        root.reloadButton?.isEnabled = false
    }

    root.toolPanel.add(DPOptionButton("Mark as Final...").apply {
        this.options = arrayOf(
            object : AbstractAction("Mark highest scores") {
                override fun actionPerformed(e: ActionEvent?) {
                    State.client.previewMarkBestSubmissions(data[0].assignmentId) {
                        if (it?.isNotEmpty() == true) {
                            State.client.markMultipleAsFinal(it.map { e -> e.id }) { res ->
                                JOptionPane.showMessageDialog(
                                    parent,
                                    if (res == true) "Marked ${it.size} submissions as final" else "Couldn't mark any submissions as final",
                                    "Mark highest score",
                                    JOptionPane.INFORMATION_MESSAGE
                                )
                            }
                        } else {
                            JOptionPane.showMessageDialog(
                                parent,
                                "Couldn't mark any submissions as final",
                                "Mark highest score",
                                JOptionPane.INFORMATION_MESSAGE
                            )
                        }
                    }
                }
            }
        )
    })

    populateRoot()

    root.startRefreshThread()

    return root
}

@Suppress("UNCHECKED_CAST")
fun submissionsTabProvider(data: List<DPData>): DPListTab<SubmissionComponent> {
    val root = SubmissionsTab(
        "Submissions", addReloadButton = true, addSearchBar = true,
        searchBarHint = "ex: 106", searchBarDescription = "Searches for a submission by ID",
        addMarkFinalButton = true
    )

    var submissions = (data as List<GroupSubmissions>)[0].allSubmissions
    var submissionsCache = submissions

    root.setMarkAsFinalButtonOptions(arrayOf(
        object : AbstractAction("Mark latest") {
            override fun actionPerformed(e: ActionEvent?) {
                root.getItems().firstOrNull()?.let { item ->
                    State.client.markAsFinal(item.submission.id) {
                        if (it == true) {
                            root.getItems().forEach { i -> i.unmarkFinal() }
                            item.markedAsFinal()

                            root.revalidate()
                            root.repaint()
                        }
                    }
                }
            }
        },
        object : AbstractAction("Mark highest score") {
            override fun actionPerformed(e: ActionEvent?) {
                fun getScore(submission: Submission) =
                    submission.teacherTests?.let { it.numTests - it.numFailures - it.numErrors } ?: 0

                val sorted = root.getItems().sortedByDescending { getScore(it.submission) }

                val filtered = getScore(sorted.first().submission).let { topScore ->
                    sorted.filter { getScore(it.submission) == topScore }
                }

                var top = filtered.first()

                if (filtered.size > 1) {
                    top = filtered.sortedByDescending { it.submission.getParsedDate() }.first()
                }

                State.client.markAsFinal(top.submission.id) {
                    if (it == true) {
                        root.getItems().forEach { i -> i.unmarkFinal() }
                        top.markedAsFinal()

                        root.revalidate()
                        root.repaint()
                    }
                }
            }
        }
    ))

    root.reloadCheckFunction = {
        var ret = false
        val response = State.client.getGroupSubmissionsBlocking(data[0].assignmentId, data[0].groupId)

        if (response != null) {
            if (response != submissionsCache) {
                ret = true
                submissionsCache = response
            }
        }

        ret
    }

    val populateRoot = {
        root.setSubmissions(submissions.sortedBy { -it.id })

        root.redraw()
    }

    root.reloadFunction = {
        submissions = submissionsCache
        root.clear()
        populateRoot()

        root.revalidate()
        root.repaint()

        root.reloadButton?.isEnabled = false
    }

    populateRoot()

    root.startRefreshThread()

    return root
}

@Suppress("UNCHECKED_CAST")
fun buildReportTabProvider(data: List<DPData>): DPTab {
    val panel = DPTab().apply {
        rootPanel.border = JBUI.Borders.empty(0, 20)
    }

    val report = (data as List<FullBuildReport>).first()

    panel.rootPanel.add(UIBuildReport().buildComponents(report, null))

    panel.toolPanel.add(JButton(object : AbstractAction("Mark as Final") {
        override fun actionPerformed(e: ActionEvent?) {
            (e?.source as JButton).isEnabled = false

            report.submission?.let {
                State.client.markAsFinal(it.id) { result ->
                    if (result == false)
                        (e.source as JButton).isEnabled = true
                }
            }
        }
    }).apply {
        isEnabled = report.submission?.markedAsFinal?.not() ?: false
    })

    return panel
}

val tabProviders = mapOf<String, (List<DPData>) -> DPTab>(
    Pair(Null::class.java.name, ::dashboardTabProvider),
    Pair(StudentHistoryPage::class.java.name, ::studentHistoryTabProvider),
    Pair(Assignment::class.java.name, ::assignmentTabProvider),
    Pair(AssignmentSubmissions::class.java.name, ::groupSubmissionsTabProvider),
    Pair(GroupSubmissions::class.java.name, ::submissionsTabProvider),
    Pair(FullBuildReport::class.java.name, ::buildReportTabProvider)
)

class DPTabHolder(val project: Project, var data: List<DPData>) : FileEditor {
    // TODO: Receive data from the caller, not the class instance
    fun getTab(className: String): JComponent = tabProviders[className]?.let {
        it(data).apply { this.holder = this@DPTabHolder }
    } ?: JPanel()

    val panel: JPanel = JPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.add(getTab(data.first().javaClass.name))
    }

    private val userData = UserDataHolderBase()

    override fun <T : Any?> getUserData(key: Key<T>): T? = userData.getUserData(key)

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) = userData.putUserData(key, value)

    override fun dispose() {}

    override fun getComponent(): JComponent = panel

    override fun getPreferredFocusedComponent(): JComponent = panel

    override fun getName(): String = "DP"

    override fun setState(state: FileEditorState) {}

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = true

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}

    override fun getFile(): VirtualFile = VirtualFile(data)
}

class VirtualFile(val data: List<DPData>) : LightVirtualFile()