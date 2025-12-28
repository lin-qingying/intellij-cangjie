/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.calls.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.config.LanguageVersionSettingsImpl
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getReceiverExpression
import org.cangnova.cangjie.psi.psiUtil.isImportDirectiveExpression
import org.cangnova.cangjie.psi.psiUtil.isPackageDirectiveExpression
import org.cangnova.cangjie.resolve.isAnnotatinoDescriptor
import org.cangnova.cangjie.resolve.scopes.DescriptorKindExclude
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import org.cangnova.cangjie.resolve.unwrapIfTypeAlias
import org.cangnova.cangjie.types.CangJieType


/**
 * 调用类型
 *
 * 表示代码补全中不同类型的调用场景，每种调用类型对应不同的描述符过滤策略。
 *
 * ## 调用类型分类
 *
 * | 类型 | 场景 | 示例 |
 * |------|------|------|
 * | UNKNOWN | 未知/无法识别 | 不完整代码 |
 * | DEFAULT | 无接收者的普通调用 | `foo()` |
 * | DOT | 点号成员访问 | `obj.member` |
 * | SAFE | 安全调用 | `obj?.member` |
 * | SUPER_MEMBERS | super 成员访问 | `super.method()` |
 * | OPERATOR | 运算符调用 | `a + b` |
 * | IMPORT_DIRECTIVE | import 指令 | `import std.core` |
 * | PACKAGE_DIRECTIVE | package 指令 | `package foo.bar` |
 * | TYPE | 类型引用 | `var x: SomeType` |
 * | ANNOTATION | 注解引用 | `@Annotation` |
 * | DELEGATE | 委托表达式 | 委托模式 |
 *
 * ## 使用方式
 *
 * ```kotlin
 * val callType = CallType.DOT
 * val filter = callType.descriptorKindFilter  // 获取对应的描述符过滤器
 * ```
 *
 * @param TReceiver 接收者表达式类型
 * @property descriptorKindFilter 此调用类型接受的描述符类型过滤器
 *
 * @see CallTypeAndReceiver 调用类型与接收者的配对
 * @see DescriptorKindFilter 描述符类型过滤器
 */
@Suppress("ClassName")
sealed class CallType<TReceiver : CjElement?>(val descriptorKindFilter: DescriptorKindFilter) {
    /**
     * 未知调用类型
     *
     * 用于无法识别的调用场景，通常是不完整的代码。
     * 接受所有类型的描述符。
     */
    data object UNKNOWN : CallType<Nothing?>(DescriptorKindFilter.ALL)

    /**
     * 默认调用类型
     *
     * 无接收者的普通调用，如直接调用函数或访问变量。
     * 接受所有类型的描述符。
     *
     * 示例：`foo()`、`bar`
     */
    data object DEFAULT : CallType<Nothing?>(DescriptorKindFilter.ALL)

    /**
     * 点号成员访问
     *
     * 通过点号访问对象成员的调用类型。
     * 接受所有类型的描述符。
     *
     * 示例：`obj.member`、`obj.method()`
     */
    data object DOT : CallType<CjExpression>(DescriptorKindFilter.ALL)

    /**
     * 安全调用
     *
     * 通过 `?.` 操作符进行的安全调用，用于可空类型。
     * 接受所有类型的描述符。
     *
     * 示例：`obj?.member`、`obj?.method()`
     */
    data object SAFE : CallType<CjExpression>(DescriptorKindFilter.ALL)

