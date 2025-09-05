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

package org.cangnova.cangjie.ide.stubindex

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.*
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
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

/**
 * 索引内部成员的函数
 * 此函数用于处理给定的stub对象，并根据条件向sink中添加索引信息
 * 主要关注是否为顶层元素，以及是否具有开放或抽象修饰符
 *
 * @param stub 一个CangJieCallableStubBase的实例，代表一个可调用的stub对象
 * @param sink 一个IndexSink实例，用于接收索引信息
 */
fun indexInternals(stub: CangJieCallableStubBase<*>, sink: IndexSink) {
    // 获取stub的名称，如果名称为空，则直接返回
    val name = stub.name ?: return

    // 获取stub的修饰符列表，如果不存在，则直接返回
    val modifierListStub = stub.modifierList ?: return

    // 如果stub是顶层元素，则直接返回，因为不处理顶层元素
    if (stub.isTopLevel()) return

    // 如果修饰符列表中包含开放或抽象修饰符，则向sink中添加相应的索引信息
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

/**
 * 索引顶级扩展函数或属性
 * 本函数专注于处理那些定义在模块顶层，用于扩展其他类型的函数或属性
 * 它通过分析给定的声明存根，来构建与接收者类型相关的索引
 *
 * @param stub CangJieCallableStubBase的实例，代表了一个函数或属性的存根
 *             这个存根包含了构建索引所需的信息，比如函数或属性的接收者类型
 * @param sink IndexSink的实例，用于接收索引过程中的输出
 *             它是一个索引数据的消费者，可以帮助建立或更新索引
 */
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
    /**
     * 对当前类型元素进行索引，如果已经访问过则直接返回
     * 此函数用于递归地索引类型元素及其嵌套的类型元素
     *
     * @param declaration 当前上下文的类型参数所有者，用于访问其类型参数
     * @param containingTypeReference 包含当前类型元素的类型引用
     * @param visited 一个集合，用于记录已经访问过的类型元素，以避免重复访问
     * @param occurrence 一个消费型函数，用于处理每个类型元素的出现情况
     */
    fun CjTypeElement.indexWithVisited(
        declaration: CjTypeParameterListOwner,
        containingTypeReference: CjTypeReference,
        visited: MutableSet<CjTypeElement>,
        occurrence: (String) -> Unit
    ) {
        // 如果当前类型元素已经访问过，则直接返回，避免重复处理
        if (this in visited) return

        if (this is CjBasicType) return
        // 将当前类型元素标记为已访问
        visited.add(this)

        // 根据当前类型元素的不同类型，执行相应的处理逻辑
        when (this) {
            is CjUserType -> {
                // 获取当前用户定义类型的引用名称，如果为空则直接返回
                val referenceName = referencedName ?: return

                // 尝试在声明的类型参数中找到与引用名称匹配的类型参数
                val typeParameter = declaration.typeParameters.firstOrNull { it.name == referenceName }
                if (typeParameter != null) {
                    // 如果找到了匹配的类型参数，并且它有上界，则递归索引上界类型
                    val bound = typeParameter.extendsBound
                    if (bound != null) {
                        bound.typeElement?.indexWithVisited(declaration, containingTypeReference, visited, occurrence)
                    } else {
                        // 如果类型参数没有上界，则记录"Any"类型
                        occurrence("Any")
                    }
                    return
                }

                // 如果没有找到匹配的类型参数，则记录引用名称
                occurrence(referenceName)
            }

            is CjOptionType -> {
                // 对于可选类型，递归索引其内部类型
                getInnerType()?.indexWithVisited(
                    declaration,
                    containingTypeReference,
                    visited,
                    occurrence
                )
            }

            is CjTupleType -> {
                // 对于元组类型，根据其元素数量记录相应的元组类型名称
                val arity = typeArgumentsAsTypes.size

                occurrence("Tuple$arity")
            }

            is CjFunctionType -> {
                // 对于函数类型，根据其参数数量（包括接收者类型）记录相应的函数类型名称
                val arity = parameters.size + (if (receiverTypeReference != null) 1 else 0)

                occurrence("Function$arity")
            }

//            is CjBasicType -> {
//                // 对于基本类型，直接记录其名称
//                occurrence(this.name)
//            }

            else -> {
                // 如果遇到不支持的类型元素，抛出错误
                error("Unsupported type: $this")
            }
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

