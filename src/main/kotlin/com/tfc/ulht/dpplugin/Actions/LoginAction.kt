package com.tfc.ulht.dpplugin.Actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.tfc.ulht.dpplugin.LoginDialog

class LoginAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        LoginDialog(e.project).show()
    }
}