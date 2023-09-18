package com.tfc.ulht.dpplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class LoginAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        LoginDialog(e.project).show()
    }
}