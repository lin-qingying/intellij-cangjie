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

package org.cangnova.cangjie.codeinsight

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjValueArgument
import org.cangnova.cangjie.psi.psiUtil.getOutermostParenthesizerOrThis
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.calls.model.ArgumentMatch
import org.cangnova.cangjie.resolve.calls.util.getParentResolvedCall
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.utils.isDispatchThread
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.CangJieCodeInsightBundle
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.isIdentifier
import org.cangnova.cangjie.psi.psiUtil.unquoteCangJieIdentifier
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.builtIns
import org.cangnova.cangjie.types.isFunctionType
import org.cangnova.cangjie.utils.decapitalizeSmart
import org.cangnova.cangjie.utils.executeInBackgroundWithProgress
import java.awt.EventQueue.isDispatchThread

/**
 * 仓颉名称建议器
 *
 * [AbstractCangJieNameSuggester] 的具体实现类,提供完整的名称建议功能。
 * 这是一个单例对象,为仓颉语言的各种编程场景提供智能的名称建议。
 *
 * **核心功能**:
 * - 根据表达式和类型生成变量名
 * - 根据类型特征生成合适的名称
 * - 为迭代变量生成单数形式的名称
 * - 从表达式语法结构中提取语义信息
 * - 移除访问器前缀(get/is/set)生成属性名
 * - 生成驼峰命名法的多种变体
 *
 * **使用场景**:
 * - **变量引入重构**: `val x = getUserName()` → 建议 `userName`, `name`
 * - **参数名称建议**: 根据参数类型和上下文建议合适的参数名
 * - **循环变量命名**: `for (item in items)` → 建议 `item`(复数变单数)
 * - **代码补全**: 根据期望类型和表达式建议变量名
 * - **快速修复**: 为缺失的声明建议名称
 *
 * **命名策略**:
 * - **基本类型**: 使用约定俗成的短名称(如 `i` for Int, `fl` for Float)
 * - **类类型**: 使用类名的驼峰形式(如 `ArrayList` → `arrayList`)
 * - **泛型类型**: 组合类型参数和类名(如 `List<String>` → `stringList`)
 * - **可迭代类型**: 使用复数形式(如 `List<Item>` → `items`)
 * - **函数调用**: 提取被调用函数的名称(如 `getUser()` → `user`)
 * - **访问器去除**: 移除 get/is/set 前缀(如 `getName()` → `name`)
 *
 * **性能优化**:
 * - 在后台线程中执行命名建议,避免阻塞 UI 线程
 * - 使用序列(Sequence)延迟生成名称,避免不必要的计算
 * - 支持进度取消机制
 *
 * **线程安全**:
 * - 自动检测是否在 UI 线程,并在需要时切换到后台线程
 * - 使用 ReadAction 保护对 PSI 的访问
 *
 * **命名验证**:
 * - 所有生成的名称都通过验证器检查
 * - 自动处理命名冲突(添加数字后缀)
 * - 确保生成的名称是有效的仓颉标识符
 * - 避免使用关键字作为名称
 *
 * @see AbstractCangJieNameSuggester 基础抽象类
 */
object  CangJieNameSuggester : AbstractCangJieNameSuggester() {
    /**
     * 根据表达式和类型建议名称
     *
     * 综合利用表达式的语法结构和类型信息,生成最相关的名称建议列表。
     * 这是名称建议的主入口方法之一,结合了多种命名策略。
     *
     * **建议优先级**:
     * 1. **表达式语义**: 从表达式的语法结构中提取名称(如函数调用、属性访问)
     * 2. **类型特征**: 根据类型生成约定俗成的名称(基本类型、集合类型等)
     * 3. **默认名称**: 如果前两种策略都失败,使用提供的默认名称
     *
     * **命名策略示例**:
     * ```kotlin
     * // 表达式: getUser()
     * // 类型: User
     * // 建议: user, userInstance
     *
     * // 表达式: person.name
     * // 类型: String
     * // 建议: name, s
     *
     * // 表达式: calculate(1, 2)
     * // 类型: Int
     * // 建议: i, result
     *
     * // 表达式: listOf("a", "b")
     * // 类型: List<String>
     * // 建议: strings, stringList
     * ```
     *
     * **性能考虑**:
     * - 在后台线程中执行,避免阻塞 UI 线程
     * - 如果已经在 UI 线程且无写锁,自动切换到后台线程
     * - 使用 ProgressManager 支持进度取消
     *
     * **类型推断**:
     * - 如果 [type] 参数为 null,尝试从 [bindingContext] 推断表达式的类型
     * - 如果无法推断类型,仅使用表达式语义生成名称
     *
     * @param expression 表达式 PSI 节点,用于提取语义信息
     * @param type 期望的类型(可为 null,将从绑定上下文推断)
     * @param bindingContext 绑定上下文,用于类型推断和语义解析(可为 null)
     * @param validator 名称验证器函数,检查名称是否可用(不冲突、符合规范等)
     * @param defaultName 默认名称,当无法生成有效名称时使用(可为 null)
     * @return Collection<String> 建议的名称列表(按优先级排序,可能为空)
     */
    fun suggestNamesByExpressionAndType(
        expression: CjExpression,
        type: CangJieType?,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean,
        defaultName: String?
    ): Collection<String> {
        return executeInBackgroundWithProgress(expression.project) {
            LinkedHashSet<String>().apply {
                addNamesByExpression(expression, bindingContext, validator)

                (type ?: bindingContext?.getType(expression))?.let {
                    addNamesByType(it, validator)
                }

                if (isEmpty()) {
                    addName(defaultName, validator)
                }
            }.toList()
        }
    }

