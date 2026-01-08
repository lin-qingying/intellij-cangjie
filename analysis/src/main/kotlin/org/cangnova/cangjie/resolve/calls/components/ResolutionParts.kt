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

package org.cangnova.cangjie.resolve.calls.components

import com.intellij.util.SmartList
import org.cangnova.cangjie.builtins.UnsignedTypes
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.DescriptorVisibilities.PRIVATE
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import org.cangnova.cangjie.resolve.calls.inference.*
import org.cangnova.cangjie.resolve.calls.inference.components.*
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.tower.*
import org.cangnova.cangjie.resolve.calls.util.getReceiverValueWithSmartCast
import org.cangnova.cangjie.resolve.isInsideInterface
import org.cangnova.cangjie.resolve.isStatic
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.receivers.ClassValueReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ClassifierQualifier
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.TypeUtils.noExpectedType
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.model.TypeConstructorMarker
import org.cangnova.cangjie.utils.compactIfPossible

/**
 * 检查操作符函数调用的解析部分
 *
 * 这个解析部分验证操作符函数的调用方式是否正确。在仓颉语言中,操作符函数应该通过
 * 操作符语法调用,而不是通过普通的函数调用语法。
 *
 * ## 工作原理
 *
 * 1. 检查候选描述符是否为函数
 * 2. 如果函数标记为操作符函数,检查调用方式
 * 3. 允许 `invoke` 和 `get` 操作符使用普通调用语法
 * 4. 对其他操作符的普通调用添加诊断错误
 *
 * ## 使用场景
 *
 * ```cangjie
 * class Counter {
 *     var value: Int64 = 0
 *
 *     // 操作符函数应该通过操作符语法调用
 *     public operator func +(other: Counter): Counter {
 *         return Counter(value + other.value)
 *     }
 * }
 *
 * let a = Counter(10)
 * let b = Counter(20)
 *
 * // 正确: 使用操作符语法
 * let c = a + b
 *
 * // 错误: 不应使用普通调用语法调用操作符函数
 * // let d = a.+(b)  // 会被 CheckOperatorCallPart 标记为错误
 *
 * // 例外: invoke 和 get 操作符可以使用普通调用
 * let func = Callable()
 * func()       // 正确: invoke 操作符
 * func.invoke() // 也正确
 * ```
 *
 * ## 设计原因
 *
 * 这个检查确保代码风格的一致性和可读性。操作符函数设计的目的就是提供简洁的
 * 操作符语法,不应该通过普通函数调用来使用。
 *
 * @see NoCallOperatorFunction 操作符调用错误的诊断类
 * @see OperatorNameConventions 操作符名称约定
 */
internal object CheckOperatorCallPart : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        if (candidateDescriptor !is FunctionDescriptor) return
        // 禁止使用 call 方式调用操作符函数
        if ((candidateDescriptor as FunctionDescriptor).isOperator && resolvedCall.atom.psiCangJieCall.psiCall.callElement is CjCallExpression) {
            val funcName = candidateDescriptor.name
            // invoke 和 get 操作符允许普通调用语法
            if (funcName == OperatorNameConventions.INVOKE
                || funcName == OperatorNameConventions.GET
            ) return

            addDiagnostic(NoCallOperatorFunction(candidateDescriptor))

        }
    }
}

/**
 * 检查 super 表达式调用的解析部分
 *
 * 这个解析部分验证通过 `super` 关键字调用父类成员时的正确性。
 * 主要防止调用抽象方法或存在歧义的方法。
 *
 * ## 工作原理
 *
 * 1. 检查分发接收者是否为 super 表达式
 * 2. 如果是,检查候选描述符的特性:
 *    - **抽象方法**: 不能通过 super 调用
 *    - **假重写(Fake Override)**: 检查多重继承时的歧义
 * 3. 对不合法的调用添加诊断错误
 *
 * ## 使用场景
 *
 * ```cangjie
 * // 场景 1: 调用抽象方法(错误)
 * open class Base {
 *     open func abstract process(): Unit  // 抽象方法
 * }
 *
 * class Derived <: Base {
 *     override func process(): Unit {
 *         // super.process()  // 错误: 不能调用抽象方法
 *     }
 * }
 *
 * // 场景 2: 正确的 super 调用
 * open class Animal {
 *     open func makeSound(): String {
 *         return "Some sound"
 *     }
 * }
 *
 * class Dog <: Animal {
 *     override func makeSound(): String {
 *         let baseSound = super.makeSound()  // 正确
 *         return baseSound + " Woof!"
 *     }
 * }
 *
 * // 场景 3: 多重继承中的歧义(错误)
 * interface A {
 *     func method(): Unit
 * }
 *
 * interface B {
 *     func method(): Unit
 * }
 *
 * class C <: A, B {
 *     override func method(): Unit {
 *         // super.method()  // 错误: 歧义,不知道调用哪个父接口的方法
 *     }
 * }
 * ```
 *
 * ## 错误类型
 *
 * - **AbstractSuperCall**: 尝试调用抽象方法
 * - **AbstractFakeOverrideSuperCall**: 尝试调用多重继承中的歧义抽象方法
 *
 * @see AbstractSuperCall 抽象super调用错误
 * @see AbstractFakeOverrideSuperCall 抽象假重写super调用错误
 * @see CallableMemberDescriptor.Kind.FAKE_OVERRIDE 假重写类型
 */
internal object CheckSuperExpressionCallPart : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        val candidateDescriptor = resolvedCall.candidateDescriptor

        if (callComponents.statelessCallbacks.isSuperExpression(resolvedCall.dispatchReceiverArgument)) {
            if (candidateDescriptor is CallableMemberDescriptor) {
                checkSuperCandidateDescriptor(candidateDescriptor)
            }
        }

    }

    /**
     * 检查 super 调用的候选描述符
     *
     * 验证通过 super 调用的成员是否合法:
     * 1. 检查是否为抽象成员
     * 2. 检查假重写(Fake Override)的情况,防止多重继承歧义
     *
     * @param candidateDescriptor 被调用的成员描述符
     */
    private fun ResolutionCandidate.checkSuperCandidateDescriptor(candidateDescriptor: CallableMemberDescriptor) {
        // 情况 1: 直接调用抽象成员
        if (candidateDescriptor.modality == Modality.ABSTRACT) {
            addDiagnostic(AbstractSuperCall(resolvedCall.dispatchReceiverArgument!!))
        }
        // 情况 2: 调用假重写成员(可能存在多重继承歧义)
        else if (candidateDescriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            var intersectionFakeOverrideDescriptor = candidateDescriptor
            // 追溯到真正的重写链
            while (intersectionFakeOverrideDescriptor.overriddenDescriptors.size == 1) {
                intersectionFakeOverrideDescriptor = intersectionFakeOverrideDescriptor.overriddenDescriptors.first()
                if (intersectionFakeOverrideDescriptor.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                    return
                }
            }
            // 如果有多个被重写的描述符,检查是否包含抽象的非接口成员
            if (intersectionFakeOverrideDescriptor.overriddenDescriptors.size > 1) {
                if (intersectionFakeOverrideDescriptor.overriddenDescriptors.firstOrNull {
                        !it.isInsideInterface
                    }?.modality == Modality.ABSTRACT
                ) {
                    addDiagnostic(AbstractFakeOverrideSuperCall)
                }
            }
        }
    }
}


/**
 * 判断词法作用域是否为静态上下文
 *
 * 静态上下文是指代码位于静态声明内部,例如:
 * - 静态方法内
 * - 静态属性的初始化器中
 * - 静态初始化块中
 *
 * @return true 表示词法作用域属于静态声明
 */
fun LexicalScope.isStaticContext(): Boolean {
    return ownerDescriptor.isStatic()
}

