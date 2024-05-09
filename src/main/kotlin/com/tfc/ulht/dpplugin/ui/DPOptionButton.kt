package com.tfc.ulht.dpplugin.ui

import com.intellij.ide.ui.laf.darcula.ui.DarculaOptionButtonUI
import com.intellij.ui.components.JBOptionButton
import java.awt.Graphics2D
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JComponent

class DPOptionButton(text: String) : JBOptionButton(null, null) {
    class DPDarculaOptionButtonUI : DarculaOptionButtonUI() {
        override fun paintSeparator(g: Graphics2D, c: JComponent) {
            return
        }

        fun getDropdownButton() = arrowButton
    }

    init {
        setUI(DPDarculaOptionButtonUI())

        action = object : AbstractAction(text) {
            override fun actionPerformed(e: ActionEvent?) {
                (ui as DPDarculaOptionButtonUI).getDropdownButton().doClick()
            }
        }
    }
}