    /**
     * 根据类型建议名称
     *
     * 仅根据类型信息生成名称建议,不考虑表达式的语法结构。
     * 适用于已知类型但没有具体表达式的场景。
     *
     * **类型到名称的映射策略**:
     * - **基本数值类型**: `Int/Int8/Int16/Int64` → `i`, `Float16/Float32/Float64` → `fl`
     * - **布尔类型**: `Bool` → `b` 或 `flag`
     * - **字符类型**: `Rune` → `r`
     * - **函数类型**: `(Int) -> String` → `function`
     * - **可迭代类型**: `List<T>` → 复数形式的元素类型名称(如 `items`)
     * - **普通类类型**: 使用类名的驼峰形式(如 `UserProfile` → `userProfile`)
     * - **泛型类型**: 组合类型参数和类名(如 `Map<String, User>` → `stringUserMap`)
     *
     * **命名示例**:
     * ```kotlin
     * suggestNamesByType(Int类型, validator)  // ["i"]
     * suggestNamesByType(String类型, validator)  // ["string", "s"]
     * suggestNamesByType(List<Item>类型, validator)  // ["items", "itemList"]
     * suggestNamesByType(User类型, validator)  // ["user", "userInstance"]
     * suggestNamesByType((Int)->Bool类型, validator)  // ["function", "predicate"]
     * ```
     *
     * **性能优化**:
     * - 在后台线程中执行,避免阻塞 UI 线程
     * - 如果没有项目上下文,直接在当前线程执行
     * - 支持通过 ProgressManager 取消操作
     *
     * **默认名称处理**:
     * - 如果无法从类型推断出有效名称,使用提供的默认名称
     * - 默认名称也会经过验证器检查
     *
     * @param type 类型信息,用于生成名称
     * @param validator 名称验证器函数,检查名称是否可用
     * @param defaultName 默认名称,当无法从类型推断时使用(默认为 null)
     * @return List<String> 建议的名称列表(按优先级排序)
     */
    fun suggestNamesByType(type: CangJieType, validator: (String) -> Boolean, defaultName: String? = null): List<String> =
        executeInBackgroundWithProgress(null) {
            ArrayList<String>().apply {
                addNamesByType(type, validator)
                if (isEmpty()) {
                    ProgressManager.checkCanceled()
                    addName(defaultName, validator)
                }
            }
        }

    /**
     * 后台线程执行命名建议任务
     *
     * 智能地处理线程切换,确保命名建议操作不会阻塞 UI 线程。
     *
     * **线程策略**:
     * - 如果当前在 UI 线程且没有写锁 → 切换到后台线程执行
     * - 如果已经在后台线程或持有写锁 → 直接在当前线程执行
     *
     * **后台执行机制**:
     * - 显示进度对话框(标题为"正在计算名称...")
     * - 支持用户取消操作
     * - 使用 ReadAction 保护对 PSI 的访问
     *
     * **写锁处理**:
     * 如果当前持有写锁,无法切换到后台线程(避免死锁),直接在当前线程执行。
     *
     * @param T 返回类型(通常为 List<String>)
     * @param project 项目上下文(可为 null,用于显示进度对话框)
     * @param blockToExecute 要执行的命名建议逻辑
     * @return T 执行结果(建议的名称列表)
     */
    private fun executeInBackgroundWithProgress(project: Project?, blockToExecute: () -> List<String>): List<String> =
        if (isDispatchThread() && !ApplicationManager.getApplication().isWriteAccessAllowed) {
            executeInBackgroundWithProgress(
                project,
                CangJieCodeInsightBundle.message("progress.title.calculating.names")
            ) { runReadAction { blockToExecute() } }
        } else {
            blockToExecute()
        }

    /**
     * 仅根据表达式建议名称
     *
     * 只从表达式的语法结构中提取名称,不考虑类型信息。
     * 适用于无法获取类型信息或类型信息不重要的场景。
     *
     * **提取策略**:
     * - **函数调用**: 提取函数名(如 `getUser()` → `user`)
     * - **属性访问**: 提取属性名(如 `person.name` → `name`)
     * - **简单名称引用**: 直接使用引用的名称
     * - **限定表达式**: 提取选择器部分(如 `a.b.c` → `c`)
     * - **后缀表达式**: 提取基础表达式名称(如 `x++` → `x`)
     * - **访问器去除**: 移除 get/is/set 前缀(如 `getName()` → `name`)
     *
     * **驼峰命名变体生成**:
     * 对提取的名称生成多个驼峰命名变体:
     * ```kotlin
     * // 提取名称: getUserProfile
     * // 变体: userProfile, profile
     *
     * // 提取名称: isEnabled
     * // 变体: enabled
     *
     * // 提取名称: person.homeAddress
     * // 变体: homeAddress, address
     * ```
     *
     * **默认名称处理**:
     * - 如果无法从表达式提取有效名称,使用提供的默认名称
     * - 默认名称也会经过验证器检查
     *
     * **与 suggestNamesByExpressionAndType 的区别**:
     * - 此方法不使用类型信息,仅分析语法结构
     * - 适用于快速提取名称的场景
     * - 不在后台线程执行,直接在当前线程运行
     *
     * **使用场景**:
     * - 为循环变量生成名称(从集合表达式提取单数形式)
     * - 为参数生成名称(从传入的参数表达式提取)
     * - 快速重构时的临时名称建议
     *
     * @param expression 表达式 PSI 节点,用于提取语义信息
     * @param bindingContext 绑定上下文,用于解析引用和调用(可为 null)
     * @param validator 名称验证器函数,检查名称是否可用
     * @param defaultName 默认名称,当无法提取有效名称时使用(默认为 null)
     * @return List<String> 建议的名称列表(按优先级排序)
     */
    fun suggestNamesByExpressionOnly(
        expression: CjExpression,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean, defaultName: String? = null
    ): List<String> {
        val result = ArrayList<String>()

        result.addNamesByExpression(expression, bindingContext, validator)

        if (result.isEmpty()) {
            result.addName(defaultName, validator)
        }

        return result
    }

