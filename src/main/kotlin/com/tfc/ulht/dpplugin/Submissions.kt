package com.tfc.ulht.dpplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.ProjectManager
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.InputStream
import javax.swing.JOptionPane

class SubmissionsAction : AnAction() {
    companion object {
        private fun createFile(id: String, stream: InputStream): String? {
            val path = System.getProperty("java.io.tmpdir") + File.separatorChar + "sub_$id.zip"

            try {
                val file = File(path)

                file.outputStream().write(stream.readAllBytes())
            } catch (e: Exception) {
                Logger.getInstance(SubmissionsAction::class.java).error("Couldn't create file, ${e.message}")
                return null
            }

            return path
        }

        private fun unzipSubmission(path: String): String? {
            val finalPath = path.removeSuffix(".zip")

            try {
                with (File(finalPath)) {
                    if (this.exists()) this.deleteRecursively()
                }

                ZipFile(path).extractAll(finalPath)

                File(path).delete()
            } catch (e: Exception) {
                Logger.getInstance(SubmissionsAction::class.java).error("Couldn't extract file, ${e.message}")
                return null
            }

            return finalPath
        }

        fun openSubmission(id: String) {

            State.client.downloadSubmission(id) {
                if (it == null) {
                    JOptionPane.showMessageDialog(
                        null, "Error", "Couldn't download the submission", JOptionPane.ERROR_MESSAGE
                    )
                } else {
                    val unzippedPath = createFile(id, it)?.let { path -> unzipSubmission(path) }

                    if (unzippedPath != null) {
                        ProjectManager.getInstance().loadAndOpenProject(unzippedPath)
                    } else {
                        JOptionPane.showMessageDialog(
                            null, "Error", "Couldn't extract the submission", JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        openSubmission(JOptionPane.showInputDialog("Please input the submission ID."))
    }
}