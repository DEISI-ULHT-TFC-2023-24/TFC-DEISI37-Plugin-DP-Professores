package com.tfc.ulht.dpplugin.actions

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLoadingPanel
import com.tfc.ulht.dpplugin.State
import com.tfc.ulht.dpplugin.VirtualFile

class AssignmentsAction : DPAction() {
    override fun perform(project: Project?) {
        val fileManager = project?.let { p -> FileEditorManager.getInstance(p) }

        val loadingPanel = JBLoadingPanel(null, Disposable {  })

        fileManager?.selectedEditor?.component?.parent?.add(loadingPanel)
        loadingPanel.startLoading()

        State.client.getAssignments {
            loadingPanel.stopLoading()

            it?.let { assignment ->
                val file = VirtualFile(assignment)

                ApplicationManager.getApplication().invokeLater {
                    fileManager?.openFile(file, true)
                }
            }
        }
    }
}