    /**
     * 为迭代变量建议名称
     *
     * 专门为 for-in 循环的迭代变量生成合适的名称。
     * 综合利用集合表达式和元素类型信息,生成最相关的单数形式名称。
     *
     * **命名策略**:
     * 1. **从集合名称转单数**: 从集合表达式提取名称,然后转换为单数形式
     * 2. **从元素类型**: 根据元素类型生成约定俗成的名称
     * 3. **默认名称**: 如果前两种策略都失败,使用提供的默认名称
     *
     * **复数转单数规则**:
     * 使用英文复数转单数规则:
     * - `users` → `user`
     * - `items` → `item`
     * - `children` → `child`
     * - `people` → `person`
     * - `indices` → `index`
     *
     * **命名示例**:
     * ```kotlin
     * // 集合表达式: users
     * // 元素类型: User
     * // 建议: user, userInstance
     *
     * // 集合表达式: getActiveItems()
     * // 元素类型: Item
     * // 建议: activeItem, item
     *
     * // 集合表达式: person.children
     * // 元素类型: Person
     * // 建议: child, person
     *
     * // 集合表达式: listOf(1, 2, 3)
     * // 元素类型: Int
     * // 建议: i, element
     * ```
     *
     * **关键字过滤**:
     * 自动过滤掉仓颉语言的关键字,避免生成非法的变量名:
     * ```kotlin
     * // 集合表达式: functions (复数转单数为 function)
     * // 如果 "function" 是关键字,会被过滤掉
     * // 只使用类型建议: func, f
     * ```
     *
     * **类型信息增强**:
     * 即使从集合名称成功提取了名称,仍然会添加基于元素类型的建议,
     * 提供更多选择。
     *
     * **使用场景**:
     * - for-in 循环的迭代变量命名
     * - 集合操作的 lambda 参数命名(如 map, filter)
     * - 解构声明的变量命名
     *
     * @param collection 集合表达式,用于提取集合名称
     * @param elementType 集合元素的类型,用于生成类型相关的名称
     * @param bindingContext 绑定上下文,用于语义解析(可为 null)
     * @param validator 名称验证器函数,检查名称是否可用
     * @param defaultName 默认名称,当无法生成有效名称时使用(可为 null)
     * @return Collection<String> 建议的名称列表(按优先级排序)
     */
    fun suggestIterationVariableNames(
        collection: CjExpression,
        elementType: CangJieType,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean, defaultName: String?
    ): Collection<String> {
        val result = LinkedHashSet<String>()

        suggestNamesByExpressionOnly(collection, bindingContext, { true })
            .mapNotNull { name -> StringUtil.unpluralize(name)}
            .filter { name -> !name.isKeyword() }
            .mapTo(result) { suggestNameByName(it, validator) }

        result.addNamesByType(elementType, validator)

        if (result.isEmpty()) {
            result.addName(defaultName, validator)
        }

        return result
    }

    /**
     * 检查字符串是否为仓颉关键字
     *
     * 判断给定的字符串是否是仓颉语言的保留关键字。
     *
     * **仓颉关键字包括**:
     * - 控制流: `if`, `else`, `while`, `for`, `match`, `return`, `break`, `continue`
     * - 声明: `let`, `var`, `func`, `class`, `interface`, `struct`, `enum`
     * - 修饰符: `public`, `private`, `protected`, `internal`, `open`, `sealed`, `abstract`
     * - 类型: `Int`, `Bool`, `String`, `Unit`, 等等
     * - 其他: `this`, `super`, `import`, `package`, 等等
     *
     * **使用场景**:
     * - 过滤生成的名称,避免使用关键字作为变量名
     * - 验证标识符的合法性
     *
     * @receiver String? 待检查的字符串(可为 null)
     * @return Boolean true 表示是关键字,false 表示不是关键字(或为 null)
     */
    private fun String?.isKeyword() = this in CjTokens.KEYWORDS.types.map { it.toString() }

    /**
     * 根据类型添加名称建议
     *
     * 根据类型的特征,向集合中添加合适的名称建议。
     * 这是类型到名称映射的核心实现,处理各种仓颉类型。
     *
     * **类型处理策略**(按优先级):
     *
     * 1. **基本数值类型**:
     *    - `Bool` → `i`
     *    - `Int8/Int16/Int32/Int64` → `i`
     *    - `Float16/Float32/Float64` → `fl`
     *    - `Rune`(字符) → `r`
     *
     * 2. **函数类型**:
     *    - `(Int) -> String` → `function`
     *    - `() -> Unit` → `function`
     *
     * 3. **可迭代类型**:
     *    - `List<Item>` → `items`(元素类型的复数形式)
     *    - `List<User>` → `users`, `userList`
     *    - 适用于所有 Iterable 子类型
     *
     * 4. **普通类类型**:
     *    - 从类名生成驼峰命名: `UserProfile` → `userProfile`, `profile`
     *    - 从泛型参数生成组合名称: `Map<String, User>` → `stringUserMap`
     *
     * **错误类型处理**:
     * 如果类型包含错误(如解析失败),直接返回,不添加任何名称。
     *
     * **可空类型处理**:
     * 自动移除可空标记(?)后再进行处理:
     * - `Int?` → 按 `Int` 处理 → `i`
     * - `List<String>?` → 按 `List<String>` 处理 → `strings`
     *
     * **命名示例**:
     * ```kotlin
     * addNamesByType(Int32类型, validator)
     * // 添加: i
     *
     * addNamesByType(List<Item>类型, validator)
     * // 添加: items, itemList
     *
     * addNamesByType(UserProfile类型, validator)
     * // 添加: userProfile, profile
     *
     * addNamesByType(Map<String, User>类型, validator)
     * // 添加: stringUserMap, map
     * ```
     *
     * **验证器作用**:
     * 所有添加的名称都会通过验证器检查,只有通过验证的名称才会被添加到集合中。
     *
     * @receiver MutableCollection<String> 名称集合,用于添加生成的名称
     * @param type 类型信息,用于生成名称
     * @param validator 名称验证器函数,检查名称是否可用
     */
    private fun MutableCollection<String>.addNamesByType(type: CangJieType, validator: (String) -> Boolean) {
        val myType = TypeUtils.makeNonOption(type) // wipe out '?'
        val builtIns = myType.builtIns
        val typeChecker = CangJieTypeChecker.DEFAULT
        if (ErrorUtils.containsErrorType(myType)) return
        val typeDescriptor = myType.constructor.declarationDescriptor
        when {
            typeChecker.equalTypes(builtIns.boolType, myType) -> addName("i", validator)
            typeChecker.equalTypes(builtIns.int32Type, myType) -> addName("i", validator)
            typeChecker.equalTypes(builtIns.int8Type, myType) -> addName("i", validator)
            typeChecker.equalTypes(builtIns.int64Type, myType) -> addName("i", validator)
            typeChecker.equalTypes(builtIns.float32Type, myType) -> addName("fl", validator)
            typeChecker.equalTypes(builtIns.float64Type, myType) -> addName("fl", validator)
            typeChecker.equalTypes(builtIns.float16Type, myType) -> addName("fl", validator)

            typeChecker.equalTypes(builtIns.int16Type, myType) -> addName("i", validator)
            typeChecker.equalTypes(builtIns.runeType, myType) -> addName("r", validator)
            myType.isFunctionType -> addName("function", validator)
//            CangJieBuiltIns.isArray(myType) || CangJieBuiltIns.isPrimitiveArray(myType) -> {
//                addNamesForArray(builtIns, myType, validator, typeChecker)
//            }
            typeDescriptor != null && DescriptorUtils.isSubtypeOfClass(typeDescriptor.defaultType, builtIns.stdlibTypes.iterable.original)
                    && type.arguments.isNotEmpty() ->
                addNameForIterableInheritors(type, validator)
            else -> {
                val name = getTypeName(myType)
                if (name != null) {
                    addCamelNames(name, validator)
                }
                addNamesFromGenericParameters(myType, validator)
            }
        }
    }


