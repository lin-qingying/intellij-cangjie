package com.linqingying.cangjie.utils

import com.linqingying.cangjie.builtins.StandardNames
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.SpecialNames
import com.linqingying.cangjie.psi.*

object CangJiePsiDeclarationRenderer {
    fun render(declaration: CjDeclaration): String? =
        when (declaration) {
            is CjTypeStatement ->
                buildString {
//                    if (declaration.isAnnotation()) {
//                        append("annotation ")
//                    }
                    if (declaration.isInterface()) {
                        append("interface")
                    }
                    else {
                        append("class")
                    }

                    append(" ")
                    append(declaration.name)
                    declaration.typeParameterList?.parameters?.let {
                        append("<")
                        for ((index, cjTypeParameter) in it.withIndex()) {
                            if (index != 0) append(", ")
                            append(cjTypeParameter.name)
                        }
                        append(">")
                    }
                    val superTypeListEntries = declaration.superTypeListEntries
                    val superClass = superTypeListEntries.filterIsInstance<CjSuperTypeCallEntry>().firstOrNull()
                    if (superClass != null) {
                        superClass.calleeExpression.constructorReferenceExpression?.getReferencedName()?.let {
                            append(" : ")
                            append(it)
                        }
                    }
                    else if (superTypeListEntries.isNotEmpty()) {
                        superTypeListEntries.first().typeReference?.referenceName()?.let {
                            append(" : ")
                            append(it)
                        }
                    }
                }
            is CjProperty ->
                buildString {
                    if (declaration.isVar) {
                        append("var")
                    }
                    else {
                        append("val")
                    }
                    append(" ")
                    declaration.receiverTypeReference?.let {
                        append(it.referenceName())
                        append(".")
                    }
                    append(declaration.name)
                    declaration.typeReference?.let {
                        append(": ")
                        append(it.referenceName())
                    }
                }
            is CjTypeParameter -> buildString {
                append("<")
                append(declaration.name)
                append(">")
            }
            is CjParameter -> buildString {
                appendCjParameter(declaration)
            }
            is CjConstructor<*> -> buildString {
                append("constructor")
                append(" ")
                append(declaration.name ?: ("`" + SpecialNames.NO_NAME_PROVIDED.asString() + "`"))
                appendValueParameters(declaration)
            }
            is CjNamedFunction -> buildString {

                if (declaration.hasModifier(CjTokens.OPERATOR_KEYWORD)) {
                    append("operator")
                    append(" ")
                }
                append("fun")
                append(" ")
                if (declaration.hasTypeParameterListBeforeFunctionName()) {
                    append("<")
                    for ((index, cjTypeParameter) in declaration.typeParameters.withIndex()) {
                        if (index != 0) append(", ")
                        append(cjTypeParameter.name)
                    }
                    append(">")
                    append(" ")
                }
                declaration.receiverTypeReference?.let {
                    append(it.referenceName())
                    append(".")
                }
                append(declaration.name)
                appendValueParameters(declaration)
                val typeReference = declaration.typeReference
                if (typeReference != null) {
                    append(": ")
                    append(typeReference.referenceName())
                } else if (declaration.hasBlockBody()) {
                    append(": ")
                    append(StandardNames.FqNames.unitUFqName.shortName())
                }
            }
            else -> null
        }

    private fun CjTypeReference.referenceName(): String? {
        val type = typeElement as? CjUserType ?: (typeElement as? CjOptionType)?.getInnerType() as? CjUserType ?: return null
        return buildString {
            append(type.referencedName)
            if (typeElement is CjOptionType) {
                append("?")
            }
            type.typeArgumentList?.arguments?.let {
                append("<")
                for ((index: Int, typeProjection: CjTypeProjection) in it.withIndex()) {
                    if (index != 0) append(", ")
                    when(typeProjection.projectionKind) {


                        else -> {}
                    }
                    append(typeProjection.typeReference?.referenceName() ?: "??")
                }
                append(">")
            }
        }
    }

    private fun StringBuilder.appendCjParameter(cjParameter: CjParameter, withName: Boolean = true) {
        if (cjParameter.isVarArg) append("vararg ")
        if (withName) {
            append(cjParameter.name)
            append(": ")
        }
        append(cjParameter.typeReference?.referenceName() ?: "??")
        if (cjParameter.defaultValue != null) {
            append(" = ...")
        }
    }

    private fun StringBuilder.appendValueParameters(declaration: CjCallableDeclaration) {
        append("(")
        for ((index, cjParameter: CjParameter) in declaration.valueParameters.withIndex()) {
            if (index != 0) append(", ")
            appendCjParameter(cjParameter, withName = false)
        }
        append(")")
    }


}
