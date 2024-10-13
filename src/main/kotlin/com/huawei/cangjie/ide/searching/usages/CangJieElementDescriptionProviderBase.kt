package com.huawei.cangjie.ide.searching.usages

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.highlighter.unwrapped
import com.huawei.cangjie.lang.CangJieLanguage
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.containingTypeStatement
import com.huawei.cangjie.psi.psiUtil.getStrictParentOfType
import com.huawei.cangjie.utils.collapseSpaces
import com.intellij.codeInsight.highlighting.HighlightUsagesDescriptionLocation
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.ElementDescriptionLocation
import com.intellij.psi.ElementDescriptionProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.util.RefactoringDescriptionLocation
import com.intellij.usageView.UsageViewLongNameLocation
import com.intellij.usageView.UsageViewShortNameLocation
import com.intellij.usageView.UsageViewTypeLocation


open class CangJieElementDescriptionProviderBase : ElementDescriptionProvider {
    private tailrec fun CjNamedDeclaration.parentForFqName(): CjNamedDeclaration? {
        val parent = getStrictParentOfType<CjNamedDeclaration>() ?: return null
        if (parent is CjProperty && parent.isLocal) return parent.parentForFqName()
        return parent
    }

    private fun CjNamedDeclaration.name() = nameAsName ?: Name.special("<no name provided>")

    private fun CjNamedDeclaration.fqName(): FqNameUnsafe {
        containingTypeStatement?.let {

            return FqNameUnsafe("${it.name()}.${name()}")
        }

        val internalSegments = generateSequence(this) { it.parentForFqName() }
            .filterIsInstance<CjNamedDeclaration>()
            .map { it.name ?: "<no name provided>" }
            .toList()
            .asReversed()
        val packageSegments = containingCjFile.packageFqName.pathSegments()
        return FqNameUnsafe((packageSegments + internalSegments).joinToString("."))
    }

    private fun CjTypeReference.renderShort(): String {
        return accept(
            object : CjVisitor<String, Unit>() {
                private val visitor get() = this

                override fun visitTypeReference(typeReference: CjTypeReference, data: Unit): String {
                    val typeText = typeReference.typeElement?.accept(this, data) ?: "???"
                    return if (!typeReference.containingCjFile.isCompiled && typeReference.hasParentheses()) "($typeText)" else typeText
                }


//                override fun visitDynamicType(type: CjDynamicType, data: Unit) = type.text

                override fun visitFunctionType(type: CjFunctionType, data: Unit): String {
                    return buildString {
                        type.receiverTypeReference?.let { append(it.accept(visitor, data)).append('.') }
                        type.parameters.joinTo(this, prefix = "(", postfix = ")") { it.accept(visitor, data) }
                        append(" -> ")
                        append(type.returnTypeReference?.accept(visitor, data) ?: "???")
                    }
                }

                override fun visitOptionType(nullableType: CjOptionType, data: Unit): String {
                    val innerTypeText = nullableType.getInnerType()?.accept(this, data) ?: return "???"
                    return "$innerTypeText?"
                }

//                override fun visitSelfType(type: CjSelfType, data: Unit) = type.text

                override fun visitUserType(type: CjUserType, data: Unit): String {
                    return buildString {
                        append(type.referencedName ?: "???")

                        val arguments = type.typeArguments
                        if (arguments.isNotEmpty()) {
                            arguments.joinTo(this, prefix = "<", postfix = ">") {
                                it.typeReference?.accept(visitor, data) ?: it.text
                            }
                        }
                    }
                }

                override fun visitParameter(parameter: CjParameter, data: Unit) =
                    parameter.typeReference?.accept(this, data) ?: "???"
            },
            Unit
        )
    }


    protected open val PsiElement.isRenameCangJiePropertyProcessor get() = false