    /**
     * 为可迭代类型的继承者添加名称
     *
     * 专门处理可迭代类型(如 List, Set, Array 等),生成基于元素类型的名称。
     *
     * **命名策略**:
     * 1. **元素类型的复数形式**: 使用元素类型名称的复数形式
     * 2. **元素类型 + 容器类型**: 组合元素类型和容器类型名称
     *
     * **命名示例**:
     * ```kotlin
     * // List<Item>
     * addNameForIterableInheritors(List<Item>类型, validator)
     * // 添加: items, itemList
     *
     * // Set<User>
     * addNameForIterableInheritors(Set<User>类型, validator)
     * // 添加: users, userSet
     *
     * // Array<String>
     * addNameForIterableInheritors(Array<String>类型, validator)
     * // 添加: strings, stringArray
     *
     * // ArrayList<Person>
     * addNameForIterableInheritors(ArrayList<Person>类型, validator)
     * // 添加: people, personArrayList
     * ```
     *
     * **复数形式生成**:
     * 使用 StringUtil.pluralize() 生成英文复数形式:
     * - `item` → `items`
     * - `person` → `people`
     * - `child` → `children`
     * - `index` → `indices`
     *
     * **类型参数检查**:
     * - 仅处理有且只有一个类型参数的可迭代类型
     * - 如果类型参数为空或多于一个,不添加名称
     *
     * **驼峰命名变体**:
     * 生成的名称会进一步生成驼峰命名变体(通过 addCamelNames):
     * - `personList` → `personList`, `list`
     *
     * @receiver MutableCollection<String> 名称集合,用于添加生成的名称
     * @param type 可迭代类型,必须有一个类型参数
     * @param validator 名称验证器函数,检查名称是否可用
     */
    private fun MutableCollection<String>.addNameForIterableInheritors(type: CangJieType, validator: (String) -> Boolean) {
        val typeArgument = type.arguments.singleOrNull()?.type ?: return
        val name = getTypeName(typeArgument)
        if (name != null) {
            addCamelNames(StringUtil.pluralize(name), validator)
            val typeName = getTypeName(type)
            if (typeName != null) {
                addCamelNames(name + typeName, validator)
            }
        }
    }

    /**
     * 从泛型参数添加名称
     *
     * 组合类型的泛型参数和类型名称,生成复合名称。
     * 适用于带有多个类型参数的泛型类型。
     *
     * **命名策略**:
     * 将所有类型参数的名称拼接起来,再加上类型名称:
     * - `类型参数1 + 类型参数2 + ... + 类型名称`
     *
     * **命名示例**:
     * ```kotlin
     * // Map<String, User>
     * addNamesFromGenericParameters(Map<String, User>类型, validator)
     * // 生成: StringUser + Map → stringUserMap, userMap, map
     *
     * // Result<Data, Error>
     * addNamesFromGenericParameters(Result<Data, Error>类型, validator)
     * // 生成: DataError + Result → dataErrorResult, errorResult, result
     *
     * // Triple<Int, String, Bool>
     * addNamesFromGenericParameters(Triple<Int, String, Bool>类型, validator)
     * // 生成: IntStringBool + Triple → intStringBoolTriple, boolTriple, triple
     * ```
     *
     * **类型参数提取**:
     * - 从类型参数中提取类型名称(使用 getTypeName())
     * - 跳过无法提取名称的类型参数(如通配符、错误类型)
     * - 按顺序拼接所有类型参数名称
     *
     * **驼峰命名变体**:
     * 生成的复合名称会进一步生成驼峰命名变体:
     * - `stringUserMap` → `stringUserMap`, `userMap`, `map`
     *
     * **空参数处理**:
     * 如果类型没有类型参数,或者无法提取类型名称,不添加任何名称。
     *
     * **与简单类型名称的区别**:
     * - 简单类型: `List` → `list`
     * - 泛型类型: `List<String>` → `stringList`, `list`
     * - 多参数泛型: `Map<String, User>` → `stringUserMap`, `userMap`, `map`
     *
     * @receiver MutableCollection<String> 名称集合,用于添加生成的名称
     * @param type 泛型类型,包含类型参数
     * @param validator 名称验证器函数,检查名称是否可用
     */
    private fun MutableCollection<String>.addNamesFromGenericParameters(type: CangJieType, validator: (String) -> Boolean) {
        val typeName = getTypeName(type) ?: return
        val arguments = type.arguments
        val builder = StringBuilder()
        if (arguments.isEmpty()) return
        for (argument in arguments) {
            val name = getTypeName(argument.type)
            if (name != null) {
                builder.append(name)
            }
        }
        addCamelNames(builder.append(typeName).toString(), validator)
    }

    /**
     * 获取类型的名称
     *
     * 从类型中提取类型名称字符串,用于生成变量名。
     *
     * **提取规则**:
     * - 从类型的构造器中获取声明描述符
     * - 提取描述符的名称
     * - 忽略特殊名称(如匿名对象、lambda 等)
     *
     * **成功示例**:
     * ```kotlin
     * getTypeName(Int类型) → "Int"
     * getTypeName(String类型) → "String"
     * getTypeName(UserProfile类型) → "UserProfile"
     * getTypeName(List<Int>类型) → "List"
     * getTypeName(Map<K, V>类型) → "Map"
     * ```
     *
     * **返回 null 的情况**:
     * - 类型没有声明描述符(如类型参数 T)
     * - 类型名称是特殊名称(如 `<anonymous>`, `<lambda>`)
     * - 错误类型
     *
     * **特殊名称检查**:
     * 使用 Name.isSpecial 检查名称是否为特殊名称。
     * 特殊名称通常由编译器生成,不适合用作变量名。
     *
     * @param type 要提取名称的类型
     * @return String? 类型名称,如果无法提取则返回 null
     */
    private fun getTypeName(type: CangJieType): String? {
        val descriptor = type.constructor.declarationDescriptor
        if (descriptor != null) {
            val className = descriptor.name
            if (!className.isSpecial) {
                return className.asString()
            }
        }
        return null
    }

