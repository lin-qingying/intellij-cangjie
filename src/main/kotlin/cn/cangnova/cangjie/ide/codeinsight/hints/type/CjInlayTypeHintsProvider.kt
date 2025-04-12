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

package cn.cangnova.cangjie.ide.codeinsight.hints.type

import cn.cangnova.cangjie.CangJieBundle
import cn.cangnova.cangjie.psi.CjElement
import cn.cangnova.cangjie.psi.CjExpression
import cn.cangnova.cangjie.psi.CjSimpleNameExpression
import cn.cangnova.cangjie.psi.CjVariable
import com.intellij.codeInsight.hints.*
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import javax.swing.JComponent
import javax.swing.JPanel


val CjExpression.declaration: CjElement?
    get() = when (this) {
//        is CjSimpleNameExpression -> path.reference?.resolve()

        else -> null
    }

@Suppress("UnstableApiUsage")
class CjInlayTypeHintsProvider : InlayHintsProvider<CjInlayTypeHintsProvider.Settings> {


    data class Settings(
        var showForVariables: Boolean = true,
        var showForLambdas: Boolean = true,
        var showForIterators: Boolean = true,
        var showForPlaceholders: Boolean = true,
        var showObviousTypes: Boolean = false
    )

    companion object {
        private val KEY: SettingsKey<Settings> = SettingsKey("cangjie.type.hints")
    }

    override val key: SettingsKey<Settings> get() = KEY
    override val name: String get() = CangJieBundle.message("settings.cangjie.inlay.hints.title.types")

    override val previewText: String
        get() = """
            func abc(test:(n1:Int,n2:Int) -> Unit)

             main() {
                let foo = abc { n1,n2 => () };
            }
            """.trimIndent()

    override fun createSettings(): Settings = Settings()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Settings,
        sink: InlayHintsSink
    ): InlayHintsCollector {

        val project = file.project

        return object : FactoryInlayHintsCollector(editor) {


//            private fun presentTypeForIdentify(identify: PsiElement, expr: CjExpression?, isExpanded: Boolean) {
//                if (!settings.showObviousTypes  ) return
//
//                for (binding in pat.descendantsOfType<CjPatBinding>()) {
//                    if (binding.referenceName.startsWith("_")) continue
//                    presentTypeForBinding(binding, isExpanded)
//                }
//            }


            private fun presentVariable(element: CjElement, isExpanded: Boolean) {
                when (element) {
                    is CjVariable -> {
//                        if (settings.showForPlaceholders) {
//                            presentTypePlaceholders(element, isExpanded)
//                        }

                        if (element.typeReference != null) return
                       val identify =  element.identifyingElement ?: return
//element.expression


//                        presentTypeForIdentify(identify, element.expression, isExpanded)
                    }

                }

            }

            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                if (project.service<DumbService>().isDumb) return true
                if (element !is CjElement) return true


                if (settings.showForVariables) {
                    presentVariable(element, isExpanded = false)
                }
//                if (settings.showForLambdas) {
//                    presentLambda(element, isExpanded = false)
//                }
//                if (settings.showForIterators) {
//                    presentIterator(element, isExpanded = false)
//                }

                return true
            }


        }


        TODO("Not yet implemented")
    }

    override fun createConfigurable(settings: Settings): ImmediateConfigurable = object : ImmediateConfigurable {

        override val mainCheckboxText: String
            get() = CangJieBundle.message("settings.cangjie.inlay.hints.for")

        /**
         * Each case may have:
         *  * Description provided by [InlayHintsProvider.getProperty].
         *  Property key has `inlay.%[InlayHintsProvider.key].id%.%case.id%` structure
         *
         *  * Preview taken from `resource/inlayProviders/%[InlayHintsProvider.key].id%/%case.id%.rs` file
         */
        override val cases: List<ImmediateConfigurable.Case>
            get() = listOf(
                ImmediateConfigurable.Case(
                    CangJieBundle.message("settings.cangjie.inlay.hints.for.variables"),
                    "variables",
                    settings::showForVariables
                ),
                ImmediateConfigurable.Case(
                    CangJieBundle.message("settings.cangjie.inlay.hints.for.closures"),
                    "closures",
                    settings::showForLambdas
                ),
                ImmediateConfigurable.Case(
                    CangJieBundle.message("settings.cangjie.inlay.hints.for.loop.variables"),
                    "loop_variables",
                    settings::showForIterators
                ),
                ImmediateConfigurable.Case(
                    CangJieBundle.message("settings.cangjie.inlay.hints.for.type.placeholders"),
                    "type_placeholders",
                    settings::showForPlaceholders
                ),
                ImmediateConfigurable.Case(
                    CangJieBundle.message("settings.cangjie.inlay.hints.for.obvious.types"),
                    "obvious_types",
                    settings::showObviousTypes
                )
            )

        override fun createComponent(listener: ChangeListener): JComponent = JPanel()
    }

}
