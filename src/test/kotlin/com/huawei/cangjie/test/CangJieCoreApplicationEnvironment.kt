package com.huawei.cangjie.test

import com.intellij.DynamicBundle
import com.intellij.codeInsight.ContainerProvider
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.lang.MetaLanguage
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.Disposable
import com.intellij.psi.FileContextProvider
import com.intellij.psi.impl.smartPointers.SmartPointerAnchorProvider
import com.intellij.psi.meta.MetaDataContributor

class CangJieCoreApplicationEnvironment private constructor(
    parentDisposable: Disposable,
    environmentMode: CangJieCoreApplicationEnvironmentMode,
) : CoreApplicationEnvironment(parentDisposable, environmentMode == CangJieCoreApplicationEnvironmentMode.UnitTest) {

    companion object {
        private fun registerExtensionPoints() {
            registerApplicationExtensionPoint(
                DynamicBundle.LanguageBundleEP.EP_NAME,
                DynamicBundle.LanguageBundleEP::class.java
            )
            registerApplicationExtensionPoint(FileContextProvider.EP_NAME, FileContextProvider::class.java)
            registerApplicationExtensionPoint(MetaDataContributor.EP_NAME, MetaDataContributor::class.java)
            registerApplicationExtensionPoint(ContainerProvider.EP_NAME, ContainerProvider::class.java)
            registerApplicationExtensionPoint(MetaLanguage.EP_NAME, MetaLanguage::class.java)
            registerApplicationExtensionPoint(
                SmartPointerAnchorProvider.EP_NAME,
                SmartPointerAnchorProvider::class.java
            )

        }

        fun create(
            parentDisposable: Disposable,
            unitTestMode: Boolean,
        ): CangJieCoreApplicationEnvironment {
            return create(parentDisposable, CangJieCoreApplicationEnvironmentMode.fromUnitTestModeFlag(unitTestMode))
        }

        fun create(
            parentDisposable: Disposable,
            environmentMode: CangJieCoreApplicationEnvironmentMode,
        ): CangJieCoreApplicationEnvironment {
            val environment = CangJieCoreApplicationEnvironment(parentDisposable, environmentMode)
            registerExtensionPoints()
            return environment
        }

    }

}