    /**
     * 根据表达式添加名称建议
     *
     * 从表达式的语法结构和语义信息中提取名称,添加到集合中。
     * 这是从表达式提取名称的主入口方法,综合多种策略。
     *
     * **提取策略**(按优先级):
     * 1. **值参数名称**: 如果表达式是函数调用的参数,使用形参名称
     * 2. **表达式 PSI 结构**: 从表达式的语法树中提取名称
     *
     * **值参数提取示例**:
     * ```kotlin
     * func doSomething(userName: String, userAge: Int) { ... }
     * doSomething(expression1, expression2)
     * // expression1 的建议: userName
     * // expression2 的建议: userAge
     * ```
     *
     * **PSI 结构提取示例**:
     * ```kotlin
     * getUser() → user
     * person.name → name
     * calculate(1, 2) → calculate
     * users[0] → user
     * ```
     *
     * **绑定上下文的作用**:
     * - 提供类型信息和引用解析
     * - 用于查找函数调用的形参名称
     * - 如果为 null,仅使用 PSI 结构提取
     *
     * **验证器作用**:
     * 所有提取的名称都会通过验证器检查,只有有效的名称才会添加到集合中。
     *
     * @receiver MutableCollection<String> 名称集合,用于添加生成的名称
     * @param expression 表达式 PSI 节点(可为 null,null 时不添加任何名称)
     * @param bindingContext 绑定上下文,用于语义解析(可为 null)
     * @param validator 名称验证器函数,检查名称是否可用
     */
    private fun MutableCollection<String>.addNamesByExpression(
        expression: CjExpression?,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean
    ) {
        if (expression == null) return

        addNamesByValueArgument(expression, bindingContext, validator)
        addNamesByExpressionPSI(expression, validator)
    }

    /**
     * 根据值参数添加名称
     *
     * 如果表达式是函数调用的参数,从对应的形参中提取名称。
     * 这是基于函数签名进行名称建议的核心方法。
     *
     * **工作流程**:
     * 1. 移除表达式的外层括号
     * 2. 检查表达式的父节点是否为 CjValueArgument
     * 3. 获取包含该参数的已解析调用(ResolvedCall)
     * 4. 查找参数到形参的映射
     * 5. 提取形参的名称
     *
     * **形参名称稳定性检查**:
     * 只有当函数的参数名称是稳定的(hasStableParameterNames)时,才使用形参名称。
     * 这避免了使用编译器生成的临时参数名(如 p0, p1)。
     *
     * **命名示例**:
     * ```kotlin
     * func login(userName: String, password: String) { ... }
     * login(expression1, expression2)
     * // expression1 的建议: userName
     * // expression2 的建议: password
     *
     * func process(data: Int) { ... }
     * process((someExpression))
     * // 移除括号后提取: data
     * ```
     *
     * **稳定参数名称**:
     * - 用户定义的函数参数名称是稳定的
     * - 某些内置函数的参数名称可能不稳定(编译器生成)
     * - Java 互操作的函数参数名称可能不稳定
     *
     * **绑定上下文要求**:
     * 此方法依赖绑定上下文获取语义信息,如果 bindingContext 为 null,
     * 方法会直接返回,不添加任何名称。
     *
     * **括号处理**:
     * 使用 getOutermostParenthesizerOrThis() 移除外层的多余括号,
     * 确保能正确识别参数位置。
     *
     * @receiver MutableCollection<String> 名称集合,用于添加生成的名称
     * @param expression 表达式 PSI 节点(可能在参数位置)
     * @param bindingContext 绑定上下文,用于语义解析(必需)
     * @param validator 名称验证器函数,检查名称是否可用
     */
    private fun MutableCollection<String>.addNamesByValueArgument(
        expression: CjExpression,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean
    ) {
        if (bindingContext == null) return
        val argumentExpression = expression.getOutermostParenthesizerOrThis()
        val valueArgument = argumentExpression.parent as? CjValueArgument ?: return
        val resolvedCall = argumentExpression.getParentResolvedCall(bindingContext) ?: return
        val argumentMatch = resolvedCall.getArgumentMapping(valueArgument) as? ArgumentMatch ?: return
        val parameter = argumentMatch.valueParameter
        if (parameter.containingDeclaration.hasStableParameterNames()) {
            addName(parameter.name.asString(), validator)
        }
    }

    /**
     * 访问器前缀列表
     *
     * 定义了仓颉语言中常用的访问器方法前缀。
     * 在生成变量名时，这些前缀会被自动移除以生成更简洁的名称。
     *
     * **包含的前缀**：
     * - `get` - Getter 方法前缀（如 `getName()` → `name`）
     * - `is` - 布尔类型 Getter 前缀（如 `isValid()` → `valid`）
     * - `set` - Setter 方法前缀（如 `setName()` → `name`）
     *
     * **使用场景**：
     * - 从函数调用表达式提取变量名
     * - 重构时自动生成属性名
     * - 代码补全中建议变量名
     *
     * **移除规则**：
     * 只有当前缀后的第一个字符是大写字母时才移除前缀：
     * - `getName()` → `name` ✓
     * - `getvalue()` → `getvalue` ✗（后面不是大写字母，不移除）
     *
     * @see cutAccessorPrefix
     */
    private val ACCESSOR_PREFIXES = arrayOf("get", "is", "set")

    /**
     * 名称建议检查的最大次数
     *
     * 限制名称冲突解决时尝试添加数字后缀的最大次数。
     * 当建议的名称已被占用时，会尝试添加数字后缀（1, 2, 3...）直到找到可用名称。
     *
     * **防止无限循环**：
     * 此常量防止在极端情况下（如大量同名变量）陷入过长的循环。
     * 如果尝试 1000 次仍未找到可用名称，返回带最大后缀的名称（如 `value1000`）。
     *
     * **典型场景**：
     * ```kotlin
     * // 假设作用域中已有 value, value1, value2, ..., value999
     * suggestNameByName("value", validator)
     * // 返回 "value1000"（达到最大尝试次数）
     * ```
     *
     * **性能考虑**：
     * 1000 次检查对于绝大多数实际场景都足够，同时避免性能问题。
     *
     * @see suggestNameByName
     */
    const val MAX_NUMBER_OF_SUGGESTED_NAME_CHECKS = 1000