/**
 * 判断解析候选项是否处于静态上下文
 *
 * 静态上下文包括:
 * - 通过类型名(ClassifierQualifier)访问成员,如 `ClassName.member`
 * - 在静态成员声明内部
 *
 * 在静态上下文中,只能访问静态成员(静态方法/属性、构造器、枚举构造器)。
 *
 * ## 工作原理
 *
 * 1. 如果没有显式接收者,检查词法作用域是否静态
 * 2. 如果有显式接收者,检查接收者类型:
 *    - 通过 `ClassifierQualifier` 访问 → 静态上下文
 *    - 其他接收者类型 → 非静态上下文
 *
 * ## 使用场景
 *
 * ```cangjie
 * class MyClass {
 *     var instanceVar: Int64 = 10
 *     static var staticVar: Int64 = 20
 *
 *     // 静态方法内部是静态上下文
 *     static func staticMethod(): Unit {
 *         // staticVar  // 正确: 可以访问静态成员
 *         // instanceVar  // 错误: 不能访问实例成员
 *     }
 *
 *     // 实例方法内部是非静态上下文
 *     func instanceMethod(): Unit {
 *         instanceVar  // 正确: 可以访问实例成员
 *         staticVar    // 正确: 也可以访问静态成员
 *     }
 * }
 *
 * // 通过类名访问是静态上下文
 * let value1 = MyClass.staticVar     // 正确
 * // let value2 = MyClass.instanceVar  // 错误: 不能通过类名访问实例成员
 *
 * // 通过实例访问是非静态上下文
 * let obj = MyClass()
 * let value3 = obj.instanceVar  // 正确
 * let value4 = obj.staticVar    // 正确
 * ```
 *
 * @return true 表示处于静态上下文
 * @see ClassifierQualifier 类型限定符
 * @see LexicalScope.isStaticContext 词法作用域的静态检查
 */
fun ResolutionCandidate.isStaticContext(): Boolean {
    /**
     * 检查调用上下文的 Scope 是否属于静态声明
     */
    fun isLexicalStaticContext(): Boolean {
        return scopeTower.lexicalScope.isStaticContext()
    }

    // 如果没有显式接收者,检查词法作用域
    resolvedCall.atom.explicitReceiver ?: return isLexicalStaticContext()

    // 所有通过类型名(ClassifierQualifier)的访问都是静态上下文
    // 包括普通类、枚举类、类型别名
    return when (resolvedCall.atom.explicitReceiver!!.receiver) {
        is ClassifierQualifier -> true
        else -> false
    }
}


/**
 * 检查静态调用的解析部分
 *
 * 这个解析部分验证静态上下文和非静态上下文的成员访问规则。
 * 目前该部分的实现被注释掉,但保留了检查逻辑作为参考。
 *
 * ## 设计的检查规则(已注释)
 *
 * 1. **非静态上下文访问静态成员检查**:
 *    - 在实例方法中通过 `this.staticMember` 访问静态成员应该给出警告
 *    - 建议使用 `ClassName.staticMember` 访问
 *
 * 2. **静态上下文访问非静态成员检查**:
 *    - 在静态方法中访问实例成员应该报错
 *    - 除非是本地变量或顶层声明
 *
 * ## 示例(基于注释的逻辑)
 *
 * ```cangjie
 * class Example {
 *     var instanceField: Int64 = 10
 *     static var staticField: Int64 = 20
 *
 *     static func staticMethod(): Unit {
 *         // let x = instanceField  // 应该报错: 静态上下文访问实例成员
 *         let y = staticField       // 正确
 *     }
 *
 *     func instanceMethod(): Unit {
 *         let x = instanceField     // 正确
 *         let y = staticField       // 正确
 *         // let z = this.staticField  // 可能警告: 建议使用 Example.staticField
 *     }
 * }
 * ```
 *
 * @see NonStaticContextAccessStaticMemberDiagnostic 非静态上下文访问静态成员诊断
 * @see StaticContextAccessNonStaticMemberDiagnostic 静态上下文访问非静态成员诊断
 * @see DescriptorKind 描述符类型
 */
internal object CheckStaticCall : ResolutionPart() {


    override fun ResolutionCandidate.process(workIndex: Int) {
//        val descriptor = this.descriptor
//
//
////        是否为static上下文
//        val isStaticContext = isStaticContext()
//
//        val kind = descriptor.getDescriptorKind()
//        val memberStatic = descriptor.isStatic()
////        非静态上下文访问静态成员
//        if (memberStatic && (!isStaticContext && (resolvedCall.explicitReceiverKind == ExplicitReceiverKind.DISPATCH_RECEIVER || resolvedCall.explicitReceiverKind == ExplicitReceiverKind.EXTENSION_RECEIVER))) {
//            addDiagnostic(NonStaticContextAccessStaticMemberDiagnostic(kind, descriptor))
//        }
//
////静态上下文访问非静成员
//        if (!memberStatic && isStaticContext && !descriptor.isLocal && !descriptor.isTopLevel
//
//        ) {
//            addDiagnostic(StaticContextAccessNonStaticMemberDiagnostic(kind, descriptor))
//        }


    }

}

/**
 * 可适用的上下文接收者参数与约束
 *
 * 这个数据类封装了与上下文接收者相关的类型信息和约束:
 * - 参数本身
 * - 参数的实际类型(展开后)
 * - 期望的类型(展开后)
 * - 约束位置信息
 *
 * 用于判断上下文接收者是否与目标参数类型兼容。
 *
 * @property argument 上下文接收者参数
 * @property argumentType 参数的展开类型
 * @property expectedType 期望的展开类型
 * @property position 约束位置,用于错误报告
 */
private data class ApplicableContextReceiverArgumentWithConstraint(
    val argument: SimpleCangJieCallArgument,
    val argumentType: UnwrappedType,
    val expectedType: UnwrappedType,
    val position: ConstraintPosition
)

/**
 * 获取兼容的接收者参数及其约束
 *
 * 检查给定的参数是否可以作为指定参数的接收者,如果类型兼容则返回封装的约束信息。
 *
 * ## 工作原理
 *
 * 1. 获取参数的期望类型
 * 2. 准备期望类型(应用替换器)
 * 3. 从类型参数上界捕获类型(如果需要)
 * 4. 检查子类型约束是否兼容
 * 5. 如果兼容,返回封装的约束信息
 *
 * @param argument 候选的接收者参数
 * @param parameter 目标参数描述符
 * @return 如果兼容,返回约束信息;否则返回 null
 */
private fun ResolutionCandidate.getReceiverArgumentWithConstraintIfCompatible(
    argument: SimpleCangJieCallArgument,
    parameter: ParameterDescriptor
): ApplicableContextReceiverArgumentWithConstraint? {
    val csBuilder = getSystem().getBuilder()
    val expectedTypeUnprepared = argument.getExpectedType(parameter, callComponents.languageVersionSettings)
    val expectedType = prepareExpectedType(expectedTypeUnprepared)
    val argumentType = captureFromTypeParameterUpperBoundIfNeeded(argument.receiver.stableType, expectedType)
    val position = ReceiverConstraintPositionImpl(argument, resolvedCall.atom)
    return if (csBuilder.isSubtypeConstraintCompatible(argumentType, expectedType, position))
        ApplicableContextReceiverArgumentWithConstraint(argument, argumentType, expectedType, position)
    else null
}

/**
 * 隐式 invoke 调用检查状态
 *
 * 用于表示隐式 invoke 调用的不同状态,特别关注安全性问题。
 *
 * ## 状态说明
 *
 * - **NO_INVOKE**: 不是隐式 invoke 调用
 * - **INVOKE_ON_NOT_NULL_VARIABLE**: 在非空变量上进行 invoke 调用(安全)
 * - **UNSAFE_INVOKE_REPORTED**: 已报告不安全的 invoke 调用(例如在 Option 类型上)
 *
 * ## 使用场景
 *
 * ```cangjie
 * // 场景 1: 正常的 invoke 调用
 * let func: () -> Unit = { println("Hello") }
 * func()  // INVOKE_ON_NOT_NULL_VARIABLE
 *
 * // 场景 2: 安全调用后的 invoke(潜在问题)
 * let optionalFunc: Option<() -> Unit> = Some({ println("Hello") })
 * optionalFunc?()  // UNSAFE_INVOKE_REPORTED: Option 类型上的 invoke 调用
 * ```
 */
internal enum class ImplicitInvokeCheckStatus {
    NO_INVOKE, INVOKE_ON_NOT_NULL_VARIABLE, UNSAFE_INVOKE_REPORTED
}


