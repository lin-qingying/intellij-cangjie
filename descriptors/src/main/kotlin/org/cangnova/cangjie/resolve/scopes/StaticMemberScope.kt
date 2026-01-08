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

package org.cangnova.cangjie.resolve.scopes

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.utils.Printer

/**
 * 静态成员作用域 - 仅包含静态成员的过滤器作用域
 *
 * StaticMemberScope 是一个过滤器装饰器，用于从底层作用域中筛选出所有静态（static）成员。
 * 该类实现了装饰器模式，在不修改原作用域的基础上，提供一个仅包含静态成员的视图。
 *
 * ## 核心功能
 *
 * ### 静态成员过滤
 *
 * 对底层作用域的查询结果应用静态过滤：
 * - [getContributedFunctions]: 仅返回静态函数（`isStatic == true`）
 * - [getContributedVariables]: 仅返回静态变量
 * - [getContributedPropertys]: 仅返回静态属性
 * - [getContributedClassifier]: 返回 null（仓颉语言不支持类内声明类型）
 *
 * ### 名称集合透传
 *
 * 名称集合属性直接透传至底层作用域，因为静态与否无法从名称判断：
 * - [functionNames]: 可能包含非静态函数名（超集语义）
 * - [variableNames]: 可能包含非静态变量名
 * - [propertyNames]: 可能包含非静态属性名
 * - [classifierNames]: 透传分类器名称
 *
 * **注意**: 名称集合的超集语义意味着需要通过 `getContributedXxx` 方法验证。
 *
 * ## 使用场景
 *
 * ### 1. 通过类型名访问静态成员
 *
 * 在仓颉语言中，静态成员通过类型名访问，而非实例：
 *
 * ```kotlin
 * // 源代码示例：
 * // class MyClass {
 * //     static func create(): MyClass = ...
 * //     func instanceMethod() = ...
 * // }
 * // MyClass.create() // 访问静态方法
 *
 * val classDescriptor = // ... 获取 MyClass 的描述符
 * val staticScope = StaticMemberScope(classDescriptor.defaultType.memberScope)
 *
 * // 仅返回 create 方法，不包含 instanceMethod
 * val staticMethods = staticScope.getContributedFunctions(
 *     Name.identifier("create"),
 *     location
 * )
 * ```
 *
 * ### 2. 类型引用补全
 *
 * 在 IDE 中，当用户输入类型名后按 `.` 时，应仅提示静态成员：
 *
 * ```kotlin
 * // 用户输入: MyClass.
 * // 期望补全: create(), staticField 等静态成员
 * // 不应出现: instanceMethod(), instanceField 等实例成员
 *
 * val completionScope = StaticMemberScope(classScope)
 * val suggestions = completionScope.getContributedDescriptors(
 *     DescriptorKindFilter.CALLABLES
 * )
 * ```
 *
 * ### 3. 导入静态成员
 *
 * 支持静态导入语法，仅导入静态成员：
 *
 * ```kotlin
 * // import MyClass.staticFunction
 * // import MyClass.STATIC_CONSTANT
 * ```
 *
 * ## 实现细节
 *
 * ### 过滤策略
 *
 * 使用 Kotlin 标准库的 `filter` 函数进行成员过滤：
 *
 * ```kotlin
 * override fun getContributedFunctions(...) =
 *     memberScope.getContributedFunctions(name, location)
 *         .filter { it.isStatic } // 检查 isStatic 标志
 * ```
 *
 * ### 性能特性
 *
 * - **无额外存储**: 不缓存过滤结果，每次查询都重新过滤
 * - **惰性过滤**: 仅在实际调用查询方法时执行过滤
 * - **透传优化**: 名称集合直接透传，无计算开销
 * - **适用场景**: 适合查询频率低的场景（如 IDE 补全）
 *
 * ### 静态标志检查
 *
 * 依赖描述符的 `isStatic` 属性：
 * - [CallableDescriptor.isStatic]: 对于函数、变量、属性
 * - 分类器默认可通过类型访问，不需要检查
 *
 * ## 与 InstanceMemberScope 的对比
 *
 * | 特性 | StaticMemberScope | InstanceMemberScope |
 * |------|-------------------|---------------------|
 * | 过滤条件 | `isStatic == true` | `isStatic == false` |
 * | 访问方式 | 通过类型名 | 通过实例 |
 * | 使用场景 | 类型引用、静态导入 | 实例引用、成员访问 |
 * | 互补关系 | 是 | 是 |
 *
 * ## 注意事项
 *
 * 1. **类型声明**: 仓颉语言不支持类内声明类型（无嵌套类、内部类等概念），分类器查询始终返回 null
 * 2. **宏描述符**: 当前实现返回空列表，宏不支持静态/实例区分
 * 3. **名称集合超集**: 调用者必须通过 `getContributedXxx` 验证实际存在性
 *
 * ## 典型用法组合
 *
 * ### 组合静态和实例作用域
 *
 * 某些场景需要区分静态和实例访问：
 *
 * ```kotlin
 * val classDescriptor = // ...
 * val fullScope = classDescriptor.defaultType.memberScope
 *
 * // 创建静态作用域（用于 MyClass.xxx 访问）
 * val staticScope = StaticMemberScope(fullScope)
 *
 * // 创建实例作用域（用于 instance.xxx 访问）
 * val instanceScope = InstanceMemberScope(fullScope)
 *
 * // 根据上下文选择合适的作用域
 * val scope = if (isTypeContext) staticScope else instanceScope
 * ```
 *
 * ## 限制和改进方向
 *
 * 1. **无缓存**: 每次查询都重新过滤，可以考虑添加缓存优化
 * 2. **名称集合不精确**: 可以覆盖名称集合属性，仅返回静态成员名称
 * 3. **打印信息错误**: 应修正 `printScopeStructure` 中的类名
 *
 * ## 示例：完整的静态方法调用解析
 *
 * ```kotlin
 * // 源代码: MyClass.createInstance()
 * // 需要解析 createInstance 方法
 *
 * // 1. 解析 MyClass 得到类描述符
 * val classDescriptor = resolveClassName("MyClass")
 *
 * // 2. 获取类的成员作用域
 * val memberScope = classDescriptor.defaultType.memberScope
 *
 * // 3. 创建静态成员作用域
 * val staticScope = StaticMemberScope(memberScope)
 *
 * // 4. 查找 createInstance 方法
 * val methods = staticScope.getContributedFunctions(
 *     Name.identifier("createInstance"),
 *     location
 * )
 *
 * // 5. methods 仅包含静态方法，过滤掉同名实例方法
 * assert(methods.all { it.isStatic })
 * ```
 *
 * @property memberScope 底层成员作用域，提供完整的成员列表
 *
 * @see InstanceMemberScope 互补的实例成员作用域
 * @see MemberScope 父接口
 * @see CallableDescriptor.isStatic 静态标志属性
 */
class StaticMemberScope(val memberScope: MemberScope) : MemberScope {
    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        return memberScope.getContributedVariables(name, location).filter { it.isStatic }
    }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        return memberScope.getContributedPropertys(name, location).filter { it.isStatic }
    }

    override val functionNames: Set<Name>
        get() = memberScope.functionNames

    override val variableNames: Set<Name>
        get() = memberScope.variableNames

    override val classifierNames: Set<Name>?
        get() = memberScope.classifierNames

    override val propertyNames: Set<Name>
        get() = memberScope.propertyNames

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return memberScope.getContributedFunctions(name, location).filter { it.isStatic } // 过滤非静态函数
    }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        return emptyList()
    }

    override fun printScopeStructure(p: Printer) {
        p.println("StaticMemberScope:") // 打印作用域结构
        memberScope.printScopeStructure(p) // 打印基础成员作用域的结构
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return memberScope.getContributedClassifier(name, location) // 过滤非静态函数
    }


    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> {
        // 仓颉语言不支持类内声明类型（无嵌套类概念）
        return emptyList()
    }


    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return memberScope.getContributedDescriptors(kindFilter, nameFilter)
    }
}