    /**
     * 抽象成员排除器
     *
     * 用于在 super 调用中排除抽象成员，因为抽象成员不能被直接调用。
     */
    private object AbstractMembersExclude : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor) =
            descriptor is CallableMemberDescriptor && descriptor.modality == Modality.ABSTRACT

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }

    /**
     * super 成员访问
     *
     * 通过 `super` 关键字访问父类成员的调用类型。
     * 仅接受可调用成员，排除扩展和抽象成员。
     *
     * 示例：`super.method()`、`super<Interface>.member`
     */
    data object SUPER_MEMBERS : CallType<CjSuperExpression>(
        DescriptorKindFilter.CALLABLES exclude DescriptorKindExclude.Extensions exclude AbstractMembersExclude
    )


    /**
     * 运算符调用
     *
     * 运算符重载调用，如二元运算符和一元运算符。
     * 仅接受标记为 operator 的函数。
     *
     * 示例：`a + b`、`-a`、`a in list`
     */
    data object OPERATOR : CallType<CjExpression>(DescriptorKindFilter.FUNCTIONS exclude NonOperatorExclude)

    /**
     * 可调用引用
     *
     * 函数引用或属性引用的调用类型。
     * 接受可调用成员，排除局部变量和合成成员。
     *
     * 示例：`::function`、`obj::method`、`Class::property`
     *
     * @property settings 语言版本设置，用于确定可用的语言特性
     */
    class CallableReference(settings: LanguageVersionSettings) :
        CallType<CjExpression?>(DescriptorKindFilter.CALLABLES exclude LocalsAndSyntheticExclude(settings)) {
        override fun equals(other: Any?): Boolean = other is CallableReference
        override fun hashCode(): Int = javaClass.hashCode()
    }

    /**
     * import 指令
     *
     * 用于 import 语句中的补全。
     * 接受所有类型的描述符（模块、包、类等）。
     *
     * 示例：`import std.core`、`import mymodule.MyClass`
     */
    data object IMPORT_DIRECTIVE : CallType<CjExpression?>(DescriptorKindFilter.ALL)

    /**
     * package 指令
     *
     * 用于 package 声明中的补全。
     * 仅接受包描述符。
     *
     * 示例：`package foo.bar`
     */
    data object PACKAGE_DIRECTIVE : CallType<CjExpression?>(DescriptorKindFilter.PACKAGES)

    /**
     * 类型引用
     *
     * 用于类型注解位置的补全。
     * 接受分类器（类、接口、枚举等）和包。
     *
     * 示例：`var x: SomeType`、`fun foo(): ReturnType`
     */
    data object TYPE : CallType<CjExpression?>(
        DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK)

    )

    /**
     * 委托表达式
     *
     * 用于委托模式中的补全。
     * 仅接受运算符函数。
     *
     * 示例：`by delegate`
     */
    data object DELEGATE : CallType<CjExpression?>(DescriptorKindFilter.FUNCTIONS exclude NonOperatorExclude)

    /**
     * 注解引用
     *
     * 用于注解位置的补全。
     * 仅接受注解类和包。
     *
     * 示例：`@Annotation`、`@MyAnnotation(value)`
     */
    data object ANNOTATION : CallType<CjExpression?>(
        DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK)
                exclude NonAnnotationClassifierExclude
    )

    /**
     * 非注解分类器排除器
     *
     * 用于在注解补全中排除非注解类型的分类器。
     * 仅允许标记为注解的类通过过滤。
     */
    private object NonAnnotationClassifierExclude : DescriptorKindExclude() {

        override fun excludes(descriptor: DeclarationDescriptor): Boolean {
            val descriptorToCheck = descriptor.unwrapIfTypeAlias()
            if (descriptorToCheck !is ClassifierDescriptor) return false
            return descriptorToCheck !is ClassDescriptor || !descriptorToCheck.isAnnotatinoDescriptor()
        }

        override val fullyExcludedDescriptorKinds: Int get() = 0

    }

    /**
     * 局部变量和合成成员排除器
     *
     * 用于在可调用引用中排除局部变量和合成成员。
     * 可调用引用只能引用成员级别的声明。
     *
     * @property settings 语言版本设置
     */
    private class LocalsAndSyntheticExclude(private val settings: LanguageVersionSettings) : DescriptorKindExclude() {

        override fun excludes(descriptor: DeclarationDescriptor): Boolean =
            descriptor !is CallableMemberDescriptor || descriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }

    /**
     * 非运算符函数排除器
     *
     * 用于在运算符调用中排除非运算符函数。
     * 仅允许标记为 operator 的简单函数通过过滤。
     */
    private object NonOperatorExclude : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor) =
            !(descriptor is SimpleFunctionDescriptor && descriptor.isOperator)

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }

}

/**
 * 调用类型与接收者配对
 *
 * 将 [CallType] 与具体的接收者表达式配对，用于在代码补全时
 * 同时提供调用类型信息和接收者上下文。
 *
 * ## 检测机制
 *
 * 通过 [detect] 方法可以自动检测表达式的调用类型和接收者：
 *
 * ```kotlin
 * val callTypeAndReceiver = CallTypeAndReceiver.detect(expression)
 * when (callTypeAndReceiver) {
 *     is CallTypeAndReceiver.DOT -> {
 *         val receiver = callTypeAndReceiver.receiver  // 接收者表达式
 *         // 处理点号成员访问
 *     }
 *     is CallTypeAndReceiver.DEFAULT -> {
 *         // 处理无接收者的调用
 *     }
 *     // ...
 * }
 * ```
 *
 * ## 类型对应关系
 *
 * | CallTypeAndReceiver | CallType | 接收者类型 |
 * |---------------------|----------|-----------|
 * | UNKNOWN | UNKNOWN | null |
 * | DEFAULT | DEFAULT | null |
 * | DOT | DOT | CjExpression |
 * | SAFE | SAFE | CjExpression |
 * | SUPER_MEMBERS | SUPER_MEMBERS | CjSuperExpression |
 * | OPERATOR | OPERATOR | CjExpression |
 * | CALLABLE_REFERENCE | CallableReference | CjExpression? |
 * | IMPORT_DIRECTIVE | IMPORT_DIRECTIVE | CjExpression? |
 * | PACKAGE_DIRECTIVE | PACKAGE_DIRECTIVE | CjExpression? |
 * | TYPE | TYPE | CjExpression? |
 * | ANNOTATION | ANNOTATION | CjExpression? |
 *
 * @param TReceiver 接收者表达式类型
 * @param TCallType 调用类型
 * @property callType 调用类型
 * @property receiver 接收者表达式
 *
 * @see CallType 调用类型
 */