/**
 * 检查安全调用后的不安全隐式 invoke
 *
 * 当一个函数类型变量通过安全调用操作符 `?` 调用时,需要特别检查。
 * 这是因为 Option<FunctionType> 上的 invoke 调用可能导致意外行为。
 *
 * ## 工作原理
 *
 * 1. 检查是否为隐式 invoke 调用(通过 `variableCandidateIfInvoke`)
 * 2. 获取变量调用的接收者参数
 * 3. 检查接收者是否为安全调用且类型为 Option
 * 4. 如果是,且函数没有类型参数,报告不安全调用错误
 *
 * ## 使用场景
 *
 * ```cangjie
 * // 不安全的情况
 * class MyClass {
 *     var callback: (() -> Unit)? = null
 *
 *     func execute(): Unit {
 *         // callback?()  // 错误: Option 类型上的不安全 invoke 调用
 *         // 应该使用:
 *         if (callback != null) {
 *             callback!()
 *         }
 *     }
 * }
 * ```
 *
 * @param argument 当前的参数
 * @return invoke 检查状态
 * @see ImplicitInvokeCheckStatus invoke 检查状态枚举
 * @see UnsafeCallError 不安全调用错误诊断
 */
private fun ResolutionCandidate.checkUnsafeImplicitInvokeAfterSafeCall(argument: SimpleCangJieCallArgument): ImplicitInvokeCheckStatus {
    val variableForInvoke = variableCandidateIfInvoke ?: return ImplicitInvokeCheckStatus.NO_INVOKE

    val receiverArgument = with(variableForInvoke.resolvedCall) {
        when (explicitReceiverKind) {
            ExplicitReceiverKind.DISPATCH_RECEIVER -> dispatchReceiverArgument

            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER -> return ImplicitInvokeCheckStatus.INVOKE_ON_NOT_NULL_VARIABLE
        }
    } ?: error("Receiver kind does not match receiver argument")

    if (receiverArgument.isSafeCall && receiverArgument.receiver.stableType.isOption && resolvedCall.candidateDescriptor.typeParameters.isEmpty()) {
        addDiagnostic(UnsafeCallError(argument, isForImplicitInvoke = true))
        return ImplicitInvokeCheckStatus.UNSAFE_INVOKE_REPORTED
    }

    return ImplicitInvokeCheckStatus.INVOKE_ON_NOT_NULL_VARIABLE
}

/**
 * 初始化 VArray 类型的解析部分
 *
 * 这个解析部分负责初始化可变数组(VArray)类型的特殊处理。
 * 目前实现尚未完成,保留作为未来功能的占位符。
 *
 * ## 预期功能
 *
 * VArray 是可变数组类型,可能需要特殊的初始化逻辑,例如:
 * - 容量预分配
 * - 初始元素类型推导
 * - 内存布局优化
 *
 * ## 使用场景(计划中)
 *
 * ```cangjie
 * // VArray 初始化
 * let array = VArray<Int64>()
 * let array2 = VArray(1, 2, 3)  // 从元素推导类型
 * ```
 *
 * @see TODO 待实现
 */
internal object InitVArray : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        // 初始化 VArray 类型
        TODO("Not yet implemented")
    }

}

/**
 * 检查扩展之间的 private 可见性的解析部分
 *
 * 这个解析部分专门处理扩展函数/属性之间的 private 可见性检查。
 * 扩展成员的可见性规则与普通成员有所不同,需要特殊处理。
 *
 * ## 工作原理
 *
 * 1. 获取调用所在的声明描述符
 * 2. 获取分发接收者参数
 * 3. 检查被调用描述符的可见性
 * 4. 如果有智能转换,检查转换后的接收者是否可见
 * 5. 如果不可见,添加可见性错误诊断
 *
 * ## 使用场景
 *
 * ```cangjie
 * // 文件 A.cj
 * class MyClass {
 *     private var data: Int64 = 10
 * }
 *
 * // 扩展函数(同文件中)
 * private func MyClass.getData(): Int64 {
 *     return this.data  // 正确: 同文件中可以访问 private 成员
 * }
 *
 * // 文件 B.cj
 * func use(obj: MyClass): Unit {
 *     // let value = obj.getData()  // 错误: getData 是 private 扩展
 * }
 * ```
 *
 * ## 智能转换支持
 *
 * 如果接收者经过智能转换后可以访问成员,会添加智能转换诊断而不是报错:
 *
 * ```cangjie
 * open class Base
 * class Derived <: Base {
 *     private func helper(): Unit { }
 * }
 *
 * func process(obj: Base): Unit {
 *     if (obj is Derived) {
 *         // 智能转换使 private 成员可见
 *     }
 * }
 * ```
 *
 * @see VisibilityError 可见性错误诊断
 * @see SmartCastDiagnostic 智能转换诊断
 * @see DescriptorVisibilityUtils.findInvisibleMember 查找不可见成员
 */
internal object CheckExtensionPrivateVisibility : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        val containingDescriptor = scopeTower.lexicalScope.ownerDescriptor  // 调用所在声明

        val dispatchReceiverArgument = resolvedCall.dispatchReceiverArgument

        val callCandidateDescriptor = resolvedCall.candidateDescriptor  // 被调用声明
        val receiverValue =
            dispatchReceiverArgument?.receiver?.receiverValue ?: DescriptorVisibilities.ALWAYS_SUITABLE_RECEIVER
        val invisibleMember =
            DescriptorVisibilityUtils.findInvisibleMember(
                receiverValue,
                callCandidateDescriptor,
                containingDescriptor,
                callComponents.languageVersionSettings
            )

        // 检查智能转换是否使成员可见
        if (dispatchReceiverArgument is ExpressionCangJieCallArgument) {
            val smartCastReceiver =
                getReceiverValueWithSmartCast(receiverValue, dispatchReceiverArgument.receiver.stableType)
            if (DescriptorVisibilityUtils.findInvisibleMember(
                    smartCastReceiver,
                    candidateDescriptor,
                    containingDescriptor,
                    callComponents.languageVersionSettings
                ) == null
            ) {
                addDiagnostic(
                    SmartCastDiagnostic(
                        dispatchReceiverArgument,
                        dispatchReceiverArgument.receiver.stableType,
                        resolvedCall.atom
                    )
                )
                return
            }
        }

        if (containingDescriptor is CallableDescriptor) {


        }


        if (invisibleMember is DeclarationDescriptorWithVisibility) {
            addDiagnostic(VisibilityError(invisibleMember))
        }


    }

}

/**
 * 主动解析可调用引用的解析部分
 *
 * 这个解析部分处理可调用引用(Callable Reference)的主动解析。
 * 可调用引用是对函数或属性的引用,可以作为函数类型的值传递。
 *
 * ## 工作原理
 *
 * 1. 从子解析原子中筛选出 `EagerCallableReferenceAtom`
 * 2. 对每个主动可调用引用原子,调用解析器处理
 * 3. 将解析结果添加到约束系统
 *
 * ## 使用场景
 *
 * ```cangjie
 * class MyClass {
 *     func method(x: Int64): String {
 *         return x.toString()
 *     }
 * }
 *
 * // 可调用引用示例
 * let obj = MyClass()
 *
 * // 方法引用
 * let methodRef: (Int64) -> String = obj::method
 * let result = methodRef(42)  // 等价于 obj.method(42)
 *
 * // 函数引用
 * func double(x: Int64): Int64 {
 *     return x * 2
 * }
 * let funcRef: (Int64) -> Int64 = ::double
 * let value = funcRef(21)  // 等价于 double(21)
 * ```
 *
 * ## 解析流程
 *
 * - 主动解析(Eager): 立即推导类型和检查兼容性
 * - 与延迟解析(Lazy)相对: 某些情况下可以延后类型推导
 *
 * @see EagerCallableReferenceAtom 主动可调用引用原子
 * @see CallableReferenceArgumentResolver 可调用引用参数解析器
 */
internal object EagerResolveOfCallableReferences : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        getSubResolvedAtoms()
            .filterIsInstance<EagerCallableReferenceAtom>()
            .forEach {
                callComponents.callableReferenceArgumentResolver.processCallableReferenceArgument(
                    getSystem().getBuilder(), it, this, resolutionCallbacks
                )
            }
    }
}

