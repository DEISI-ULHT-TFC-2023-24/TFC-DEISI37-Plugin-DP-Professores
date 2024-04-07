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
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.tfc.ulht.dpplugin.dplib.*
import com.tfc.ulht.dpplugin.ui.*
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.beans.PropertyChangeListener
import javax.swing.*

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

    private val backButton: JButton
    private val forwardButton: JButton
    private val homeButton: JButton
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

        val toolPanel = JPanel().apply {
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

        homeButton = JButton(AllIcons.Nodes.HomeFolder).apply {
            this.addActionListener {
                navigateHome()
            }
        }

        updateNavButtons()

        toolPanel.add(backButton)
        toolPanel.add(forwardButton)
        toolPanel.add(homeButton)

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
        homeButton.isEnabled = parent != null
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

    private fun navigateHome() = parent?.let {
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

open class DPListTab<T : DPComponent>(title: String, addReloadButton: Boolean, addSearchBar: Boolean = false,
                                      searchBarHint: String = "", searchBarDescription: String = "") :
    DPTab(addReloadButton) {

    class HeaderComponent(cols: Set<String>) : DPComponent() {
        init {
            initCols(cols.toList())

            cols.forEach {
                this.addComponent(it, JLabel(it).apply {
                    font = JBFont.regular().asBold()
                })
            }
        }
    }

    private val allItems: MutableList<T> = mutableListOf()
    private val items: MutableList<T> = mutableListOf()
    val itemsPanel: JPanel = JPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.border = JBUI.Borders.empty(0, 10)
    }

    private val colAssoc: MutableMap<String, MutableList<Component>> = mutableMapOf()

    private var header: HeaderComponent? = null

    constructor(title: String) : this(title, false)

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

        header = HeaderComponent(cols.filter { it !in endCols && colAssoc[it]?.isNotEmpty() == true }.toSet())

        header!!.getBindings().forEach { (k, v) ->
            colAssoc[k]?.add(v!!)
        }

        itemsPanel.add(header!!, itemsPanel.components.indexOf(items.first()))

        var acc = 0

        for (i in 0 until cols.size - 1) {
            val it = cols.elementAt(i)

            colStartPositions.putIfAbsent(it, 0)

            val colItems = colAssoc[it]!!
            val value = colItems.maxOfOrNull { item -> item.preferredSize.width + (item.parent as DPComponent).padding } ?: 0

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

    val studentSearchField = SearchBar("ex: Student, 22111333", "Searches for all submissions made by a student").apply {
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
                                State.client.getStudentHistory(r.value) { sh ->
                                    sh?.let {
                                        panel.holder.data = sh.history
                                        panel.navigateForward(panel.holder.getTab(StudentHistoryEntry::class.java.name) as DPTab)
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

    val assignmentSearchField = SearchBar("ex: sampleJavaAssignment, tagName", "Searches for an assignment with matching name or tag").apply {
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

@Suppress("UNCHECKED_CAST")
private fun studentHistoryTabProvider(data: List<DPData>): DPListTab<SubmissionComponent> {
    val root = DPListTab<SubmissionComponent>("Student History")

    val studentHistory = data as List<StudentHistoryEntry>

    studentHistory.forEach { entry ->
        root.itemsPanel.add(JLabel("<html><h2>${entry.assignment.name}: ${entry.sortedSubmissions[0].group?.authors?.joinToString { it.name }}</h2></html>"))

        for (s in entry.sortedSubmissions) {
            root.addItem(SubmissionComponent(s).apply {
                this.addBuildReportClickListener { _ ->
                    val loadingPanel = root.startLoading()

                    State.client.getBuildReport(s.id.toString()) { report ->
                        if (report == null) {
                            root.stopLoading(loadingPanel.component1(), loadingPanel.component2())
                        }

                        report?.let { data ->
                            root.stopLoading(loadingPanel.component1(), loadingPanel.component2())
                            root.holder.data = listOf(data)
                            root.navigateForward((root.holder.getTab(data.javaClass.name) as DPTab))
                        }
                    }
                }
                this.addSubmissionDownloadClickListener {
                    SubmissionsAction.openSubmission(this.submission.id.toString())
                }
                this.addMarkAsFinalClickListener {
                    State.client.markAsFinal(this.submission.id.toString()) { result ->
                        if (result == true) {
                            this.markedAsFinal()

                            this.revalidate()
                            this.repaint()
                        }
                    }
                }
            })
        }

        root.redraw()
    }

    return root
}

@Suppress("UNCHECKED_CAST")
private fun assignmentTabProvider(data: List<DPData>): DPListTab<AssignmentComponent> {
    val root = DPListTab<AssignmentComponent>("Assignments", addReloadButton = false, addSearchBar = true,
                                              searchBarHint = "ex: sampleJavaAssignment, tagName",
                                              searchBarDescription = "Searches for an assignment with matching name or tag")

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
    val root = DPListTab<GroupSubmissionsComponent>("Submissions", addReloadButton = true, addSearchBar = true,
                                                    searchBarHint = "ex: Student, 22111333", searchBarDescription = "Searches for a matching student/group")

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

    populateRoot()

    root.startRefreshThread()

    return root
}

@Suppress("UNCHECKED_CAST")
fun submissionsTabProvider(data: List<DPData>): DPListTab<SubmissionComponent> {
    val root = DPListTab<SubmissionComponent>("Submissions", addReloadButton = true, addSearchBar = true,
                                              searchBarHint = "ex: 106", searchBarDescription = "Searches for a submission by ID")

    var submissions = (data as List<GroupSubmissions>)[0].allSubmissions
    var submissionsCache = submissions

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
        submissions.forEach {
            root.addItem(SubmissionComponent(it).apply {
                this.addBuildReportClickListener { _ ->
                    val loadingPanel = root.startLoading()

                    State.client.getBuildReport(it.id.toString()) { report ->
                        if (report == null) {
                            root.stopLoading(loadingPanel.component1(), loadingPanel.component2())
                        }

                        report?.let { data ->
                            root.stopLoading(loadingPanel.component1(), loadingPanel.component2())
                            root.holder.data = listOf(data)
                            root.navigateForward((root.holder.getTab(data.javaClass.name) as DPTab))
                        }
                    }
                }
                this.addSubmissionDownloadClickListener {
                    SubmissionsAction.openSubmission(this.submission.id.toString())
                }
                this.addMarkAsFinalClickListener {
                    State.client.markAsFinal(this.submission.id.toString()) { result ->
                        if (result == true) {
                            this.markedAsFinal()

                            this.revalidate()
                            this.repaint()
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

    /*root.addItem(DPComponent().apply {
        
    })*/

    /*report.summary?.forEach {
        root.addItem(SubmissionReportComponent(it))
    }

    report.buildReport?.let { root.addItem(BuildReportComponent(it)) }*/

    panel.rootPanel.add(UIBuildReport().buildComponents(report, null))

    return panel
}

val tabProviders = mapOf<String, (List<DPData>) -> DPTab>(
    Pair(Null::class.java.name, ::dashboardTabProvider),
    Pair(StudentHistoryEntry::class.java.name, ::studentHistoryTabProvider),
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