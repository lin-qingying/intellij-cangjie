@file:JvmName("LanguageVersionSettingsProviderUtils")
package com.huawei.cangjie.idea.projectStructure

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.config.LanguageVersionSettingsImpl
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiElement

val PsiElement.languageVersionSettings: LanguageVersionSettings
    get() {
        return LanguageVersionSettingsImpl.DEFAULT
//        if (project.serviceOrNull<ProjectFileIndex>() == null) {
//            return LanguageVersionSettingsImpl.DEFAULT
//        }
//
//        return runReadAction {
//            project.service<LanguageSettingsProvider>().getLanguageVersionSettings(this.moduleInfo, project)
//        }
    }
