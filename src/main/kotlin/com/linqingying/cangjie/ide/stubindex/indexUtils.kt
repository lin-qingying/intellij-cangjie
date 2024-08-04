package com.linqingying.cangjie.ide.stubindex

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.stubs.*
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.TokenSet






private fun CjTypeElement.index(
    declaration: CjTypeParameterListOwner,
    containingTypeReference: CjTypeReference,
    occurrence: (String) -> Unit
) {
    fun CjTypeElement.indexWithVisited(
        declaration: CjTypeParameterListOwner,
        containingTypeReference: CjTypeReference,
        visited: MutableSet<CjTypeElement>,
        occurrence: (String) -> Unit
    ) {
        if (this in visited) return

        visited.add(this)

        when (this) {
            is CjUserType -> {
                val referenceName = referencedName ?: return

                val typeParameter = declaration.typeParameters.firstOrNull { it.name == referenceName }
                if (typeParameter != null) {
                    val bound = typeParameter.extendsBound
                    if (bound != null) {
                        bound.typeElement?.indexWithVisited(declaration, containingTypeReference, visited, occurrence)
                    } else {
                        occurrence("Any")
                    }
                    return
                }

                occurrence(referenceName)


            }





            else -> error("Unsupported type: $this")
        }
    }

    indexWithVisited(declaration, containingTypeReference, mutableSetOf(), occurrence)
}



//private val STRING_TEMPLATE_EMPTY_ARRAY = emptyArray<CjStringTemplateExpression>()
//private val STRING_TEMPLATE_TYPES = TokenSet.create(CjStubElementTypes.STRING_TEMPLATE)

//private fun ValueArgument.stringTemplateExpression(): CjStringTemplateExpression? {
//    if (this is StubBasedPsiElement<*>) {
//        stub?.let {
//            val constantExpressions = it.getChildrenByType(STRING_TEMPLATE_TYPES, STRING_TEMPLATE_EMPTY_ARRAY)
//            return constantExpressions.firstOrNull()
//        }
//    }
//    return getArgumentExpression() as? CjStringTemplateExpression
//}





private val StubElement<*>.annotatedJvmNameElementName: String?
    get() = when (this) {
        is CangJieFileStub -> psi.name
        is NamedStub -> name ?: ""

        is CangJiePlaceHolderStub -> parentStub?.annotatedJvmNameElementName
        else -> null
    }

private val CangJieStubWithFqName<*>.modifierList: CangJieModifierListStub?
    get() = findChildStubByType(CjStubElementTypes.MODIFIER_LIST)

