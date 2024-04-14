package com.tfc.ulht.dpplugin

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.tfc.ulht.dpplugin.dplib.BASE_URL
import org.jetbrains.concurrency.runAsync

class ProjectListener : ProjectManagerListener {
    override fun projectOpened(project: Project) {
        runAsync {
            val credentials = PasswordSafe.instance.get(CredentialAttributes("DP", "dp"))

            credentials?.userName?.let {
                credentials.password?.let {
                    val value = it.toString().split(";")
                    BASE_URL = value[1]
                    State.client.login(value[0], null)
                }
            }
        }

        if (project.name.startsWith("sub_")) {
            ActionUtil.getAction("Synchronize")?.let {
                ActionUtil.invokeAction(
                    it,
                    DataContext.EMPTY_CONTEXT,
                    ActionPlaces.UNKNOWN,
                    null
                ) {  }
            }
        }
    }
}