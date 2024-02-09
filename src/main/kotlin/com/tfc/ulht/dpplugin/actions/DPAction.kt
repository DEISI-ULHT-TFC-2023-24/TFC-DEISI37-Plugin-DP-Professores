package com.tfc.ulht.dpplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.tfc.ulht.dpplugin.LoginDialog
import com.tfc.ulht.dpplugin.State
import com.tfc.ulht.dpplugin.ui.dialogs.showLoginInProgressDialog

abstract class DPAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        if (State.client.loggingIn) {
            showLoginInProgressDialog(null)
            return
        } else if (!State.client.loggedIn) {
            LoginDialog(e.project) { perform(e.project) }.show()
            return
        }

        perform(e.project)
    }

    protected abstract fun perform(project: Project?)
}