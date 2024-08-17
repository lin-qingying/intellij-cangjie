package com.huawei.cangjie.ide.notifications


import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.cjpm.CjpmConstants
import com.huawei.cangjie.cjpm.project.model.CjpmProjectsService
import com.huawei.cangjie.cjpm.project.model.cjpmProjects
import com.huawei.cangjie.cjpm.project.model.guessAndSetupCangJieProject
import com.huawei.cangjie.cjpm.project.settings.CjProjectSettingsServiceBase
import com.huawei.cangjie.cjpm.project.settings.CjProjectSettingsServiceBase.Companion.CANGJIE_SETTINGS_TOPIC
import com.huawei.cangjie.cjpm.project.settings.cangjieSettings
import com.huawei.cangjie.ide.run.cjpm.isUnitTestMode
import com.huawei.cangjie.ide.run.cjpm.toolchain
import com.huawei.cangjie.lang.CangJieFileType
import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile


class MissingToolchainNotificationProvider(project: Project) : CjNotificationProvider(project), DumbAware {

    override val VirtualFile.disablingKey: String get() = NOTIFICATION_STATUS_KEY

    init {
        project.messageBus.connect().apply {
            subscribe(CANGJIE_SETTINGS_TOPIC, object : CjProjectSettingsServiceBase.CjSettingsListener {
                override fun <T : CjProjectSettingsServiceBase.CjProjectSettingsBase<T>> settingsChanged(e: CjProjectSettingsServiceBase.SettingsChangedEventBase<T>) {
                    updateAllNotifications()
                }
            })

            subscribe(CjpmProjectsService.CJPM_PROJECTS_TOPIC, CjpmProjectsService.CjpmProjectsListener { _, _ ->
                updateAllNotifications()
            })
        }
    }

    override fun createNotificationPanel(
        file: VirtualFile,
        editor: FileEditor,
        project: Project
    ): CjEditorNotificationPanel? {
        if (isUnitTestMode) return null
        if (!(file.isCangJieFile || file.isCjpmManifestFile) || isNotificationDisabled(file)) return null
        @Suppress("UnstableApiUsage")
        if (!project.isTrusted()) return null
        if (guessAndSetupCangJieProject(project)) return null

        val toolchain = project.toolchain
        if (toolchain == null || !toolchain.looksLikeValidToolchain()) {
            return createBadToolchainPanel(file)
        }

        val cjpmProjects = project.cjpmProjects

        if (!cjpmProjects.initialized) return null


        return null
    }

    private fun createBadToolchainPanel(file: VirtualFile): CjEditorNotificationPanel =
        CjEditorNotificationPanel(NO_CANGJIE_TOOLCHAIN).apply {
            text = CangJieBundle.message("notification.no.toolchain.configured")
            createActionLabel(CangJieBundle.message("notification.action.set.up.toolchain.text")) {
                project.cangjieSettings.configureToolchain()
            }
            createActionLabel(CangJieBundle.message("notification.action.do.not.show.again.text")) {
                disableNotification(file)
                updateAllNotifications()
            }
        }


    companion object {
        private const val NOTIFICATION_STATUS_KEY = "com.huawei.cangjie.hideToolchainNotifications"
        const val NO_CANGJIE_TOOLCHAIN = "NoCangjieToolchain"

    }
}

val VirtualFile.isCangJieFile: Boolean get() = fileType == CangJieFileType.INSTANCE
val VirtualFile.isCjpmManifestFile: Boolean get() = name in CjpmConstants.MANIFEST_FILE