/**
 * 检查可见性的解析部分
 *
 * 这个解析部分检查成员访问的可见性规则,确保调用者有权限访问目标成员。
 * 这是通用的可见性检查,适用于所有类型的成员访问。
 *
 * ## 工作原理
 *
 * 1. 获取调用所在的包含描述符
 * 2. 获取分发接收者参数(如果有)
 * 3. 使用 `DescriptorVisibilityUtils.findInvisibleMember` 查找不可见成员
 * 4. 如果成员不可见:
 *    - 检查智能转换是否使其可见
 *    - 如果智能转换可见,添加智能转换诊断
 *    - 否则,添加可见性错误诊断
 *
 * ## 可见性修饰符
 *
 * 仓颉语言的可见性修饰符:
 * - **public**: 公开可见(默认)
 * - **internal**: 模块内可见
 * - **protected**: 子类和同包可见
 * - **private**: 仅当前声明内可见
 *
 * ## 使用场景
 *
 * ```cangjie
 * // 文件 A.cj
 * class Library {
 *     private var secret: String = "hidden"
 *     internal var config: Int64 = 42
 *     public var data: String = "visible"
 *
 *     func publicMethod(): Unit { }
 *     private func privateMethod(): Unit { }
 * }
 *
 * // 文件 B.cj (同模块)
 * func use(lib: Library): Unit {
 *     lib.publicMethod()  // 正确: public 成员
 *     let x = lib.config  // 正确: internal 成员在同模块可见
 *     let y = lib.data    // 正确: public 成员
 *     // lib.privateMethod()  // 错误: private 成员不可见
 *     // let z = lib.secret   // 错误: private 属性不可见
 * }
 *
 * // 文件 C.cj (不同模块)
 * func useFromOtherModule(lib: Library): Unit {
 *     lib.publicMethod()  // 正确: public 成员
 *     let y = lib.data    // 正确: public 成员
 *     // let x = lib.config  // 错误: internal 成员在不同模块不可见
 * }
 * ```
 *
 * ## 智能转换支持
 *
 * ```cangjie
 * open class Base
 * class Derived <: Base {
 *     private var value: Int64 = 10
 * }
 *
 * func process(obj: Base): Unit {
 *     if (obj is Derived) {
 *         // 智能转换后可以访问 Derived 的成员
 *         // (但 private 成员仍然不可见)
 *     }
 * }
 * ```
 *
 * @see VisibilityError 可见性错误诊断
 * @see SmartCastDiagnostic 智能转换诊断
 * @see DescriptorVisibilityUtils.findInvisibleMember 查找不可见成员
 * @see DescriptorVisibilities 可见性常量
 */
internal object CheckVisibility : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        val containingDescriptor = scopeTower.lexicalScope.ownerDescriptor
        val dispatchReceiverArgument = resolvedCall.dispatchReceiverArgument

        val receiverValue =
            dispatchReceiverArgument?.receiver?.receiverValue ?: DescriptorVisibilities.ALWAYS_SUITABLE_RECEIVER
        val invisibleMember =
            DescriptorVisibilityUtils.findInvisibleMember(
                receiverValue,
                resolvedCall.candidateDescriptor,
                containingDescriptor,
                callComponents.languageVersionSettings
            ) ?: return

        // 检查智能转换是否使成员可见
        if (dispatchReceiverArgument is ExpressionCangJieCallArgument) {
            val smartCastReceiver =
                getReceiverValueWithSmartCast(receiverValue, dispatchReceiverArgument.receiver.stableType)
            if (DescriptorVisibilityUtils.findInvisibleMember(
                    smartCastReceiver,
                    candidateDescriptor,
                    containingDescriptor,
                    callComponents.languageVersionSettings
                ) == null
            ) {
                addDiagnostic(
                    SmartCastDiagnostic(
                        dispatchReceiverArgument,
                        dispatchReceiverArgument.receiver.stableType,
                        resolvedCall.atom
                    )
                )
                return
            }
        }


        if (invisibleMember is DeclarationDescriptorWithVisibility) {
            addDiagnostic(VisibilityError(invisibleMember))

        }
    }
}


/**
 * 无类型参数的解析部分
 *
 * 这个解析部分处理不带显式类型参数的调用,例如变量访问。
 * 它断言调用确实没有类型参数,并设置空的类型参数映射。
 *
 * ## 工作原理
 *
 * 1. 断言调用没有显式类型参数
 * 2. 设置类型参数映射为 `NoExplicitArguments`
 *
 * ## 使用场景
 *
 * ```cangjie
 * class MyClass<T> {
 *     var value: T
 * }
 *
 * let obj = MyClass<Int64>()
 *
 * // 变量访问,没有类型参数
 * let v = obj.value  // NoTypeArguments 处理这种情况
 *
 * // 函数调用可能有类型参数
 * // func get<T>(): T
 * // let x = get<Int64>()  // 这种情况由 MapTypeArguments 处理
 * ```
 *
 * @see TypeArgumentsToParametersMapper.TypeArgumentsMapping.NoExplicitArguments
 * @see MapTypeArguments 映射显式类型参数的解析部分
 */
internal object NoTypeArguments : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        assert(cangjieCall.typeArguments.isEmpty()) {
            "Variable call cannot has explicit type arguments: ${cangjieCall.typeArguments}. Call: $cangjieCall"
        }
        resolvedCall.typeArgumentMappingByOriginal =
            TypeArgumentsToParametersMapper.TypeArgumentsMapping.NoExplicitArguments
    }
}




/**
 * 映射类型参数的解析部分
 *
 * 这个解析部分将调用中的显式类型参数映射到候选函数的类型参数上。
 * 这是泛型函数调用解析的关键步骤。
 *
 * ## 工作原理
 *
 * 1. 调用 `typeArgumentsToParametersMapper.mapTypeArguments` 进行映射
 * 2. 映射器会处理:
 *    - 位置参数映射: `func<T, U>` 调用为 `func<Int64, String>`
 *    - 命名参数映射: (如果支持)
 *    - 数量检查: 类型参数数量与类型形参数量匹配
 * 3. 将映射结果和诊断信息存储到 `resolvedCall`
 *
 * ## 使用场景
 *
 * ```cangjie
 * // 泛型函数定义
 * func identity<T>(value: T): T {
 *     return value
 * }
 *
 * func pair<K, V>(key: K, value: V): (K, V) {
 *     return (key, value)
 * }
 *
 * // 显式类型参数调用
 * let x = identity<Int64>(42)  // T 映射到 Int64
 * let p = pair<String, Int64>("answer", 42)  // K->String, V->Int64
 *
 * // 类型参数数量错误
 * // let y = identity<Int64, String>(42)  // 错误: 类型参数过多
 * // let z = pair<Int64>("key", 10)      // 错误: 类型参数不足
 * ```
 *
 * ## 错误处理
 *
 * 映射器会生成以下诊断:
 * - 类型参数数量不匹配
 * - 类型参数不满足约束
 * - 无效的类型参数
 *
 * @see TypeArgumentsToParametersMapper 类型参数映射器
 * @see TypeArgumentsToParametersMapper.TypeArgumentsMapping 映射结果
 * @see NoTypeArguments 处理无类型参数情况
 */
internal object MapTypeArguments : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        resolvedCall.typeArgumentMappingByOriginal =
            callComponents.typeArgumentsToParametersMapper.mapTypeArguments(
                cangjieCall,
                candidateDescriptor.original
            )
                .also {
                    it.diagnostics.forEach(this@process::addDiagnostic)
                }
    }
}

internal object CollectionTypeVariableUsagesInfo : ResolutionPart() {
    private val CangJieType.isComputed get() = this !is WrappedType || isComputed()

    private fun NewConstraintSystem.isContainedInInvariantOrContravariantPositions(
        variableTypeConstructor: TypeConstructorMarker,
        baseType: CangJieTypeMarker,
        wasOutVariance: Boolean = true
    ): Boolean {
        if (baseType !is CangJieType) return false

        val dependentTypeParameter = getTypeParameterByVariable(variableTypeConstructor) ?: return false
        val declaredTypeParameters = baseType.constructor.parameters

        if (declaredTypeParameters.size < baseType.arguments.size) return false

        for ((_, argument) in baseType.arguments.withIndex()) {
//            if ( argument.type.isMarkedOption) continue

            val currentEffectiveVariance = false
//                declaredTypeParameters[argumentsIndex].variance == Variance.OUT_VARIANCE || argument.projectionKind == Variance.OUT_VARIANCE
            val effectiveVarianceFromTopLevel = wasOutVariance && currentEffectiveVariance

            if ((argument.type.constructor == dependentTypeParameter || argument.type.constructor == variableTypeConstructor) && !effectiveVarianceFromTopLevel)
                return true

            if (isContainedInInvariantOrContravariantPositions(
                    variableTypeConstructor,
                    argument.type,
                    effectiveVarianceFromTopLevel
                )
            )
                return true
        }

        return false
    }

