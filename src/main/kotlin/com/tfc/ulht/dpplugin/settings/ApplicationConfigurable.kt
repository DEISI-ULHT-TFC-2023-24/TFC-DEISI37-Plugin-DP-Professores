package com.tfc.ulht.dpplugin.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.util.ui.FormBuilder
import java.net.URL
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JTextField

class ApplicationConfigurable: SearchableConfigurable {
    private val settings = ApplicationSettings.getSettings()

    private val urlField: JTextField = JTextField()
    private val usernameField: JTextField = JTextField()
    private val tokenField: JTextField = JPasswordField()

    override fun createComponent(): JComponent? {
        reset()

        return FormBuilder.createFormBuilder()
            .addLabeledComponent("DP URL: ", urlField)
            .addLabeledComponent("Username: ", usernameField)
            .addLabeledComponent("API token: ", tokenField)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    private fun isUrlValid(url: String): Boolean {
        return try {
            URL(url).toURI()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun isModified(): Boolean {
        return (urlField.text != settings.url
                || usernameField.text != settings.username
                || tokenField.text != settings.token)
                &&
                (urlField.text.isNotEmpty()
                        && isUrlValid(urlField.text)
                        && usernameField.text.isNotEmpty()
                        && tokenField.text.isNotEmpty())
    }

    override fun apply() {
        settings.apply {
            url = urlField.text
            setCredentials(usernameField.text, tokenField.text)
        }
    }

    override fun getDisplayName(): String {
        return "Drop Project"
    }

    override fun getId(): String {
        return "com.tfc.ulht.dpplugin.settings.ApplicationConfigurable"
    }

    override fun reset() {
        urlField.text = settings.url
        usernameField.text = settings.username
        tokenField.text = settings.token
    }
}