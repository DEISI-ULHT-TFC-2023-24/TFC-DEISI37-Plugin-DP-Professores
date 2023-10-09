package com.tfc.ulht.dpplugin.ui

import java.awt.Font
import java.awt.font.TextAttribute
import javax.swing.JLabel

class CustomLabel(text: String) : JLabel(text) {
    init {
        val attrs = this.font.attributes.toMutableMap()
        attrs[TextAttribute.WEIGHT] = TextAttribute.WEIGHT_EXTRABOLD
        this.font = Font(attrs)
    }
}