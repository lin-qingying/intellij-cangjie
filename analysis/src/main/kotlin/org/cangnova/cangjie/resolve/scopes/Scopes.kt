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
import org.cangnova.cangjie.utils.Printer

/**
 * 层次化作用域接口
 *
 * 表示具有父子层次结构的作用域,支持向父作用域递归查找声明。
 * 继承自 [ResolutionScope],提供符号解析的基础能力。
 */
interface HierarchicalScope : ResolutionScope {
    /**
     * 父作用域
     *
     * 当前作用域的上级作用域,如果为 null 表示已经到达作用域链的顶部。
     */
    val parent: HierarchicalScope?

    /**
     * 打印作用域结构
     *
     * 用于调试和诊断,将作用域的结构信息输出到打印器中。
     *
     * @param p 打印器,用于输出结构信息
     */
    fun printStructure(p: Printer)
}

/**
 * 词法作用域类型枚举
 *
 * 定义了仓颉语言中各种语法结构对应的词法作用域类型。
 *
 * @property withLocalDescriptors 该作用域是否包含本地描述符(如局部变量、参数等)
 */
enum class LexicalScopeKind(val withLocalDescriptors: Boolean) {
    /** 空作用域 */
    EMPTY(false),
    /** 抛出语句作用域 */
    THROWING(false),

    /** 类头部作用域 */
    CLASS_HEADER(false),
    /** 类继承声明作用域 */
    CLASS_INHERITANCE(false),
    /** 构造器头部作用域 */
    CONSTRUCTOR_HEADER(false),
    /** 类静态作用域 */
    CLASS_STATIC_SCOPE(false),
    /** 类成员作用域 */
    CLASS_MEMBER_SCOPE(false),

    /** 默认值作用域 */
    DEFAULT_VALUE(true),

    /** 属性头部作用域 */
    PROPERTY_HEADER(false),
    /** 变量初始化器或委托作用域 */
    VARIABLE_INITIALIZER_OR_DELEGATE(true),
    /** 属性访问器主体作用域 */
    PROPERTY_ACCESSOR_BODY(true),
    /** 属性委托方法作用域 */
    PROPERTY_DELEGATE_METHOD(false),
    /** extend 头部作用域 */
    EXTEND_HEADER(false),

    /** 函数头部作用域 */
    FUNCTION_HEADER(false),
    /** 解构用函数头部作用域 */
    FUNCTION_HEADER_FOR_DESTRUCTURING(false),
    /** 函数内部作用域 */
    FUNCTION_INNER_SCOPE(true),

    /** 类型别名头部作用域 */
    TYPE_ALIAS_HEADER(false),

    /** 代码块作用域 */
    CODE_BLOCK(true),

    /** 布尔表达式左侧作用域 */
    LEFT_BOOLEAN_EXPRESSION(true),
    /** 布尔表达式右侧作用域 */
    RIGHT_BOOLEAN_EXPRESSION(true),
    /** while 循环作用域 */
    WHILE(true),

    /** then 分支作用域 */
    THEN(true),
    /** else 分支作用域 */
    ELSE(true),
    /** do-while 循环主体作用域 */
    DO_WHILE_BODY(true),
    /** catch 块作用域 */
    CATCH(true),
    /** try 块作用域 */
    TRY(true),
    /** if 语句作用域 */
    IF(true),

    /** for 循环作用域 */
    FOR(true),
    /** while 循环主体作用域 */
    WHILE_BODY(true),
    /** match 表达式作用域 */
    MATCH(true),
    /** match 分支作用域 */
    MATCH_CASE(true),

    /** 可调用引用作用域 */
    CALLABLE_REFERENCE(false),

    /** 合成作用域(用于测试、文档注释和 IDE) */
    SYNTHETIC(false)
}

/**
 * 层次化作用域的基础抽象类
 *
 * 提供 [HierarchicalScope] 接口的默认实现,所有方法默认返回空集合或 null。
 * 子类可以根据需要覆盖特定方法以提供实际的符号解析功能。
 *
 * @property parent 父作用域,可为 null
 */
abstract class BaseHierarchicalScope(override val parent: HierarchicalScope?) : HierarchicalScope {
    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> = emptyList()

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null


    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> =
        emptyList()

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
        emptyList()

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        return emptyList()
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> =
        emptyList()
}