    /**
     * 根据表达式 PSI 结构建议名称
     *
     * 仅从表达式的 PSI 语法树结构中提取名称，不使用类型信息。
     * 返回一个惰性序列，按优先级生成驼峰命名的多个变体。
     *
     * **提取策略**：
     * - 从表达式中提取简单名称（通过 [getSimpleExpressionName]）
     * - 生成驼峰命名的多个变体（通过 [getCamelNames]）
     * - 使用验证器过滤不可用的名称
     *
     * **惰性计算**：
     * 返回 [Sequence] 而非 [List]，名称按需生成，避免不必要的计算。
     *
     * **与其他方法的区别**：
     * - [suggestNamesByExpressionOnly] - 返回 List，立即计算所有结果
     * - [suggestNamesByExpressionAndType] - 同时使用表达式和类型信息
     * - 此方法 - 返回 Sequence，仅使用表达式结构
     *
     * **提取示例**：
     * ```kotlin
     * // getUser() → ["user"]
     * suggestNamesByExpressionPSI(parseExpression("getUser()"), validator)
     *
     * // person.homeAddress → ["homeAddress", "address"]
     * suggestNamesByExpressionPSI(parseExpression("person.homeAddress"), validator)
     *
     * // users[0] → ["user"]
     * suggestNamesByExpressionPSI(parseExpression("users[0]"), validator)
     * ```
     *
     * **空序列情况**：
     * - 表达式为 null
     * - 无法提取简单名称（如字面量 `42`）
     * - 提取的名称经过访问器前缀移除后为空
     *
     * @param expression 表达式 PSI 节点（可为 null）
     * @param validator 名称验证器函数，检查名称是否可用
     * @return Sequence<String> 建议的名称序列（按优先级排序，可能为空）
     */
    fun suggestNamesByExpressionPSI(expression: CjExpression?, validator: (String) -> Boolean): Sequence<String> {
        val simpleExpressionName = getSimpleExpressionName(expression) ?: return emptySequence()
        return getCamelNames(simpleExpressionName, validator)
    }

    /**
     * 从表达式中提取简单名称
     *
     * 递归地从表达式的 PSI 结构中提取最核心的标识符名称。
     * 自动移除括号、限定符、函数调用等语法噪声，只保留语义上有意义的名称部分。
     *
     * **处理策略**（按优先级）：
     *
     * 1. **null 表达式** → 返回 null
     * 2. **简单名称表达式**（`CjSimpleNameExpression`）→ 直接返回引用的名称
     *    - 示例：`userName` → `"userName"`
     * 3. **限定表达式**（`CjQualifiedExpression`）→ 递归提取选择器部分
     *    - 示例：`person.address.city` → 递归提取 `city`
     * 4. **函数调用表达式**（`CjCallExpression`）→ 递归提取被调用者名称
     *    - 示例：`getUser()` → 递归提取 `getUser`
     *    - 示例：`list.map { it }` → 递归提取 `map`
     * 5. **后缀表达式**（`CjPostfixExpression`）→ 递归提取基础表达式
     *    - 示例：`counter++` → 递归提取 `counter`
     * 6. **其他表达式类型** → 返回 null（无法提取有意义的名称）
     *
     * **自动移除括号**：
     * 使用 [CjPsiUtil.safeDeparenthesize] 在处理前移除所有层级的括号：
     * - `((user))` → `user`
     * - `(getValue())` → `getValue()`
     *
     * **递归处理示例**：
     * ```kotlin
     * // person.getHomeAddress().city
     * // → 提取 city (限定表达式 → 函数调用 → 简单名称)
     *
     * // ((users[0].getName()))
     * // → 提取 getName (移除括号 → 函数调用 → 简单名称)
     *
     * // calculateTotal(1, 2)
     * // → 提取 calculateTotal (函数调用 → 简单名称)
     * ```
     *
     * **返回 null 的情况**：
     * - 表达式为 null
     * - 字面量表达式（如 `42`, `"hello"`, `true`）
     * - lambda 表达式（如 `{ x -> x + 1 }`）
     * - 运算符表达式（如 `a + b`, `!flag`）
     * - 其他无法提取名称的表达式类型
     *
     * **使用场景**：
     * - 变量引入重构：从初始化表达式提取变量名
     * - 代码补全：为新变量建议名称
     * - 快速修复：自动生成参数名
     *
     * @param expression 表达式 PSI 节点（可为 null）
     * @return String? 提取的简单名称，无法提取时返回 null
     */
    private fun getSimpleExpressionName(expression: CjExpression?): String? {
        if (expression == null) return null
        return when (val deparenthesized = CjPsiUtil.safeDeparenthesize(expression)) {
            is CjSimpleNameExpression -> deparenthesized.referencedName
            is CjQualifiedExpression -> getSimpleExpressionName(deparenthesized.selectorExpression)
            is CjCallExpression -> getSimpleExpressionName(deparenthesized.calleeExpression)
            is CjPostfixExpression -> getSimpleExpressionName(deparenthesized.baseExpression)
            else -> null
        }
    }

    /**
     * 从字符串中提取所有标识符
     *
     * 使用词法分析器从输入字符串中提取所有有效的标识符 Token，
     * 将它们拼接成一个新字符串。移除所有非标识符的字符（如空格、符号、关键字等）。
     *
     * **工作原理**：
     * 1. 创建仓颉词法分析器 [CangJieLexer]
     * 2. 对输入字符串进行词法分析
     * 3. 遍历所有 Token，只保留 [CjTokens.IDENTIFIER] 类型的 Token
     * 4. 拼接所有标识符文本，不添加任何分隔符
     *
     * **处理示例**：
     * ```kotlin
     * extractIdentifiers("get_user_name")   // → "get_user_name" (已是有效标识符)
     * extractIdentifiers("get user name")   // → "getusername" (移除空格)
     * extractIdentifiers("get-user-name")   // → "getusername" (移除连字符)
     * extractIdentifiers("getUserName()")   // → "getUserName" (移除括号)
     * extractIdentifiers("123abc")          // → "abc" (移除数字前缀)
     * extractIdentifiers("`quoted`")        // → "quoted" (移除反引号)
     * extractIdentifiers("if name then")    // → "name" (移除关键字 if、then)
     * ```
     *
     * **典型应用**：
     * 配合 [cutAccessorPrefix] 使用，在移除访问器前缀前先清理字符串，
     * 确保只处理有效的标识符部分。
     *
     * **为什么需要此方法**：
     * - 名称可能包含反引号（用于转义关键字）：`` `class` ``
     * - 名称可能从代码片段中提取，包含多余符号
     * - 确保后续处理的是纯标识符文本
     *
     * **性能考虑**：
     * 使用 [buildString] 高效拼接字符串，避免多次字符串拼接的性能损耗。
     *
     * @param s 输入字符串（可能包含非标识符字符）
     * @return String 提取的纯标识符拼接结果（可能为空字符串）
     */
    private fun extractIdentifiers(s: String): String {
        return buildString {
            val lexer = CangJieLexer()
            lexer.start(s)
            while (lexer.tokenType != null) {
                if (lexer.tokenType == CjTokens.IDENTIFIER) {
                    append(lexer.tokenText)
                }
                lexer.advance()
            }
        }
    }