@Suppress("ClassName")
sealed class CallTypeAndReceiver<TReceiver : CjElement?, out TCallType : CallType<TReceiver>>(
    val callType: TCallType,
    val receiver: TReceiver
) {
    /** 未知调用类型，无接收者 */
    data object UNKNOWN : CallTypeAndReceiver<Nothing?, CallType.UNKNOWN>(CallType.UNKNOWN, null)

    /** 默认调用类型，无接收者 */
    data object DEFAULT : CallTypeAndReceiver<Nothing?, CallType.DEFAULT>(CallType.DEFAULT, null)

    /** 点号成员访问，如 `receiver.member` */
    class DOT(receiver: CjExpression) : CallTypeAndReceiver<CjExpression, CallType.DOT>(CallType.DOT, receiver)

    /** 安全调用，如 `receiver?.member` */
    class SAFE(receiver: CjExpression) : CallTypeAndReceiver<CjExpression, CallType.SAFE>(CallType.SAFE, receiver)

    /** super 成员访问，如 `super.method()` */
    class SUPER_MEMBERS(receiver: CjSuperExpression) : CallTypeAndReceiver<CjSuperExpression, CallType.SUPER_MEMBERS>(
        CallType.SUPER_MEMBERS, receiver
    )


    /** 运算符调用，如 `a + b` */
    class OPERATOR(receiver: CjExpression) :
        CallTypeAndReceiver<CjExpression, CallType.OPERATOR>(CallType.OPERATOR, receiver)

    /**
     * 可调用引用，如 `::function` 或 `receiver::method`
     *
     * @property settings 语言版本设置
     */
    class CALLABLE_REFERENCE(
        receiver: CjExpression?,
        val settings: LanguageVersionSettings
    ) : CallTypeAndReceiver<CjExpression?, CallType.CallableReference>(CallType.CallableReference(settings), receiver)

    /** import 指令，如 `import std.core` */
    class IMPORT_DIRECTIVE(receiver: CjExpression?) : CallTypeAndReceiver<CjExpression?, CallType.IMPORT_DIRECTIVE>(
        CallType.IMPORT_DIRECTIVE, receiver
    )

    /** package 指令，如 `package foo.bar` */
    class PACKAGE_DIRECTIVE(receiver: CjExpression?) :
        CallTypeAndReceiver<CjExpression?, CallType.PACKAGE_DIRECTIVE>(CallType.PACKAGE_DIRECTIVE, receiver)

    /** 类型引用，如 `var x: Type` */
    class TYPE(receiver: CjExpression?) : CallTypeAndReceiver<CjExpression?, CallType.TYPE>(CallType.TYPE, receiver)

    /** 委托表达式，如 `by delegate` */
    class DELEGATE(receiver: CjExpression?) :
        CallTypeAndReceiver<CjExpression?, CallType.DELEGATE>(CallType.DELEGATE, receiver)

    /** 注解引用，如 `@Annotation` */
    class ANNOTATION(receiver: CjExpression?) :
        CallTypeAndReceiver<CjExpression?, CallType.ANNOTATION>(CallType.ANNOTATION, receiver)

    companion object {
        /**
         * 检测表达式的调用类型和接收者
         *
         * 根据表达式的上下文自动判断调用类型，并提取接收者表达式。
         *
         * ## 检测逻辑
         *
         * 1. **可调用引用**: 如果父节点是 `CjCallableReferenceExpression`，返回 CALLABLE_REFERENCE
         * 2. **import/package 指令**: 检查表达式是否在导入或包声明中
         * 3. **类型引用**: 如果父节点是 `CjUserType`，返回 TYPE 或 ANNOTATION
         * 4. **运算符引用**: 如果是 `CjOperationReferenceExpression`，返回 OPERATOR
         * 5. **名称引用**: 根据接收者类型返回 DEFAULT、SUPER_MEMBERS、DOT 或 SAFE
         *
         * @param expression 要检测的简单名称表达式
         * @return 检测到的调用类型和接收者配对
         */
        fun detect(expression: CjSimpleNameExpression): CallTypeAndReceiver<*, *> {
            val parent = expression.parent
            if (parent is CjCallableReferenceExpression && expression == parent.callableReference) {
                return CALLABLE_REFERENCE(parent.receiverExpression, expression.languageVersionSettings)
            }

            val receiverExpression = expression.getReceiverExpression()

            if (parent != null) {
                if (expression.isImportDirectiveExpression()) {
                    return IMPORT_DIRECTIVE(receiverExpression)
                }

                if (expression.isPackageDirectiveExpression()) {
                    return PACKAGE_DIRECTIVE(receiverExpression)
                }
                if (parent is CjUserType) {
                    val constructorCallee =
                        (parent.parent as? CjTypeReference)?.parent as? CjConstructorCalleeExpression
                    if (constructorCallee != null && constructorCallee.parent is CjAnnotation) {
                        return ANNOTATION(receiverExpression)
                    }

                    return TYPE(receiverExpression)
                }
            }

            when (expression) {
                is CjOperationReferenceExpression -> {
                    if (receiverExpression == null) {
                        return UNKNOWN // incomplete code
                    }
                    return when (parent) {
                        is CjBinaryExpression -> {

                            OPERATOR(receiverExpression)
                        }

                        is CjUnaryExpression -> OPERATOR(receiverExpression)

                        else -> error("Unknown parent for CjOperationReferenceExpression: $parent with text '${parent?.text}'")
                    }
                }

                is CjNameReferenceExpression -> {
                    if (receiverExpression == null) {
                        return DEFAULT
                    }

                    if (receiverExpression is CjSuperExpression) {
                        return SUPER_MEMBERS(receiverExpression)
                    }

                    return when (parent) {
                        is CjCallExpression -> {
                            if ((parent.parent as CjQualifiedExpression).operationSign == CjTokens.SAFE_ACCESS)
                                SAFE(receiverExpression)
                            else
                                DOT(receiverExpression)
                        }

                        is CjQualifiedExpression -> {
                            if (parent.operationSign == CjTokens.SAFE_ACCESS)
                                SAFE(receiverExpression)
                            else
                                DOT(receiverExpression)
                        }

                        else -> error("Unknown parent for CjNameReferenceExpression with receiver: $parent")
                    }
                }

                else -> return UNKNOWN
            }
        }
    }
}

