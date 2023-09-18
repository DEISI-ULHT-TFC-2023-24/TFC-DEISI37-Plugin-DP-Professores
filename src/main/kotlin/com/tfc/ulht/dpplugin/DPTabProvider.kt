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
import com.tfc.ulht.dpplugin.dplib.Assignment
import java.beans.PropertyChangeListener
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class DPTabProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file is com.tfc.ulht.dpplugin.VirtualFile

    override fun createEditor(project: Project, file: VirtualFile): FileEditor = DPTab((file as com.tfc.ulht.dpplugin.VirtualFile).assignment)

    override fun getEditorTypeId(): String = "dp-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

}

class DPTab(private val data: List<Assignment>) : FileEditor {
    private val root: JPanel = JPanel().apply {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    private val userData = UserDataHolderBase()

    init {
        data.forEach {
            root.add(JLabel(it.name))
        }
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