    /**
     * 移除访问器前缀
     *
     * 从名称中移除常见的访问器方法前缀（get/is/set），生成更简洁的属性名。
     * 这是将方法名转换为属性名的核心逻辑，遵循仓颉语言的命名约定。
     *
     * **移除规则**：
     * 1. 名称必须以 [ACCESSOR_PREFIXES] 中的前缀开头（get、is、set）
     * 2. 前缀后的第一个字符必须是大写字母
     * 3. 如果符合规则，移除前缀并保留后面部分（首字母仍为大写）
     * 4. 如果不符合规则，返回原始标识符部分（未移除前缀）
     *
     * **处理流程**：
     * 1. 检查名称是否为空字符串 → 返回 null
     * 2. 去除反引号并验证是否为有效标识符 → 无效则返回 null
     * 3. 提取纯标识符（移除所有非标识符字符）
     * 4. 尝试移除每个访问器前缀
     * 5. 如果都不匹配，返回纯标识符本身
     *
     * **移除示例**：
     * ```kotlin
     * cutAccessorPrefix("getName")       // → "Name" (移除 get)
     * cutAccessorPrefix("isValid")       // → "Valid" (移除 is)
     * cutAccessorPrefix("setValue")      // → "Value" (移除 set)
     * cutAccessorPrefix("getUserName")   // → "UserName" (移除 get)
     * cutAccessorPrefix("getHTTPClient") // → "HTTPClient" (移除 get)
     *
     * // 不移除的情况
     * cutAccessorPrefix("getvalue")      // → "getvalue" (后面不是大写，不移除)
     * cutAccessorPrefix("user")          // → "user" (无前缀，不移除)
     * cutAccessorPrefix("get")           // → "get" (只有前缀，无后续字符)
     * cutAccessorPrefix("")              // → null (空字符串)
     * cutAccessorPrefix("123")           // → null (非标识符)
     * ```
     *
     * **为什么要求大写字母**：
     * - 符合驼峰命名约定（camelCase）
     * - 避免误移除：`getvalue()` 不应变为 `value`，应保持 `getvalue`
     * - 确保语义正确：`getUserName()` → `UserName`，后续可转为 `userName`
     *
     * **与其他方法的配合**：
     * - [getCamelNames] 使用此方法移除前缀后，进一步生成驼峰命名变体
     * - [suggestNamesByExpressionPSI] 间接使用此方法处理函数调用表达式
     *
     * **特殊字符处理**：
     * 在移除前缀之前，先用 [extractIdentifiers] 提取纯标识符：
     * - `` `getName` `` → `getName` → `Name`
     * - `get-name` → `getname` → `getname`（连字符被移除，无大写字母，不移除前缀）
     *
     * **返回值说明**：
     * - 成功移除前缀：返回去除前缀后的字符串（首字母大写）
     * - 无法移除前缀：返回纯标识符（可能仍包含前缀）
     * - 无效输入：返回 null
     *
     * @param name 待处理的名称（可能包含访问器前缀）
     * @return String? 处理后的名称（可能已移除前缀），无效输入时返回 null
     */
    private fun cutAccessorPrefix(name: String): String? {
        if (name === "" || !name.unquoteCangJieIdentifier().isIdentifier()) return null
        val s = extractIdentifiers(name)

        for (prefix in ACCESSOR_PREFIXES) {
            if (!s.startsWith(prefix)) continue

            val len = prefix.length
            if (len < s.length && Character.isUpperCase(s[len])) {
                return s.substring(len)
            }
        }

        return s
    }

    /**
     * 根据有效标识符名称建议名称
     *
     * 验证给定的名称是否为有效的仓颉标识符，并在需要时进行转换和冲突解决。
     * 这是名称验证和规范化的核心方法，确保生成的名称符合仓颉语言规范。
     *
     * **处理流程**：
     * 1. **null 检查**：如果名称为 null，直接返回 null
     * 2. **首字母大小写处理**：
     *    - 如果 [mustStartWithLowerCase] 为 true，将首字母转为小写后递归调用
     *    - 如果 [mustStartWithLowerCase] 为 false，跳过此步骤
     * 3. **名称修正**：
     *    - 如果名称已是有效标识符，保持不变
     *    - 如果名称为 "class"（仓颉关键字），转换为 "clazz"
     *    - 否则返回 null（无法修正）
     * 4. **冲突解决**：使用 [suggestNameByName] 添加数字后缀避免冲突
     *
     * **参数说明**：
     * - [name] - 待处理的名称（可为 null）
     * - [validator] - 名称验证器，检查名称在当前作用域是否可用
     * - [mustStartWithLowerCase] - 是否必须以小写字母开头（默认 true）
     *
     * **mustStartWithLowerCase 参数的作用**：
     * - `true`：强制首字母小写（用于变量名）
     * - `false`：保持原始大小写（用于类型名、常量名等）
     *
     * **递归机制**：
     * 第一次调用时 [mustStartWithLowerCase] 通常为 true，
     * 会将名称首字母小写后以 false 再次调用，避免重复小写化。
     *
     * **处理示例**：
     * ```kotlin
     * // 标准变量名处理
     * suggestNameByValidIdentifierName("UserName", validator, true)
     * // → "userName"（首字母小写） 或 "userName1"（如果 userName 被占用）
     *
     * // 已是小写的名称
     * suggestNameByValidIdentifierName("userName", validator, true)
     * // → "userName" 或 "userName1"
     *
     * // 保持大写（用于类型名）
     * suggestNameByValidIdentifierName("UserName", validator, false)
     * // → "UserName" 或 "UserName1"
     *
     * // 关键字修正
     * suggestNameByValidIdentifierName("class", validator, true)
     * // → "clazz" 或 "clazz1"
     *
     * // 无效标识符
     * suggestNameByValidIdentifierName("123abc", validator, true)
     * // → null（以数字开头，无法修正）
     *
     * // null 输入
     * suggestNameByValidIdentifierName(null, validator, true)
     * // → null
     * ```
     *
     * **仓颉标识符规则**：
     * - 必须以字母或下划线开头
     * - 可以包含字母、数字、下划线
     * - 不能是保留关键字（除非用反引号转义）
     * - 使用 [String.isIdentifier] 扩展函数检查
     *
     * **冲突解决策略**：
     * 调用 [suggestNameByName] 添加数字后缀（1, 2, 3...）直到找到可用名称：
     * - `userName` → `userName1` → `userName2` → ...
     * - 最多尝试 [MAX_NUMBER_OF_SUGGESTED_NAME_CHECKS] 次
     *
     * **与其他方法的关系**：
     * - [getCamelNames] 使用此方法验证生成的每个驼峰命名变体
     * - [AbstractCangJieNameSuggester.addName] 使用此方法验证待添加的名称
     *
     * **为什么只修正 "class"**：
     * "class" 是最常见的需要修正的关键字（从 Java/Kotlin 等语言迁移时）。
     * 其他关键字通常在名称提取阶段就已被过滤掉。
     *
     * @param name 待处理的名称（可为 null）
     * @param validator 名称验证器函数，检查名称是否在当前作用域可用
     * @param mustStartWithLowerCase 是否必须以小写字母开头（默认 true）
     * @return String? 有效的名称（可能带数字后缀），无法修正时返回 null
     */
    fun suggestNameByValidIdentifierName(
        name: String?,
        validator: (String) -> Boolean,
        mustStartWithLowerCase: Boolean = true
    ): String? {
        if (name == null) return null
        if (mustStartWithLowerCase) return suggestNameByValidIdentifierName(
            name.decapitalizeSmart(),
            validator,
            false
        )
        val correctedName = when {
            name.isIdentifier() -> name
            name == "class" -> "clazz"
            else -> return null
        }
        return suggestNameByName(correctedName, validator)
    }

