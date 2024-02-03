package com.tfc.ulht.dpplugin

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.util.preferredWidth
import com.tfc.ulht.dpplugin.dplib.BASE_URL
import com.tfc.ulht.dpplugin.dplib.addSuffix
import okhttp3.Credentials
import java.awt.Dimension
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import javax.swing.*

class LoginDialog(project: Project?) : DialogWrapper(project, null, false, IdeModalityType.PROJECT, false) {
    private val userField = JBTextField()
    private val tokenField = JBTextField()
    private val instanceField = JBTextField()
    private val loginButton = JButton("Login")
    private val resultLabel = JBLabel()

    init {
        this.setSize(250, 150)

        init()
        title = "DP - Login"

        loginButton.addActionListener {
            Credentials.basic(userField.text, tokenField.text).let { token ->
                BASE_URL = instanceField.text.addSuffix("/").ifBlank { BASE_URL }
                State.client.login(token) { res ->
                    PasswordSafe.instance.set(
                        CredentialAttributes("DP", "dp"),
                        com.intellij.credentialStore.Credentials(if (res) token else null, BASE_URL))

                    resultLabel.text = "Login " + if (res) "successful" else "unsuccessful"
                    resultLabel.border = BorderFactory.createEmptyBorder(5, 0, 10, 0)
                }
            }
        }
    }

    override fun createCenterPanel(): JComponent = JPanel().apply {
        val labels = mutableListOf<JLabel>()

        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.add(JPanel().apply {
            this.layout = BoxLayout(this, BoxLayout.X_AXIS)

            val label = JLabel("User: ")
            labels.add(label)

            this.add(label)
            this.add(userField)

            this.addComponentListener(object: ComponentListener {
                override fun componentResized(e: ComponentEvent?) {
                    e!!
                    e.component.size = Dimension(e.component.parent.width, e.component.height)
                }

                override fun componentMoved(e: ComponentEvent?) {  }

                override fun componentShown(e: ComponentEvent?) {  }

                override fun componentHidden(e: ComponentEvent?) {  }

            })
        })
        this.add(JPanel().apply {
            this.layout = BoxLayout(this, BoxLayout.X_AXIS)


            JLabel("Token: ").also {
                labels.add(it)
                this.add(it)
            }

            this.add(tokenField)
        })
        this.add(JPanel().apply {
            this.layout = BoxLayout(this, BoxLayout.X_AXIS)

            JLabel("Server: ").also {
                labels.add(it)
                this.add(it)
            }

            this.add(instanceField)
        })
        this.add(JPanel().apply {
            this.layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(Box.createHorizontalGlue())
            add(loginButton)
        })
        this.add(resultLabel)

        val highestWidth = labels.map { it.preferredWidth }.maxOf { it }

        labels.forEach {
            it.preferredWidth = highestWidth
        }
    }
}