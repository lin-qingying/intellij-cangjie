/*
 * Copyright 2026 LinQingYing. and contributors.
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

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.Name.Companion.identifier
import org.cangnova.cangjie.psi.stubs.CangJieAnnotationStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

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
 * ## 与 CjAnnotations 的关系
 * - **CjAnnotation**: 单个注解的使用 (例如: `@Deprecated`)
 * - **CjAnnotations**: 注解容器,可包含多个连续的注解条目
 *
 * 在语法树中:
 * ```
 * CjAnnotations (注解容器)
 *   └─ CjAnnotation (@Deprecated)
 *   └─ CjAnnotation (@Frozen)
 *   └─ CjAnnotation (@ConstSafe)
 * ```
 *
 * ## 作为 CjCallElement
 * 注解条目继承 [CjCallElement],因为注解的使用本质上是对注解类构造函数的调用:
 * -  [calleeExpression]: 注解名称及类型引用
 * - [valueArgumentList]: 注解参数列表
 * - [valueArguments]: 具体的参数值
 *
 * ## 主要属性
 * - [atSymbol]: `@` 符号
 * - [typeReference]:  注解类型引用
 * - [shortName]: 注解的简短名称
 * - [valueArguments]: 注解参数列表
 *
 * ## 常见用途
 * - FFI 互操作: `@C`, `@Java`, `@JavaImpl`
 * - 编译器指令: `@Intrinsic`, `@OverflowThrowing`
 * - 语义标记: `@Deprecated`, `@Frozen`
 * - 条件编译: `@When`
 *
 * @see CjAnnotations 注解容器
 * @see CjCallElement 调用表达式接口
 * @see CjBuiltInAnnotation 内置注解枚举
 */
