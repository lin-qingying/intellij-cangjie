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

package cn.cangnova.cangjie.ide.run.cjpm.test

import cn.cangnova.cangjie.ide.run.CangJieRunConfigurationProducer
import cn.cangnova.cangjie.ide.run.cjpm.CjpmCommandConfiguration
import cn.cangnova.cangjie.ide.run.cjpm.CjpmCommandConfigurationType
import cn.cangnova.cangjie.psi.CjMacroExpression
import cn.cangnova.cangjie.psi.CjNamedDeclaration
import cn.cangnova.cangjie.psi.CjNamedFunction
import cn.cangnova.cangjie.psi.CjTypeStatement
import cn.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import cn.cangnova.cangjie.name.*
import cn.cangnova.cangjie.name.Name.Companion.identifier

internal class CjpmTestRunConfigurationProducer : CangJieRunConfigurationProducer<CjpmCommandConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory {
        return CjpmCommandConfigurationType.instance.factory

    }

    override fun setupConfigurationFromContext(
        configuration: CjpmCommandConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {

        if (!isTestCase(sourceElement.get())) return false

        val declaration =
            sourceElement.get().getStrictParentOfType<CjNamedDeclaration>()
                ?.takeIf { it.nameIdentifier == sourceElement.get() }
                ?: return false


        val command = StringBuilder()
        command.append("test")
        command.append(" ")
        command.append("--filter")
        command.append("=")
        if (declaration is CjTypeStatement) {
            configuration.name = "test ${declaration.name}"
            command.append(declaration.name)
        } else if (declaration is CjNamedFunction) {
            val type =
                declaration.getStrictParentOfType<CjTypeStatement>() ?: return false
            configuration.name = "test ${type.name}.${declaration.name}"
            command.append(type.name)
            command.append(".")
            command.append(declaration.name)

        }

        configuration.command = command.toString()
        configuration.setRunAsTest(true)
        sourceElement.set(context.psiLocation)

        return true
    }

    override fun isConfigurationFromContext(
        configuration: CjpmCommandConfiguration,
        context: ConfigurationContext
    ): Boolean {
        if (!configuration.isRunAsTest()) return false
        val location = context.getLocation()

        val element = location?.psiElement ?: return false

        val declaration =
            element.getStrictParentOfType<CjNamedDeclaration>()
                ?.takeIf { it.nameIdentifier == element }
                ?: return false


        val command = StringBuilder()
        command.append("test")
        command.append(" ")
        command.append("--filter")
        command.append("=")
        if (declaration is CjTypeStatement) {

            command.append(declaration.name)
        } else if (declaration is CjNamedFunction) {
            val type =
                declaration.getStrictParentOfType<CjTypeStatement>() ?: return false

            command.append(type.name)
            command.append(".")
            command.append(declaration.name)

        }

        return command.toString() == configuration.command

    }
}

fun isTestCase(element: PsiElement): Boolean {
    val declaration =
        element.getStrictParentOfType<CjNamedDeclaration>()?.takeIf { it.nameIdentifier == element }
            ?: return false
    val macroException = declaration.getStrictParentOfType<CjMacroExpression>() ?: return false

    if (declaration is CjTypeStatement && macroException.shortName == identifier("Test")) {
        return true

    }

    val type =
        element.getStrictParentOfType<CjTypeStatement>() ?: return false




    if (declaration is CjNamedFunction && macroException.shortName == identifier("TestCase")) {
        val typeMacro = type.getStrictParentOfType<CjMacroExpression>() ?: return false
        if (typeMacro.shortName != identifier("Test")) return false
        return true
    }


    return false
}