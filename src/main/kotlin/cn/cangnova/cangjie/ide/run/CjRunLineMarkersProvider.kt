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

package cn.cangnova.cangjie.ide.run

import cn.cangnova.cangjie.ide.run.cjpm.RunMainAction
import cn.cangnova.cangjie.lexer.CjTokens.IDENTIFIER
import cn.cangnova.cangjie.psi.CjAnnotated
import cn.cangnova.cangjie.psi.CjMainFunction
import cn.cangnova.cangjie.psi.psiUtil.elementType
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement

class CjRunLineMarkersProvider : RunLineMarkerContributor(), DumbAware {

    override fun getInfo(element: PsiElement): Info? {
        if (element is CjMainFunction) {
            val action = ActionManager.getInstance().getAction(RunMainAction.ID)
            return Info(
                AllIcons.RunConfigurations.TestState.Run,
                ExecutorAction.getActions(), { "run main" })
        } /*else if (isTestCase(element)) {
            // TODO: mem leak ?
            val action = RunTestAction(element)
            return Info(AllIcons.RunConfigurations.TestState.Run, arrayOf(action), { "run test" })
        }*/
        return null
    }

    private fun isTestCase(element: PsiElement): Boolean {
        if (element.elementType == IDENTIFIER && element.parent is CjAnnotated) {
            val annotated: CjAnnotated = element.parent as CjAnnotated
            if (hasTestAnnotation(annotated)) {
                return true
            }
        }
        return false
    }

    private fun hasTestAnnotation(element: CjAnnotated): Boolean {
        return element.annotationEntries.any {
            it.text == "@Test" || it.text == "@TestCase"
        }
    }

}
