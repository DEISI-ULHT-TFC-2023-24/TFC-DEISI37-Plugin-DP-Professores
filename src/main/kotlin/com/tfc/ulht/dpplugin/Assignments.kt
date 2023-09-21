package com.tfc.ulht.dpplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.tfc.ulht.dpplugin.dplib.DPClient
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

class AssignmentsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        State.client.getAssignments {
            it?.let { assignment ->
                val file = VirtualFile(assignment)

                val fileManager = e.project?.let { p -> FileEditorManager.getInstance(p) }

                ApplicationManager.getApplication().invokeLater {
                    fileManager?.openFile(file, true)
                }
            }
        }
    }

}

class AssignmentsDialog(project: Project?) : DialogWrapper(project) {
    init {
        init()
        title = "DP - Assignments"
    }

    override fun createCenterPanel(): JComponent {
        return JPanel().apply {
            val label = JLabel()
            this.add(label)

        }
    }
}