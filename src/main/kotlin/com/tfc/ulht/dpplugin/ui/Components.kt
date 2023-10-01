package com.tfc.ulht.dpplugin.ui

import com.intellij.ui.JBColor
import com.tfc.ulht.dpplugin.dplib.Assignment
import com.tfc.ulht.dpplugin.dplib.SubmissionsResponse
import java.awt.Cursor
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.font.TextAttribute
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel

// TODO: Create a component superclass

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
            this.foreground = JBColor.BLUE

            val style = this.font.attributes.toMutableMap()
            style[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
            this.font = this.font.deriveFont(style)

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
    val idLabel: JLabel
    val submissionDownloadLabel: JLabel

    init {
        this.layout = BoxLayout(this, BoxLayout.X_AXIS)
        this.alignmentX = 0.0f

        idLabel = JLabel("<html><h3>${submissions.projectGroup.authors.joinToString { a -> "student" + a.id }}</h3></html>")
        this.add(idLabel)

        this.add(Box.createHorizontalGlue())

        submissionDownloadLabel = JLabel("Download").apply {
            this.foreground = JBColor.BLUE

            val style = this.font.attributes.toMutableMap()
            style[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
            this.font = this.font.deriveFont(style)

            this.cursor = Cursor(Cursor.HAND_CURSOR)
        }

        this.add(submissionDownloadLabel)
    }

    fun addSubmissionDownloadClickListener(listener: (MouseEvent?) -> Unit) = submissionDownloadLabel.addMouseListener(object : MouseListener {
        override fun mouseClicked(e: MouseEvent?) = listener(e)

        override fun mousePressed(e: MouseEvent?) {  }

        override fun mouseReleased(e: MouseEvent?) {  }

        override fun mouseEntered(e: MouseEvent?) {  }

        override fun mouseExited(e: MouseEvent?) {  }
    })
}