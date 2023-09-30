package com.tfc.ulht.dpplugin.ui

import com.tfc.ulht.dpplugin.dplib.*
import java.awt.Cursor
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*

class AssignmentComponent(val assignment: Assignment) : JComponent() {
    val idLabel: JLabel
    val submissionsLabel: JLabel

    init {
        this.layout = BoxLayout(this, BoxLayout.X_AXIS)
        this.alignmentX = 0.0f

        idLabel = JLabel("<html><h3>${assignment.id}</h3></html>")
        this.add(idLabel)

        this.add(Box.createHorizontalGlue())

        submissionsLabel = JLabel("Submissions").apply {
            this.cursor = Cursor(Cursor.HAND_CURSOR)
        }

        this.add(submissionsLabel)
    }

    fun addSubmissionClickListener(listener: (MouseEvent?) -> Unit) = submissionsLabel.addMouseListener(object : MouseListener {
        override fun mouseClicked(e: MouseEvent?) = listener(e)

        override fun mousePressed(e: MouseEvent?) {  }

        override fun mouseReleased(e: MouseEvent?) {  }

        override fun mouseEntered(e: MouseEvent?) {  }

        override fun mouseExited(e: MouseEvent?) {  }
    })
}

class GroupSubmissionsComponent(submissions: SubmissionsResponse) : JComponent() {
    init {
        this.layout = BoxLayout(this, BoxLayout.X_AXIS)
        this.alignmentX = 0.0f

        this.add(JLabel("<html><h3>${submissions.projectGroup.id}</h3></html>"))
    }
}