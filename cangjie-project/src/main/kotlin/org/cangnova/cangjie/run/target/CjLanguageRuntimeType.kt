/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

@file:Suppress("UnstableApiUsage")

package org.cangnova.cangjie.run.target

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.target.LanguageRuntimeType
import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.execution.target.TargetEnvironmentType
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import java.util.function.Supplier
import javax.swing.Icon

/**
 * CangJie language runtime type for targetPlatform environments
 */
class CjLanguageRuntimeType : LanguageRuntimeType<CjLanguageRuntimeConfiguration>(TYPE_ID) {

    companion object {
        const val TYPE_ID: String = "CjLanguageRuntime"
    }

    override val icon: Icon = AllIcons.Nodes.Module

    override val displayName: String
        get() = "CangJie"

    override val configurableDescription: String
        get() = "Configure CangJie runtime for remote targets"

    override val launchDescription: String
        get() = "Run CangJie application"

    override fun createDefaultConfig(): CjLanguageRuntimeConfiguration {
        return CjLanguageRuntimeConfiguration()
    }

    override fun duplicateConfig(config: CjLanguageRuntimeConfiguration): CjLanguageRuntimeConfiguration {
        return CjLanguageRuntimeConfiguration().also {
            it.sdkId = config.sdkId
            it.localBuildArgs = config.localBuildArgs
        }
    }

    override fun createSerializer(config: CjLanguageRuntimeConfiguration): PersistentStateComponent<*> {
        return config
    }

    override fun createConfigurable(
        project: Project,
        config: CjLanguageRuntimeConfiguration,
        targetEnvironmentType: TargetEnvironmentType<*>,
        targetSupplier: Supplier<TargetEnvironmentConfiguration>
    ): Configurable {
        return CjLanguageRuntimeConfigurable(config)
    }

    override fun findLanguageRuntime(target: TargetEnvironmentConfiguration): CjLanguageRuntimeConfiguration? {
        return target.runtimes.findByType()
    }

    override fun isApplicableTo(runConfig: RunnerAndConfigurationSettings): Boolean {
        // Applicable to all CangJie run configurations
        val configuration = runConfig.configuration
        return configuration is org.cangnova.cangjie.run.CangJieRunConfigurationBase
    }
}

/**
 * Configurable for CangJie language runtime
 */
private class CjLanguageRuntimeConfigurable(
    private val config: CjLanguageRuntimeConfiguration
) : Configurable {

    override fun getDisplayName(): String = "CangJie Runtime"

    override fun createComponent(): javax.swing.JComponent? {
        // TODO: Create UI for configuring CangJie runtime paths
        return javax.swing.JPanel().apply {
            add(javax.swing.JLabel("CangJie Runtime Configuration (UI not yet implemented)"))
        }
    }

    override fun isModified(): Boolean = false

    override fun apply() {
        // TODO: Apply changes from UI to config
    }
}