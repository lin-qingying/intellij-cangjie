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

package org.cangnova.cangjie.resolve.calls.tower

import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.EnumConstructorFinder
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.scopes.receivers.DetailedReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.QualifierReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo

/**
 * 简单作用域塔处理器的抽象基类
 *
 * 负责使用 [candidateFactory] 将候选者转换为特定类型的候选者对象。
 * 这是所有简单处理器(仅处理单层作用域的处理器)的基类。
 *
 * @param C 候选者类型
 * @param candidateFactory 用于创建候选者的工厂
 */
internal abstract class AbstractSimpleScopeTowerProcessor<C : Candidate>(
    val candidateFactory: CandidateFactory<C>
) : SimpleScopeTowerProcessor<C> {
    /**
     * 使用候选者工厂创建候选者集合
     *
     * @param collector 带有绑定分发接收者的候选者集合
     * @param kind 显式接收者类型(分发接收者/扩展接收者/无显式接收者)
     * @return 转换后的候选者集合
     */
    fun createCandidates(
        collector: Collection<CandidateWithBoundDispatchReceiver>,
        kind: ExplicitReceiverKind,
        receiver: ReceiverValueWithSmartCastInfo?

    ): Collection<C> {
        return collector.mapNotNull { candidate ->
            // 如果提供了接收者，但候选者不需要分发接收者（例如顶层函数），则跳过
            // 这避免了 obj.topLevelFunc() 这种情况错误地解析到顶层函数
            if (receiver != null && candidate.dispatchReceiver == null) {
                return@mapNotNull null
            }
            candidateFactory.createCandidate(candidate, kind)
        }
    }
}

/**
 * 候选者收集器类型别名
 *
 * 这是一个函数类型,用于在作用域塔层级中收集候选者。
 * 它接收一个可选的扩展接收者,返回带有绑定分发接收者的候选者集合。
 */
private typealias CandidatesCollector =
        ScopeTowerLevel.(extensionReceiver: ReceiverValueWithSmartCastInfo?) -> Collection<CandidateWithBoundDispatchReceiver>

/**
 * 显式接收者作用域塔处理器
 *
 * 处理带有显式接收者的调用,例如: `receiver.foo()` 或 `receiver.property`
 *
 * 工作流程:
 * 1. 当塔数据为空时,在显式接收者的成员作用域中查找
 * 2. 当塔数据为作用域层级时,将显式接收者作为扩展接收者在该层级中查找
 *
 * 示例:
 * ```kotlin
 * class A {
 *     fun foo() {}
 * }
 * val a: A = ...
 * a.foo()  // 这里 'a' 是显式接收者(分发接收者)
 * ```
 *
 * @param C 候选者类型
 * @param scopeTower 隐式作用域塔,用于获取作用域层级
 * @param context 候选者工厂
 * @param explicitReceiver 显式接收者(例如 `a.foo()` 中的 `a`)
 * @param collectCandidates 候选者收集函数
 */
internal class ExplicitReceiverScopeTowerProcessor<C : Candidate>(
    val scopeTower: ImplicitScopeTower,
    context: CandidateFactory<C>,
    val explicitReceiver: ReceiverValueWithSmartCastInfo,
    val collectCandidates: CandidatesCollector
) : AbstractSimpleScopeTowerProcessor<C>(context) {
    /**
     * 处理简单的作用域塔数据
     *
     * @param data 塔数据(空/作用域层级/其他)
     * @return 候选者集合
     */
    override fun simpleProcess(data: TowerData): Collection<C> {
        return when (data) {
            // 塔数据为空时,在显式接收者的成员作用域中查找
            // 例如: a.foo() 中在 A 类的成员作用域中查找 foo 函数
            TowerData.Empty -> createCandidates(
                MemberScopeTowerLevel(scopeTower, explicitReceiver).collectCandidates(null),
                ExplicitReceiverKind.DISPATCH_RECEIVER,
                null
            )

            // 塔数据为作用域层级时,将显式接收者作为扩展接收者在该层级中查找
            // 例如: a.bar() 其中 bar 是定义在外部作用域的扩展函数: fun A.bar()
            is TowerData.TowerLevel -> createCandidates(
                data.level.collectCandidates(explicitReceiver),
                ExplicitReceiverKind.DISPATCH_RECEIVER,
                explicitReceiver
            )

            // 其他情况不处理
            else -> emptyList()
        }
    }

    /**
     * 记录在跳过的塔数据中进行的查找
     *
     * 用于性能跟踪和调试,记录哪些作用域被查找过但没有找到候选者。
     *
     * @param skippedData 跳过的塔数据集合
     * @param name 查找的名称
     */
    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {
        for (data in skippedData) {
            if (data is TowerData.TowerLevel) {
                data.level.recordLookup(name)
            }
        }
    }
}

