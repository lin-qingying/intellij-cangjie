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

package org.cangnova.cangjie.resolve.scopes

import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.Substitutable
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.psiUtil.sure
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.DefaultTypeSubstitutor
import org.cangnova.cangjie.types.checker.wrapWithCapturingSubstitution
import org.cangnova.cangjie.utils.Printer
import org.cangnova.cangjie.utils.newLinkedHashSetWithExpectedSize

/**
 * 类型替换作用域 - 对底层作用域的所有符号应用类型替换的装饰器实现
 *
 * SubstitutingScope 是装饰器模式的典型应用，用于在不修改原作用域的情况下，
 * 对其返回的所有描述符应用类型替换（Type Substitution）。这在泛型实例化、
 * 类型推导等场景中极为关键。
 *
 * ## 核心功能
 *
 * ### 类型替换
 *
 * 所有从底层作用域获取的描述符都会经过类型替换处理：
 * - 函数描述符：替换返回类型、参数类型、类型参数等
 * - 变量描述符：替换变量类型
 * - 属性描述符：替换属性类型、Getter/Setter 类型等
 * - 分类器描述符：替换类型参数的上界
 *
 * ### 透传语义
 *
 * 名称集合属性直接透传给底层作用域，避免重复计算：
 * - [functionNames]: 透传至 workerScope
 * - [variableNames]: 透传至 workerScope
 * - [classifierNames]: 透传至 workerScope
 * - [propertyNames]: 透传至 workerScope
 *
 * ## 使用场景
 *
 * ### 1. 泛型类实例化
 *
 * 当访问泛型类的具体实例的成员时，需要将类型参数替换为具体类型：
 *
 * ```kotlin
 * // 定义: class List<T> { func add(item: T) }
 * // 使用: let list: List<Int> = ...
 *
 * val listScope = listClassDescriptor.defaultType.memberScope
 * val substitutedScope = SubstitutingScope(
 *     listScope,
 *     DefaultTypeSubstitutor.create(mapOf(T -> Int))
 * )
 *
 * // 查找 add 函数，返回的参数类型已经是 Int 而非 T
 * val addFunction = substitutedScope.getContributedFunctions(Name.identifier("add"), location)
 * // addFunction.valueParameters[0].type == Int
 * ```
 *
 * ### 2. 继承关系中的类型替换
 *
 * 当子类继承泛型父类时，父类成员需要进行类型替换：
 *
 * ```kotlin
 * // class Parent<T> { func process(item: T): T }
 * // class Child : Parent<String>
 *
 * val parentScope = parentClassDescriptor.defaultType.memberScope
 * val substitutedScope = SubstitutingScope(
 *     parentScope,
 *     DefaultTypeSubstitutor.create(mapOf(T -> String))
 * )
 *
 * // Child 继承的 process 方法的签名是 (String) -> String
 * ```
 *
 * ### 3. 扩展函数的接收者类型替换
 *
 * 为泛型类型定义的扩展函数，在具体类型上使用时需要替换接收者类型。
 *
 * ## 实现细节
 *
 * ### 捕获替换器（Capturing Substitutor）
 *
 * 使用 `wrapWithCapturingSubstitution()` 包装原替换器，以正确处理通配符类型：
 *
 * ```kotlin
 * private val capturingSubstitutor = givenSubstitutor.substitution
 *     .wrapWithCapturingSubstitution()
 *     .buildSubstitutor()
 * ```
 *
 * ### 替换缓存
 *
 * 为避免重复替换同一描述符，使用 `substitutedDescriptors` 缓存已替换的结果：
 *
 * ```kotlin
 * private var substitutedDescriptors: MutableMap<DeclarationDescriptor, DeclarationDescriptor>? = null
 * ```
 *
 * ### 惰性初始化
 *
 * 所有描述符列表通过 lazy 惰性计算，仅在实际访问时才进行替换：
 *
 * ```kotlin
 * private val _allDescriptors by lazy { substitute(workerScope.getContributedDescriptors()) }
 * ```
 *
 * ### 空替换优化
 *
 * 如果替换器为空（无需替换），直接返回原对象避免不必要的开销：
 *
 * ```kotlin
 * if (capturingSubstitutor.isEmpty) return type // 快速路径
 * ```
 *
 * ## 替换算法
 *
 * ### 单个描述符替换
 *
 * 1. 检查替换器是否为空，是则直接返回
 * 2. 查找缓存，如已替换则返回缓存结果
 * 3. 调用描述符的 `substitute()` 方法进行替换
 * 4. 将结果存入缓存
 * 5. 返回替换后的描述符
 *
 * ### 描述符集合替换
 *
 * 对集合中的每个描述符逐一应用替换，保持原有顺序。
 *
 * ## 性能特性
 *
 * - **缓存机制**: 避免重复替换同一描述符
 * - **惰性计算**: 仅在实际访问时才执行替换
 * - **快速路径**: 空替换器直接返回原对象
 * - **透传优化**: 名称集合直接透传，无额外开销
 *
 * ## 注意事项
 *
 * 1. **不可变性**: SubstitutingScope 本身是不可变的，替换器和底层作用域在构造后不应改变
 * 2. **线程安全**: 替换缓存使用 HashMap，非线程安全，应在单线程环境使用
 * 3. **循环引用**: 替换过程中应避免循环引用导致无限递归
 * 4. **类型一致性**: 替换器应保证类型系统的一致性，避免产生非法类型
 *
 * ## 与其他作用域的配合
 *
 * SubstitutingScope 常与 ChainedMemberScope 配合使用：
 *
 * ```kotlin
 * // 组合类自身作用域和替换后的父类作用域
 * val combinedScope = ChainedMemberScope.create(
 *     "MyClass with substituted Parent",
 *     classMemberScope,
 *     SubstitutingScope(parentScope, substitutor)
 * )
 * ```
 *
 * ## 示例：完整的泛型类实例化
 *
 * ```kotlin
 * // 源代码: class Box<T>(var value: T) { func get(): T = value }
 * // 实例化: let box: Box<String> = Box("hello")
 *
 * val boxClassDescriptor = // ... 获取 Box 类描述符
 * val typeParameter_T = boxClassDescriptor.declaredTypeParameters[0]
 *
 * // 创建替换器：T -> String
 * val substitutor = DefaultTypeSubstitutor.create(mapOf(
 *     typeParameter_T.defaultType to stringType
 * ))
 *
 * // 创建替换作用域
 * val boxStringScope = SubstitutingScope(
 *     boxClassDescriptor.defaultType.memberScope,
 *     substitutor
 * )
 *
 * // 查找 get 方法，返回类型已是 String 而非 T
 * val getMethod = boxStringScope.getContributedFunctions(
 *     Name.identifier("get"),
 *     location
 * ).first()
 *
 * assert(getMethod.returnType == stringType) // T 已替换为 String
 * ```
 *
 * @property workerScope 底层工作作用域，提供原始未替换的符号
 * @property substitutor 类型替换器（public，供外部访问当前的替换规则）
 * @property capturingSubstitutor 包含捕获逻辑的内部替换器
 * @property substitutedDescriptors 替换结果缓存，避免重复替换
 * @property _allDescriptors 惰性计算的所有描述符列表（已替换）
 *
 * @see DefaultTypeSubstitutor 类型替换器
 * @see MemberScope 父接口
 * @see ChainedMemberScope 常与本类配合使用的组合作用域
 * @see Substitutable 支持替换的描述符接口
 */
