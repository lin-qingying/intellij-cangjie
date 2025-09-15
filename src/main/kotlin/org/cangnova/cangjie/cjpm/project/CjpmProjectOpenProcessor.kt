package org.cangnova.cangjie.cjpm.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenProcessor
import org.cangnova.cangjie.cjpm.CjpmConstants
import org.cangnova.cangjie.cjpm.project.model.guessAndSetupCangJieProject
import org.cangnova.cangjie.icon.CangJieIcons
import javax.swing.Icon


class CjpmProjectOpenProcessor : ProjectOpenProcessor() {

    override val icon: Icon get() = CangJieIcons.TOML
    override val name: String get() = "Cjpm"

    override fun canOpenProject(file: VirtualFile): Boolean {
        return FileUtil.namesEqual(file.name, CjpmConstants.MANIFEST_FILE) ||
                file.isDirectory && file.findChild(CjpmConstants.MANIFEST_FILE) != null
    }

    override fun doOpenProject(virtualFile: VirtualFile, projectToClose: Project?, forceOpenInNewFrame: Boolean): Project? {
        val basedir = if (virtualFile.isDirectory) virtualFile else virtualFile.parent

        return PlatformProjectOpenProcessor.getInstance().doOpenProject(basedir, projectToClose, forceOpenInNewFrame)?.also {
            StartupManager.getInstance(it).runWhenProjectIsInitialized { guessAndSetupCangJieProject(it) }
        }
    }
}