/**
 * 限定符作用域塔处理器
 *
 * 处理使用限定符(包名、类名等)的调用,例如: `kotlin.collections.List` 或 `Companion.foo()`
 *
 * 这种处理器只在空塔数据时工作,因为限定符访问不涉及隐式接收者和作用域层级。
 * 它直接在限定符对应的作用域中查找成员。
 *
 * 示例:
 * ```kotlin
 * object MyObject {
 *     fun foo() {}
 * }
 * MyObject.foo()  // 这里 MyObject 是限定符
 * ```
 *
 * @param C 候选者类型
 * @param scopeTower 隐式作用域塔
 * @param context 候选者工厂
 * @param qualifier 限定符接收者(例如包名、对象名、伴生对象等)
 * @param collectCandidates 候选者收集函数
 */
private class QualifierScopeTowerProcessor<C : Candidate>(
    val scopeTower: ImplicitScopeTower,
    context: CandidateFactory<C>,
    val qualifier: QualifierReceiver,
    val collectCandidates: CandidatesCollector
) : AbstractSimpleScopeTowerProcessor<C>(context) {
    /**
     * 处理简单的作用域塔数据
     *
     * 限定符处理器只在空塔数据时工作,直接在限定符作用域中查找。
     *
     * @param data 塔数据
     * @return 候选者集合,如果不是空塔数据则返回空列表
     */
    override fun simpleProcess(data: TowerData): Collection<C> {
        // 限定符访问只在空塔数据时处理
        if (data != TowerData.Empty) return emptyList()

        // 在限定符对应的作用域中查找成员
        return createCandidates(
            QualifierScopeTowerLevel(scopeTower, qualifier).collectCandidates(null),
            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            null
        )
    }

    /**
     * 记录查找
     *
     * 限定符处理器只处理空塔数据,该数据不应被忽略,所以这里不需要记录任何查找。
     *
     * @param skippedData 跳过的塔数据集合
     * @param name 查找的名称
     */
    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {}
}

/**
 * 无显式接收者作用域塔处理器
 *
 * 处理没有显式接收者的调用,例如: `foo()` 或 `property`
 *
 * 工作流程:
 * 1. 在各个作用域层级中查找不带接收者的成员(局部变量、顶层函数等)
 * 2. 在各个作用域层级中查找扩展函数,使用隐式接收者
 *
 * 示例:
 * ```kotlin
 * class Outer {
 *     fun foo() {}
 *     fun bar() {
 *         foo()  // 无显式接收者,会在当前类的作用域中查找,this 是隐式接收者
 *     }
 * }
 * ```
 *
 * @param C 候选者类型
 * @param context 候选者工厂
 * @param collectCandidates 候选者收集函数
 */
