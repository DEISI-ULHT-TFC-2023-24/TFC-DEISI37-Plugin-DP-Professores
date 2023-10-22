package com.tfc.ulht.dpplugin.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.tfc.ulht.dpplugin.dplib.Assignment
import com.tfc.ulht.dpplugin.dplib.Submission
import com.tfc.ulht.dpplugin.dplib.SubmissionsResponse
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.font.TextAttribute
import javax.swing.Box
import javax.swing.Box.Filler
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel

class DashboardItemComponent(id: Int, text: String, icon: Icon?, listener: (Int) -> Unit) : JLabel(text, icon, LEFT) {
    init {
        this.foreground = JBColor.BLUE

        this.font = JBFont.h2().deriveFont(JBFont.h2().attributes.toMutableMap().apply {
            this[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
        })

        this.cursor = Cursor(Cursor.HAND_CURSOR)

        this.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) = listener(id)

            override fun mousePressed(e: MouseEvent?) {  }

            override fun mouseReleased(e: MouseEvent?) {  }

            override fun mouseEntered(e: MouseEvent?) {  }

            override fun mouseExited(e: MouseEvent?) {  }
        })
    }
}

open class DPComponent : JComponent() {
    val fillers: MutableList<Component> = mutableListOf()

    init {
        this.layout = BoxLayout(this, BoxLayout.X_AXIS)
        this.border = JBUI.Borders.empty(2, 0)
        this.alignmentX = 0.0f

        this.add(Box.createHorizontalGlue())
    }

    protected fun addComponent(component: Component) {
        if (component is Filler) {
            fillers.add(component)
        }

        for (i in 0..components.size) {
            if (components[i] is Filler && !fillers.contains(components[i])) {
                add(component, i)
                return
            }
        }

        add(component, -1)
    }

    protected fun addComponentEnd(component: JComponent) = add(component, -1)
}

class AssignmentComponent(val assignment: Assignment) : DPComponent() {
    val idLabel: JLabel
    val submissionsLabel: JLabel

    init {
        idLabel = CustomLabel(assignment.id)
        this.addComponent(idLabel)

        assignment.dueDate?.let {
            this.addComponent(Box.createRigidArea(Dimension(10, 0)))
            this.addComponent(LabelWithDescription(it, "due"))
        }

        submissionsLabel = JLabel("Submissions").apply {
            this.foreground = JBColor.BLUE

            val style = this.font.attributes.toMutableMap()
            style[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
            this.font = this.font.deriveFont(style)

            this.cursor = Cursor(Cursor.HAND_CURSOR)
        }

        this.addComponentEnd(submissionsLabel)
    }

    fun addSubmissionClickListener(listener: (MouseEvent?) -> Unit) = submissionsLabel.addMouseListener(object : MouseListener {
        override fun mouseClicked(e: MouseEvent?) = listener(e)

        override fun mousePressed(e: MouseEvent?) {  }

        override fun mouseReleased(e: MouseEvent?) {  }

        override fun mouseEntered(e: MouseEvent?) {  }

        override fun mouseExited(e: MouseEvent?) {  }
    })
}

class GroupSubmissionsComponent(submissions: SubmissionsResponse) : DPComponent() {
    private val idLabel: JLabel
    private val allSubmissions: NumberBox
    private val submissionDownloadLabel: JLabel

    init {
        idLabel = CustomLabel(submissions.projectGroup.authors.joinToString { a -> "student" + a.id })
        this.addComponent(idLabel)
        this.addComponent(Box.createRigidArea(Dimension(10, 0)))
        allSubmissions = NumberBox(submissions.allSubmissions.size)
        this.addComponent(allSubmissions)

        submissionDownloadLabel = JLabel("Download").apply {
            this.foreground = JBColor.BLUE

            this.font = this.font.deriveFont(this.font.attributes.toMutableMap().apply {
                this[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
            })

            this.cursor = Cursor(Cursor.HAND_CURSOR)
        }

        this.addComponentEnd(submissionDownloadLabel)
    }

    fun addSubmissionDownloadClickListener(listener: (MouseEvent?) -> Unit) = submissionDownloadLabel.addMouseListener(object : MouseListener {
        override fun mouseClicked(e: MouseEvent?) = listener(e)

        override fun mousePressed(e: MouseEvent?) {  }

        override fun mouseReleased(e: MouseEvent?) {  }

        override fun mouseEntered(e: MouseEvent?) {  }

        override fun mouseExited(e: MouseEvent?) {  }
    })

    fun addAllSubmissionsClickListener(listener: () -> Unit) {
        allSubmissions.clickListener = listener
    }
}

class LabelWithDescription(text: String, description: String) : JComponent() {
    init {
        this.layout = BoxLayout(this, BoxLayout.X_AXIS)

        this.add(JLabel("$description:").apply {
            this.font = JBFont.small().asBold()
            this.alignmentY = 0.4f
        })

        this.add(JLabel(text))
    }
}

class NumberBox(number: Int) : JComponent() {
    private val numString = number.toString()
    var mouseOver = false

    var clickListener: (() -> Unit)? = null

    init {
        this.minimumSize = Dimension(5, 10)
        cursor = Cursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent) {
                clickListener?.invoke()
            }
            override fun mousePressed(e: MouseEvent) {}
            override fun mouseReleased(e: MouseEvent) {}
            override fun mouseEntered(e: MouseEvent) {
                mouseOver = true
                repaint()
            }

            override fun mouseExited(e: MouseEvent) {
                mouseOver = false
                repaint()
            }
        })
    }

    override fun getMaximumSize(): Dimension {
        return Dimension(25, 25)
    }

    override fun getPreferredSize(): Dimension {
        return maximumSize
    }

    public override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.color = JBColor.namedColor("Separator.separatorColor")

        g2.drawRoundRect(0, 0, width - 1, height - 1, width / 4, height / 4)

        if (mouseOver) {
            g2.color = Color(JBColor.namedColor("Panel.background").rgb and (0xFF shl 4 * 8), true)
            g2.fillRoundRect(1, 1, width - 2, height - 2, width / 4, height / 4)
            g2.color = JBColor.BLACK
        }

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        val rec = g.getFont().getStringBounds(numString, g2.fontRenderContext)
        g.drawString(numString, (width / 2 - rec.width / 2).toInt() + 1, (height / 2 - rec.y / 2).toInt())

        super.paintComponent(g)
    }
}

class SubmissionComponent(val submission: Submission) : DPComponent() {
    private val idLabel: JLabel
    private val submissionDownloadLabel: JLabel

    init {
        idLabel = CustomLabel("${submission.id}: ${submission.submissionDate}")
        this.addComponent(idLabel)

        submissionDownloadLabel = JLabel("Download").apply {
            this.foreground = JBColor.BLUE

            val style = this.font.attributes.toMutableMap()
            style[TextAttribute.UNDERLINE] = TextAttribute.UNDERLINE_ON
            this.font = this.font.deriveFont(style)

            this.cursor = Cursor(Cursor.HAND_CURSOR)
        }

        this.addComponentEnd(submissionDownloadLabel)
    }

    fun addSubmissionDownloadClickListener(listener: (MouseEvent?) -> Unit) = submissionDownloadLabel.addMouseListener(object : MouseListener {
        override fun mouseClicked(e: MouseEvent?) = listener(e)

        override fun mousePressed(e: MouseEvent?) {  }

        override fun mouseReleased(e: MouseEvent?) {  }

        override fun mouseEntered(e: MouseEvent?) {  }

        override fun mouseExited(e: MouseEvent?) {  }
    })
}
