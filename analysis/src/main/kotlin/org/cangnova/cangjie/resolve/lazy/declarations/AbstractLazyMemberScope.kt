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

package org.cangnova.cangjie.resolve.lazy.declarations

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.builtins.StandardNames.FqNames.core
import org.cangnova.cangjie.builtins.StandardNames.MAIN
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.data.CjClassInfoUtil
import org.cangnova.cangjie.descriptors.data.CjTypeStatementInfo
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.findParentOfType
import org.cangnova.cangjie.psi.stubs.elements.getAllBindings
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.components.InferenceSession
import org.cangnova.cangjie.resolve.lazy.LazyClassContext
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyClassDescriptor
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyClassMemberScope
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.source.MemberScopeImpl
import org.cangnova.cangjie.storage.MemoizedFunctionToNotNull
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.utils.Printer

/**
 * 抽象延迟成员作用域
 *
 * 为类、接口、扩展等提供统一的成员解析框架。
 * 使用延迟求值策略，避免一次性解析所有成员，提升性能。
 *
 * @param D 描述符类型（ClassDescriptor、ExtendDescriptor 等）
 * @param DP 声明提供者类型（ClassMemberDeclarationProvider 等）
 * @param c 延迟类上下文，提供解析器、存储管理器等基础设施
 * @param declarationProvider 声明提供者，用于获取 PSI 声明
 * @param thisDescriptor 当前作用域所属的描述符
 * @param trace 绑定跟踪器，用于记录 PSI 到描述符的映射
 * @param mainScope 主作用域（用于多文件类等场景）
 */