private class NoExplicitReceiverScopeTowerProcessor<C : Candidate>(
    context: CandidateFactory<C>,
    val collectCandidates: CandidatesCollector
) : AbstractSimpleScopeTowerProcessor<C>(context) {
    /**
     * 处理简单的作用域塔数据
     *
     * @param data 塔数据
     * @return 候选者集合
     */
    override fun simpleProcess(data: TowerData): Collection<C> = when (data) {
        // 仅有作用域层级,无隐式接收者
        // 查找不需要接收者的成员(例如局部变量、顶层函数)
        is TowerData.TowerLevel -> createCandidates(
            data.level.collectCandidates(null),
            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,null
        )

        // 既有作用域层级又有隐式接收者
        // 查找可以使用隐式接收者的成员(例如扩展函数)
        is TowerData.BothTowerLevelAndImplicitReceiver -> createCandidates(
            data.level.collectCandidates(data.implicitReceiver),
            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,    data.implicitReceiver
        )

        // 其他情况不处理
        else -> emptyList()
    }

    /**
     * 记录在跳过的塔数据中进行的查找
     *
     * @param skippedData 跳过的塔数据集合
     * @param name 查找的名称
     */
    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {
        for (data in skippedData) {
            when (data) {
                is TowerData.TowerLevel -> data.level.recordLookup(name)
                is TowerData.BothTowerLevelAndImplicitReceiver -> data.level.recordLookup(name)
                is TowerData.ForLookupForNoExplicitReceiver -> data.level.recordLookup(name)
                else -> {}
            }
        }
    }
}

/**
 * 创建不考虑类值接收者的简单处理器
 *
 * 这是一个工厂函数,根据接收者类型创建相应的处理器:
 * - 如果有显式接收者值,创建 [ExplicitReceiverScopeTowerProcessor]
 * - 如果是限定符接收者,创建 [QualifierScopeTowerProcessor]
 * - 如果没有接收者,创建 [NoExplicitReceiverScopeTowerProcessor]
 *
 * 注意：在仓颉语言中，类不能作为值使用，只能作为类型限定符。
 * 因此不存在"类值接收者"的概念，所有通过类名的访问都通过静态作用域处理。
 *
 * @param C 候选者类型
 * @param scopeTower 隐式作用域塔
 * @param context 候选者工厂
 * @param explicitReceiver 显式接收者(可能为 null、ReceiverValue 或 Qualifier)
 * @param collectCandidates 候选者收集函数
 * @return 简单作用域塔处理器
 */
private fun <C : Candidate> createSimpleProcessor(
    scopeTower: ImplicitScopeTower,
    context: CandidateFactory<C>,
    explicitReceiver: DetailedReceiver?,
    collectCandidates: CandidatesCollector
): SimpleScopeTowerProcessor<C> =
    when (explicitReceiver) {
        // 显式接收者是一个值(带智能转换信息)
        is ReceiverValueWithSmartCastInfo -> ExplicitReceiverScopeTowerProcessor(
            scopeTower,
            context,
            explicitReceiver,
            collectCandidates
        )

        // 显式接收者是限定符(包、类名等)
        // 仓颉语言中，类只能作为类型限定符，不能作为值
        // 通过 QualifierScopeTowerProcessor 在静态作用域中查找
        is QualifierReceiver -> QualifierScopeTowerProcessor(scopeTower, context, explicitReceiver, collectCandidates)

        // 没有显式接收者
        else -> {
            // 断言确保 explicitReceiver 确实为 null
            assert(explicitReceiver == null) {
                "Illegal explicit receiver: $explicitReceiver(${explicitReceiver!!::class.java.simpleName})"
            }
            NoExplicitReceiverScopeTowerProcessor(context, collectCandidates)
        }
    }

/**
 * 创建可调用引用处理器
 *
 * 用于解析可调用引用表达式(例如 `::foo`)。
 * 目前只处理函数引用,未来可能扩展支持属性引用。
 *
 * @param C 候选者类型
 * @param scopeTower 隐式作用域塔
 * @param name 引用的名称
 * @param context 候选者工厂
 * @param explicitReceiver 显式接收者
 * @return 简单作用域塔处理器
 */
fun <C : Candidate> createCallableReferenceProcessor(
    scopeTower: ImplicitScopeTower,
    name: Name, context: CandidateFactory<C>,
    explicitReceiver: DetailedReceiver?
): SimpleScopeTowerProcessor<C> {
    // 注释掉的代码:可能未来会支持函数引用的简化版本
    // return createSimpleFunctionProcessor(scopeTower, name, context, explicitReceiver)

    // 注释掉的代码:可能未来会支持变量/属性引用
    // val variable = createSimpleProcessorWithoutClassValueReceiver(scopeTower, context, explicitReceiver) { getVariables(name, it) }

    // 创建函数引用处理器
    val function =
        createSimpleProcessor(scopeTower, context, explicitReceiver) { getFunctions(name) }

    // 返回组合处理器(目前只包含函数处理器,未来可能添加变量处理器)
    return SamePriorityCompositeScopeTowerProcessor(/*variable,*/ function)
}

