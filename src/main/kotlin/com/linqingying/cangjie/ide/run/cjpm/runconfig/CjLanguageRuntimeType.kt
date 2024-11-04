package com.linqingying.cangjie.ide.run.cjpm.runconfig

import com.linqingying.cangjie.icon.CangJieIcons
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.target.LanguageRuntimeType
import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.execution.target.TargetEnvironmentType
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import java.util.function.Supplier
import javax.swing.Icon

class CjLanguageRuntimeType : LanguageRuntimeType<CjLanguageRuntimeConfiguration>(TYPE_ID) {
    companion object {
        const val TYPE_ID: String = "CjLanguageRuntime"
    }

    override val configurableDescription: String
        get() = TODO("Not yet implemented")
    override val displayName: String
        get() = TODO("Not yet implemented")
    override val icon: Icon = CangJieIcons.CANGJIE
    override val launchDescription: String
        get() = TODO("Not yet implemented")

    override fun createConfigurable(
        project: Project,
        config: CjLanguageRuntimeConfiguration,
        targetEnvironmentType: TargetEnvironmentType<*>,
        targetSupplier: Supplier<TargetEnvironmentConfiguration>
    ): Configurable {
        TODO("Not yet implemented")
    }

    override fun createDefaultConfig(): CjLanguageRuntimeConfiguration {
        TODO("Not yet implemented")
    }

    override fun findLanguageRuntime(target: TargetEnvironmentConfiguration): CjLanguageRuntimeConfiguration? {
        TODO("Not yet implemented")
    }

    override fun isApplicableTo(runConfig: RunnerAndConfigurationSettings): Boolean {
        TODO("Not yet implemented")
    }

    override fun duplicateConfig(config: CjLanguageRuntimeConfiguration): CjLanguageRuntimeConfiguration {
        TODO("Not yet implemented")
    }

    override fun createSerializer(config: CjLanguageRuntimeConfiguration): PersistentStateComponent<*> {
        TODO("Not yet implemented")
    }
}
