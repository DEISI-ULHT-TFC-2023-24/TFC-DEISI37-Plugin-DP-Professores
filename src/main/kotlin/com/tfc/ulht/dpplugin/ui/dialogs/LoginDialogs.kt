package com.tfc.ulht.dpplugin.ui.dialogs

import java.awt.Component
import javax.swing.JOptionPane

fun showLoginInProgressDialog(parent: Component?) {
    JOptionPane.showMessageDialog(
        parent,
        "A login attempt is still in progress.",
        "Login in progress",
        JOptionPane.INFORMATION_MESSAGE
    )
}
