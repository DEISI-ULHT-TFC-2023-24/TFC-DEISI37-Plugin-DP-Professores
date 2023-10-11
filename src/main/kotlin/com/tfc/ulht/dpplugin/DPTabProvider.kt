package com.tfc.ulht.dpplugin

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.util.ui.JBUI
import com.tfc.ulht.dpplugin.dplib.Assignment
import com.tfc.ulht.dpplugin.dplib.DPData
import com.tfc.ulht.dpplugin.dplib.Submission
import com.tfc.ulht.dpplugin.dplib.SubmissionsResponse
import com.tfc.ulht.dpplugin.ui.AssignmentComponent
import com.tfc.ulht.dpplugin.ui.GroupSubmissionsComponent
import com.tfc.ulht.dpplugin.ui.SubmissionComponent
import java.awt.Dimension
import java.beans.PropertyChangeListener
import javax.swing.*

class DPTabProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file is com.tfc.ulht.dpplugin.VirtualFile

    override fun createEditor(project: Project, file: VirtualFile): FileEditor = DPTab(project, (file as com.tfc.ulht.dpplugin.VirtualFile).data)

    override fun getEditorTypeId(): String = "dp-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

class DPPanel : JPanel() {
    lateinit var tab: DPTab
}

@Suppress("UNCHECKED_CAST")
private fun assignmentTabProvider(data: List<DPData>) : DPPanel {
    val root = DPPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.border = JBUI.Borders.empty(0, 20)
    }

    val assignments = data as List<Assignment>

    root.add(JLabel("<html><h1>Assignments</h1></html>").apply { alignmentX = 0.0f })
    val assignmentsPanel = JPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.border = JBUI.Borders.empty(0, 10)
    }
    root.add(assignmentsPanel)

    assignments.forEach {
        assignmentsPanel.add(AssignmentComponent(it).apply { this.addSubmissionClickListener {
            val loadingPanel = JBLoadingPanel(null, Disposable {  })
            root.add(loadingPanel)
            loadingPanel.startLoading()

            State.client.getSubmissions(this.assignment.id) { subs ->
                subs?.let { data ->
                    loadingPanel.stopLoading()
                    root.tab.data = data
                    root.tab.panel.removeAll()
                    root.tab.panel.add(root.tab.getTab(data.first().javaClass.name))
                    root.tab.panel.repaint()
                }
            }
        }})

        assignmentsPanel.add(JSeparator(SwingConstants.HORIZONTAL).apply {
            maximumSize = Dimension(maximumSize.width, 2)
        })
    }

    if (assignmentsPanel.componentCount > 1) assignmentsPanel.remove(assignmentsPanel.componentCount - 1)

    return root
}

@Suppress("UNCHECKED_CAST")
fun groupSubmissionsTabProvider(data: List<DPData>) : DPPanel {
    val root = DPPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.border = JBUI.Borders.empty(0, 20)
    }

    val submissions = data as List<SubmissionsResponse>

    root.add(JLabel("<html><h1>Submissions</h1></html>").apply { alignmentX = 0.0f })
    val submissionsPanel = JPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.border = JBUI.Borders.empty(0, 10)
    }
    root.add(submissionsPanel)

    submissions.forEach {
        submissionsPanel.add(GroupSubmissionsComponent(it).apply {
            this.addSubmissionDownloadClickListener { _ ->
                SubmissionsAction.openSubmission(it.allSubmissions.first().id.toString())
            }
            this.addAllSubmissionsClickListener {
                root.tab.data = it.allSubmissions
                root.tab.panel.removeAll()
                root.tab.panel.add(root.tab.getTab(it.allSubmissions.first().javaClass.name))
                root.tab.panel.repaint()
            }
        })

        submissionsPanel.add(JSeparator(SwingConstants.HORIZONTAL).apply {
            maximumSize = Dimension(maximumSize.width, 2)
        })
    }

    if (submissionsPanel.componentCount > 1) submissionsPanel.remove(submissionsPanel.componentCount - 1)

    return root
}
@Suppress("UNCHECKED_CAST")
fun submissionsTabProvider(data: List<DPData>) : DPPanel {
    val root = DPPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.border = JBUI.Borders.empty(0, 20)
    }

    val submissions = data as List<Submission>

    root.add(JLabel("<html><h1>Submissions</h1></html>").apply { alignmentX = 0.0f })
    val submissionsPanel = JPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.border = JBUI.Borders.empty(0, 10)
    }
    root.add(submissionsPanel)

    submissions.forEach {
        submissionsPanel.add(SubmissionComponent(it).apply {
            this.addSubmissionDownloadClickListener {
                SubmissionsAction.openSubmission(this.submission.id.toString())
            }
        })

        submissionsPanel.add(JSeparator(SwingConstants.HORIZONTAL).apply {
            maximumSize = Dimension(maximumSize.width, 2)
        })
    }

    if (submissionsPanel.componentCount > 1) submissionsPanel.remove(submissionsPanel.componentCount - 1)

    return root
}

val tabProviders = mapOf<String, (List<DPData>) -> DPPanel>(
    Pair(Assignment::class.java.name, ::assignmentTabProvider),
    Pair(SubmissionsResponse::class.java.name, ::groupSubmissionsTabProvider),
    Pair(Submission::class.java.name, ::submissionsTabProvider)
)

class DPTab(val project: Project, var data: List<DPData>) : FileEditor {
    // TODO: Receive data from the caller, not the class instance
    fun getTab(className: String): JPanel = tabProviders[className]?.let {
        it(data).apply { this.tab = this@DPTab }
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