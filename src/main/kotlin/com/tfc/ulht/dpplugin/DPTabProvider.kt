package com.tfc.ulht.dpplugin

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.WindowManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBLoadingPanel
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

    override fun createEditor(project: Project, file: VirtualFile): FileEditor = DPTabHolder(project, (file as com.tfc.ulht.dpplugin.VirtualFile).data)

    override fun getEditorTypeId(): String = "dp-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

open class DPTab(addReloadButton: Boolean = false) : JScrollPane(VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER) {
    lateinit var holder: DPTabHolder
    var parent: DPTab? = null
    var next: DPTab? = null

    val backButton: JButton
    val forwardButton: JButton
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

        updateNavButtons()

        rootPanel.add(backButton)
        rootPanel.add(forwardButton)

        if (addReloadButton) {
            reloadButton = JButton(AllIcons.Actions.Refresh).apply {
                this.isEnabled = false
                this.addActionListener {
                    reloadFunction?.let { fn -> fn() }
                }
            }

            rootPanel.add(reloadButton)
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

open class DPListTab<T : Component>(title: String, addReloadButton: Boolean) : DPTab(addReloadButton) {
    private val items: MutableList<T> = mutableListOf()
    private val itemsPanel: JPanel = JPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.border = JBUI.Borders.empty(0, 10)
    }

    constructor(title:String) : this(title, false)

    init {
        rootPanel.border = JBUI.Borders.empty(0, 20)

        rootPanel.add(JLabel("<html><h1>$title</h1></html>").apply { alignmentX = 0.0f })
        rootPanel.add(itemsPanel)
    }

    fun addItem(component: T) {
        if (items.isNotEmpty()) {
            itemsPanel.add(JSeparator(SwingConstants.HORIZONTAL).apply {
                maximumSize = Dimension(maximumSize.width, 2)
            })
        }

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

private const val LOGIN_ID = 0
private const val ASSIGNMENT_ID = 1

@Suppress("UNUSED_PARAMETER")
private fun dashboardTabProvider(data: List<DPData>) : DPTab {
    val panel = DPTab().apply {
        rootPanel.border = JBUI.Borders.empty(0, 20)
    }

    val root = panel.rootPanel

    root.add(JLabel("<html><h1>Dashboard</h1></html>").apply { alignmentX = 0.0f })

    val listener: (Int) -> Unit = {
        when (it) {
            LOGIN_ID -> LoginDialog(null).show()
            ASSIGNMENT_ID -> {
                val loadingPanel = JBLoadingPanel(null, Disposable {  })
                panel.add(loadingPanel)

                loadingPanel.startLoading()

                State.client.getAssignments { assignments ->
                    if (assignments == null) {
                        JDialog(WindowManager.getInstance().findVisibleFrame(), "Couldn't load assignments.").run {
                            this.isVisible = true
                        }

                        loadingPanel.stopLoading()
                        panel.remove(loadingPanel)
                    }

                    assignments?.let { data ->
                        loadingPanel.stopLoading()
                        panel.remove(loadingPanel)
                        panel.holder.data = data
                        panel.navigateForward((panel.holder.getTab(data.first().javaClass.name) as DPTab))
                    }
                }
            }
        }
    }

    root.add(DashboardItemComponent(LOGIN_ID, "Login", null, listener))
    root.add(DashboardItemComponent(ASSIGNMENT_ID, "Assignments", IconLoader.getIcon("actions/listFiles.svg", AllIcons::class.java), listener))

    return panel
}

@Suppress("UNCHECKED_CAST")
private fun assignmentTabProvider(data: List<DPData>) : DPListTab<AssignmentComponent> {
    val root = DPListTab<AssignmentComponent>("Assignments")

    val assignments = data as List<Assignment>

    assignments.forEach { assignment ->
        root.addItem(AssignmentComponent(assignment).apply { this.addSubmissionClickListener {
            val loadingPanel = JBLoadingPanel(null, Disposable {  })
            root.add(loadingPanel)

            loadingPanel.startLoading()

            State.client.getSubmissions(this.assignment.id) { subs ->
                if (subs == null) {
                    JDialog(WindowManager.getInstance().findVisibleFrame(), "Couldn't load submissions.").run {
                        this.isVisible = true
                    }

                    loadingPanel.stopLoading()
                    root.remove(loadingPanel)
                }

                subs?.let { data ->
                    loadingPanel.stopLoading()
                    root.remove(loadingPanel)
                    root.holder.data = listOf(AssignmentSubmissions(assignment.id, data))
                    root.navigateForward((root.holder.getTab(AssignmentSubmissions::class.java.name) as DPTab))
                }
            }
        }})
    }

    return root
}

@Suppress("UNCHECKED_CAST")
fun groupSubmissionsTabProvider(data: List<DPData>) : DPListTab<GroupSubmissionsComponent> {
    val root = DPListTab<GroupSubmissionsComponent>("Submissions", true)

    var submissions = (data as List<AssignmentSubmissions>)[0].submissionsResponse
    var submissionsCache = submissions

    root.reloadCheckFunction = {
        var ret = false
        val response = State.client.getSubmissionsBlocking(data[0].assignmentId)

        if (response != null) {
            if (response != submissions) {
                ret = true
                submissionsCache = response
            }
        }

        ret
    }

    val populateRoot = {
        submissions.forEach {
            root.addItem(GroupSubmissionsComponent(it).apply {
                this.addBuildReportClickListener { _ ->
                    val loadingPanel = JBLoadingPanel(null, Disposable {  })
                    root.add(loadingPanel)

                    State.client.getBuildReport(it.allSubmissions.first().id.toString()) { report ->
                        loadingPanel.stopLoading()
                        root.remove(loadingPanel)

                        report?.let { data ->
                            root.holder.data = listOf(data)
                            root.navigateForward((root.holder.getTab(data.javaClass.name) as DPTab))
                        }
                    }
                }
                this.addSubmissionDownloadClickListener { _ ->
                    SubmissionsAction.openSubmission(it.allSubmissions.first().id.toString())
                }
                this.addAllSubmissionsClickListener {
                    val loadingPanel = JBLoadingPanel(null, Disposable {  })
                    root.add(loadingPanel)

                    State.client.getGroupSubmissions(data[0].assignmentId, it.projectGroup.id) { subs ->
                        loadingPanel.stopLoading()
                        root.remove(loadingPanel)

                        subs?.let { s ->
                            root.holder.data = listOf(GroupSubmissions(data[0].assignmentId, it.projectGroup.id, s))
                            root.navigateForward((root.holder.getTab(GroupSubmissions::class.java.name) as DPTab))
                        }
                    }
                }
            })
        }
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
fun submissionsTabProvider(data: List<DPData>) : DPListTab<SubmissionComponent> {
    val root = DPListTab<SubmissionComponent>("Submissions", true)

    var submissions = (data as List<GroupSubmissions>)[0].allSubmissions
    var submissionsCache = submissions

    root.reloadCheckFunction = {
        var ret = false
        val response = State.client.getSubmissionsBlocking(data[0].assignmentId)

        if (response != null) {
            for (i in response) {
                if (i.projectGroup.id == data[0].groupId && i.allSubmissions != submissions) {
                    ret = true
                    submissionsCache = i.allSubmissions
                    break
                }
            }
        }

        ret
    }

    val populateRoot = {
        submissions.forEach {
            root.addItem(SubmissionComponent(it).apply {
                this.addBuildReportClickListener { _ ->
                    val loadingPanel = JBLoadingPanel(null, Disposable {  })
                    root.add(loadingPanel)

                    State.client.getBuildReport(it.id.toString()) { report ->
                        if (report == null) {
                            loadingPanel.stopLoading()
                            root.remove(loadingPanel)
                        }

                        report?.let { data ->
                            loadingPanel.stopLoading()
                            root.remove(loadingPanel)
                            root.holder.data = listOf(data)
                            root.navigateForward((root.holder.getTab(data.javaClass.name) as DPTab))
                        }
                    }
                }
                this.addSubmissionDownloadClickListener {
                    SubmissionsAction.openSubmission(this.submission.id.toString())
                }
            })
        }
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
fun buildReportTabProvider(data: List<DPData>) : DPTab {
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

    override fun dispose() {  }

    override fun getComponent(): JComponent = panel

    override fun getPreferredFocusedComponent(): JComponent = panel

    override fun getName(): String = "DP"

    override fun setState(state: FileEditorState) {  }

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = true

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {  }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {  }

    override fun getFile(): VirtualFile = VirtualFile(data)
}

class VirtualFile(val data: List<DPData>) : LightVirtualFile()