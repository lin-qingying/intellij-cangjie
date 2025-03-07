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

package cn.cangnova.cangjie.ide.inspections.cdoc

import cn.cangnova.cangjie.CangJieBundle
import cn.cangnova.cangjie.descriptors.DeclarationDescriptorWithVisibility
import cn.cangnova.cangjie.doc.psi.impl.CDocSection
import cn.cangnova.cangjie.ide.cdoc.CDocElementFactory
import cn.cangnova.cangjie.ide.codeinsight.inspections.AbstractCangJieInspection
import cn.cangnova.cangjie.ide.codeinsight.quickDoc.cdoc.findCDoc
import cn.cangnova.cangjie.ide.intentions.findExistingEditor
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.*
import cn.cangnova.cangjie.references.util.DescriptorToSourceUtilsIde
import cn.cangnova.cangjie.resolve.caches.resolveToDescriptorIfAny
import cn.cangnova.cangjie.resolve.isEffectivelyPublicApi
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.annotations.Nls

/**
 * @return string description of declaration, like `Function "describe"`
 */
@Nls
fun CjNamedDeclaration.describe(): String? = when (this) {
    is CjClass -> CangJieBundle.message("class") + name
    is CjInterface -> CangJieBundle.message("interface") + "$name"
    is CjEnum -> CangJieBundle.message("enum") + "$name"
    is CjStruct -> CangJieBundle.message("struct") + "$name"

    is CjNamedFunction -> CangJieBundle.message("function.01", name.toString())
    is CjSecondaryConstructor -> CangJieBundle.message("constructor")
    is CjProperty, is CjVariable -> CangJieBundle.message("property.0", name.toString())
    is CjParameter -> if (this.isPropertyParameter())
        CangJieBundle.message("property.0", name.toString())
    else
        CangJieBundle.message("parameter.0", name.toString())

    is CjTypeParameter -> CangJieBundle.message("type.parameter.0", name.toString())
    is CjTypeAlias -> CangJieBundle.message("type.alias.0", name.toString())
    else -> null
}

class CDocMissingDocumentationInspection : AbstractCangJieInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        namedDeclarationVisitor { element ->
//            if (TestUtils.isInTestSourceContent(element)) {
//                return@namedDeclarationVisitor
//            }
            val nameIdentifier = element.nameIdentifier
            if (nameIdentifier != null) {
                if (element.findCDoc { DescriptorToSourceUtilsIde.getAnyDeclaration(element.project, it) } == null) {
                    val descriptor =
                        element.resolveToDescriptorIfAny() as? DeclarationDescriptorWithVisibility
                            ?: return@namedDeclarationVisitor
                    if (descriptor.isEffectivelyPublicApi) {
                        val message =
                            element.describe()?.let { CangJieBundle.message("0.is.missing.documentation", it) }
                                ?: CangJieBundle.message("missing.documentation")
                        holder.registerProblem(nameIdentifier, message, AddDocumentationFix())
                    }
                }
            }

        }

    class AddDocumentationFix : LocalQuickFix {
        override fun getName(): String = CangJieBundle.message("add.documentation.fix.text")

        override fun getFamilyName(): String = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val declaration = descriptor.psiElement.getParentOfType<CjNamedDeclaration>(true)
                ?: throw IllegalStateException("Can't find declaration")

            declaration.addBefore(
                CDocElementFactory(project).createCDocFromText("/**\n*\n*/\n"),
                declaration.firstChild
            )

            val editor = descriptor.psiElement.findExistingEditor() ?: return


            // If we just add whitespace
            // /**
            //  *[HERE]
            // it will be erased by formatter, so following code adds it right way and moves caret then
            editor.unblocCDocument()

            val section = declaration.firstChild.getChildOfType<CDocSection>() ?: return
            val asterisk = section.firstChild

            editor.caretModel.moveToOffset(asterisk.endOffset)
            EditorModificationUtil.insertStringAtCaret(editor, " ")
        }
    }
}