class CjAnnotation : CjElementImplStub<CangJieAnnotationStub>, CjCallElement {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieAnnotationStub) : super(stub, CjStubElementTypes.ANNOTATION)

    override val calleeExpression: CjConstructorCalleeExpression?
        get() {
            return getStubOrPsiChild(CjStubElementTypes.CONSTRUCTOR_CALLEE)
        }

    override val lambdaArguments: List<CjLambdaArgument>
        get() {
            return emptyList()
        }

    override val typeArguments: List<CjTypeProjection>
        get() {
            val typeArgumentList = typeArgumentList ?: return emptyList()
            return typeArgumentList.arguments
        }

    override val typeArgumentList: CjTypeArgumentList? = null

    override val valueArgumentList: CjValueArgumentList?
        get() {
            val stub = stub
            if (stub == null && greenStub != null) {
                return findChildByType(CjNodeTypes.VALUE_ARGUMENT_LIST)
            }

            return getStubOrPsiChild(CjStubElementTypes.VALUE_ARGUMENT_LIST)
        }

    override val valueArguments: List<ValueArgument>
        get() {
            val stub = stub
            if (stub != null && !stub.hasValueArguments()) {
                return emptyList<CjValueArgument>()
            }

            val list = valueArgumentList
            return list?.arguments ?: emptyList<CjValueArgument>()
        }
    val atSymbol: PsiElement?
        get() {
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
                checkNotNull(typeReference) { "Annotation constructor hasn't typeReference $text" }
            val typeElement = typeReference.typeElement
            if (typeElement is CjUserType) {
                val shortName = typeElement.referencedName
                if (shortName != null) {
                    return identifier(shortName)
                }
            }
            return null
        }

    /**
     * 判断是否为内置注解
     *
     * 通过注解名称检查是否属于 [CjBuiltInAnnotation] 枚举定义的内置注解。
     *
     * @return 如果是内置注解返回 true，否则返回 false
     */
    val isBuiltInAnnotation: Boolean
        get() {
            val name = shortName?.asString() ?: return false
            return CjBuiltInAnnotation.isBuiltIn(name)
        }

    /**
     * 获取对应的内置注解枚举值
     *
     * 如果当前注解是内置注解，返回对应的 [CjBuiltInAnnotation] 枚举值。
     *
     * @return 对应的内置注解枚举值，如果不是内置注解返回 null
     */
    val builtInAnnotation: CjBuiltInAnnotation?
        get() {
            val name = shortName?.asString() ?: return null
            return CjBuiltInAnnotation.fromName(name)
        }

    /**
     * 获取 @CallingConv 注解的调用约定参数
     *
     * 仅当注解为 @CallingConv 时有效。
     * 通过获取 ANNOTATION_CALLING_CONV 子元素来解析调用约定。
     *
     * @return 调用约定枚举值，如果不是 @CallingConv 注解或解析失败返回 null
     */
    val callingConvention: CallingConvention?
        get() {
            if (builtInAnnotation != CjBuiltInAnnotation.CALLING_CONV) return null

            // 获取 ANNOTATION_CALLING_CONV 子元素
            val callingConvElement = findChildByType<CjAnnotationCallingConv>(CjNodeTypes.ANNOTATION_CALLING_CONV)
            val conventionText = callingConvElement?.getCallingConventionText() ?: return null

            return CallingConvention.fromName(conventionText)
        }

    /**
     * 获取 @When 注解的条件表达式
     *
     * 仅当注解为 @When 时有效。
     * 通过获取 ANNOTATION_WHEN_CONDITION 子元素来解析条件。
     *
     * @return 条件表达式文本，如果不是 @When 注解或解析失败返回 null
     */
    val whenCondition: String?
        get() {
            if (builtInAnnotation != CjBuiltInAnnotation.WHEN) return null

            // 获取 ANNOTATION_WHEN_CONDITION 子元素
            val whenConditionElement = findChildByType<CjAnnotationWhenCondition>(CjNodeTypes.ANNOTATION_WHEN_CONDITION)
            return whenConditionElement?.getConditionText()
        }

    /**
     * 获取 @When 注解的条件表达式 PSI 元素
     *
     * 仅当注解为 @When 时有效，返回条件的表达式 PSI 元素用于进一步分析。
     *
     * @return 条件表达式的 PSI 元素，如果不是 @When 注解或没有表达式返回 null
     */
    val whenConditionExpression: CjExpression?
        get() {
            if (builtInAnnotation != CjBuiltInAnnotation.WHEN) return null

            // 获取 ANNOTATION_WHEN_CONDITION 子元素
            val whenConditionElement = findChildByType<CjAnnotationWhenCondition>(CjNodeTypes.ANNOTATION_WHEN_CONDITION)
            return whenConditionElement?.getConditionExpression()
        }

    /**
     * 获取溢出处理注解的溢出策略
     *
     * 适用于 @OverflowThrowing、@OverflowWrapping、@OverflowSaturating 注解。
     * 通过获取 ANNOTATION_OVERFLOW_STRATEGY 子元素来解析溢出策略。
     *
     * @return 溢出策略枚举值，如果不是溢出处理注解返回 null
     */
    val overflowStrategy: OverflowStrategy?
        get() {
            val builtIn = builtInAnnotation ?: return null

            // 只处理溢出相关注解
            if (builtIn !in listOf(
                    CjBuiltInAnnotation.OVERFLOW_THROWING,
                    CjBuiltInAnnotation.OVERFLOW_WRAPPING,
                    CjBuiltInAnnotation.OVERFLOW_SATURATING
                )
            ) {
                return null
            }

            // 尝试从 ANNOTATION_OVERFLOW_STRATEGY 子元素获取
            val overflowStrategyElement =
                findChildByType<CjAnnotationOverflowStrategy>(CjNodeTypes.ANNOTATION_OVERFLOW_STRATEGY)
            val strategyText = overflowStrategyElement?.getOverflowStrategyText()

            if (strategyText != null) {
                return OverflowStrategy.fromName(strategyText)
            }

            // 如果没有子元素，根据注解类型返回默认策略
            return when (builtIn) {
                CjBuiltInAnnotation.OVERFLOW_THROWING -> OverflowStrategy.THROWING
                CjBuiltInAnnotation.OVERFLOW_WRAPPING -> OverflowStrategy.WRAPPING
                CjBuiltInAnnotation.OVERFLOW_SATURATING -> OverflowStrategy.SATURATING
                else -> null
            }
        }

    /**
     * 判断是否为 FFI 互操作注解
     *
     * FFI 注解包括: @C, @Java, @JavaMirror, @JavaImpl, @ObjCMirror, @ObjCImpl, @ForeignName, @CallingConv
     *
     * @return 如果是 FFI 注解返回 true，否则返回 false
     */
    val isFFIAnnotation: Boolean
        get() = builtInAnnotation?.category == AnnotationCategory.FFI

    /**
     * 判断是否为编译器指令注解
     *
     * 编译器指令注解包括: @Intrinsic, @When, @ConstSafe, @FastNative, @Attribute 等
     *
     * @return 如果是编译器指令注解返回 true，否则返回 false
     */
    val isCompilerDirectiveAnnotation: Boolean
        get() = builtInAnnotation?.category == AnnotationCategory.COMPILER_DIRECTIVE

    /**
     * 判断是否为语义标记注解
     *
     * 语义标记注解包括: @Deprecated, @Frozen
     *
     * @return 如果是语义标记注解返回 true，否则返回 false
     */
    val isSemanticAnnotation: Boolean
        get() = builtInAnnotation?.category == AnnotationCategory.SEMANTIC
}
