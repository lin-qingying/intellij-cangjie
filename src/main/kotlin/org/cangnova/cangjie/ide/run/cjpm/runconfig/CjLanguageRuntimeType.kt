/*
 * Copyright 2024 LinQingYing. and contributors.
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

package org.cangnova.cangjie.ide.run.cjpm.runconfig

import org.cangnova.cangjie.icon.CangJieIcons
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
