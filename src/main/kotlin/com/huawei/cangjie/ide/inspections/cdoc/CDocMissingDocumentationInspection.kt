package com.huawei.cangjie.ide.inspections.cdoc

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.descriptors.DeclarationDescriptorWithVisibility
import com.huawei.cangjie.doc.psi.impl.CDocSection
import com.huawei.cangjie.ide.cdoc.CDocElementFactory
import com.huawei.cangjie.ide.codeinsight.inspections.AbstractCangJieInspection
import com.huawei.cangjie.ide.codeinsight.quickDoc.cdoc.findCDoc
import com.huawei.cangjie.ide.intentions.findExistingEditor
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.*
import com.huawei.cangjie.references.util.DescriptorToSourceUtilsIde
import com.huawei.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.huawei.cangjie.resolve.isEffectivelyPublicApi
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
