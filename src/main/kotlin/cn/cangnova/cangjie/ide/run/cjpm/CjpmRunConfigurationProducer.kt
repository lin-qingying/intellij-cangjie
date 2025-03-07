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

package cn.cangnova.cangjie.ide.run.cjpm


import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.json.JsonFileType
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import cn.cangnova.cangjie.cjpm.project.model.currentCjpmProject
import cn.cangnova.cangjie.lang.CangJieFileType
import org.toml.lang.psi.TomlFileType

class CjpmRunConfigurationProducer : LazyRunConfigurationProducer<CjpmCommandConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory {
        return CjpmCommandConfigurationType.instance.factory
    }

    override fun setupConfigurationFromContext(
        configuration: CjpmCommandConfiguration, context: ConfigurationContext, sourceElement: Ref<PsiElement>
    ): Boolean {
//如果该项目有运行配置中command为run的运行配置，就使用这个运行配置，否则就创建一个run的运行配置


        val file = context.location?.virtualFile
        val fileName = file?.name
        val fileType = file?.fileType

        val isInSrc = file?.path?.contains("/src/") == true


        if (fileType !is CangJieFileType) {

            if (fileType is TomlFileType) {
                if (configuration.project.currentCjpmProject == null) {
                    return false

                }
            } else {
                return false

            }

        } else if (!isInSrc) {
            return false
        }

        // 获取项目
        val project = context.project
        // 获取所有的运行配置
//        val runManager = RunManager.getInstance(project)

        // 如果不存在，则创建一个新的运行配置
        configuration.name = project.name
        configuration.command = "run"
        sourceElement.set(context.psiLocation)
        return true
//            true
//        }
    }

    override fun isConfigurationFromContext(
        configuration: CjpmCommandConfiguration, context: ConfigurationContext
    ): Boolean {
//  判断是否是cjpm项目
//        文件扩展名为.cj
//        文件为module.json


        val file = context.location?.virtualFile
        val fileName = file?.name
        val fileType = file?.fileType

//该文件是否处于src目录及其子目录下
        val isInSrc = file?.path?.contains("/src/") == true



        return if (isInSrc) {
            fileType is CangJieFileType && configuration.command == "run"
        } else {
            //    判断是否是module.json文件
            fileType is JsonFileType && fileName?.startsWith("module") == true && configuration.command == "run"
        }


    }


}
