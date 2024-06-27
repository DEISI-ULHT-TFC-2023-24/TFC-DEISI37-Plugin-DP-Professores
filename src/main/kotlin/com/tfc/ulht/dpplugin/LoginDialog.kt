package com.tfc.ulht.dpplugin

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.BrowserUtil
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.tfc.ulht.dpplugin.dplib.BASE_URL
import com.tfc.ulht.dpplugin.dplib.addSuffix
import com.tfc.ulht.dpplugin.dplib.checkAndAddPrefix
import com.tfc.ulht.dpplugin.settings.ApplicationSettings
import okhttp3.Credentials
import java.awt.Dimension
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import javax.swing.*
import javax.swing.event.DocumentEvent

class LoginDialog(project: Project?) : DialogWrapper(project, null, false, IdeModalityType.IDE, false) {
    private val instanceField = JBTextField()
    private val userField = JBTextField()
    private val tokenField = JBPasswordField()
    private val tokenUrlLink = ActionLink("Get token")
    private val loginButton = JButton("Login")
    private val resultLabel = JBLabel()
    private val messageLabel = JBLabel()

    private var callback: (() -> Unit)? = null

    constructor(project: Project?, callback: () -> Unit) : this(project) {
        this.callback = callback
    }

    init {
        this.setSize(325, 150)

        init()
        title = "DP Teacher Plugin - Login"

        val settings = ApplicationSettings.getSettings()

        userField.text = settings.username ?: ""
        tokenField.text = settings.token ?: ""
        instanceField.text = settings.url

        loginButton.addActionListener {
            Credentials.basic(userField.text, tokenField.text).let { token ->
                BASE_URL =
                    instanceField.text
                        .checkAndAddPrefix(
                            listOf("http://", "https://"),
                            if (instanceField.text.contains(":") && !instanceField.text.contains("/")) "http://" else "https://"
                        )
                        .addSuffix("/")
                        .ifBlank { BASE_URL }

                State.client.login(token) { result, response ->
                    PasswordSafe.instance.set(
                        CredentialAttributes("DP", "dp"),
                        com.intellij.credentialStore.Credentials("dp", if (result) "$token;$BASE_URL" else BASE_URL)
                    )

                    resultLabel.text = "Login " + if (result) "successful" else "unsuccessful"
                    if (!result) {
                        val fullMessage =
                            response ?: "Couldn't send request to server"

                        messageLabel.text = fullMessage
                    } else {
                        messageLabel.text = "You can close this window now."
                    }

                    resultLabel.border = BorderFactory.createEmptyBorder(5, 0, 0, 0)
                    messageLabel.border = BorderFactory.createEmptyBorder(0, 0, 10, 0)

                    callback?.let { it() }
                }
            }

            messageLabel.text = "Login in progress..."
        }
    }

    override fun createCenterPanel(): JComponent = JPanel().apply {
        val labels = mutableListOf<JLabel>()

        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.add(JPanel().apply {
            this.layout = BoxLayout(this, BoxLayout.X_AXIS)

            JLabel("Server: ").also {
                labels.add(it)
                this.add(it)
            }

            instanceField.document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    tokenUrlLink.model.isEnabled = e.document.length > 0
                    tokenUrlLink.isVisible = true
                }
            })

            this.add(instanceField)
        })
        this.add(JPanel().apply {
            this.layout = BoxLayout(this, BoxLayout.X_AXIS)

            val label = JLabel("User: ")
            labels.add(label)

            this.add(label)
            this.add(userField)

            this.addComponentListener(object : ComponentListener {
                override fun componentResized(e: ComponentEvent?) {
                    e!!
                    e.component.size = Dimension(e.component.parent.width, e.component.height)
                }

                override fun componentMoved(e: ComponentEvent?) {}

                override fun componentShown(e: ComponentEvent?) {}

                override fun componentHidden(e: ComponentEvent?) {}

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

            this.add(tokenUrlLink.apply {
                addActionListener {
                    BrowserUtil.browse(
                        instanceField.text
                            .checkAndAddPrefix(
                                listOf("http://", "https://"),
                                if (instanceField.text.contains(":") && !instanceField.text.contains("/")) "http://" else "https://"
                            )
                            .addSuffix("/") + "personalToken"
                    )
                }

                model.isEnabled = tokenUrlLink.text.isNotEmpty()
                isVisible = true

                setExternalLinkIcon()
            })

            this.add(Box.createHorizontalGlue())
        })
        this.add(JPanel().apply {
            this.layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(Box.createHorizontalGlue())
            add(loginButton)
        })

        this.add(resultLabel)
        this.add(messageLabel)

        val highestWidth = labels.map { it.preferredSize.width }.maxOf { it }

        labels.forEach {
            it.preferredSize = Dimension(highestWidth, it.preferredSize.height)
        }
    }
}