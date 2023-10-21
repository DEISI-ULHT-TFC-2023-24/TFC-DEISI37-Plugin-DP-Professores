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
import com.tfc.ulht.dpplugin.ui.AssignmentComponent
import com.tfc.ulht.dpplugin.ui.DashboardItemComponent
import com.tfc.ulht.dpplugin.ui.GroupSubmissionsComponent
import com.tfc.ulht.dpplugin.ui.SubmissionComponent
import java.awt.Component
import java.awt.Dimension
import java.beans.PropertyChangeListener
import javax.swing.*

class DPTabProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file is com.tfc.ulht.dpplugin.VirtualFile

    override fun createEditor(project: Project, file: VirtualFile): FileEditor = DPTabHolder(project, (file as com.tfc.ulht.dpplugin.VirtualFile).data)

    override fun getEditorTypeId(): String = "dp-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

open class DPTab : JPanel() {
    lateinit var holder: DPTabHolder
}

open class DPListTab<T : Component>(title: String): DPTab() {
    private val items: MutableList<T> = mutableListOf()
    private val itemsPanel: JPanel = JPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.border = JBUI.Borders.empty(0, 10)
    }

    init {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.border = JBUI.Borders.empty(0, 20)

        this.add(JLabel("<html><h1>$title</h1></html>").apply { alignmentX = 0.0f })
        this.add(itemsPanel)
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
}

private const val LOGIN_ID = 0
private const val ASSIGNMENT_ID = 1

@Suppress("UNUSED_PARAMETER")
private fun dashboardTabProvider(data: List<DPData>) : DPTab {
    val root = DPTab().apply {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.border = JBUI.Borders.empty(0, 20)
    }

    root.add(JLabel("<html><h1>Dashboard</h1></html>").apply { alignmentX = 0.0f })

    val listener: (Int) -> Unit = {
        when (it) {
            LOGIN_ID -> LoginDialog(null).show()
            ASSIGNMENT_ID -> {
                val loadingPanel = JBLoadingPanel(null, Disposable {  })
                root.add(loadingPanel)

                loadingPanel.startLoading()

                State.client.getAssignments { assignments ->
                    if (assignments == null) {
                        JDialog(WindowManager.getInstance().findVisibleFrame(), "Couldn't load assignments.").run {
                            this.isVisible = true
                        }

                        loadingPanel.stopLoading()
                        root.remove(loadingPanel)
                    }

                    assignments?.let { data ->
                        loadingPanel.stopLoading()
                        root.remove(loadingPanel)
                        root.holder.data = data
                        root.holder.panel.removeAll()
                        root.holder.panel.add(root.holder.getTab(data.first().javaClass.name))
                        root.holder.panel.repaint()
                    }
                }
            }
        }
    }

    root.add(DashboardItemComponent(LOGIN_ID, "Login", null, listener))
    root.add(DashboardItemComponent(ASSIGNMENT_ID, "Assignments", IconLoader.getIcon("actions/listFiles.svg", AllIcons::class.java), listener))

    return root
}

@Suppress("UNCHECKED_CAST")
private fun assignmentTabProvider(data: List<DPData>) : DPListTab<AssignmentComponent> {
    val root = DPListTab<AssignmentComponent>("Assignments")

    val assignments = data as List<Assignment>

    assignments.forEach {
        root.addItem(AssignmentComponent(it).apply { this.addSubmissionClickListener {
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
                    root.holder.data = data
                    root.holder.panel.removeAll()
                    root.holder.panel.add(root.holder.getTab(data.first().javaClass.name))
                    root.holder.panel.repaint()
                }
            }
        }})
    }

    return root
}

@Suppress("UNCHECKED_CAST")
fun groupSubmissionsTabProvider(data: List<DPData>) : DPListTab<GroupSubmissionsComponent> {
    val root = DPListTab<GroupSubmissionsComponent>("Submissions")

    val submissions = data as List<SubmissionsResponse>

    submissions.forEach {
        root.addItem(GroupSubmissionsComponent(it).apply {
            this.addSubmissionDownloadClickListener { _ ->
                SubmissionsAction.openSubmission(it.allSubmissions.first().id.toString())
            }
            this.addAllSubmissionsClickListener {
                root.holder.data = it.allSubmissions
                root.holder.panel.removeAll()
                root.holder.panel.add(root.holder.getTab(it.allSubmissions.first().javaClass.name))
                root.holder.panel.repaint()
            }
        })
    }

    return root
}

@Suppress("UNCHECKED_CAST")
fun submissionsTabProvider(data: List<DPData>) : DPListTab<SubmissionComponent> {
    val root = DPListTab<SubmissionComponent>("Submissions")

    val submissions = data as List<Submission>

    submissions.forEach {
        root.addItem(SubmissionComponent(it).apply {
            this.addSubmissionDownloadClickListener {
                SubmissionsAction.openSubmission(this.submission.id.toString())
            }
        })
    }

    return root
}

val tabProviders = mapOf<String, (List<DPData>) -> DPTab>(
    Pair(Null::class.java.name, ::dashboardTabProvider),
    Pair(Assignment::class.java.name, ::assignmentTabProvider),
    Pair(SubmissionsResponse::class.java.name, ::groupSubmissionsTabProvider),
    Pair(Submission::class.java.name, ::submissionsTabProvider)
)

class DPTabHolder(val project: Project, var data: List<DPData>) : FileEditor {
    // TODO: Receive data from the caller, not the class instance
    fun getTab(className: String): JPanel = tabProviders[className]?.let {
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