    private fun isContainedInInvariantOrContravariantPositionsAmongTypeParameters(
        checkingType: TypeVariableFromCallableDescriptor,
        typeParameters: List<TypeParameterDescriptor>
    ) = typeParameters.any {
        it.typeConstructor == checkingType.originalTypeParameter.typeConstructor
    }

    private fun NewConstraintSystem.getDependentTypeParameters(
        variable: TypeConstructorMarker,
        dependentTypeParametersSeen: List<Pair<TypeConstructorMarker, CangJieTypeMarker?>> = listOf()
    ): List<Pair<TypeConstructorMarker, CangJieTypeMarker?>> {
        val context = asConstraintSystemCompleterContext()
        val dependentTypeParameters = getBuilder().currentStorage().notFixedTypeVariables.asSequence()
            .flatMap { (typeConstructor, constraints) ->
                val upperBounds = constraints.constraints.filter {
                    it.position.from is DeclaredUpperBoundConstraintPositionImpl && it.kind == ConstraintKind.UPPER
                }

                upperBounds.mapNotNull { constraint ->
                    if (constraint.type.typeConstructor(context) != variable) {
                        val suitableUpperBound = upperBounds.find { upperBound ->
                            with(context) { upperBound.type.contains { it.typeConstructor() == variable } }
                        }?.type

                        if (suitableUpperBound != null) typeConstructor to suitableUpperBound else null
                    } else typeConstructor to null
                }
            }.filter { it !in dependentTypeParametersSeen && it.first != variable }.toList()

        return dependentTypeParameters + dependentTypeParameters.flatMapTo(SmartList()) { (typeConstructor, _) ->
            if (typeConstructor != variable) {
                getDependentTypeParameters(typeConstructor, dependentTypeParameters + dependentTypeParametersSeen)
            } else emptyList()
        }
    }

    private fun NewConstraintSystem.isContainedInInvariantOrContravariantPositionsAmongUpperBound(
        checkingType: TypeConstructorMarker,
        dependentTypeParameters: List<Pair<TypeConstructorMarker, CangJieTypeMarker?>>
    ): Boolean {
        var currentTypeParameterConstructor = checkingType

        return dependentTypeParameters.any { (typeConstructor, upperBound) ->
            val isContainedOrNoUpperBound =
                upperBound == null || isContainedInInvariantOrContravariantPositions(
                    currentTypeParameterConstructor,
                    upperBound
                )
            currentTypeParameterConstructor = typeConstructor
            isContainedOrNoUpperBound
        }
    }

    private fun NewConstraintSystem.getTypeParameterByVariable(typeConstructor: TypeConstructorMarker) =
        (getBuilder().currentStorage().allTypeVariables[typeConstructor] as? TypeVariableFromCallableDescriptor)?.originalTypeParameter?.typeConstructor

    private fun NewConstraintSystem.getDependingOnTypeParameter(variable: TypeConstructor) =
        getBuilder().currentStorage().notFixedTypeVariables[variable]?.constraints?.mapNotNull {
            if (it.position.from is DeclaredUpperBoundConstraintPositionImpl && it.kind == ConstraintKind.UPPER) {
                it.type.typeConstructor(asConstraintSystemCompleterContext())
            } else null
        } ?: emptyList()

    private fun NewConstraintSystem.isContainedInInvariantOrContravariantPositionsWithDependencies(
        variable: TypeVariableFromCallableDescriptor,
        declarationDescriptor: DeclarationDescriptor?
    ): Boolean {
        if (declarationDescriptor !is CallableDescriptor) return false

        val returnType = declarationDescriptor.returnType ?: return false

        if (!returnType.isComputed) return false

        val typeVariableConstructor = variable.freshTypeConstructor
        val dependentTypeParameters = getDependentTypeParameters(typeVariableConstructor)
        val dependingOnTypeParameter = getDependingOnTypeParameter(typeVariableConstructor)

        val isContainedInUpperBounds =
            isContainedInInvariantOrContravariantPositionsAmongUpperBound(
                typeVariableConstructor,
                dependentTypeParameters
            )
        val isContainedAnyDependentTypeInReturnType = dependentTypeParameters.any { (typeParameter, _) ->
            returnType.contains {
                it.typeConstructor(asConstraintSystemCompleterContext()) == getTypeParameterByVariable(typeParameter) && !it.isOption
            }
        }

        return isContainedInInvariantOrContravariantPositions(typeVariableConstructor, returnType)
                || dependingOnTypeParameter.any { isContainedInInvariantOrContravariantPositions(it, returnType) }
                || dependentTypeParameters.any { isContainedInInvariantOrContravariantPositions(it.first, returnType) }
                || (isContainedAnyDependentTypeInReturnType && isContainedInUpperBounds)
    }

    private fun TypeVariableFromCallableDescriptor.recordInfoAboutTypeVariableUsagesAsInvariantOrContravariantParameter() {
        freshTypeConstructor.isContainedInInvariantOrContravariantPositions = true
    }

    override fun ResolutionCandidate.process(workIndex: Int) {
        for (variable in resolvedCall.freshVariablesSubstitutor.freshVariables) {
            val candidateDescriptor = resolvedCall.candidateDescriptor
            if (candidateDescriptor is ClassConstructorDescriptor) {
                val typeParameters = candidateDescriptor.containingDeclaration.declaredTypeParameters

                if (isContainedInInvariantOrContravariantPositionsAmongTypeParameters(variable, typeParameters)) {
                    variable.recordInfoAboutTypeVariableUsagesAsInvariantOrContravariantParameter()
                }
            } else if (getSystem().isContainedInInvariantOrContravariantPositionsWithDependencies(
                    variable,
                    this.candidateDescriptor
                )
            ) {
                variable.recordInfoAboutTypeVariableUsagesAsInvariantOrContravariantParameter()
            }
        }
    }
}

internal object CreateFreshVariablesSubstitutor : ResolutionPart() {
    fun TypeParameterDescriptor.shouldBeFlexible(flexibleCheck: (CangJieType) -> Boolean = { it.isFlexible() }): Boolean {
        return upperBounds.any {
            flexibleCheck(it) || ((it.constructor.declarationDescriptor as? TypeParameterDescriptor)?.run { shouldBeFlexible() }
                ?: false)
        }
    }

    fun createToFreshVariableSubstitutorAndAddInitialConstraints(
        candidateDescriptor: CallableDescriptor,
        cangjieCall: CangJieCall,
        csBuilder: ConstraintSystemOperation,
        typeParameters: List<TypeParameterDescriptor> = candidateDescriptor.typeParameters
    ): FreshVariableNewTypeSubstitutor {

        val freshTypeVariables = typeParameters.map { TypeVariableFromCallableDescriptor(it) }

        val toFreshVariables = FreshVariableNewTypeSubstitutor(freshTypeVariables)

        for (freshVariable in freshTypeVariables) {
            csBuilder.registerVariable(freshVariable)
        }

        fun TypeVariableFromCallableDescriptor.addSubtypeConstraint(
            upperBound: CangJieType,
            position: DeclaredUpperBoundConstraintPositionImpl
        ) {
            csBuilder.addSubtypeConstraint(
                defaultType,
                toFreshVariables.safeSubstitute(upperBound.unwrap()),
                position
            )
        }

        for (index in typeParameters.indices) {
            val typeParameter = typeParameters[index]
            val freshVariable = freshTypeVariables[index]
            val position = DeclaredUpperBoundConstraintPositionImpl(typeParameter, cangjieCall)

            for (upperBound in typeParameter.upperBounds) {
                freshVariable.addSubtypeConstraint(upperBound, position)
            }
        }

        if (candidateDescriptor is TypeAliasConstructorDescriptor) {
            val typeAliasDescriptor = candidateDescriptor.typeAliasDescriptor
            val originalTypes = typeAliasDescriptor.underlyingType.arguments.map { it.type }
            val originalTypeParameters = candidateDescriptor.underlyingConstructorDescriptor.typeParameters
            for (index in typeParameters.indices) {
                val typeParameter = typeParameters[index]
                val freshVariable = freshTypeVariables[index]
                val typeMapping = originalTypes.mapIndexedNotNull { i: Int, cangjieType: CangJieType ->
                    if (cangjieType == typeParameter.defaultType) i else null
                }
                for (originalIndex in typeMapping) {
                    // there can be null in case we already captured type parameter in outer class (in case of inner classes)
                    // see test innerClassTypeAliasConstructor.cj
                    val originalTypeParameter = originalTypeParameters.getOrNull(originalIndex) ?: continue
                    val position = DeclaredUpperBoundConstraintPositionImpl(originalTypeParameter, cangjieCall)
                    for (upperBound in originalTypeParameter.upperBounds) {
                        freshVariable.addSubtypeConstraint(upperBound, position)
                    }
                }
            }
        }
        return toFreshVariables
    }

