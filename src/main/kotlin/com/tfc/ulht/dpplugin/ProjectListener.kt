package com.tfc.ulht.dpplugin

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.tfc.ulht.dpplugin.dplib.BASE_URL

class ProjectListener : ProjectManagerListener {
    override fun projectOpened(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            val credentials = PasswordSafe.instance.get(CredentialAttributes("DP", "dp"))

            credentials?.userName?.let {
                credentials.password?.let {
                    BASE_URL = credentials.password.toString()
                }
                State.client.login(it, null)
            }
        }

        if (project.name.startsWith("sub_")) {
            ActionUtil.getAction("Synchronize")?.let {
                ActionUtil.invokeAction(
                    it,
                    DataContext.EMPTY_CONTEXT,
                    ActionPlaces.UNKNOWN,
                    null
                ) {
                    Logger.getInstance(ProjectListener::class.java).debug("Action!")
                }
            }
        }
    }
}