    override fun getElementDescription(element: PsiElement, location: ElementDescriptionLocation): String? {
        val shouldUnwrap = location !is UsageViewShortNameLocation && location !is UsageViewLongNameLocation
        val targetElement = if (shouldUnwrap) element.unwrapped ?: element else element

        fun elementKind() = when (targetElement) {
            is CjInterface -> CangJieBundle.message("find.usages.interface")
            is CjClass -> CangJieBundle.message("find.usages.class")
            is CjStruct -> CangJieBundle.message("find.usages.struct")
            is CjNamedFunction -> CangJieBundle.message("find.usages.function")
            is CjPropertyAccessor -> CangJieBundle.message(
                "find.usages.for.property",
                (if (targetElement.isGetter)
                    CangJieBundle.message("find.usages.getter")
                else
                    CangJieBundle.message("find.usages.setter"))
            ) + " "

            is CjFunctionLiteral -> CangJieBundle.message("find.usages.lambda")
            is CjPrimaryConstructor, is CjSecondaryConstructor -> CangJieBundle.message("find.usages.constructor")
            is CjProperty -> if (targetElement.isLocal)
                CangJieBundle.message("find.usages.variable")
            else
                CangJieBundle.message("find.usages.property")

            is CjTypeParameter -> CangJieBundle.message("find.usages.type.parameter")
            is CjParameter -> CangJieBundle.message("find.usages.parameter")
            is CjDestructuringDeclarationEntry -> CangJieBundle.message("find.usages.variable")
            is CjTypeAlias -> CangJieBundle.message("find.usages.type.alias")

            is CjImportAlias -> CangJieBundle.message("find.usages.import.alias")

            else -> {

                when {

                    targetElement.isRenameCangJiePropertyProcessor -> CangJieBundle.message("find.usages.property.accessor")
                    else -> null
                }
            }
        }

        val namedElement = if (targetElement is CjPropertyAccessor) {
            targetElement.parent as? CjProperty
        } else targetElement as? PsiNamedElement

        if (namedElement == null) {
            return if (targetElement is CjElement) "'" + StringUtil.shortenTextWithEllipsis(
                targetElement.text.collapseSpaces(),
                53,
                0
            ) + "'" else null
        }

        if (namedElement.language != CangJieLanguage) return null

        return when (location) {
            is UsageViewTypeLocation -> elementKind()
            is UsageViewShortNameLocation, is UsageViewLongNameLocation -> namedElement.name
            is RefactoringDescriptionLocation -> {
                val kind = elementKind() ?: return null
                if (namedElement !is CjNamedDeclaration) return null
                val renderFqName = location.includeParent() &&
                        namedElement !is CjTypeParameter &&
                        namedElement !is CjParameter &&
                        namedElement !is CjConstructor<*>

                @Suppress("HardCodedStringLiteral")
                val desc = when (namedElement) {
                    is CjFunction -> {
                        val baseText = buildString {
                            append(namedElement.name ?: "")
                            namedElement.valueParameters.joinTo(this, prefix = "(", postfix = ")") {
                                (if (it.isVarArg) "vararg " else "") + (it.typeReference?.renderShort() ?: "")
                            }
                            namedElement.receiverTypeReference?.let { append(" on ").append(it.renderShort()) }
                        }
                        val parentFqName = if (renderFqName) namedElement.fqName().parent() else null
                        if (parentFqName?.isRoot != false) baseText else "${parentFqName.asString()}.$baseText"
                    }

                    else -> (if (renderFqName) namedElement.fqName().asString() else namedElement.name) ?: ""
                }

                "$kind ${CommonRefactoringUtil.htmlEmphasize(desc)}"
            }

            is HighlightUsagesDescriptionLocation -> {
                val kind = elementKind() ?: return null
                if (namedElement !is CjNamedDeclaration) return null
                "$kind ${namedElement.name}"
            }

            else -> null
        }
    }
}

class CangJieElementDescriptionProvider : CangJieElementDescriptionProviderBase()
