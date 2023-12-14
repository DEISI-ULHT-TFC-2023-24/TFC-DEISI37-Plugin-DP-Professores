package com.tfc.ulht.dpplugin

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.tfc.ulht.dpplugin.dplib.BASE_URL

class ProjectListener : ProjectManagerListener {
    override fun projectOpened(project: Project) {
        val credentials = PasswordSafe.instance.get(CredentialAttributes("DP", "dp"))

        credentials?.userName?.let {
            credentials.password?.let {
                BASE_URL = credentials.password.toString()
            }
            State.client.login(it, null)
        }
    }
}