/**
 * 创建简单函数处理器
 *
 * 用于解析函数调用表达式,查找匹配的函数候选者。
 *
 * @param C 候选者类型
 * @param scopeTower 隐式作用域塔
 * @param name 函数名称
 * @param context 候选者工厂
 * @param explicitReceiver 显式接收者
 * @param isEnumConstructor 是否是枚举构造器
 * @return 作用域塔处理器
 */
fun <C : Candidate> createSimpleFunctionProcessor(
    scopeTower: ImplicitScopeTower,
    name: Name,
    context: CandidateFactory<C>,
    explicitReceiver: DetailedReceiver?,
    isEnumConstructor: Boolean = false
) = createSimpleProcessor(scopeTower, context, explicitReceiver) {
    getFunctions(
        name,
        isEnumConstructor
    )
}

/**
 * 创建带接收者值或空值的处理器
 *
 * 用于处理可能有接收者的情况。
 * 如果显式接收者是限定符(QualifierReceiver),则返回空结果处理器,
 * 因为在仓颉语言中类不能作为值使用;
 * 否则使用接收者值创建处理器。
 *
 * @param C 候选者类型
 * @param explicitReceiver 显式接收者
 * @param create 处理器创建函数
 * @return 作用域塔处理器
 */
fun <C : Candidate> createProcessorWithReceiverValueOrEmpty(
    explicitReceiver: DetailedReceiver?,
    create: (ReceiverValueWithSmartCastInfo?) -> ScopeTowerProcessor<C>
): ScopeTowerProcessor<C> {
    return if (explicitReceiver is QualifierReceiver) {
        // 仓颉语言中,类不能作为值使用,只能作为类型限定符
        // 因此 QualifierReceiver 不提供类值接收者
        KnownResultProcessor(listOf())
    } else {
        // 使用接收者值(可能为 null)创建处理器
        create(explicitReceiver as ReceiverValueWithSmartCastInfo?)
    }
}

/**
 * 已知结果处理器
 *
 * 这是一个特殊的处理器,它不执行实际的查找,而是直接返回预先准备好的结果。
 * 通常用于表示查找失败或特殊情况。
 *
 * @param C 候选者类型
 * @param result 预先准备好的候选者集合
 */
class KnownResultProcessor<out C>(
    val result: Collection<C>
) : ScopeTowerProcessor<C> {
    /**
     * 处理塔数据
     *
     * 只在空塔数据时返回结果(如果结果非空)。
     *
     * @param data 塔数据
     * @return 候选者集合列表(如果是空塔数据且结果非空,返回包含结果的列表;否则返回空列表)
     */
    override fun process(data: TowerData) =
        if (data == TowerData.Empty) listOfNotNull(result.takeIf { it.isNotEmpty() }) else emptyList()

    /**
     * 记录查找
     *
     * 已知结果处理器不执行实际查找,所以不需要记录。
     *
     * @param skippedData 跳过的塔数据集合
     * @param name 查找的名称
     */
    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {}
}

/**
 * 枚举构造器查找处理器
 *
 * 使用 EnumConstructorFinder 的混合策略快速查找枚举构造器。
 * 这个处理器专门用于无显式接收者的枚举构造器调用（如 `Red` 而不是 `Color.Red`）。
 *
 * @param C 候选者类型
 * @param scopeTower 隐式作用域塔
 * @param name 构造器名称
 * @param context 候选者工厂
 */
