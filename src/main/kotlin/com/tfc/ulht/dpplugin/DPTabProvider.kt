package com.tfc.ulht.dpplugin

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.ui.JBUI
import com.tfc.ulht.dpplugin.dplib.Assignment
import com.tfc.ulht.dpplugin.ui.AssignmentComponent
import java.awt.Dimension
import java.beans.PropertyChangeListener
import javax.swing.*

class DPTabProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file is com.tfc.ulht.dpplugin.VirtualFile

    override fun createEditor(project: Project, file: VirtualFile): FileEditor = DPTab((file as com.tfc.ulht.dpplugin.VirtualFile).assignment)

    override fun getEditorTypeId(): String = "dp-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

}

class DPTab(private val data: List<Assignment>) : FileEditor {
    private val root: JPanel = JPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.border = JBUI.Borders.empty(0, 20)
    }

    private val userData = UserDataHolderBase()

    init {
        root.add(JLabel("<html><h1>Assignments</h1></html").apply { alignmentX = 0.0f })
        val assignmentPanel = JPanel().apply {
            this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
            this.border = JBUI.Borders.empty(0, 10)
        }
        root.add(assignmentPanel)

        data.forEach {
            assignmentPanel.add(AssignmentComponent(it))

            assignmentPanel.add(JSeparator(SwingConstants.HORIZONTAL).apply {
                maximumSize = Dimension(maximumSize.width, 2)
            })
        }

        if (assignmentPanel.componentCount > 1) assignmentPanel.remove(assignmentPanel.componentCount - 1)
    }

    override fun <T : Any?> getUserData(key: Key<T>): T? = userData.getUserData(key)

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) = userData.putUserData(key, value)

    override fun dispose() {  }

    override fun getComponent(): JComponent = root

    override fun getPreferredFocusedComponent(): JComponent = root

    override fun getName(): String = "DP"

    override fun setState(state: FileEditorState) {  }

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = true

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {  }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {  }

    override fun getFile(): VirtualFile = VirtualFile(data)
}

class VirtualFile(val assignment: List<Assignment>) : LightVirtualFile()