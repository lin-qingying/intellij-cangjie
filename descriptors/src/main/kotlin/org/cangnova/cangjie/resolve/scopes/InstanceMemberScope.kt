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
 * 实例成员作用域 - 仅包含实例（非静态）成员的过滤器作用域
 *
 * InstanceMemberScope 是一个过滤器装饰器，用于从底层作用域中筛选出所有实例（非静态）成员。
 * 该类与 [StaticMemberScope] 互补，共同实现了静态成员和实例成员的分离访问。
 *
 * ## 核心功能
 *
 * ### 实例成员过滤
 *
 * 对底层作用域的查询结果应用实例过滤：
 * - [getContributedFunctions]: 仅返回实例函数（`isStatic == false`）
 * - [getContributedVariables]: 仅返回实例变量
 * - [getContributedPropertys]: 仅返回实例属性
 * - [getContributedClassifier]: 返回 null（仓颉语言不支持类内声明类型）
 *
 * ### 名称集合透传
 *
 * 名称集合属性直接透传至底层作用域：
 * - [functionNames]: 可能包含静态函数名（超集语义）
 * - [variableNames]: 可能包含静态变量名
 * - [propertyNames]: 可能包含静态属性名
 * - [classifierNames]: 透传分类器名称
 *
 * ## 使用场景
 *
 * ### 1. 通过实例访问成员
 *
 * 在仓颉语言中，实例成员通过对象实例访问：
 *
 * ```kotlin
 * // 源代码示例：
 * // class MyClass {
 * //     func instanceMethod(): String = ...
 * //     static func staticMethod(): String = ...
 * //     var instanceField: Int = 0
 * // }
 * // let obj = MyClass()
 * // obj.instanceMethod() // 访问实例方法
 * // obj.instanceField // 访问实例字段
 *
 * val classDescriptor = // ... 获取 MyClass 的描述符
 * val instanceScope = InstanceMemberScope(classDescriptor.defaultType.memberScope)
 *
 * // 仅返回 instanceMethod，不包含 staticMethod
 * val instanceMethods = instanceScope.getContributedFunctions(
 *     Name.identifier("instanceMethod"),
 *     location
 * )
 * ```
 *
 * ### 2. 实例引用补全
 *
 * 在 IDE 中，当用户输入实例变量后按 `.` 时，应仅提示实例成员：
 *
 * ```kotlin
 * // 用户输入: obj.
 * // 期望补全: instanceMethod(), instanceField 等实例成员
 * // 不应出现: staticMethod(), staticField 等静态成员
 *
 * val instanceType = resolveExpressionType("obj")
 * val completionScope = InstanceMemberScope(instanceType.memberScope)
 * val suggestions = completionScope.getContributedDescriptors(
 *     DescriptorKindFilter.CALLABLES
 * )
 * ```
 *
 * ### 3. this 关键字作用域
 *
 * 在类方法内部，`this` 关键字引用当前实例，其作用域应为实例成员作用域：
 *
 * ```kotlin
 * // class MyClass {
 * //     func method() {
 * //         this.instanceMethod() // 可以访问
 * //         this.staticMethod()   // 编译错误
 * //     }
 * // }
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
 *         .filter { !it.isStatic } // 排除静态成员
 * ```
 *
 * ### 过滤逻辑对比
 *
 * | 成员类型 | 过滤条件 | 说明 |
 * |---------|---------|------|
 * | 函数 | `!isStatic` | 仅保留实例方法 |
 * | 变量 | `!isStatic` | 仅保留实例变量 |
 * | 属性 | `!isStatic` | 仅保留实例属性 |
 * | 分类器 | 返回 null | 仓颉语言不支持类内声明类型 |
 * | 宏 | 返回空 | 当前不支持宏 |
 *
 * ### 性能特性
 *
 * - **无缓存**: 不存储过滤结果，每次查询重新过滤
 * - **惰性过滤**: 仅在调用时执行过滤逻辑
 * - **透传优化**: 名称集合直接透传，零开销
 * - **适用场景**: 高频访问场景（大部分成员访问都是实例访问）
 *
 * ## 与 StaticMemberScope 的对比
 *
 * | 特性 | InstanceMemberScope | StaticMemberScope |
 * |------|---------------------|-------------------|
 * | 过滤条件 | `isStatic == false` | `isStatic == true` |
 * | 访问语法 | `instance.member` | `ClassName.member` |
 * | 访问频率 | 高（大部分访问） | 低（特定场景） |
 * | 典型场景 | 方法调用、字段访问 | 工厂方法、常量访问 |
 * | 关键字支持 | `this`, `super` | 类型名 |
 *
 * ## 互补关系
 *
 * InstanceMemberScope 和 StaticMemberScope 形成完整的成员分割：
 *
 * ```kotlin
 * val fullScope = classDescriptor.defaultType.memberScope
 * val staticScope = StaticMemberScope(fullScope)
 * val instanceScope = InstanceMemberScope(fullScope)
 *
 * // 完整性：staticScope ∪ instanceScope ≈ fullScope（分类器除外）
 * // 互斥性：staticScope ∩ instanceScope = ∅
 * ```
 *
 * ## 注意事项
 *
 * 1. **继承成员**: 包含从父类继承的实例成员（通过底层 memberScope 提供）
 * 2. **类型声明**: 仓颉语言不支持类内声明类型（无嵌套类、内部类等概念），分类器查询始终返回 null
 * 3. **宏支持**: 当前返回空列表，未来可能扩展
 * 4. **名称集合超集**: 需通过 `getContributedXxx` 验证实际存在性
 *
 * ## 典型使用场景
 *
 * ### 场景 1: 解析成员访问表达式
 *
 * ```kotlin
 * // 源代码: obj.doSomething()
 *
 * // 1. 解析 obj 的类型
 * val objType = resolveExpression("obj").type
 *
 * // 2. 获取实例成员作用域
 * val instanceScope = InstanceMemberScope(objType.memberScope)
 *
 * // 3. 查找 doSomething 方法
 * val methods = instanceScope.getContributedFunctions(
 *     Name.identifier("doSomething"),
 *     location
 * )
 * ```
 *
 * ### 场景 2: this 关键字解析
 *
 * ```kotlin
 * // 在类方法内部解析 this
 *
 * val currentClass = getCurrentClassDescriptor()
 * val thisScope = InstanceMemberScope(
 *     currentClass.defaultType.memberScope
 * )
 *
 * // this 只能访问实例成员
 * val availableMembers = thisScope.getContributedDescriptors()
 * ```
 *
 * ### 场景 3: super 关键字解析
 *
 * ```kotlin
 * // 在子类方法内部解析 super
 *
 * val parentClass = getCurrentClassDescriptor().getSuperClassDescriptor()
 * val superScope = InstanceMemberScope(
 *     parentClass.defaultType.memberScope
 * )
 *
 * // super 访问父类实例成员
 * val parentMethods = superScope.getContributedFunctions(
 *     methodName,
 *     location
 * )
 * ```
 *
 * ## 限制和改进方向
 *
 * 1. **无缓存**: 高频访问场景可考虑添加缓存
 * 2. **名称集合不精确**: 可覆盖属性，仅返回实例成员名称
 * 3. **批量过滤**: `getContributedDescriptors` 可以应用过滤优化
 *
 * ## 示例：完整的实例方法调用解析
 *
 * ```kotlin
 * // 源代码: myList.add(item)
 * // 需要解析 add 方法
 *
 * // 1. 解析 myList 得到其类型
 * val listType = resolveExpression("myList").type
 *
 * // 2. 获取类型的成员作用域
 * val memberScope = listType.memberScope
 *
 * // 3. 创建实例成员作用域（因为通过实例访问）
 * val instanceScope = InstanceMemberScope(memberScope)
 *
 * // 4. 查找 add 方法
 * val addMethods = instanceScope.getContributedFunctions(
 *     Name.identifier("add"),
 *     location
 * )
 *
 * // 5. addMethods 仅包含实例方法，不含静态方法
 * assert(addMethods.all { !it.isStatic })
 *
 * // 6. 进行重载解析选择合适的 add 方法
 * val selectedMethod = resolveOverload(addMethods, arguments)
 * ```
 *
 * @property memberScope 底层成员作用域，提供完整的成员列表
 *
 * @see StaticMemberScope 互补的静态成员作用域
 * @see MemberScope 父接口
 * @see CallableDescriptor.isStatic 静态标志属性
 */
class InstanceMemberScope(private val memberScope: MemberScope) : MemberScope {
    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        return memberScope.getContributedVariables(name, location).filter { !it.isStatic }
    }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        return memberScope.getContributedPropertys(name, location).filter { !it.isStatic }
    }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        return emptyList()
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
        return memberScope.getContributedFunctions(name, location).filter { !it.isStatic } // 过滤非静态函数
    }

    override fun printScopeStructure(p: Printer) {
        p.println("InstanceMemberScope:") // 打印作用域结构
        memberScope.printScopeStructure(p) // 打印基础成员作用域的结构
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        // 仓颉语言不支持类内声明类型（无嵌套类概念）
        return null
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return memberScope.getContributedDescriptors(kindFilter, nameFilter)  // 过滤非静态描述符
    }
}