    private fun getTypePreservingFlexibilityWrtTypeVariable(
        type: CangJieType,
        typeVariable: TypeVariableFromCallableDescriptor
    ): CangJieType {
        fun createFlexibleType() =
            CangJieTypeFactory.flexibleType(
                type.makeNonOption().lowerIfFlexible(),
                type.makeOption().upperIfFlexible()
            )

        return when {
            typeVariable.originalTypeParameter.shouldBeFlexible { it is FlexibleTypeWithEnhancement } ->
                createFlexibleType().wrapEnhancement(type)

            typeVariable.originalTypeParameter.shouldBeFlexible() -> createFlexibleType()
            else -> type
        }
    }

    private fun createKnownParametersFromFreshVariablesSubstitutor(
        freshVariableSubstitutor: FreshVariableNewTypeSubstitutor,
        knownTypeParametersSubstitutor: TypeSubstitutor,
    ): NewTypeSubstitutor {
        if (knownTypeParametersSubstitutor.isEmpty)
            return EmptySubstitutor

        val knownTypeParameterByTypeVariable = mutableMapOf<TypeConstructor, UnwrappedType>().let { map ->
            for (typeVariable in freshVariableSubstitutor.freshVariables) {
                val typeParameterType = typeVariable.originalTypeParameter.defaultType
                val substitutedKnownTypeParameter = knownTypeParametersSubstitutor.substitute(typeParameterType)

                if (substitutedKnownTypeParameter !== typeParameterType)
                    map[typeVariable.defaultType.constructor] = substitutedKnownTypeParameter
            }
            map
        }

        return knownTypeParametersSubstitutor.composeWith(
            NewTypeSubstitutorByConstructorMap(
                knownTypeParameterByTypeVariable
            )
        )
    }

    /**
     *
     */
    fun ResolutionCandidate.getTypeParameters(): List<TypeParameterDescriptor> {



//        如果接收器是DISPATCH_RECEIVER ，并且它是一个静态调用，可能要分析上一层的类型参数
        if (resolvedCall.dispatchReceiverArgument != null && resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue is ClassValueReceiver) {

            return (resolvedCall.dispatchReceiverArgument!!.receiver.receiverValue as ClassValueReceiver).classQualifier.descriptor.declaredTypeParameters + candidateDescriptor.original.typeParameters
        }

        return candidateDescriptor.original.typeParameters

    }

    override fun ResolutionCandidate.process(workIndex: Int) {
        val csBuilder = getSystem().getBuilder()
//        val toFreshVariables =
//            if (descriptor.typeParameters.isEmpty())
//                FreshVariableNewTypeSubstitutor.Empty
//            else
//                createToFreshVariableSubstitutorAndAddInitialConstraints(
//                    descriptor,
//                    resolvedCall.atom,
//                    csBuilder
//                )

        val typeParameters = getTypeParameters()
        val toFreshVariables =
            if (typeParameters.isEmpty())
                FreshVariableNewTypeSubstitutor.Empty
            else
                createToFreshVariableSubstitutorAndAddInitialConstraints(
                    candidateDescriptor,
                    resolvedCall.atom,
                    csBuilder,
                    getTypeParameters()
                )

        val knownTypeParametersSubstitutor = knownTypeParametersResultingSubstitutor?.let {
            createKnownParametersFromFreshVariablesSubstitutor(toFreshVariables, it)
        } ?: EmptySubstitutor

        resolvedCall.freshVariablesSubstitutor = toFreshVariables
        resolvedCall.knownParametersSubstitutor = knownTypeParametersSubstitutor
//        if (descriptor.typeParameters.isEmpty()) {
//            return
//        }
        if (typeParameters.isEmpty()) {
            return
        }

        // bad function -- error on declaration side
        if (csBuilder.hasContradiction) return

        // optimization
        if (resolvedCall.typeArgumentMappingByOriginal == TypeArgumentsToParametersMapper.TypeArgumentsMapping.NoExplicitArguments && knownTypeParametersResultingSubstitutor == null) {
            return
        }

//        val typeParameters = descriptor.original.typeParameters
        for (index in typeParameters.indices) {
            val typeParameter = typeParameters[index]
//            TODO 会不会出现通过索引获取错误的情况，有待验证
            val freshVariable = toFreshVariables.freshVariables[index]

            val knownTypeArgument = knownTypeParametersResultingSubstitutor?.substitute(typeParameter.defaultType)
            if (knownTypeArgument != null) {
                csBuilder.addEqualityConstraint(
                    freshVariable.defaultType,
                    getTypePreservingFlexibilityWrtTypeVariable(knownTypeArgument.unwrap(), freshVariable),
                    KnownTypeParameterConstraintPositionImpl(knownTypeArgument)
                )
                continue
            }

            val typeArgument = resolvedCall.typeArgumentMappingByOriginal.getTypeArgument(typeParameter)

            if (typeArgument is SimpleTypeArgument) {
                csBuilder.addEqualityConstraint(
                    freshVariable.defaultType,
                    getTypePreservingFlexibilityWrtTypeVariable(typeArgument.type, freshVariable),
                    ExplicitTypeParameterConstraintPositionImpl(typeArgument)
                )
            } else {
                assert(typeArgument == TypeArgumentPlaceholder) {
                    "Unexpected typeArgument: $typeArgument, ${typeArgument.javaClass.canonicalName}"
                }
            }
        }
    }


}

/**
 * 无参数的解析部分
 *
 * 这个解析部分处理不带参数的调用,例如变量访问或无参函数调用。
 * 它断言调用确实没有参数,并设置空的参数映射。
 *
 * ## 工作原理
 *
 * 1. 断言括号内没有参数
 * 2. 断言没有外部参数(如尾随 lambda)
 * 3. 设置空的参数映射
 *
 * ## 使用场景
 *
 * ```cangjie
 * class MyClass {
 *     var count: Int64 = 0
 *
 *     func getCount(): Int64 {
 *         return count
 *     }
 * }
 *
 * let obj = MyClass()
 *
 * // 属性访问,没有参数
 * let c1 = obj.count  // NoArguments 处理这种情况
 *
 * // 无参函数调用
 * let c2 = obj.getCount()  // 也由 NoArguments 处理
 * ```
 *
 * @see MapArguments 映射参数的解析部分
 */
internal object NoArguments : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        assert(cangjieCall.argumentsInParenthesis.isEmpty()) {
            "Variable call cannot has arguments: ${cangjieCall.argumentsInParenthesis}. Call: $cangjieCall"
        }
        assert(cangjieCall.externalArgument == null) {
            "Variable call cannot has external argument: ${cangjieCall.externalArgument}. Call: $cangjieCall"
        }
        resolvedCall.argumentMappingByOriginal = emptyMap()
        resolvedCall.argumentToCandidateParameter = emptyMap()
    }
}


