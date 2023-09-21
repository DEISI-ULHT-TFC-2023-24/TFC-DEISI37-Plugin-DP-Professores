package com.tfc.ulht.dpplugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.tfc.ulht.dpplugin.dplib.DPClient
import java.awt.Dimension
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import javax.swing.*

class LoginDialog(project: Project?) : DialogWrapper(project) {
    private val userField = JTextField().apply { this.maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE) }
    private val tokenField = JTextField()
    private val loginButton = JButton("Login")
    private val resultLabel = JLabel()

    init {
        init()
        title = "DP - Login"

        loginButton.addActionListener {
            State.client.login(userField.text, tokenField.text) { res ->
                resultLabel.text = "Login " + if (res) "successful" else "unsuccessful"
            }
        }
    }

    override fun createCenterPanel(): JComponent = JPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.add(JPanel().apply {
            val label = JLabel("User: ")
            label.maximumSize = Dimension(this.width / 4, this.height)
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
        this.add(JPanel().apply { this.add(JLabel("Token: ")); this.add(tokenField) })
        this.add(loginButton)
        this.add(resultLabel)
    }
}