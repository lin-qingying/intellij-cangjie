package com.linqingying.cangjie.cjpm.project.model.impl

import com.linqingying.cangjie.cjpm.CjpmConstants
import com.linqingying.cangjie.cjpm.project.model.CjpmProject
import com.linqingying.cangjie.cjpm.project.model.cjpmProjects
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service
class CjpmSettingsFilesService(private val project: Project) {


    @Volatile
    private var settingsFilesCache: Map<String, SettingFileType>? = null
    private fun collectSettingsFiles(): Map<String, SettingFileType> {
        val result = mutableMapOf<String, SettingFileType>()
        for (cjpmProject in project.cjpmProjects.allProjects) {
            cjpmProject.collectSettingsFiles(result)
        }

        settingsFilesCache = result

        return result
    }

    private fun CjpmProject.collectSettingsFiles(out: MutableMap<String, SettingFileType>) {
        val rootPath = rootDir?.path
        if (rootPath != null) {
            CjpmConstants.MANIFEST_FILE.forEach {
                out["$rootPath/$it"] = SettingFileType.CONFIG
            }
            CjpmConstants.LOCK_FILE.forEach {
                out["$rootPath/$it"] = SettingFileType.CONFIG
            }

//            out["$rootPath/${CjpmConstants.MANIFEST_FILE}"] = SettingFileType.CONFIG
//            out["$rootPath/${CjpmConstants.LOCK_FILE}"] = SettingFileType.CONFIG

        }


    }

    fun collectSettingsFiles(useCache: Boolean): Map<String, SettingFileType> {
        return if (useCache) {
            settingsFilesCache ?: collectSettingsFiles()
        } else {
            collectSettingsFiles()
        }
    }

    companion object {
        fun getInstance(project: Project): CjpmSettingsFilesService = project.service()

    }

    enum class SettingFileType {
        CONFIG,
        IMPLICIT_TARGET
    }
}