private class EnumConstructorFinderProcessor<C : Candidate>(
    private val scopeTower: ImplicitScopeTower,
    private val name: Name,
    private val context: CandidateFactory<C>
) : AbstractSimpleScopeTowerProcessor<C>(context) {

    override fun simpleProcess(data: TowerData): Collection<C> {
        // 仅处理顶层作用域查找（TowerLevel），不处理其他类型
        if (data !is TowerData.TowerLevel) {
            return emptyList()
        }

        // 使用 EnumConstructorFinder 查找构造器
        val lexicalScope = scopeTower.lexicalScope

        // 获取 project：通过 ownerDescriptor 向上查找 ModuleDescriptor

        // 查找枚举构造器（arity = null 表示不限制参数数量）
        val constructors = EnumConstructorFinder.findConstructor(
            name = name,
            arity = null,  // 在这个阶段不限制参数数量，后续由重载解析处理
            scope = lexicalScope,

            )

        if (constructors.isEmpty()) {
            return emptyList()
        }

        // 将找到的枚举构造器转换为 CandidateWithBoundDispatchReceiver
        val candidatesWithReceiver = constructors.map { constructor ->
            CandidateWithBoundDispatchReceiver(
                dispatchReceiver = null,  // 无显式接收者
                descriptor = constructor,
                diagnostics = emptyList()
            )
        }

        // 使用基类的 createCandidates 方法转换为最终候选者
        return createCandidates(candidatesWithReceiver, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            null)
    }

    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {
        // EnumConstructorFinder 已经处理了查找，不需要额外记录
    }
}

/**
 * 创建枚举构造器处理器
 *
 * 用于解析枚举类型的构造器调用。枚举构造器是枚举类型的静态成员,通过类型名访问。
 *
 * 处理两种枚举构造器:
 * 1. 简单构造器(无关联值): `Color.Red` - 作为变量访问
 * 2. 函数构造器(有关联值): `Result.Success(value)` - 作为函数调用
 *
 * 工作流程:
 * 1. 优先查找简单构造器（无参数，作为属性访问）
 * 2. 再查找函数构造器（有参数，作为函数调用）
 * 3. 不使用 invoke 约定，因为枚举构造器直接返回枚举实例
 *
 * 示例:
 * ```cangjie
 * enum Color {
 *     Red,      // 简单构造器
 *     Green,
 *     Blue
 * }
 *
 * enum Result<T> {
 *     Success(T),  // 函数构造器
 *     Error(String)
 * }
 *
 * val c1 = Color.Red           // 访问简单构造器（属性）
 * val r1 = Result.Success(42)  // 调用函数构造器（函数）
 * ```
 *createEnumConstructorProcessor只负责找到名称为某某的枚举构造器，类型推导，是否正确由类型推导系统和约束系统进行
 *
 * @param C 候选者类型
 * @param scopeTower 隐式作用域塔
 * @param name 构造器名称
 * @param simpleContext 简单候选者工厂
 * @param factoryProviderForInvoke invoke 调用的候选者工厂提供者（未使用）
 * @param explicitReceiver 显式接收者(通常是枚举类型本身)
 * @return 优先级组合作用域塔处理器
 */
fun <C : Candidate> createEnumConstructorProcessor(
    scopeTower: ImplicitScopeTower,
    name: Name,
    simpleContext: CandidateFactory<C>,
    factoryProviderForInvoke: CandidateFactoryProviderForInvoke<C>,
    explicitReceiver: DetailedReceiver?
): PrioritizedCompositeScopeTowerProcessor<C> {

    // 如果没有显式接收者，优先使用 EnumConstructorFinder 进行快速查找
    // 例如：直接使用 Red 而不是 Color.Red
    val enumFinderProcessor = if (explicitReceiver == null) {
        EnumConstructorFinderProcessor(scopeTower, name, simpleContext)
    } else {
        null
    }

    // 处理器 1: 简单构造器（无关联值）
    // 优先查找作为变量/属性的枚举构造器
    // 例如：Color.Red
    val simpleConstructorProcessor = createSimpleProcessor(
        scopeTower,
        simpleContext,
        explicitReceiver
    ) { getVariables(name, it, true) }

    // 处理器 2: 函数构造器（有关联值）
    // 查找作为函数的枚举构造器
    // 例如：Result.Success(42)
    val functionConstructorProcessor = createSimpleFunctionProcessor(
        scopeTower, name, simpleContext, explicitReceiver,true
    )

    // 返回优先级组合处理器
    // 优先级：
    // 1. EnumConstructorFinder（无显式接收者时）- 最快
    // 2. 简单构造器（变量）
    // 3. 函数构造器（函数）
    return if (enumFinderProcessor != null) {
        PrioritizedCompositeScopeTowerProcessor(
            enumFinderProcessor,
            simpleConstructorProcessor,
            functionConstructorProcessor
        )
    } else {
        PrioritizedCompositeScopeTowerProcessor(
            simpleConstructorProcessor,
            functionConstructorProcessor
        )
    }
}