/**
 * 词法作用域接口
 *
 * 表示仓颉语言中的词法作用域,如函数体、代码块、类成员等。
 * 词法作用域与声明的嵌套关系一致,支持隐式接收者和上下文接收者。
 */
interface LexicalScope : HierarchicalScope {
    /**
     * 父作用域
     *
     * 词法作用域必定有父作用域(非 null)。
     */
    override val parent: HierarchicalScope

    /**
     * 作用域所有者描述符
     *
     * 标识创建此作用域的声明,如函数、类等。
     */
    val ownerDescriptor: DeclarationDescriptor

    /**
     * 所有者描述符是否可通过标签访问
     *
     * 例如在 lambda 中可以通过标签引用外层函数。
     */
    val isOwnerDescriptorAccessibleByLabel: Boolean

    /**
     * 隐式接收者
     *
     * 在类成员或扩展函数中,this 对应的接收者参数描述符。
     */
    val implicitReceiver: ReceiverParameterDescriptor?

     /**
     * 作用域类型
     *
     * 标识该词法作用域对应的语法结构类型。
     */
    val kind: LexicalScopeKind

    /**
     * 添加变量描述符
     *
     * 在支持本地描述符的作用域中注册局部变量。
     * 默认实现为空,子类可根据需要覆盖。
     *
     * @param variableDescriptor 要添加的变量描述符
     */
    fun addVariableDescriptor(variableDescriptor: VariableDescriptor) {

    }

    /**
     * 基础词法作用域实现
     *
     * 提供 [LexicalScope] 的简单实现,作为创建词法作用域的基础。
     *
     * @property parent 父作用域,必须非 null
     * @property ownerDescriptor 作用域所有者描述符
     */
    class Base(
        parent: HierarchicalScope,
        override val ownerDescriptor: DeclarationDescriptor
    ) : BaseHierarchicalScope(parent), LexicalScope {
        override val parent: HierarchicalScope
            get() = super.parent!!

        override val isOwnerDescriptorAccessibleByLabel: Boolean
            get() = false

        override val implicitReceiver: ReceiverParameterDescriptor?
            get() = null

        override val kind: LexicalScopeKind
            get() = LexicalScopeKind.EMPTY

        override fun printStructure(p: Printer) {
            p.println("Base lexical scope with owner = $ownerDescriptor and parent = $parent")
        }


    }
}

/**
 * 导入作用域接口
 *
 * 表示通过 import 语句引入的作用域,用于解析导入的符号和包。
 * 与词法作用域不同,导入作用域主要关注跨包和跨模块的符号可见性。
 */
interface ImportingScope : HierarchicalScope {
    /**
     * 父导入作用域
     *
     * 可为 null,表示没有更外层的导入作用域。
     */
    override val parent: ImportingScope?

    /**
     * 获取导入的包
     *
     * 根据名称查找通过 import 语句导入的包视图描述符。
     *
     * @param name 包名
     * @return 包视图描述符,如果未找到则返回 null
     */
    fun getContributedPackage(name: Name): PackageViewDescriptor?

    /**
     * 获取贡献的描述符
     *
     * 重载版本,支持控制是否更改别名的名称。
     *
     * @param kindFilter 描述符类型过滤器
     * @param nameFilter 名称过滤函数
     * @param changeNamesForAliased 是否为别名声明更改名称
     * @return 符合条件的声明描述符集合
     */
    fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter = DescriptorKindFilter.ALL,
        nameFilter: (Name) -> Boolean = MemberScope.ALL_NAME_FILTER,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor>

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return getContributedDescriptors(kindFilter, nameFilter, changeNamesForAliased = false)
    }

    /**
     * 计算导入的名称集合
     *
     * 返回该导入作用域中所有导入的符号名称。
     *
     * @return 导入的名称集合,如果无法确定则返回 null
     */
    fun computeImportedNames(): Set<Name>?

    /**
     * 空导入作用域
     *
     * 表示不包含任何导入的空作用域,用作导入作用域链的终点。
     */
    object Empty : BaseImportingScope(null) {
        override fun printStructure(p: Printer) {
            p.println("ImportingScope.Empty")
        }

        override fun computeImportedNames() = emptySet<Name>()

        override fun definitelyDoesNotContainName(name: Name) = true
    }
}

