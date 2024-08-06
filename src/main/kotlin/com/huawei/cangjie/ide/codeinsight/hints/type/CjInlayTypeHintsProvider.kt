package com.huawei.cangjie.ide.codeinsight.hints.type

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjSimpleNameExpression
import com.huawei.cangjie.psi.CjVariable
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
//                for (binding in pat.descendantsOfType<RsPatBinding>()) {
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