/**
 * 创建函数处理器
 *
 * 用于解析函数调用表达式。这个处理器支持两种调用方式:
 * 1. 简单函数调用: `a.foo()` - 直接调用函数
 * 2. invoke 约定: `a.foo()` - 其中 foo 是属性,然后调用其 invoke() 方法
 *
 * 优先级策略:
 * - 简单函数调用优先级高于 invoke 约定
 * - 如果找到简单函数,不会考虑 invoke 约定
 *
 * @param C 候选者类型
 * @param scopeTower 隐式作用域塔
 * @param name 函数/属性名称
 * @param simpleContext 简单候选者工厂
 * @param factoryProviderForInvoke invoke 调用的候选者工厂提供者
 * @param explicitReceiver 显式接收者
 * @return 优先级组合作用域塔处理器
 */
fun <C : Candidate> createFunctionProcessor(
    scopeTower: ImplicitScopeTower,
    name: Name,
    simpleContext: CandidateFactory<C>,
    factoryProviderForInvoke: CandidateFactoryProviderForInvoke<C>,
    explicitReceiver: DetailedReceiver?
): PrioritizedCompositeScopeTowerProcessor<C> {

    // 处理器 1: 简单函数调用 - a.foo()
    val simpleFunction = createSimpleFunctionProcessor(scopeTower, name, simpleContext, explicitReceiver)

    // 处理器 2: invoke 约定 - a.foo() 其中 foo 是属性,调用 foo.invoke()
    val invokeProcessor = InvokeTowerProcessor(scopeTower, name, factoryProviderForInvoke, explicitReceiver)

    // 注释掉的代码: 扩展函数的 invoke 约定
    // 处理器 3: a.foo() - 其中 foo 是扩展属性,接收者是 a,然后调用 foo.invoke()
    // val invokeExtensionProcessor = createProcessorWithReceiverValueOrEmpty(explicitReceiver) {
    //     InvokeExtensionTowerProcessor(scopeTower, name, factoryProviderForInvoke, it)
    // }

    // 返回优先级组合处理器:简单函数调用优先级最高,invoke 约定次之
    return PrioritizedCompositeScopeTowerProcessor(simpleFunction, invokeProcessor/*invokeExtensionProcessor*/)
}

/**
 * 创建属性处理器
 *
 * 用于解析属性访问表达式,查找匹配的变量(属性、字段等)候选者。
 *
 * @param C 候选者类型
 * @param scopeTower 隐式作用域塔
 * @param name 属性名称
 * @param context 候选者工厂
 * @param explicitReceiver 显式接收者
 * @return 作用域塔处理器
 */
fun <C : Candidate> createPropertyProcessor(
    scopeTower: ImplicitScopeTower, name: Name,
    context: CandidateFactory<C>, explicitReceiver: DetailedReceiver?
) = createSimpleProcessor(scopeTower, context, explicitReceiver) { getVariables(name, it) }


/**
 * 创建变量处理器
 *
 * 用于解析变量访问表达式,查找匹配的变量候选者。
 * 功能上与 [createPropertyProcessor] 相同,但语义上更强调变量的概念。
 *
 * @param C 候选者类型
 * @param scopeTower 隐式作用域塔
 * @param name 变量名称
 * @param context 候选者工厂
 * @param explicitReceiver 显式接收者
 * @return 作用域塔处理器
 */
fun <C : Candidate> createVariableProcessor(
    scopeTower: ImplicitScopeTower, name: Name,
    context: CandidateFactory<C>, explicitReceiver: DetailedReceiver?
) = createSimpleProcessor(scopeTower, context, explicitReceiver) { getVariables(name, it) }


