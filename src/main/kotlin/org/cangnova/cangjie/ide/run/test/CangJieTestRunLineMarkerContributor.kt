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

package org.cangnova.cangjie.ide.run.test

import org.cangnova.cangjie.ide.run.cjpm.test.isTestCase
import org.cangnova.cangjie.messages.CangJieUiBundle
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement

class CangJieTestRunLineMarkerContributor : RunLineMarkerContributor() {

    private fun calculateIcon(
        element: PsiElement,
        includeSlowProviders: Boolean
    ): Info? {
        if (isTestCase(element)) {
            return Info(
                AllIcons.RunConfigurations.TestState.Run,
                ExecutorAction.getActions(),
                {
                    CangJieUiBundle.message("test.tip.text.run")
                }
            )

        }

        return null


    }

    override fun getInfo(element: PsiElement): Info? = calculateIcon(element, false)
//    override fun getSlowInfo(element: PsiElement): Info? = calculateIcon(element, true)

}