abstract class AbstractLazyMemberScope<out D : DeclarationDescriptor, out DP : DeclarationProvider>
protected constructor(
    protected val c: LazyClassContext,
    protected val declarationProvider: DP,
    protected val thisDescriptor: D,
    protected val trace: BindingTrace,
    protected val mainScope: AbstractLazyMemberScope<D, DP>? = null
) : MemberScopeImpl() {
    /** 存储管理器，用于创建延迟计算值 */
    protected val storageManager: StorageManager = c.storageManager

    // ===== 缓存的描述符集合（通过 MemoizedFunction 实现延迟计算） =====

    /** 函数描述符缓存（包含声明的和继承的） */
    private val functionDescriptors: MemoizedFunctionToNotNull<Name, Collection<SimpleFunctionDescriptor>> =
        storageManager.createMemoizedFunction { doGetFunctions(it) }

    /** main 函数描述符缓存 */
    private val mainFunctionDescriptors: MemoizedFunctionToNotNull<Name, Collection<SimpleFunctionDescriptor>> =
        storageManager.createMemoizedFunction { doGetMainFunctions() }

    /** 类描述符缓存 */
    private val classDescriptors: MemoizedFunctionToNotNull<Name, List<ClassDescriptor>> =
        storageManager.createMemoizedFunction { doGetClasses(it) }

    /** 宏描述符缓存 */
    private val macroDescriptors: MemoizedFunctionToNotNull<Name, Collection<MacroDescriptor>> =
        storageManager.createMemoizedFunction { doGetMacros(it) }

    /** 属性描述符缓存 */
    private val propertyDescriptors: MemoizedFunctionToNotNull<Name, Collection<PropertyDescriptor>> =
        storageManager.createMemoizedFunction { doGetProperties(it) }

    /** 变量描述符缓存 */
    private val variableDescriptors: MemoizedFunctionToNotNull<Name, Collection<VariableDescriptor>> =
        storageManager.createMemoizedFunction { doGetVariables(it) }

    // ===== 仅包含声明的描述符缓存（不含继承的） =====

    /** 声明的 main 函数描述符 */
    private val declaredMainFunctionDescriptors: MemoizedFunctionToNotNull<Name, Collection<SimpleFunctionDescriptor>> =
        storageManager.createMemoizedFunction { getMainDeclaredFunctions() }

    /** 声明的函数描述符 */
    private val declaredFunctionDescriptors: MemoizedFunctionToNotNull<Name, Collection<SimpleFunctionDescriptor>> =
        storageManager.createMemoizedFunction { getDeclaredFunctions(it) }

    /** 声明的宏描述符 */
    private val declaredMacroDescriptors: MemoizedFunctionToNotNull<Name, Collection<MacroDescriptor>> =
        storageManager.createMemoizedFunction { getDeclaredMacros(it) }

    /** 类型别名描述符（支持递归调用处理） */
    private val typeAliasDescriptors: MemoizedFunctionToNotNull<Name, Collection<TypeAliasDescriptor>> =
        storageManager.createMemoizedFunction({ doGetTypeAliases(it) }, onRecursiveCall = { _, _ -> emptyList() })

    /** 声明的属性描述符 */
    private val declaredPropertyDescriptors: MemoizedFunctionToNotNull<Name, Collection<PropertyDescriptor>> =
        storageManager.createMemoizedFunction { getDeclaredProperties(it) }

    /** 声明的变量描述符 */
    private val declaredVariableDescriptors: MemoizedFunctionToNotNull<Name, Collection<VariableDescriptor>> =
        storageManager.createMemoizedFunction { getDeclaredVariables(it) }

    /**
     * 获取指定名称的类型别名描述符
     *
     * @param name 类型别名名称
     * @return 类型别名描述符集合
     */
    private fun doGetTypeAliases(name: Name): Collection<TypeAliasDescriptor> {
        // 如果有主作用域，从主作用域获取
        mainScope?.typeAliasDescriptors?.invoke(name)?.let { return it }

        // 解析类型别名声明
        return declarationProvider.getTypeAliasDeclarations(name).map { cjTypeAlias ->
            c.descriptorResolver.resolveTypeAliasDescriptor(
                thisDescriptor,
                getScopeForMemberDeclarationResolution(cjTypeAlias),
                cjTypeAlias,
                trace
            )
        }.toList()
    }

    /**
     * 获取初始化器解析作用域
     *
     * 用于解析属性和变量的初始化表达式
     *
     * @param declaration 声明元素
     * @return 词法作用域
     */
    protected abstract fun getScopeForInitializerResolution(declaration: CjDeclaration): LexicalScope

    /**
     * 获取声明的属性描述符（不包含继承的）
     *
     * @param name 属性名称
     * @return 属性描述符集合
     */
    private fun getDeclaredProperties(
        name: Name
    ): Collection<PropertyDescriptor> {

        // TODO: 是否真的需要复制描述符？
        if (mainScope != null) return mainScope.declaredPropertyDescriptors(name).map {
            it.newCopyBuilder().setPreserveSourceElement().build()!!
        }
        val result = LinkedHashSet<PropertyDescriptor>()
        val propDeclarations = declarationProvider.getPropertyDeclarations(name)
        for (propertyDeclaration in propDeclarations) {
            val propertyDescriptor = c.descriptorResolver.resolvePropertyDescriptor(
                thisDescriptor,
                getScopeForMemberDeclarationResolution(propertyDeclaration),
                getScopeForInitializerResolution(propertyDeclaration),
                propertyDeclaration,
                trace,
                c.declarationScopeProvider.getOuterDataFlowInfoForDeclaration(propertyDeclaration),
                c.inferenceSession ?: InferenceSession.default
            )
            result.add(propertyDescriptor)
        }


        return result
    }

    /**
     * 获取声明的变量描述符（不包含继承的）
     *
     * 支持三种变量声明形式：
     * 1. 字段变量 (CjFieldVariable)
     * 2. 模式变量 (CjPatternVariable) - 支持解构绑定
     * 3. 其他变量类型
     *
     * @param name 变量名称
     * @return 变量描述符集合
     */
    private fun getDeclaredVariables(
        name: Name
    ): Collection<VariableDescriptor> {

        if (mainScope != null) return mainScope.declaredPropertyDescriptors(name).map {
            it.newCopyBuilder().setPreserveSourceElement().build()!!
        }
        val result = LinkedHashSet<VariableDescriptor>()

        val declarations = declarationProvider.getVariableDeclarations(name)
        for (variableDeclaration in declarations) {
            when (variableDeclaration) {
                // 字段变量
                is CjFieldVariable -> {
                    val variableDescriptor = c.descriptorResolver.resolveVariableDescriptor(
                        thisDescriptor,
                        getScopeForMemberDeclarationResolution(variableDeclaration),
                        getScopeForInitializerResolution(variableDeclaration),
                        variableDeclaration,
                        trace,
                        c.declarationScopeProvider.getOuterDataFlowInfoForDeclaration(variableDeclaration),
                        c.inferenceSession ?: InferenceSession.default
                    )
                    result.add(variableDescriptor)
                }

                // 模式变量（支持解构绑定）
                is CjPatternVariable -> {
                    if (variableDeclaration.pattern != null) {
                        // 从模式中提取所有绑定的变量
                        val variableDescriptors = c.descriptorResolver.resolveVariableDescriptorByPattern(
                            name,
                            thisDescriptor,
                            getScopeForMemberDeclarationResolution(variableDeclaration),
                            variableDeclaration,
                            trace,
                            c.declarationScopeProvider.getOuterDataFlowInfoForDeclaration(variableDeclaration),
                            c.inferenceSession ?: InferenceSession.default
                        )
                        result.addAll(variableDescriptors)
                    } else {
                        // 无模式的变量声明，直接解析
                        val variableDescriptor = c.descriptorResolver.resolveVariableDescriptor(
                            thisDescriptor,
                            getScopeForMemberDeclarationResolution(variableDeclaration),
                            getScopeForInitializerResolution(variableDeclaration),
                            variableDeclaration,
                            trace,
                            c.declarationScopeProvider.getOuterDataFlowInfoForDeclaration(variableDeclaration),
                            c.inferenceSession ?: InferenceSession.default
                        )
                        result.add(variableDescriptor)
                    }
                }

                else -> {
                    val variableDescriptor = c.descriptorResolver.resolveVariableDescriptor(
                        thisDescriptor,
                        getScopeForMemberDeclarationResolution(variableDeclaration),
                        getScopeForInitializerResolution(variableDeclaration),
                        variableDeclaration,
                        trace,
                        c.declarationScopeProvider.getOuterDataFlowInfoForDeclaration(variableDeclaration),
                        c.inferenceSession ?: InferenceSession.default
                    )
                    result.add(variableDescriptor)
                }
            }
        }
        return result

    }


    /**
     * 获取属性描述符（包含声明的和继承的）
     *
     * @param name 属性名称
     * @return 属性描述符集合
     */
    private fun doGetProperties(name: Name): Collection<PropertyDescriptor> {

        // 这里返回变量声明和属性声明
        val result = LinkedHashSet(declaredPropertyDescriptors(name))

        // 添加非声明的属性（继承的、合成的等）
        getNonDeclaredProperties(name, result)

        return result.toList()
    }


    /**
     * 获取变量描述符（包含声明的和继承的）
     *
     * @param name 变量名称
     * @return 变量描述符集合
     */
    private fun doGetVariables(name: Name): Collection<VariableDescriptor> {
        val result = LinkedHashSet(declaredVariableDescriptors(name))

        // 添加非声明的变量（继承的、合成的等）
        getNonDeclaredVariables(name, result)

        return result.toList()
    }


    /**
     * 获取非声明的属性（由子类实现以添加继承的或合成的属性）
     *
     * @param name 属性名称
     * @param result 结果集合（包含已声明的属性）
     */
    protected abstract fun getNonDeclaredProperties(name: Name, result: MutableSet<PropertyDescriptor>)

    /**
     * 获取非声明的变量（由子类实现以添加继承的或合成的变量）
     *
     * @param name 变量名称
     * @param result 结果集合（包含已声明的变量）
     */
    protected abstract fun getNonDeclaredVariables(name: Name, result: MutableSet<VariableDescriptor>)

    /**
     * 获取贡献的分类器（类、接口、类型别名等）
     *
     * @param name 分类器名称
     * @param location 查找位置
     * @return 分类器描述符列表
     */
    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> {
        recordLookup(name, location)
        // 注意：即使存在同名的类描述符，也应该解析类型别名描述符
        val classes = classDescriptors(name)
        val typeAliases = typeAliasDescriptors(name)

        return classes + typeAliases
    }


    /**
     * 获取贡献的包视图
     *
     * 成员作用域不包含包，返回 null
     *
     * @param name 包名
     * @param location 查找位置
     * @return 始终返回 null
     */
    override fun getContributedPackageView(name: Name, location: LookupLocation): PackageViewDescriptor? {
        return null
    }

    /**
     * 获取单个贡献的分类器
     *
     * 优先返回类描述符，如果没有则返回类型别名
     *
     * @param name 分类器名称
     * @param location 查找位置
     * @return 分类器描述符或 null
     */
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        recordLookup(name, location)
        // 注意：即使存在同名的类描述符，也应该解析类型别名描述符
        val classes = classDescriptors(name)
        val typeAliases = typeAliasDescriptors(name)
        // 参见 getFirstClassifierDiscriminateHeaders()
        var result: ClassifierDescriptor? = null
        for (cclass in classes) {
            // if (!cclass.isExpect) return cclass  // 预期声明的处理
            if (result == null) result = cclass
        }
        for (typeAlias in typeAliases) {

            if (result == null) result = typeAlias
        }
        // if ((result?.source as? CangJieSourceElement)?.psi?.isValid == false) {
        //     throw AssertionError("PSI is invalidated for contributed classifier ${result.fqNameSafe}")
        // }


        return result
    }

    /**
     * 获取贡献的变量
     *
     * @param name 变量名称
     * @param location 查找位置
     * @return 变量描述符集合
     */
    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> {
        recordLookup(name, location)
        return variableDescriptors(name)
    }

    /**
     * 获取贡献的属性
     *
     * @param name 属性名称
     * @param location 查找位置
     * @return 属性描述符集合
     */
    override fun getContributedPropertys(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard PropertyDescriptor> {
        recordLookup(name, location)
        return propertyDescriptors(name)
    }

    /**
     * 打印作用域结构（用于调试）
     *
     * @param p 打印器
     */
    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.println("thisDescriptor = ", thisDescriptor)

        p.popIndent()
        p.println("}")
    }

    /**
     * 从声明的元素计算描述符
     *
     * 根据类型过滤器和名称过滤器，从 PSI 声明提取对应的描述符
     *
     * @param kindFilter 描述符类型过滤器
     * @param nameFilter 名称过滤器
     * @param location 查找位置
     * @return 描述符集合
     */
    protected fun computeDescriptorsFromDeclaredElements(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        location: LookupLocation
    ): MutableSet<DeclarationDescriptor> {
        val declarations = declarationProvider.getDeclarations(kindFilter, nameFilter)
        val result = LinkedHashSet<DeclarationDescriptor>(declarations.size)
        for (declaration in declarations) {
            when (declaration) {
                // 类型声明（类、接口等）
                is CjTypeStatement -> {
                    val name = declaration.nameAsSafeName
                    if (nameFilter(name)) {
                        result.addAll(classDescriptors(name))
                    }
                }

                // 函数声明
                is CjFunction -> {
                    val name = declaration.nameAsSafeName
                    if (nameFilter(name)) {
                        result.addAll(getContributedFunctions(name, location))
                    }
                }

                // 模式变量（支持解构绑定）
                is CjPatternVariable -> {
                    val names = declaration.pattern.getAllBindings().map { it.nameAsSafeName }
                   names.forEach {
                       if (nameFilter(it)) {
                               result.addAll(getContributedVariables(it, location))
                       }
                   }


                }

                // 字段变量
                is CjFieldVariable -> {
                    val name = declaration.nameAsSafeName
                    if (nameFilter(name)) {
                        result.addAll(getContributedVariables(name, location))
                    }
                }

                // 属性声明
                is CjProperty -> {
                    val name = declaration.nameAsSafeName
                    if (nameFilter(name)) {
                        result.addAll(getContributedPropertys(name, location))
                    }
                }

                // 参数
                is CjParameter -> {
                    val name = declaration.nameAsSafeName
                    if (nameFilter(name)) {
                        result.addAll(getContributedVariables(name, location))
                    }
                }

                // 类型别名
                is CjTypeAlias -> {
                    val name = declaration.nameAsSafeName
                    if (nameFilter(name)) {
                        result.addAll(getContributedTypeAliasDescriptors(name, location))
                    }
                }



                else -> throw IllegalArgumentException("Unsupported declaration kind: $declaration")
            }
        }
        return result
    }



    /**
     * 获取贡献的类型别名描述符
     *
     * @param name 类型别名名称
     * @param location 查找位置
     * @return 类型别名描述符集合
     */
    protected fun getContributedTypeAliasDescriptors(
        name: Name,
        location: LookupLocation
    ): Collection<TypeAliasDescriptor> {
        recordLookup(name, location)
        return typeAliasDescriptors(name)
    }

    /**
     * 获取成员声明解析作用域（由子类实现）
     *
     * 用于解析成员声明的类型引用、表达式等
     *
     * @param declaration 声明元素
     * @return 词法作用域
     */
    abstract fun getScopeForMemberDeclarationResolution(declaration: CjDeclaration): LexicalScope

    /**
     * 获取声明的 main 函数
     *
     * @return main 函数描述符集合
     */
    private fun getMainDeclaredFunctions(

    ): Collection<SimpleFunctionDescriptor> {
        if (mainScope != null) return mainScope.declaredMainFunctionDescriptors(MAIN).map {
            it.newCopyBuilder().setPreserveSourceElement().build()!!
        }
        val result = linkedSetOf<SimpleFunctionDescriptor>()

        val declarations = declarationProvider.getMainFunctionDeclarations()
        for (functionDeclaration in declarations) {
            result.add(
                c.functionDescriptorResolver.resolveFunctionDescriptor(
                    thisDescriptor,
                    getScopeForMemberDeclarationResolution(functionDeclaration),
                    functionDeclaration,
                    trace,
                    c.declarationScopeProvider.getOuterDataFlowInfoForDeclaration(functionDeclaration),
                    c.inferenceSession
                )
            )
        }

        return result
    }

    /**
     * 获取声明的宏描述符
     *
     * @param name 宏名称
     * @return 宏描述符集合
     */
    private fun getDeclaredMacros(
        name: Name
    ): Collection<MacroDescriptor> {
        // TODO: 是否真的需要复制描述符？
        if (mainScope != null) return mainScope.declaredMacroDescriptors(name).map {
            it.newCopyBuilder().setPreserveSourceElement().build()!!
        }
        val result = linkedSetOf<MacroDescriptor>()
        val declarations = declarationProvider.getMacroDeclarations(name)
        for (marcoDeclaration in declarations) {
            result.add(
                c.functionDescriptorResolver.resolveMacroDescriptor(
                    thisDescriptor,
                    getScopeForMemberDeclarationResolution(marcoDeclaration),
                    marcoDeclaration,
                    trace,
                    c.declarationScopeProvider.getOuterDataFlowInfoForDeclaration(marcoDeclaration),
                    c.inferenceSession
                )
            )
        }
        return result
    }

    /**
     * 获取声明的函数描述符
     *
     * @param name 函数名称
     * @return 函数描述符集合
     */
    private fun getDeclaredFunctions(
        name: Name
    ): Collection<SimpleFunctionDescriptor> {

        // TODO: 是否真的需要复制描述符？
        if (mainScope != null) return mainScope.declaredFunctionDescriptors(name).map {
            it.newCopyBuilder().setPreserveSourceElement().build()!!
        }

        val result = linkedSetOf<SimpleFunctionDescriptor>()
        val declarations = (declarationProvider.getFunctionDeclarations(name)  ).distinct()
        for (functionDeclaration in declarations) {
            result.add(
                c.functionDescriptorResolver.resolveFunctionDescriptor(
                    thisDescriptor,
                    getScopeForMemberDeclarationResolution(functionDeclaration),
                    functionDeclaration,
                    trace,
                    c.declarationScopeProvider.getOuterDataFlowInfoForDeclaration(functionDeclaration),
                    c.inferenceSession
                )
            )
        }

        return result
    }

    /**
     * 获取 main 函数描述符
     *
     * @return main 函数描述符集合
     */
    private fun doGetMainFunctions(): Collection<SimpleFunctionDescriptor> {
        return LinkedHashSet(declaredMainFunctionDescriptors.invoke(MAIN))
    }

    /**
     * 获取宏描述符（包含声明的和继承的）
     *
     * @param name 宏名称
     * @return 宏描述符集合
     */
    private fun doGetMacros(name: Name): Collection<MacroDescriptor> {
        val result = LinkedHashSet(declaredMacroDescriptors.invoke(name))

        getNonDeclaredMacros(name, result)

        return result.toList()
    }

    /**
     * 获取函数描述符（包含声明的和继承的）
     *
     * @param name 函数名称
     * @return 函数描述符集合
     */
    private fun doGetFunctions(name: Name): Collection<SimpleFunctionDescriptor> {
        val result = LinkedHashSet(declaredFunctionDescriptors.invoke(name))

        getNonDeclaredFunctions(name, result)






        return result.toList()
    }


    /**
     * 获取非声明的宏（由子类实现以添加继承的或合成的宏）
     *
     * @param name 宏名称
     * @param result 结果集合（包含已声明的宏）
     */
    protected abstract fun getNonDeclaredMacros(name: Name, result: MutableSet<MacroDescriptor>)

    /**
     * 获取非声明的函数（由子类实现以添加继承的或合成的函数）
     *
     * @param name 函数名称
     * @param result 结果集合（包含已声明的函数）
     */
    protected abstract fun getNonDeclaredFunctions(name: Name, result: MutableSet<SimpleFunctionDescriptor>)

    /**
     * 创建类描述符
     *
     * @param name 类名称
     * @param types 类型声明信息集合
     * @return 类描述符列表
     */
    private fun createClassDescriptor(name: Name, types: Collection<CjTypeStatementInfo<*>>): List<ClassDescriptor> {
        val result = mutableListOf<ClassDescriptor>()

        val isExternal =   false


        types.forEach {

            when (it.classKind) {
                // 枚举类型和枚举构造器的处理（当前已注释）
//                ClassKind.ENUM -> {
//                    result.add(LazyEnumDescriptor(c, it, thisDescriptor, name))
//                }
//
//                ClassKind.ENUM_CONSTRUCTOR -> {
//                    result.add(
//                        c.enumDescriptorResolver.resolveEnumEntryDescriptor(
//                            c, thisDescriptor, name, it as CjEnmuEntryInfo, isExternal
//                        )
//                    )
//                }

                // 其他类型（类、接口等）
                else -> {
                    result.add(LazyClassDescriptor(c, thisDescriptor, name, it, isExternal))

                }
            }

//            }
        }


        return result.toList()
    }


    /**
     * 获取类描述符（包含声明的和非声明的）
     *
     * @param name 类名称
     * @return 类描述符列表
     */
    private fun doGetClasses(name: Name): List<ClassDescriptor> {
        mainScope?.classDescriptors?.invoke(name)?.let { return it }

        val result = linkedSetOf<ClassDescriptor>()
//        val result1 = linkedSetOf<ClassDescriptor>()

        // 从声明中创建类描述符
        result.addAll(createClassDescriptor(name, declarationProvider.getTypeStatementDeclarations(name)))

        // 添加非声明的类（继承的、合成的等）
        getNonDeclaredClasses(name, result)




        return result.toList() /*+ result1.toList()*/
    }





    /**
     * 获取贡献的宏
     *
     * @param name 宏名称
     * @param location 查找位置
     * @return 宏描述符集合
     */
    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> {

        recordLookup(name, location)
        return macroDescriptors(name)
    }

    /**
     * 获取贡献的函数
     *
     * @param name 函数名称
     * @param location 查找位置
     * @return 函数描述符集合
     */
    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        recordLookup(name, location)

        val d = if (name == MAIN) {
            mainFunctionDescriptors(MAIN)
        } else {
            // TODO: 如果在推断方法返回值类型时，方法返回了自己，那么这里会报出递归错误
            functionDescriptors(name)
        }


        return d
    }

    /**
     * 获取非声明的类（由子类实现以添加继承的或合成的类）
     *
     * @param name 类名称
     * @param result 结果集合（包含已声明的类）
     */
    protected abstract fun getNonDeclaredClasses(name: Name, result: MutableSet<ClassDescriptor>)

}
