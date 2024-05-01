package com.tfc.ulht.dpplugin.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.tfc.ulht.dpplugin.dplib.BASE_URL
import okhttp3.Credentials
import io.ktor.util.*
import org.jetbrains.concurrency.runAsync

@Suppress("unused")
@State(
    name = "com.tfc.ulht.dpplugin.settings.ApplicationSettings",
    storages = [Storage("dp-settings.xml")]
)
class ApplicationSettings : PersistentStateComponent<ApplicationSettings> {
    private var credentials: Pair<String, String>? = getCredentials()

    var url: String
        get() = BASE_URL
        set(value) {
            BASE_URL = value
        }
    var username: String?
        get() = credentials?.first
        private set(_) {}
    var token: String?
        get() = credentials?.second
        private set(_) {}

    companion object {
        fun getCredentials(): Pair<String, String>? {
            return runAsync {
                val password = PasswordSafe.instance.get(CredentialAttributes("DP", "dp"))?.password
                    ?.toString()
                    ?.removePrefix("Basic ")
                    ?.split(";")?.get(0)
                    ?.decodeBase64String()
                    ?.split(":")

                if (password != null) Pair(password[0], password[1]) else null
            }.blockingGet(5000)
        }

        fun getSettings(): ApplicationSettings {
            return ApplicationManager.getApplication().getService(ApplicationSettings::class.java)
        }
    }

    fun setCredentials(username: String, token: String) {
        Credentials.basic(username, token).let { authToken ->
            PasswordSafe.instance.set(CredentialAttributes("DP", "dp"),
                com.intellij.credentialStore.Credentials("dp",  "$authToken;$BASE_URL")
            )

            com.tfc.ulht.dpplugin.State.client.login(authToken, null)
        }

        credentials = getCredentials()

        callListeners()
    }

    @Transient
    private val listeners = mutableListOf<(ApplicationSettings) -> Unit>()

    private fun callListeners() {
        for (listener in listeners) {
            listener(this)
        }
    }

    fun addListener(listener: (ApplicationSettings) -> Unit) {
        listeners.add(listener)
    }

    override fun getState(): ApplicationSettings {
        return this
    }

    override fun loadState(state: ApplicationSettings) {
        credentials = getCredentials()
    }
}