/**
 * 优先级组合作用域塔处理器
 *
 * 组合多个处理器,按照优先级顺序处理。
 * 先尝试第一个处理器,如果没有找到候选者,再尝试下一个处理器。
 *
 * 使用场景:
 * - 函数调用时,先查找简单函数,再查找 invoke 约定
 * - 限定符访问时,先查找限定符成员,再查找类值接收者成员
 *
 * 示例:
 * ```kotlin
 * // 1. 先查找函数 foo
 * // 2. 如果没找到,再查找属性 foo 并调用其 invoke() 方法
 * a.foo()
 * ```
 *
 * @param C 候选者类型
 * @param processors 处理器数组,按优先级从高到低排列
 */
class PrioritizedCompositeScopeTowerProcessor<out C>(
    vararg val processors: ScopeTowerProcessor<C>
) : ScopeTowerProcessor<C> {
    /**
     * 处理塔数据
     *
     * 依次调用所有处理器的 process 方法,合并结果。
     * 注意:这里会合并所有处理器的结果,但作用域塔会按照返回的顺序决定优先级。
     *
     * @param data 塔数据
     * @return 候选者集合列表(来自所有处理器)
     */
    override fun process(data: TowerData): List<Collection<C>> = processors.flatMap { it.process(data) }

    /**
     * 记录查找
     *
     * 通知所有处理器记录查找。
     *
     * @param skippedData 跳过的塔数据集合
     * @param name 查找的名称
     */
    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {
        processors.forEach { it.recordLookups(skippedData, name) }
    }

}


/**
 * 简单作用域塔处理器接口
 *
 * 简化版的 [ScopeTowerProcessor],只需要实现 [simpleProcess] 方法。
 * [simpleProcess] 返回简单的候选者集合,而不是候选者集合的列表。
 *
 * 自动提供了 [process] 方法的默认实现:
 * - 如果 [simpleProcess] 返回非空结果,将其包装为列表返回
 * - 如果 [simpleProcess] 返回空结果,返回空列表
 *
 * @param C 候选者类型
 */
interface SimpleScopeTowerProcessor<out C> : ScopeTowerProcessor<C> {
    /**
     * 处理简单的作用域塔数据
     *
     * @param data 塔数据
     * @return 候选者集合(如果没有找到候选者,返回空集合)
     */
    fun simpleProcess(data: TowerData): Collection<C>

    /**
     * 处理塔数据
     *
     * 默认实现:调用 [simpleProcess],如果结果非空则包装为列表返回。
     *
     * @param data 塔数据
     * @return 候选者集合列表(如果 [simpleProcess] 返回非空结果,返回包含该结果的列表;否则返回空列表)
     */
    override fun process(data: TowerData): List<Collection<C>> =
        listOfNotNull(simpleProcess(data).takeIf { it.isNotEmpty() })
}


/**
 * 相同优先级组合作用域塔处理器
 *
 * 组合多个简单处理器,所有处理器具有相同的优先级。
 * 合并所有处理器的结果并一起返回。
 *
 * 使用场景:
 * - 可调用引用时,同时查找函数引用和变量引用
 *
 * 与 [PrioritizedCompositeScopeTowerProcessor] 的区别:
 * - PrioritizedCompositeScopeTowerProcessor: 处理器有优先级,结果分开返回
 * - SamePriorityCompositeScopeTowerProcessor: 处理器无优先级,结果合并返回
 *
 * @param C 候选者类型
 * @param processors 处理器数组,所有处理器具有相同优先级
 */
class SamePriorityCompositeScopeTowerProcessor<out C>(
    private vararg val processors: SimpleScopeTowerProcessor<C>
) : SimpleScopeTowerProcessor<C> {
    /**
     * 处理简单的作用域塔数据
     *
     * 调用所有处理器的 simpleProcess 方法,合并结果。
     *
     * @param data 塔数据
     * @return 合并后的候选者集合
     */
    override fun simpleProcess(data: TowerData): Collection<C> = processors.flatMap { it.simpleProcess(data) }

    /**
     * 记录查找
     *
     * 通知所有处理器记录查找。
     *
     * @param skippedData 跳过的塔数据集合
     * @param name 查找的名称
     */
    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {
        processors.forEach { it.recordLookups(skippedData, name) }
    }

}