/**
 * 接收者类型信息
 *
 * 表示代码补全时的接收者类型，用于确定可用的成员和扩展。
 *
 * ## 隐式接收者 vs 显式接收者
 *
 * - **显式接收者**: 代码中明确写出的接收者，如 `obj.member` 中的 `obj`
 *   - `implicitValue = null`
 *   - `implicit = false`
 *
 * - **隐式接收者**: 由作用域隐式提供的接收者，如类内部的 `this`
 *   - `implicitValue != null`
 *   - `implicit = true`
 *
 * ## 接收者索引
 *
 * 当存在多个隐式接收者时（如嵌套类或扩展函数内部），
 * `receiverIndex` 用于标识接收者的优先级：
 * - 0: 最内层/最近的接收者
 * - 1, 2, ...: 外层接收者
 *
 * @property type 接收者的仓颉类型
 * @property receiverIndex 接收者索引（用于区分多个隐式接收者）
 * @property implicitValue 隐式接收者值（如果是隐式接收者）
 */
data class ReceiverType(
    val type: CangJieType,
    val receiverIndex: Int,
    val implicitValue: ReceiverValue? = null
) {
    /**
     * 是否为隐式接收者
     *
     * 如果 [implicitValue] 不为 null，则表示这是一个隐式接收者。
     */
    val implicit: Boolean get() = implicitValue != null


}


/**
 * 获取 PSI 元素的语言版本设置
 *
 * 返回与该元素关联的语言版本配置，用于确定可用的语言特性。
 * 目前返回默认配置，未来可以根据模块设置返回不同的配置。
 */
val PsiElement.languageVersionSettings: LanguageVersionSettings
    get() {
        return LanguageVersionSettingsImpl.DEFAULT
//        if (project.serviceOrNull<ProjectFileIndex>() == null) {
//            return LanguageVersionSettingsImpl.DEFAULT
//        }
//
//        return runReadAction {
//            project.service<LanguageSettingsProvider>().getLanguageVersionSettings(this.moduleInfo, project)
//        }
    }

/**
 * 获取模块的语言版本设置
 *
 * 返回与该模块关联的语言版本配置。
 * 目前返回默认配置。
 */
val Module.languageVersionSettings: LanguageVersionSettings
    get() = LanguageVersionSettingsImpl.DEFAULT

/**
 * 获取项目的语言版本设置
 *
 * 返回与该项目关联的语言版本配置。
 * 目前返回默认配置。
 */
val Project.languageVersionSettings: LanguageVersionSettings
    get() = LanguageVersionSettingsImpl.DEFAULT
