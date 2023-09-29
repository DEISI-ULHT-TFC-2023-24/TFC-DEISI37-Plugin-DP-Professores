package com.tfc.ulht.dpplugin.ui

import com.tfc.ulht.dpplugin.dplib.Assignment
import java.awt.Cursor
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*

class AssignmentComponent(assignment: Assignment) : JComponent() {
    init {
        this.layout = BoxLayout(this, BoxLayout.X_AXIS)
        this.alignmentX = 0.0f

        this.add(JLabel("<html><h3>${assignment.id}</h3></html>"))

        this.add(Box.createHorizontalGlue())

        this.add(JLabel("Submissions").apply {
            this.cursor = Cursor(Cursor.HAND_CURSOR)
            this.addMouseListener(object : MouseListener {
                override fun mouseClicked(e: MouseEvent?) {
                    // TODO Not yet implemented
                }

                override fun mousePressed(e: MouseEvent?) {  }

                override fun mouseReleased(e: MouseEvent?) {  }

                override fun mouseEntered(e: MouseEvent?) {  }

                override fun mouseExited(e: MouseEvent?) {  }
            })
        })
    }
}