/**
 * 导入作用域的基础抽象类
 *
 * 提供 [ImportingScope] 接口的默认实现,作为自定义导入作用域的基类。
 * 所有方法默认返回空集合或 null。
 *
 * @param parent 父导入作用域,可为 null
 */
abstract class BaseImportingScope(parent: ImportingScope?) : BaseHierarchicalScope(parent), ImportingScope {
    override val parent: ImportingScope?
        get() = super.parent as ImportingScope?

    override fun getContributedPackage(name: Name): PackageViewDescriptor? = null



    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return getContributedDescriptors(kindFilter, nameFilter, changeNamesForAliased = false)
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor> = emptyList()
}

/**
 * 处理当前作用域及其所有父作用域
 *
 * 从当前作用域开始,向上遍历整个作用域链,对每个作用域执行指定的处理函数。
 *
 * @param process 对每个作用域执行的处理函数
 */
inline fun HierarchicalScope.processForMeAndParent(process: (HierarchicalScope) -> Unit) {
    var currentScope = this
    while (true) {
        process(currentScope)
        currentScope = currentScope.parent ?: break
    }
}

/**
 * 从当前作用域及其父作用域收集列表
 *
 * 遍历作用域链,对每个作用域调用 [fetch] 函数获取列表,并将所有非空结果合并。
 *
 * @param T 列表元素类型
 * @param fetch 从作用域提取列表的函数
 * @return 合并后的列表
 */
inline fun <T : Any> HierarchicalScope.getListFromMeAndParent(fetch: (HierarchicalScope) -> List<T>?): List<T> {
    val result = mutableListOf<T>()
    processForMeAndParent { fetch(it)?.let { result.addAll(it) } }

    return result
}

/**
 * 从当前作用域及其父作用域查找第一个匹配项
 *
 * 遍历作用域链,对每个作用域调用 [fetch] 函数,返回第一个非 null 的结果。
 *
 * @param T 结果类型
 * @param fetch 从作用域提取结果的函数
 * @return 第一个非 null 的结果,如果都为 null 则返回 null
 */
inline fun <T : Any> HierarchicalScope.findFirstFromMeAndParent(fetch: (HierarchicalScope) -> T?): T? {
    processForMeAndParent { fetch(it)?.let { return it } }
    return null
}


/**
 * 复合优先级导入作用域
 *
 * 将两个导入作用域组合成一个,并按优先级查找符号。
 * 首先在主作用域中查找,如果未找到则在次级作用域中查找。
 *
 * 用于实现多层导入策略,如文件导入覆盖包导入。
 *
 * @property primaryScope 主作用域,优先级较高
 * @property secondaryScope 次级作用域,优先级较低
 */
class CompositePrioritizedImportingScope(
    private val primaryScope: ImportingScope,
    private val secondaryScope: ImportingScope,
) : ImportingScope {
    override val parent: ImportingScope?
        get() = primaryScope.parent ?: secondaryScope.parent

    override fun getContributedPackage(name: Name): PackageViewDescriptor? {
        return primaryScope.getContributedPackage(name) ?: secondaryScope.getContributedPackage(name)
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor> {
        return primaryScope.getContributedDescriptors(kindFilter, nameFilter, changeNamesForAliased).union(
            secondaryScope.getContributedDescriptors(kindFilter, nameFilter, changeNamesForAliased)
        )
    }


    override fun computeImportedNames(): Set<Name>? {
        val primaryNames = primaryScope.computeImportedNames()
        val secondaryNames = secondaryScope.computeImportedNames()
        return primaryNames?.union(secondaryNames.orEmpty()) ?: secondaryNames
    }

    override fun printStructure(p: Printer) {
        p.println(primaryScope::class.java.simpleName)
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return primaryScope.getContributedClassifier(name, location) ?: secondaryScope.getContributedClassifier(
            name,
            location
        )
    }

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> {
        return primaryScope.getContributedVariables(name, location).union(
            secondaryScope.getContributedVariables(name, location)
        )
    }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        return primaryScope.getContributedPropertys(name, location).union(
            secondaryScope.getContributedPropertys(name, location)
        )
    }

    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {
        return primaryScope.getContributedMacros(name, location).union(
            secondaryScope.getContributedMacros(name, location)
        )
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        return primaryScope.getContributedFunctions(name, location).union(
            secondaryScope.getContributedFunctions(name, location)
        )
    }


}
