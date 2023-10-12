package com.tfc.ulht.dpplugin

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.ui.components.JBLoadingPanel

class AssignmentsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val fileManager = e.project?.let { p -> FileEditorManager.getInstance(p) }

        val loadingPanel = JBLoadingPanel(null, Disposable {  })
        fileManager?.selectedEditor?.component?.add(loadingPanel)

        loadingPanel.startLoading()

        State.client.getAssignments {
            it?.let { assignment ->
                loadingPanel.stopLoading()

                val file = VirtualFile(assignment)

                ApplicationManager.getApplication().invokeLater {
                    fileManager?.openFile(file, true)
                }
            }
        }
    }
}