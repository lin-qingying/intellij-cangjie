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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.psi.CjFunction
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.service.CjProjectsService
import java.nio.file.Paths

/**
 * Run configuration producer for CangJie modules.
 * Automatically creates run configurations when right-clicking on CangJie files or main functions.
 */
class CangJieProgramRunConfigurationProducer : LazyRunConfigurationProducer<CangJieProgramRunConfiguration>() {

    override fun getConfigurationFactory(): ConfigurationFactory {
        return CangJieProgramRunConfigurationType.instance.factory
    }

    override fun setupConfigurationFromContext(
        configuration: CangJieProgramRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val element = sourceElement.get() ?: return false
        val psiFile = element.containingFile as? CjFile ?: return false
        val project = context.project

        // Get the CangJie module
        val projectsService = CjProjectsService.getInstance(project)
        val cjProject = projectsService.cjProject

        // Try to determine module from file location (simplified)
        val moduleName = psiFile.virtualFile.parent.name // Simplified logic
        configuration.moduleName = moduleName

        // Try to find the main function
        val mainFunction = findMainFunction(psiFile)
        if (mainFunction != null) {
            configuration.mainFunction = mainFunction.name
            configuration.name = "Run ${moduleName}"
        } else {
            configuration.mainFunction = "main"
            configuration.name = "Run ${moduleName}"
        }

        // Set working directory to file directory
        configuration.workingDirectory = Paths.get(psiFile.virtualFile.parent.path)

        return true
    }

    override fun isConfigurationFromContext(
        configuration: CangJieProgramRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val element = context.location?.psiElement ?: return false
        val psiFile = element.containingFile as? CjFile ?: return false

        val moduleName = psiFile.virtualFile.parent.name // Simplified logic
        return configuration.moduleName == moduleName &&
                configuration.workingDirectory?.toString() == psiFile.virtualFile.parent.path
    }

    /**
     * Find main function in the given file
     */
    private fun findMainFunction(psiFile: CjFile): CjFunction? {
        return PsiTreeUtil.findChildrenOfType(psiFile, CjFunction::class.java)
            .find { function ->
                function.name == "main" && function.hasMainFunctionSignature()
            }
    }

    /**
     * Find main function in the module (searches all files)
     */
    private fun findMainFunctionInModule(cjModule: CjModule): CjFunction? {
        // This would need to be implemented based on how files are accessed in the module
        // For now, return null - this can be extended later
        return null
    }
}

/**
 * Extension function to check if a function has main function signature
 */
private fun CjFunction.hasMainFunctionSignature(): Boolean {
    // Check if the function has no parameters or appropriate signature for main
    // This depends on the actual PSI structure of CangJie functions
    return true // Simplified for now
}