/**
 * 映射参数的解析部分
 *
 * 这个解析部分将调用参数映射到函数参数上,处理位置参数、命名参数、
 * 默认参数、可变参数等各种情况。
 *
 * ## 工作原理
 *
 * 1. 调用 `argumentsToParametersMapper.mapArguments` 进行映射
 * 2. 映射器处理:
 *    - 位置参数匹配
 *    - 命名参数匹配
 *    - 可变参数展开
 *    - 默认参数填充
 *    - 参数数量检查
 * 3. 将映射结果存储到 `resolvedCall`
 * 4. 收集并添加映射过程中的诊断
 *
 * ## 使用场景
 *
 * ```cangjie
 * // 函数定义
 * func greet(name: String, age: Int64 = 18, prefix: String = "Hello"): String {
 *     return prefix + " " + name + ", age " + age.toString()
 * }
 *
 * // 位置参数
 * let msg1 = greet("Alice", 25, "Hi")  // 全部位置参数
 *
 * // 部分默认参数
 * let msg2 = greet("Bob", 30)  // prefix 使用默认值
 * let msg3 = greet("Charlie")  // age 和 prefix 使用默认值
 *
 * // 命名参数(如果支持)
 * // let msg4 = greet(name: "David", prefix: "Hey")  // age 使用默认值
 *
 * // 可变参数
 * func sum(vararg numbers: Int64): Int64 {
 *     var total: Int64 = 0
 *     for (n in numbers) {
 *         total += n
 *     }
 *     return total
 * }
 * let result = sum(1, 2, 3, 4, 5)  // 可变参数匹配
 * ```
 *
 * ## 错误处理
 *
 * 映射器会生成以下诊断:
 * - 参数过多
 * - 缺少必需参数
 * - 命名参数不存在
 * - 参数类型不匹配
 *
 * @see ArgumentsToParametersMapper 参数映射器
 * @see NoArguments 处理无参数情况
 * @see CheckArgumentsInParenthesis 检查括号内参数
 * @see CheckExternalArgument 检查外部参数
 */
internal object MapArguments : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        val mapping = callComponents.argumentsToParametersMapper.mapArguments(cangjieCall, candidateDescriptor)
        mapping.diagnostics.forEach(this::addDiagnostic)

        resolvedCall.argumentMappingByOriginal = mapping.parameterToCallArgumentMap

        // TODO 当没有使用()调用时,它是一个函数类型,不检查参数
//        if (/*cangjieCall.psiCangJieCall.psiCall.callElement !is CjCallExpression
//            && cangjieCall.psiCangJieCall.psiCall.callElement !is CjBinaryExpression
//            && cangjieCall.psiCangJieCall.psiCall.callElement !is CjCollectionLiteralExpression*/
//            cangjieCall.psiCangJieCall.psiCall.callElement is CjNameReferenceExpression
//            && !DescriptorUtils.isEnumConstructor(this.descriptor)
//        ) {
//            resolvedCall.argumentMappingByOriginal = emptyMap()
//            return
//        }
//        val mapping = callComponents.argumentsToParametersMapper.mapArguments(cangjieCall, descriptor)
//        mapping.diagnostics.forEach(this::addDiagnostic)
//
//        resolvedCall.argumentMappingByOriginal = mapping.parameterToCallArgumentMap
    }
}

/**
 * 接收者信息
 *
 * 这个数据类封装了参数作为接收者时的相关信息,特别关注安全调用的处理。
 *
 * ## 属性说明
 *
 * - [isReceiver]: 参数是否作为接收者使用(而不是普通参数)
 * - [shouldReportUnsafeCall]: 是否应该报告不安全调用
 * - [reportUnsafeCallAsUnsafeImplicitInvoke]: 将不安全调用报告为不安全的隐式 invoke
 * - [selectorCall]: 可选的选择器调用,用于链式调用
 *
 * ## 使用场景
 *
 * ```cangjie
 * class MyClass {
 *     func method(): Unit { }
 * }
 *
 * // 接收者参数
 * let obj: MyClass? = MyClass()
 * obj?.method()  // obj 是接收者,需要检查安全调用
 *
 * // 普通参数
 * func process(arg: MyClass): Unit { }
 * process(obj)  // obj 是普通参数,不是接收者
 * ```
 *
 * ## 不安全调用报告
 *
 * `shouldReportUnsafeCall` 为 false 的情况:
 * - 已经报告过不安全的隐式 invoke 调用
 * - 避免重复报告相同的错误
 *
 * @property isReceiver 是否为接收者
 * @property shouldReportUnsafeCall 是否应报告不安全调用
 * @property reportUnsafeCallAsUnsafeImplicitInvoke 作为不安全隐式 invoke 报告
 * @property selectorCall 选择器调用(用于链式调用)
 * @see notReceiver 非接收者的默认实例
 */
class ReceiverInfo(
    val isReceiver: Boolean,
    val shouldReportUnsafeCall: Boolean, // 如果已报告不安全的隐式调用,则不应报告
    val reportUnsafeCallAsUnsafeImplicitInvoke: Boolean,
    val selectorCall: CangJieCall? = null,
) {
    init {
        assert(!reportUnsafeCallAsUnsafeImplicitInvoke || shouldReportUnsafeCall) { "Inconsistent receiver info" }
    }

    companion object {
        /**
         * 非接收者的默认实例
         *
         * 用于表示参数不是接收者,需要正常的安全检查。
         */
        val notReceiver = ReceiverInfo(
            isReceiver = false,
            shouldReportUnsafeCall = true,
            reportUnsafeCallAsUnsafeImplicitInvoke = false
        )
    }
}

object CaseEnumArgument : CangJieCallArgument {
    override val isSpread: Boolean = false
    override val argumentName: Name? = null

}

//当具有期望类型时
internal object CheckDesiredEnumType : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {

        if (scopeTower is PSICallResolver.ASTScopeTower) {
            val expectedType = (scopeTower as PSICallResolver.ASTScopeTower).context.expectedType

            val type = descriptor.returnType

            if (!noExpectedType(expectedType) && type?.let {
                    CangJieTypeChecker.DEFAULT.equalsIgnoringGenerics(
                        expectedType,
                        it
                    )
                } != true) {
                addDiagnostic(EmptyDiagnostic)
            }
        }


    }

}

internal object CheckCaseEnumArgumentSize : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {

        val dASize = candidateDescriptor.valueParameters.size
        val argSize = cangjieCall.argumentsInParenthesis.size
        if (argSize > dASize) {
//          参数过多

            addDiagnostic(TooManyArguments(CaseEnumArgument, candidateDescriptor))
        } else if (argSize < dASize) {
//          确实参数
//         未传递参数
            val args = candidateDescriptor.valueParameters.dropLast(dASize - argSize)
            args.forEach {
                addDiagnostic(NoValueForParameter(it, candidateDescriptor))

            }


        }


    }

    override fun ResolutionCandidate.workCount() = cangjieCall.argumentsInParenthesis.size
}

internal object CheckArgumentsInParenthesis : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        val argument = cangjieCall.argumentsInParenthesis[workIndex]
        resolveCangJieArgument(
            argument,
            resolvedCall.argumentToCandidateParameter[argument],
            ReceiverInfo.notReceiver
        )
    }

    override fun ResolutionCandidate.workCount() = cangjieCall.argumentsInParenthesis.size
}

private fun ResolutionCandidate.resolveCangJieArgument(
    argument: CangJieCallArgument,
    candidateParameter: ParameterDescriptor?,
    receiverInfo: ReceiverInfo
) {
    val csBuilder = getSystem().getBuilder()
    val candidateExpectedType =
        candidateParameter?.let { argument.getExpectedType(it, callComponents.languageVersionSettings) }

    val isReceiver = receiverInfo.isReceiver
    val conversionDataBeforeSubtyping =
        if (isReceiver || candidateParameter == null || candidateExpectedType == null) {
            null
        } else {
            TypeConversions.performCompositeConversionBeforeSubtyping(
                this, argument, candidateParameter, candidateExpectedType
            )
        }

    val convertedExpectedType = conversionDataBeforeSubtyping?.convertedType
    val unsubstitutedExpectedType = conversionDataBeforeSubtyping?.convertedType ?: candidateExpectedType
    val expectedType = unsubstitutedExpectedType?.let { prepareExpectedType(it) }

    val convertedArgument =
        if (expectedType != null && !isReceiver && shouldRunConversionForConstants(expectedType)) {
            val convertedConstant = resolutionCallbacks.convertSignedConstantToUnsigned(argument)
            if (convertedConstant != null) {
                resolvedCall.registerArgumentWithConstantConversion(argument, convertedConstant)
            }

            convertedConstant
        } else null


    val inferenceSession = resolutionCallbacks.inferenceSession
    if (candidateExpectedType == null || // Nothing to convert
        convertedExpectedType != null || // Type is already converted
        isReceiver || // Receivers don't participate in conversions
        conversionDataBeforeSubtyping?.wasConversion == true || // We tried to convert type but failed
        conversionDataBeforeSubtyping?.conversionDefinitelyNotNeeded == true ||
        csBuilder.hasContradiction
    ) {
        val resolvedAtom = resolveCjPrimitive(
            csBuilder,
            argument,
            expectedType,
            this,
            receiverInfo,
            convertedArgument?.unknownIntegerType?.unwrap(),
            inferenceSession,
            selectorCall = receiverInfo.selectorCall
        )

        addResolvedCjPrimitive(resolvedAtom)
    } else {
        var convertedTypeAfterSubtyping: UnwrappedType? = null
        csBuilder.runTransaction {
            val resolvedAtom = resolveCjPrimitive(
                csBuilder,
                argument,
                expectedType,
                this@resolveCangJieArgument,
                receiverInfo,
                convertedArgument?.unknownIntegerType?.unwrap(),
                inferenceSession
            )

            if (!hasContradiction) {
                addResolvedCjPrimitive(resolvedAtom)
                return@runTransaction true
            }

            convertedTypeAfterSubtyping =
                TypeConversions.performCompositeConversionAfterSubtyping(
                    this@resolveCangJieArgument,
                    argument,
                    candidateParameter,
                    candidateExpectedType
                )?.let { prepareExpectedType(it) }

            if (convertedTypeAfterSubtyping == null) {
                addResolvedCjPrimitive(resolvedAtom)
                return@runTransaction true
            }

            false
        }

        if (convertedTypeAfterSubtyping != null) {
            val resolvedAtom = resolveCjPrimitive(
                csBuilder,
                argument,
                convertedTypeAfterSubtyping,
                this@resolveCangJieArgument,
                receiverInfo,
                convertedArgument?.unknownIntegerType?.unwrap(),
                inferenceSession
            )
            addResolvedCjPrimitive(resolvedAtom)
        }

    }
}

