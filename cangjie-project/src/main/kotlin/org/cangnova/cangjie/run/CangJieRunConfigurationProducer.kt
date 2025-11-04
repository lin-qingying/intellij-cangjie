/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import java.nio.file.Paths

/**
 * Run configuration producer for CangJie projects.
 * Automatically creates run configurations based on project context.
 */
class CangJieRunConfigurationProducer : LazyRunConfigurationProducer<CangJieCommandRunConfiguration>() {

    override fun getConfigurationFactory(): ConfigurationFactory {
        return CangJieCommandRunConfigurationType.instance.factory
    }

    override fun setupConfigurationFromContext(
        configuration: CangJieCommandRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val project = context.project

        configuration.name = "Run ${project.name}"
        configuration.command = "run"
        configuration.workingDirectory = project.basePath?.let { Paths.get(it) }

        return true
    }

    override fun isConfigurationFromContext(
        configuration: CangJieCommandRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val project = context.project
        return configuration.workingDirectory?.toString() == project.basePath
    }
}