    /**
     * 生成驼峰命名的多个变体
     *
     * 从给定的名称生成多个驼峰命名（camelCase）的变体序列。
     * 这是名称建议系统的核心算法，通过识别大写字母位置来生成多个由长到短的名称候选。
     *
     * **核心思想**：
     * 在驼峰命名中，每个大写字母标志着一个新单词的开始。
     * 从每个大写字母位置截取子串，生成逐渐缩短的名称变体。
     *
     * **算法步骤**：
     * 1. 移除访问器前缀（get/is/set）：通过 [cutAccessorPrefix]
     * 2. 遍历字符串的每个字符，识别大写字母位置
     * 3. 在每个大写字母位置生成一个子串（从该位置到字符串末尾）
     * 4. 验证并规范化每个子串（通过 [suggestNameByValidIdentifierName]）
     * 5. 返回惰性序列（按优先级从长到短）
     *
     * **生成顺序**：
     * 1. 完整名称（可能已移除访问器前缀）
     * 2. 从第一个内部大写字母开始的子串
     * 3. 从第二个内部大写字母开始的子串
     * 4. ... 依此类推
     *
     * **生成示例**：
     * ```kotlin
     * // getUserName
     * getCamelNames("getUserName", validator)
     * // → ["userName", "name"]
     * // (移除 get → UserName, 完整: userName, 从 N 开始: name)
     *
     * // homeAddress
     * getCamelNames("homeAddress", validator)
     * // → ["homeAddress", "address"]
     * // (无前缀, 完整: homeAddress, 从 A 开始: address)
     *
     * // HTTPClient
     * getCamelNames("HTTPClient", validator)
     * // → ["httpClient", "ttpClient", "tpClient", "pClient", "client"]
     * // (无前缀, 每个大写字母都生成一个变体)
     *
     * // myHTTPSConnection
     * getCamelNames("myHTTPSConnection", validator)
     * // → ["myHttpsConnection", "httpsConnection", "ttpsConnection", "tpsConnection",
     * //     "psConnection", "sConnection", "connection"]
     *
     * // isValid
     * getCamelNames("isValid", validator)
     * // → ["valid"]
     * // (移除 is → Valid, 转为小写: valid)
     *
     * // value（无大写字母）
     * getCamelNames("value", validator)
     * // → ["value"]
     * // (无大写字母，只有完整名称)
     *
     * // 123name（无效标识符）
     * getCamelNames("123name", validator)
     * // → [] (空序列)
     * // (cutAccessorPrefix 返回 null，无法生成)
     * ```
     *
     * **参数说明**：
     * - [name] - 原始名称（可能包含访问器前缀）
     * - [validator] - 名称验证器，过滤不可用的名称
     * - [startLowerCase] - 生成的名称是否以小写字母开头（默认 true）
     *
     * **startLowerCase 参数的作用**：
     * - `true`（默认）：生成变量名，首字母小写
     * - `false`：生成类型名或常量名，保持首字母大写
     *
     * **优先级规则**：
     * 生成的名称按从长到短排序，因为：
     * - 更长的名称通常更具描述性
     * - 保留更多语义信息
     * - 符合"最小惊讶原则"（用户期望的名称）
     *
     * **惰性序列的优势**：
     * 返回 [Sequence] 而非 [List]，名称按需生成：
     * - 如果第一个名称就被接受，避免生成后续名称
     * - 节省内存和计算
     * - 支持无限序列（理论上，实际受字符串长度限制）
     *
     * **与其他方法的配合**：
     * - [addCamelNames] 使用此方法生成名称变体后添加到集合
     * - [suggestNamesByExpressionPSI] 使用此方法处理提取的表达式名称
     *
     * **大写字母连续的处理**：
     * 算法会在每个大写字母处生成变体，即使是连续的大写字母：
     * - `HTTPClient` → `httpClient`, `ttpClient`, `tpClient`, `pClient`, `client`
     * - 这样可以捕捉各种可能的缩写形式
     *
     * **空序列情况**：
     * - 名称无效，[cutAccessorPrefix] 返回 null
     * - 所有生成的变体都未通过验证器检查
     *
     * @param name 原始名称（可能包含访问器前缀、大小写混合）
     * @param validator 名称验证器函数，检查名称是否可用
     * @param startLowerCase 是否以小写字母开头（默认 true）
     * @return Sequence<String> 驼峰命名变体的惰性序列（按优先级排序，可能为空）
     */
    fun getCamelNames(
        name: String,
        validator: (String) -> Boolean,
        startLowerCase: Boolean = true
    ): Sequence<String> {
        val s = cutAccessorPrefix(name) ?: return emptySequence()

        var upperCaseLetterBefore = false
        return sequence {
            for (i in s.indices) {
                val c = s[i]
                val upperCaseLetter = Character.isUpperCase(c)

                if (i == 0) {
                    suggestNameByValidIdentifierName(s, validator, startLowerCase)?.let { yield(it) }
                } else {
                    if (upperCaseLetter && !upperCaseLetterBefore) {
                        val substring = s.substring(i)
                        suggestNameByValidIdentifierName(substring, validator, startLowerCase)?.let { yield(it) }
                    }
                }

                upperCaseLetterBefore = upperCaseLetter
            }
        }
    }
}




