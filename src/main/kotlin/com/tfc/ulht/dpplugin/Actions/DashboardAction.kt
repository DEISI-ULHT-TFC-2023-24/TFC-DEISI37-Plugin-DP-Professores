package com.tfc.ulht.dpplugin.Actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.tfc.ulht.dpplugin.VirtualFile
import com.tfc.ulht.dpplugin.dplib.Null

class DashboardAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val fileManager = e.project?.let { p -> FileEditorManager.getInstance(p) }

        val file = VirtualFile(listOf(Null()))

        ApplicationManager.getApplication().invokeLater {
            fileManager?.openFile(file, true)
        }
    }
}