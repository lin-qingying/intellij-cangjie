package com.linqingying.cangjie.ide.stubindex

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.stubs.*
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.StubElement

fun indexTypeAliasExpansion(stub: CangJieTypeAliasStub, sink: IndexSink) {
    val declaration = stub.psi
    val typeReference = declaration.getTypeReference() ?: return
    val typeElement = typeReference.typeElement ?: return
    typeElement.index(declaration, typeReference) { typeName ->
        sink.occurrence(CangJieTypeAliasByExpansionShortNameIndex.indexKey, typeName)
    }
}

fun indexInternals(stub: CangJieCallableStubBase<*>, sink: IndexSink) {
    val name = stub.name ?: return

    val modifierListStub = stub.modifierList ?: return



    if (stub.isTopLevel()) return

    if (modifierListStub.hasModifier(CjTokens.OPEN_KEYWORD) || modifierListStub.hasModifier(CjTokens.ABSTRACT_KEYWORD)) {
        sink.occurrence(CangJieOverridableInternalMembersShortNameIndex.indexKey, name)
    }
}

private fun <TDeclaration : CjCallableDeclaration> CangJieExtensionsByReceiverTypeStubIndexHelper.indexExtension(
    stub: CangJieCallableStubBase<TDeclaration>,
    sink: IndexSink
) {
    if (!stub.isExtension()) return

    val declaration = stub.psi
    val callableName = declaration.name ?: return
    val containingTypeReference = declaration.receiverTypeReference!!
    containingTypeReference.typeElement?.index(declaration, containingTypeReference) { typeName ->
        sink.occurrence(indexKey, buildKey(typeName, callableName))
    }
}

fun <TDeclaration : CjCallableDeclaration> indexTopLevelExtension(
    stub: CangJieCallableStubBase<TDeclaration>,
    sink: IndexSink
) {
    CangJieTopLevelExtensionsByReceiverTypeIndex.indexExtension(stub, sink)
}

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

            is CjOptionType -> getInnerType()?.indexWithVisited(
                declaration,
                containingTypeReference,
                visited,
                occurrence
            )

            is CjTupleType -> {

                val arity = typeArgumentsAsTypes.size

                occurrence("Tuple$arity")
            }

            is CjFunctionType -> {
                val arity = parameters.size + (if (receiverTypeReference != null) 1 else 0)

                occurrence("Function$arity")
            }

            is CjBasicType -> {
                occurrence(this.name)

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