class SubstitutingScope(private val workerScope: MemberScope, givenSubstitutor: DefaultTypeSubstitutor) : MemberScope {
    val substitutor by lazy { givenSubstitutor.substitution.buildSubstitutor() }

        private val capturingSubstitutor = givenSubstitutor.substitution.wrapWithCapturingSubstitution().buildSubstitutor()
    private var substitutedDescriptors: MutableMap<DeclarationDescriptor, DeclarationDescriptor>? = null

    private val _allDescriptors by lazy { substitute(workerScope.getContributedDescriptors()) }
    override val functionNames: Set<Name>
        get() = workerScope.functionNames
    override val variableNames: Set<Name>
        get() = workerScope.variableNames
    override val classifierNames: Set<Name>?
        get() = workerScope.classifierNames
    override val propertyNames: Set<Name>
        get() = workerScope.propertyNames
    fun substitute(type: CangJieType): CangJieType {
        if (capturingSubstitutor.isEmpty) return type
        return capturingSubstitutor.safeSubstitute(type)
    }

    private fun <D : DeclarationDescriptor> substitute(descriptor: D): D {
        if (capturingSubstitutor.isEmpty) return descriptor

        if (substitutedDescriptors == null) {
            substitutedDescriptors = HashMap<DeclarationDescriptor, DeclarationDescriptor>()
        }

        val substituted = substitutedDescriptors!!.getOrPut(descriptor) {
            when (descriptor) {
                is Substitutable<*> -> descriptor.substitute(capturingSubstitutor).sure {
                    "We expect that no conflict should happen while substitution is guaranteed to generate invariant argument, " +
                            "but $descriptor substitution fails"
                }

                else -> error("Unknown descriptor in scope: $descriptor")
            }
        }

        @Suppress("UNCHECKED_CAST")
        return substituted as D
    }

    private fun <D : DeclarationDescriptor> substitute(descriptors: Collection<D>): Collection<D> {
        if (capturingSubstitutor.isEmpty) return descriptors
        if (descriptors.isEmpty()) return descriptors

        val result = newLinkedHashSetWithExpectedSize<D>(descriptors.size)
        for (descriptor in descriptors) {
            val substitute = substitute(descriptor)
            result.add(substitute)
        }

        return result
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor> =
        substitute(workerScope.getContributedVariables(name, location))

    override fun getContributedPropertys(name: Name, location: LookupLocation) =
        substitute(workerScope.getContributedPropertys(name, location))
    override fun getContributedClassifiers( name: Name, location: LookupLocation): List<ClassifierDescriptor> =
        workerScope.getContributedClassifiers(name, location) .map { substitute(it) }

    override fun getContributedClassifier(name: Name, location: LookupLocation) =
        workerScope.getContributedClassifier(name, location)?.let { substitute(it) }


    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> =
        substitute(workerScope.getContributedMacros(name, location))
    override fun getContributedFunctions(name: Name, location: LookupLocation) =
        substitute(workerScope.getContributedFunctions(name, location))


    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return _allDescriptors.filter {
            nameFilter.invoke(it.name)
        }
    }

//    override fun getFunctionNames() = workerScope.getFunctionNames()
//    override fun getVariableNames() = workerScope.getVariableNames()
//    override fun classifierNames() = workerScope.classifierNames()

    override fun definitelyDoesNotContainName(name: Name) = workerScope.definitelyDoesNotContainName(name)

    //
    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.println("substitutor = ")
        p.pushIndent()
        p.println(capturingSubstitutor)
        p.popIndent()

        p.print("workerScope = ")
        workerScope.printScopeStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }
}
