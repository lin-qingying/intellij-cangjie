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

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.Name.Companion.identifier
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.stubs.CangJieAnnotationEntryStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

/**
 * 注解条目 (Annotation Entry)
 *
 * 表示源代码中单个注解的使用,是注解应用的基本单位。
 * 每个注解条目对应一个以 `@` 开头的注解使用,可能包含参数。
 *
 * ## 语法结构
 * ```
 * annotationEntry ::= '@' constructorCalleeExpression valueArgumentList?
 * ```
 *
 * ## 示例
 * ```cangjie
 * @Deprecated                              // 简单注解条目
 * @ForeignName(name: "native_function")   // 带参数的注解条目
 * @CallingConv(convention: CDECL)         // 带枚举参数的注解条目
 * ```
 *
 * ## 与 CjAnnotation 的关系
 * - **CjAnnotationEntry**: 单个注解的使用 (例如: `@Deprecated`)
 * - **CjAnnotation**: 注解容器,可包含多个连续的注解条目
 *
 * 在语法树中:
 * ```
 * CjAnnotation (注解容器)
 *   └─ CjAnnotationEntry (@Deprecated)
 *   └─ CjAnnotationEntry (@Frozen)
 *   └─ CjAnnotationEntry (@ConstSafe)
 * ```
 *
 * ## 作为 CjCallElement
 * 注解条目继承 [CjCallElement],因为注解的使用本质上是对注解类构造函数的调用:
 * - [calleeExpression]: 注解名称及类型引用
 * - [valueArgumentList]: 注解参数列表
 * - [valueArguments]: 具体的参数值
 *
 * ## 主要属性
 * - [atSymbol]: `@` 符号
 * - [typeReference]: 注解类型引用
 * - [shortName]: 注解的简短名称
 * - [valueArguments]: 注解参数列表
 *
 * ## 常见用途
 * - FFI 互操作: `@C`, `@Java`, `@JavaImpl`
 * - 编译器指令: `@Intrinsic`, `@OverflowThrowing`
 * - 语义标记: `@Deprecated`, `@Frozen`
 * - 条件编译: `@When`
 *
 * @see CjAnnotation 注解容器
 * @see CjCallElement 调用表达式接口
 * @see CjBuiltInAnnotation 内置注解枚举
 */
class CjAnnotationEntry : CjElementImplStub<CangJieAnnotationEntryStub>, CjCallElement {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieAnnotationEntryStub) : super(stub, CjStubElementTypes.ANNOTATION_ENTRY)

    override val calleeExpression: CjConstructorCalleeExpression? get() {
        return getStubOrPsiChild(CjStubElementTypes.CONSTRUCTOR_CALLEE)
    }

    override val lambdaArguments: List<CjLambdaArgument> get() {
        return emptyList()
    }

    override val typeArguments: List<CjTypeProjection> get() {
        val typeArgumentList = typeArgumentList ?: return emptyList()
        return typeArgumentList.arguments
    }

    override val typeArgumentList: CjTypeArgumentList? = null

    override val valueArgumentList: CjValueArgumentList? get() {
        val stub = stub
        if (stub == null && greenStub != null) {
            return findChildByType(CjNodeTypes.VALUE_ARGUMENT_LIST)
        }

        return getStubOrPsiChild(CjStubElementTypes.VALUE_ARGUMENT_LIST)
    }

    override val valueArguments: List<ValueArgument> get() {
        val stub = stub
        if (stub != null && !stub.hasValueArguments()) {
            return emptyList<CjValueArgument>()
        }

        val list = valueArgumentList
        return list?.arguments ?: emptyList<CjValueArgument>()
    }
    val atSymbol: PsiElement? get() {
        return findChildByType(CjTokens.AT)
    }

    @get:IfNotParsed
    val typeReference: CjTypeReference?
        get() {
            val calleeExpression = calleeExpression ?: return null
            return calleeExpression.typeReference
        }
    val shortName: Name?
        get() {
            val stub = stub
            if (stub != null) {
                val shortName = stub.getShortName()
                if (shortName != null) {
                    return identifier(shortName)
                }
                return null
            }
            typeReference ?: return null
            val typeReference =
                checkNotNull(typeReference) { "Annotation entry hasn't typeReference $text" }
            val typeElement = typeReference.typeElement
            if (typeElement is CjUserType) {
                val shortName = typeElement.referencedName
                if (shortName != null) {
                    return identifier(shortName)
                }
            }
            return null
        }
}
