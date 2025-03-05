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

package cn.cangnova.cangjie.ide.run.cjc


import cn.cangnova.cangjie.icon.CangJieIcons
import cn.cangnova.cangjie.ide.run.cjpm.CangJieRunConfigurationOptions
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue

class CjcRunConfigurationType : SimpleConfigurationType("CjcRunConfigurationType",
    "Cjc",
    "Cjc",
    NotNullLazyValue.createValue { CangJieIcons.CANGJIE }

) {
    override fun createTemplateConfiguration(project: Project): CjcRunConfiguration {
        return CjcRunConfiguration(project, this, "Cjc")
    }

    companion object {
        val instance: CjcRunConfigurationType
            get() = ConfigurationTypeUtil.findConfigurationType(CjcRunConfigurationType::class.java)
    }
}


class CjcRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<CangJieRunConfigurationOptions>(project, factory, name) {
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        TODO("Not yet implemented")
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        TODO("Not yet implemented")
    }

}