private fun ResolutionCandidate.shouldRunConversionForConstants(expectedType: UnwrappedType): Boolean {
    if (UnsignedTypes.isUnsignedType(expectedType)) return true
    val csBuilder = getSystem().getBuilder()
    if (csBuilder.isTypeVariable(expectedType)) {
        val variableWithConstraints =
            csBuilder.currentStorage().notFixedTypeVariables[expectedType.constructor] ?: return false
        return variableWithConstraints.constraints.any {
            it.kind == ConstraintKind.EQUALITY &&
                    it.position.from is ExplicitTypeParameterConstraintPositionImpl &&
                    UnsignedTypes.isUnsignedType(it.type as UnwrappedType)

        }
    }

    return false
}

/**
 * 根据预期类型准备实际类型
 * 此函数通过应用当前解析上下文中的变量替换和参数替换，来调整预期类型
 *
 * @param expectedType 预期的类型，即函数调用或表达式期望返回的类型
 * @return 调整后的类型，即经过变量和参数替换后，预期类型在当前上下文中的实际表示
 */
private fun ResolutionCandidate.prepareExpectedType(expectedType: UnwrappedType): UnwrappedType {
    // 使用当前解析调用的变量替换器，安全地替换预期类型中的泛型变量
    val resultType = resolvedCall.freshVariablesSubstitutor.safeSubstitute(expectedType)
    // 使用当前解析调用的已知参数替换器，安全地替换resultType中的参数类型
    return resolvedCall.knownParametersSubstitutor.safeSubstitute(resultType)
}


internal object ErrorDescriptorResolutionPart : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        assert(ErrorUtils.isError(candidateDescriptor)) {
            "Should be error descriptor: $candidateDescriptor"
        }
        resolvedCall.typeArgumentMappingByOriginal =
            TypeArgumentsToParametersMapper.TypeArgumentsMapping.NoExplicitArguments
        resolvedCall.argumentMappingByOriginal = emptyMap()
        resolvedCall.freshVariablesSubstitutor = FreshVariableNewTypeSubstitutor.Empty
        resolvedCall.knownParametersSubstitutor = EmptySubstitutor
        resolvedCall.argumentToCandidateParameter = emptyMap()

//        (cangjieCall.explicitReceiver as? SimpleCangJieCallArgument)?.let {
//            resolveCangJieArgument(it, null, ReceiverInfo.notReceiver)
//        }
//        for (argument in cangjieCall.argumentsInParenthesis) {
//            resolveCangJieArgument(argument, null, ReceiverInfo.notReceiver)
//        }
//
//        cangjieCall.externalArgument?.let {
//            resolveCangJieArgument(it, null, ReceiverInfo.notReceiver)
//        }
    }
}

internal object ArgumentsToCandidateParameterDescriptor : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        val map = hashMapOf<CangJieCallArgument, ValueParameterDescriptor>()
        for ((originalValueParameter, resolvedCallArgument) in resolvedCall.argumentMappingByOriginal) {
            val valueParameter =
                candidateDescriptor.valueParameters.getOrNull(originalValueParameter.index) ?: continue
            for (argument in resolvedCallArgument.arguments) {
                map[argument] = valueParameter
            }
        }
        resolvedCall.argumentToCandidateParameter = map.compactIfPossible()
    }
}

internal object CheckExternalArgument : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        val argument = cangjieCall.externalArgument ?: return

        resolveCangJieArgument(
            argument,
            resolvedCall.argumentToCandidateParameter[argument],
            ReceiverInfo.notReceiver
        )
    }
}



// 提取的方法
fun DeclarationDescriptor.getDescriptorKind(): DescriptorKind {
    return when (this) {
        is PropertyDescriptor -> DescriptorKind.PROPERTY
        is VariableDescriptor -> DescriptorKind.VARIABLE
        is FunctionDescriptor -> DescriptorKind.FUNCTION
        else -> DescriptorKind.UNKNOWN
    }
}

internal object CheckIncompatibleTypeVariableUpperBounds : ResolutionPart() {
    /*
     * Check if the candidate was already discriminated by `CompatibilityOfTypeVariableAsIntersectionTypePart` resolution part
     * If it's true we shouldn't mark the candidate with warning, but should mark with error, to repeat the existing proper behaviour
     */
    private fun ResolutionCandidate.wasPreviouslyDiscriminated(upperTypes: List<CangJieTypeMarker>): Boolean {
        @Suppress("UNCHECKED_CAST")
        return callComponents.statelessCallbacks.isOldIntersectionIsEmpty(upperTypes as List<CangJieType>)
    }

    override fun ResolutionCandidate.process(workIndex: Int) =
        with(getSystem().asConstraintSystemCompleterContext()) {
            val constraintSystem = getSystem()
            for (variableWithConstraints in constraintSystem.getBuilder()
                .currentStorage().notFixedTypeVariables.values) {
                val upperTypes = variableWithConstraints.constraints.extractUpperTypesToCheckIntersectionEmptiness()

                when {
                    // TODO: consider reporting errors on bounded type variables by incompatible types but with other lower constraints
                    upperTypes.size <= 1 || variableWithConstraints.constraints.any { it.kind.isLower() } ->
                        continue

                    wasPreviouslyDiscriminated(upperTypes) -> {
                        markCandidateForCompatibilityResolve(needToReportWarning = false)
                        continue
                    }

                    (variableWithConstraints.typeVariable as? TypeVariableFromCallableDescriptor)?.originalTypeParameter?.let { parameter ->
                        resolvedCall.typeArgumentMappingByOriginal.getTypeArgument(parameter)
                    } is SimpleTypeArgument -> continue

                    else -> {
                        val emptyIntersectionTypeInfo =
                            constraintSystem.getEmptyIntersectionTypeKind(upperTypes) ?: continue
//                    val isInferredEmptyIntersectionForbidden = callComponents.languageVersionSettings.supportsFeature(
//                        LanguageFeature.ForbidInferringTypeVariablesIntoEmptyIntersection
//                    )
                        val errorFactory = ::InferredEmptyIntersectionError
//                        if (isInferredEmptyIntersectionForbidden) ::InferredEmptyIntersectionError else ::InferredEmptyIntersectionWarning

                        addError(
                            errorFactory(
                                upperTypes,
                                emptyIntersectionTypeInfo.casingTypes.toList(),
                                variableWithConstraints.typeVariable,
                                emptyIntersectionTypeInfo.kind
                            )
                        )
                    }
                }
            }
        }
}

internal object CheckCallableReference : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        if (this !is CallableReferenceResolutionCandidate) {
            error("`CheckCallableReferences` resolution part is applicable only to callable reference calls")
        }

        val constraintSystem = getSystem().takeIf { !it.hasContradiction } ?: return

        addConstraints(constraintSystem.getBuilder(), resolvedCall.freshVariablesSubstitutor, cangjieCall)
    }
}
