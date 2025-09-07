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

package org.cangnova.cangjie.serialization.deserialization.descriptors


import com.intellij.configurationStore.Property
import org.cangnova.cangjie.descriptors.ClassDescriptor


import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.metadata.model.fb.FbDecl
import org.cangnova.cangjie.metadata.model.fb.FbDeclKind
import org.cangnova.cangjie.metadata.model.wrapper.ClassDeclWrapper
import org.cangnova.cangjie.metadata.model.wrapper.FunctionWrapper
import org.cangnova.cangjie.metadata.model.wrapper.PropertyWrapper
import org.cangnova.cangjie.metadata.model.wrapper.TypeAliasWrapper
import org.cangnova.cangjie.metadata.model.wrapper.VariableWrapper
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.MemberComparator
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.MemberScopeImpl
import org.cangnova.cangjie.serialization.deserialization.DeserializationContext
import org.cangnova.cangjie.serialization.deserialization.DeclarationDeserializer
import org.cangnova.cangjie.storage.getValue
import org.cangnova.cangjie.utils.Printer
import org.cangnova.cangjie.utils.addIfNotNull
import org.cangnova.cangjie.utils.compact

/**
 * 反序列化成员作用域抽象类
 *
 * 该类负责管理从序列化数据反序列化得到的成员描述符，包括函数、变量、属性、类型别名和类。
 * 它提供了两种实现策略：
 * - [OptimizedImplementation]：优化实现，通过预计算和缓存提高性能
 * - [NoReorderImplementation]：无重排序实现，保持原始声明顺序
 *
 * 这两种实现策略适用于不同的场景：
 * - 优化实现适用于大多数情况，提供更好的性能
 * - 无重排序实现适用于需要保持原始声明顺序的场景，如调试或测试
 *
 * @param c 反序列化上下文，提供反序列化所需的组件和存储管理器
 * @param functionList 函数声明列表，包含所有需要反序列化的函数声明
 * @param variableList 变量声明列表，包含所有需要反序列化的变量声明
 * @param propertyList 属性声明列表，包含所有需要反序列化的属性声明
 * @param typeAliasList 类型别名声明列表，包含所有需要反序列化的类型别名声明
 * @param classList 类声明列表，包含所有需要反序列化的类声明
 */
abstract class DeserializedMemberScope protected constructor(
    protected val c: DeserializationContext,
    functionList: List<FunctionWrapper>,
    variableList: List<VariableWrapper>,
    propertyList: List<PropertyWrapper>,
    typeAliasList: List<TypeAliasWrapper>,
    classList: List<ClassDeclWrapper>,

    ) : MemberScopeImpl() {
    internal val classNames by c.storageManager.createLazyValue { classList.map{it.name}.toSet() }



    /**
     * 成员作用域实现策略说明
     *
     * 本类提供了两种实现策略：
     *
     * 1. [OptimizedImplementation]（优化实现）：
     *    - 更高效的空间利用和性能表现
     *    - 不保留[addFunctionsAndPropertiesTo]中声明的原始顺序，需要手动恢复
     *    - 在大多数情况下创建[DeserializedMemberScope]时使用
     *
     * 2. [NoReorderImplementation]（无重排序实现）：
     *    - 效率较低，但保持描述符与序列化的flatbuffer对象中相同的顺序
     *    - 仅在[cn.cangnova.cangjie.serialization.deserialization.DeserializationConfiguration.preserveDeclarationsOrdering]
     *      设置为`true`时使用，通常在从反序列化描述符进行反编译时
     *
     * 反编译的描述符用于构建PSI，然后与直接从类文件和元数据构建的PSI进行比较。
     * 如果第一个和第二个PSI中的声明顺序不同，会引发PSI-Stub不匹配错误。
     *
     * 来自类文件和元数据的PSI使用与序列化flatbuffer对象中相同的声明顺序，
     * 这个顺序由[MemberComparator]决定。
     *
     * [OptimizedImplementation]使用[MemberComparator.NameAndTypeMemberComparator]恢复声明的顺序，
     * 使其与序列化对象中的顺序相同。但这并不总是有效（例如，当Kotlin类被ProGuard混淆时）。
     *
     * ProGuard可能会重命名序列化对象中的一些声明，然后比较器将根据它们的新名称重新排序。
     * 这将导致PSI-Stub不匹配错误，因为声明现在的顺序不同。
     *
     * 为避免这种情况，我们提供了[NoReorderImplementation]实现，它完全不对声明进行重新排序。
     * 由于它的空间效率较低，仅在作用域将在反编译期间使用时才使用。
     *
     * [createImplementation]用于创建[Implementation]的正确实现。
     *
     * [OptimizedImplementation]和[NoReorderImplementation]都被设计为内部类，
     * 以便能够访问受保护的`getNonDeclared*`函数。
     */
    /**
     * 成员作用域实现接口
     *
     * 定义了成员作用域实现所需的核心功能，包括获取各类成员名称集合、
     * 根据名称获取特定成员描述符，以及将函数和属性添加到结果集合中。
     * 该接口有两种实现：OptimizedImplementation和NoReorderImplementation。
     */
    private interface Implementation {
        /**
         * 函数名称集合
         */
        val functionNames: Set<Name>

        /**
         * 变量名称集合
         */
        val variableNames: Set<Name>

        /**
         * 类型别名名称集合
         */
        val typeAliasNames: Set<Name>
        val classNames: Set<Name>

        /**
         * 属性名称集合
         */
        val propertyNames: Set<Name>

        /**
         * 获取指定名称的函数描述符集合
         *
         * @param name 函数名称
         * @param location 查找位置
         * @return 函数描述符集合
         */
        fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor>

        /**
         * 获取指定名称的属性描述符集合
         *
         * @param name 属性名称
         * @param location 查找位置
         * @return 属性描述符集合
         */
        fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor>

        /**
         * 获取指定名称的类型别名描述符
         *
         * @param name 类型别名名称
         * @return 类型别名描述符，如果不存在则返回null
         */
        fun getTypeAliasByName(name: Name): TypeAliasDescriptor?
        fun getClassByName(name: Name): ClassDescriptor?

        /**
         * 获取指定名称的变量描述符集合
         *
         * @param name 变量名称
         * @param location 查找位置
         * @return 变量描述符集合
         */
        fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor>

        /**
         * 将函数和属性添加到结果集合中
         *
         * @param result 结果集合，用于存储找到的描述符
         * @param kindFilter 描述符类型过滤器
         * @param nameFilter 名称过滤器
         * @param location 查找位置
         */
        fun addFunctionsAndPropertiesTo(
            result: MutableCollection<DeclarationDescriptor>,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            location: LookupLocation
        )
    }


    /**
     * 成员作用域实现实例
     *
     * 根据配置选择合适的实现策略（优化实现或无重排序实现）
     */
    private val impl: Implementation =
        createImplementation(functionList, variableList, propertyList, typeAliasList, classList)

    /**
     * 创建成员作用域实现实例
     *
     * 根据反序列化配置中的preserveDeclarationsOrdering标志选择合适的实现：
     * - 如果需要保留声明顺序（通常用于反编译），则使用NoReorderImplementation
     * - 否则使用更高效的OptimizedImplementation
     *
     * @param functionList 函数声明列表
     * @param variableList 变量声明列表
     * @param propertyList 属性声明列表
     * @param typeAliasList 类型别名声明列表
     * @return 适合当前配置的Implementation实现
     */
    private fun createImplementation(
        functionList: List<FunctionWrapper>,
        variableList: List<VariableWrapper>,
        propertyList: List<PropertyWrapper>,
        typeAliasList: List<TypeAliasWrapper>,
        classList: List<ClassDeclWrapper>
    ): Implementation =
        if (c.components.configuration.preserveDeclarationsOrdering)
            NoReorderImplementation(functionList, variableList, propertyList, typeAliasList, classList)
        else
            OptimizedImplementation(functionList, variableList, propertyList, typeAliasList, classList)


    /**
     * 优化实现类
     *
     * 这是DeserializedMemberScope的主要实现，提供更高效的空间利用和性能表现。
     * 它使用懒加载和缓存机制，仅在需要时计算描述符，并通过分组和索引优化查找性能。
     * 该实现不保留声明的原始顺序，但会在需要时通过MemberComparator恢复顺序。
     *
     * @param functionList 函数声明列表
     * @param variableList 变量声明列表
     * @param propertyList 属性声明列表
     * @param typeAliasList 类型别名声明列表
     */
    private inner class OptimizedImplementation(
       functionList: List<FunctionWrapper>,
         variableList: List<VariableWrapper>,

         propertyList: List<PropertyWrapper>,
        typeAliasList: List<TypeAliasWrapper>,
        classList: List<ClassDeclWrapper>

    ) : Implementation {
        /**
         * 按名称分组的函数声明映射
         * 将函数声明列表按标识符分组，便于快速查找
         */
        private val functionDecls = functionList.groupByName { it.name }

        /**
         * 按名称分组的变量声明映射
         * 将变量声明列表按标识符分组，便于快速查找
         */
        private val variableDecls = variableList.groupByName {it.name }

        /**
         * 按名称分组的属性声明映射
         * 将属性声明列表按标识符分组，便于快速查找
         */
        private val propertyDecls = propertyList.groupByName { it.name}

        /**
         * 按名称分组的类型别名声明映射
         * 根据配置决定是否支持类型别名，如果支持则按标识符分组，否则返回空映射
         */
        private val typeAliases =
            if (c.components.configuration.typeAliasesAllowed)
                typeAliasList.groupByName { it.name }
            else
                emptyMap()

        /**
         * 按名称分组的类声明映射
         * 将类声明列表按标识符分组，便于快速查找
         */
        private val classs = classList.groupByName { it.name }

        /**
         * 函数描述符缓存
         * 使用存储管理器创建的记忆化函数，根据名称计算并缓存函数描述符集合
         */
        private val functions =
            c.storageManager.createMemoizedFunction<Name, Collection<SimpleFunctionDescriptor>> { computeFunctions(it) }

        /**
         * 属性描述符缓存
         * 使用存储管理器创建的记忆化函数，根据名称计算并缓存属性描述符集合
         */
        private val properties =
            c.storageManager.createMemoizedFunction<Name, Collection<PropertyDescriptor>> { computeProperties(it) }

        /**
         * 类型别名描述符缓存
         * 使用存储管理器创建的记忆化函数，根据名称计算并缓存类型别名描述符
         */
        private val typeAliasByName =
            c.storageManager.createMemoizedFunctionWithNullableValues<Name, TypeAliasDescriptor> { createTypeAlias(it) }

        /**
         * 类描述符缓存
         * 使用存储管理器创建的记忆化函数，根据名称计算并缓存类描述符
         */
        private val classByName =
            c.storageManager.createMemoizedFunctionWithNullableValues<Name, ClassDescriptor> { createClasss(it) }


        /**
         * 变量描述符缓存
         * 使用存储管理器创建的记忆化函数，根据名称计算并缓存变量描述符集合
         */
        private val variables =
            c.storageManager.createMemoizedFunction<Name, Collection<VariableDescriptor>> { computeVariables(it) }

        override val functionNames by c.storageManager.createLazyValue {
            functionDecls.keys + getNonDeclaredFunctionNames()
        }
        override val propertyNames by c.storageManager.createLazyValue {
            propertyDecls.keys + getNonDeclaredPropertyNames()
        }
        override val variableNames by c.storageManager.createLazyValue {
            variableDecls.keys + getNonDeclaredVariableNames()
        }

        override val typeAliasNames: Set<Name> get() = typeAliases.keys
        override val classNames: Set<Name>
            get() = classs.keys

        private inline fun <T> Collection<T>.groupByName(
            getName: (T) -> Name
        ) = groupBy {  getName(it)  }

        /**
         * 计算指定名称的函数描述符列表
         *
         * 根据给定名称查找并计算对应的函数描述符，使用通用的computeDescriptors方法实现。
         * 该方法会从函数声明映射中查找对应名称的声明，并通过反序列化器加载函数描述符。
         *
         * @param name 要查找的函数名称
         * @return 对应名称的函数描述符列表
         */
        private fun computeFunctions(name: Name) =
            computeDescriptors(
                functionDecls[name] ?: emptyList(),
                { c.declDeserializer.loadFunction(it).takeIf(::isDeclaredFunctionAvailable) },
                { computeNonDeclaredFunctions(name, it) }
            )

        private inline fun <M,D : DeclarationDescriptor> computeDescriptors(
            decls: Collection<M>,
            factory: (M) -> D?,
            computeNonDeclared: (MutableList<D>) -> Unit
        ): Collection<D> {
            val descriptors = decls.mapNotNullTo(ArrayList(decls.size), factory)
            computeNonDeclared(descriptors)
            return descriptors.compact()
        }

        /**
         * 计算指定名称的变量描述符列表
         *
         * 根据给定名称查找并计算对应的变量描述符，使用通用的computeDescriptors方法实现。
         * 该方法会从变量声明映射中查找对应名称的声明，并通过反序列化器加载变量描述符。
         *
         * @param name 要查找的变量名称
         * @return 对应名称的变量描述符列表
         */
        private fun computeVariables(name: Name) =
            computeDescriptors(
                variableDecls[name] ?: emptyList(),
                { c.declDeserializer.loadVariable(it) },
                { computeNonDeclaredVariables(name, it) }
            )

        /**
         * 计算指定名称的属性描述符列表
         *
         * 根据给定名称查找并计算对应的属性描述符，使用通用的computeDescriptors方法实现。
         * 该方法会从属性声明映射中查找对应名称的声明，并通过反序列化器加载属性描述符。
         *
         * @param name 要查找的属性名称
         * @return 对应名称的属性描述符列表
         */
        private fun computeProperties(name: Name) =
            computeDescriptors(
                propertyDecls[name] ?: emptyList(),
                { c.declDeserializer.loadProperty(it) },
                { computeNonDeclaredProperties(name, it) }
            )

        /**
         * 创建指定名称的类型别名描述符
         *
         * 根据给定名称查找并创建对应的类型别名描述符。如果找不到对应名称的类型别名声明，
         * 或者声明列表为空，则返回null。否则返回通过反序列化器加载的类型别名描述符。
         *
         * @param name 要查找的类型别名名称
         * @return 对应名称的类型别名描述符，如果不存在则返回null
         */
        private fun createTypeAlias(name: Name): TypeAliasDescriptor? {
            val decls = typeAliases[name] ?: return null
            val decl = decls.firstOrNull() ?: return null
            return c.declDeserializer.loadTypeAlias(decl)
        }

        /**
         * 创建指定名称的类描述符
         *
         * 根据给定名称查找并创建对应的类描述符。如果找不到对应名称的类声明，
         * 或者声明列表为空，则返回null。否则返回通过反序列化器加载的类描述符。
         *
         * @param name 要查找的类名称
         * @return 对应名称的类描述符，如果不存在则返回null
         */
        private fun createClasss(name: Name): ClassDescriptor? {
            val decls = classs[name] ?: return null
            val decl = decls.firstOrNull() ?: return null
            return c.declDeserializer.loadClass(decl)
        }

        override fun getContributedFunctions(
            name: Name,
            location: LookupLocation
        ): Collection<SimpleFunctionDescriptor> {
            if (name !in functionNames) return emptyList()
            return functions(name)
        }


        override fun getTypeAliasByName(name: Name): TypeAliasDescriptor? {
            return typeAliasByName(name)
        }

        override fun getClassByName(name: Name): ClassDescriptor? {
            return classByName(name)
        }

        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
            if (name !in variableNames) return emptyList()
            return variables(name)
        }

        override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
            if (name !in propertyNames) return emptyList()
            return properties(name)
        }

        override fun addFunctionsAndPropertiesTo(
            result: MutableCollection<DeclarationDescriptor>,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            location: LookupLocation
        ) {

            if (kindFilter.acceptsKinds(DescriptorKindFilter.PROPERTYS_MASK)) {
                addMembers(
                    propertyNames,
                    nameFilter,
                    result
                ) { getContributedPropertys(it, location) }
            }
            if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
                addMembers(
                    variableNames,
                    nameFilter,
                    result
                ) { getContributedVariables(it, location) }
            }

            if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
                addMembers(
                    functionNames,
                    nameFilter,
                    result
                ) { getContributedFunctions(it, location) }
            }
        }

        /**
         * 添加成员描述符到结果集合
         *
         * 该方法根据名称过滤器筛选符合条件的成员名称，获取对应的描述符，
         * 并按名称和类型排序后添加到结果集合中。这确保了成员描述符在结果中
         * 的顺序与原始序列化对象中的顺序一致。
         *
         * @param names 要处理的名称集合
         * @param nameFilter 名称过滤器函数
         * @param result 存储结果的集合
         * @param descriptorsByName 根据名称获取描述符的函数
         */
        private inline fun addMembers(
            names: Collection<Name>,
            nameFilter: (Name) -> Boolean,
            result: MutableCollection<DeclarationDescriptor>,
            descriptorsByName: (Name) -> Collection<DeclarationDescriptor>
        ) {
            val subResult = ArrayList<DeclarationDescriptor>()
            for (name in names) {
                if (nameFilter(name)) {
                    subResult.addAll(descriptorsByName(name))
                }
            }

            // We perform the sort just in case
            subResult.sortWith(MemberComparator.NameAndTypeMemberComparator.INSTANCE)
            result.addAll(subResult)
        }
    }


    /**
     * 判断已声明函数是否可用
     *
     * 可以被子类重写以过滤特定的已声明函数。该方法不会被应用于非声明函数。
     * 默认实现返回true，表示所有已声明函数都可用。
     *
     * @param function 要检查的函数描述符
     * @return 如果函数可用则返回true，否则返回false
     */
    protected open fun isDeclaredFunctionAvailable(function: SimpleFunctionDescriptor): Boolean = true

    /**
     * 计算非声明函数
     *
     * 该方法具有以下约定：
     * * 它只能向[functions]列表的末尾添加元素，不得以其他方式修改列表（例如从中删除元素）。
     * * 在调用之前，[functions]应该已经包含所有具有[name]名称的已声明函数。
     *
     * 子类可以重写此方法以添加额外的非声明函数，如继承的函数或合成函数。
     *
     * @param name 函数名称
     * @param functions 已包含所有已声明函数的可变列表，可以向其中添加非声明函数
     */
    protected open fun computeNonDeclaredFunctions(name: Name, functions: MutableList<SimpleFunctionDescriptor>) {
    }

    /**
     * 计算非声明属性
     *
     * 该方法具有以下约定：
     * * 它只能向[descriptors]列表的末尾添加元素，不得以其他方式修改列表（例如从中删除元素）。
     * * 在调用之前，[descriptors]应该已经包含所有具有[name]名称的已声明属性。
     *
     * 子类可以重写此方法以添加额外的非声明属性，如继承的属性或合成属性。
     *
     * @param name 属性名称
     * @param descriptors 已包含所有已声明属性的可变列表，可以向其中添加非声明属性
     */
    protected open fun computeNonDeclaredProperties(name: Name, descriptors: MutableList<PropertyDescriptor>) {
    }

    /**
     * 计算非声明变量
     *
     * 该方法具有以下约定：
     * * 它只能向[descriptors]列表的末尾添加元素，不得以其他方式修改列表（例如从中删除元素）。
     * * 在调用之前，[descriptors]应该已经包含所有具有[name]名称的已声明变量。
     *
     * 子类可以重写此方法以添加额外的非声明变量，如继承的变量或合成变量。
     *
     * @param name 变量名称
     * @param descriptors 已包含所有已声明变量的可变列表，可以向其中添加非声明变量
     */
    protected open fun computeNonDeclaredVariables(name: Name, descriptors: MutableList<VariableDescriptor>) {
    }

    /**
     * Take a note that [NoReorderImplementation] still adds non-declared members together with directly declared in class.
     * This is not a problem for ordering, since during decompilation from descriptors those non-declared members are just ignored,
     * and the declared members will be added to decompiled text in the proper (i.e. original) order.
     */
    /**
     * 无重排序实现类
     *
     * 这个实现保留了声明的原始顺序，主要用于反编译场景。
     * 与OptimizedImplementation不同，它不会对声明进行分组或优化，而是保持原始序列化顺序。
     * 这对于生成与原始源代码结构相似的反编译结果非常重要。
     *
     * @param functionList 函数声明列表，保持原始顺序
     * @param variableList 变量声明列表，保持原始顺序
     * @param propertyList 属性声明列表，保持原始顺序
     * @param typeAliasList 类型别名声明列表，保持原始顺序
     */
    /**
     * 无重排序实现类
     *
     * 这个实现保留了声明的原始顺序，主要用于反编译场景。
     * 与OptimizedImplementation不同，它不会对声明进行分组或优化，而是保持原始序列化顺序。
     * 这对于生成与原始源代码结构相似的反编译结果非常重要。
     *
     * @param functionList 函数声明列表，保持原始顺序
     * @param variableList 变量声明列表，保持原始顺序
     * @param propertyList 属性声明列表，保持原始顺序
     * @param typeAliasList 类型别名声明列表，保持原始顺序
     * @param classList 类声明列表，保持原始顺序
     */
    /**
     * 无重排序实现类
     *
     * 这个实现保留了声明的原始顺序，主要用于反编译场景。
     * 与OptimizedImplementation不同，它不会对声明进行分组或优化，而是保持原始序列化顺序。
     * 这对于生成与原始源代码结构相似的反编译结果非常重要。
     *
     * @param functionList 函数声明列表，保持原始顺序
     * @param variableList 变量声明列表，保持原始顺序
     * @param propertyList 属性声明列表，保持原始顺序
     * @param typeAliasList 类型别名声明列表，保持原始顺序
     * @param classList 类声明列表，保持原始顺序
     */
    /**
     * 无重排序实现类
     *
     * 这个实现保留了声明的原始顺序，主要用于反编译场景。
     * 与OptimizedImplementation不同，它不会对声明进行分组或优化，而是保持原始序列化顺序。
     * 这对于生成与原始源代码结构相似的反编译结果非常重要。
     *
     * @param functionList 函数声明列表，保持原始顺序
     * @param variableList 变量声明列表，保持原始顺序
     * @param propertyList 属性声明列表，保持原始顺序
     * @param typeAliasList 类型别名声明列表，保持原始顺序
     * @param classList 类声明列表，保持原始顺序
     */
    private inner class NoReorderImplementation(
        private val functionList: List<FunctionWrapper>,
        private val variableList: List<VariableWrapper>,

        private val propertyList: List<PropertyWrapper>,
        typeAliasList: List<TypeAliasWrapper>,
        private val classList: List<ClassDeclWrapper>
    ) : Implementation {

        /**
         * 类型别名列表
         * 根据配置决定是否支持类型别名，如果支持则使用传入的列表，否则使用空列表
         */
        private val typeAliasList = if (c.components.configuration.typeAliasesAllowed) typeAliasList else emptyList()

        /**
         * 已声明函数描述符列表
         * 懒加载计算所有已声明的函数描述符
         */
        private val declaredFunctions: List<SimpleFunctionDescriptor>
                by c.storageManager.createLazyValue { computeFunctions() }

        /**
         * 已声明变量描述符列表
         * 懒加载计算所有已声明的变量描述符
         */
        private val declaredVariables: List<VariableDescriptor>
                by c.storageManager.createLazyValue { computeVariables() }

        /**
         * 已声明属性描述符列表
         * 懒加载计算所有已声明的属性描述符
         */
        private val declaredProperties: List<PropertyDescriptor>
                by c.storageManager.createLazyValue { computeProperties() }

        /**
         * 所有类描述符列表
         * 懒加载计算所有类描述符
         */
        private val allClass: List<ClassDescriptor>
                by c.storageManager.createLazyValue { computeClasss() }

        /**
         * 所有类型别名描述符列表
         * 懒加载计算所有类型别名描述符
         */
        private val allTypeAliases: List<TypeAliasDescriptor>
                by c.storageManager.createLazyValue { computeTypeAliases() }

        /**
         * 所有函数描述符列表
         * 懒加载计算所有函数描述符，包括已声明和非声明的函数
         */
        private val allFunctions: List<SimpleFunctionDescriptor>
                by c.storageManager.createLazyValue { declaredFunctions + computeAllNonDeclaredFunctions() }

        /**
         * 所有变量描述符列表
         * 懒加载计算所有变量描述符，包括已声明和非声明的变量
         */
        private val allVariables: List<VariableDescriptor>
                by c.storageManager.createLazyValue { declaredVariables + computeAllNonDeclaredVariables() }

        /**
         * 所有属性描述符列表
         * 懒加载计算所有属性描述符，包括已声明和非声明的属性
         */
        private val allProperties: List<PropertyDescriptor>
                by c.storageManager.createLazyValue { declaredProperties + computeAllNonDeclaredProperties() }

        /**
         * 按名称索引的类描述符映射
         * 懒加载计算所有类描述符的名称映射
         */
        private val classByName: Map<Name, ClassDescriptor>
                by c.storageManager.createLazyValue { allClass.associateBy { it.name } }

        /**
         * 按名称索引的类型别名描述符映射
         * 懒加载计算所有类型别名描述符的名称映射
         */
        private val typeAliasesByName: Map<Name, TypeAliasDescriptor>
                by c.storageManager.createLazyValue { allTypeAliases.associateBy { it.name } }

        /**
         * 按名称分组的函数描述符映射
         * 懒加载计算所有函数描述符按名称分组的映射
         */
        private val functionsByName: Map<Name, Collection<SimpleFunctionDescriptor>>
                by c.storageManager.createLazyValue { allFunctions.groupBy { it.name } }

        /**
         * 按名称分组的属性描述符映射
         * 懒加载计算所有属性描述符按名称分组的映射
         */
        private val propertiesByName: Map<Name, Collection<PropertyDescriptor>>
                by c.storageManager.createLazyValue { allProperties.groupBy { it.name } }

        /**
         * 按名称分组的变量描述符映射
         * 懒加载计算所有变量描述符按名称分组的映射
         */
        private val variablesByName: Map<Name, Collection<VariableDescriptor>>
                by c.storageManager.createLazyValue { allVariables.groupBy { it.name } }

        /**
         * 函数名称集合
         * 懒加载计算所有函数的名称集合，包括已声明和非声明的函数名称
         */
        override val functionNames by c.storageManager.createLazyValue {
            functionList.mapToNames { it.name } + getNonDeclaredFunctionNames()
        }

        /**
         * 属性名称集合
         * 懒加载计算所有属性的名称集合，包括已声明和非声明的属性名称
         */
        override val propertyNames by c.storageManager.createLazyValue {
            propertyList.mapToNames { it.name } + getNonDeclaredPropertyNames()
        }

        /**
         * 变量名称集合
         * 懒加载计算所有变量的名称集合，包括已声明和非声明的变量名称
         */
        override val variableNames by c.storageManager.createLazyValue {
            variableList.mapToNames { it.name } + getNonDeclaredVariableNames()
        }

        /**
         * 类型别名名称集合
         * 计算所有类型别名的名称集合
         */
        override val typeAliasNames: Set<Name>
            get() = typeAliasList.mapToNames { it.name }

        /**
         * 类名称集合
         * 计算所有类的名称集合
         */
        override val classNames: Set<Name>
            get() = classList.mapToNames { it.name }

        /**
         * 计算所有函数描述符列表
         *
         * 保持原始顺序反序列化所有函数声明，并应用可用性过滤
         *
         * @return 所有函数描述符列表，保持原始顺序
         */
        private fun computeFunctions(): List<SimpleFunctionDescriptor> =
            functionList.mapWithDeserializer { loadFunction(it).takeIf(::isDeclaredFunctionAvailable) }

        /**
         * 计算所有变量描述符列表
         *
         * 保持原始顺序反序列化所有变量声明
         *
         * @return 所有变量描述符列表，保持原始顺序
         */
        private fun computeVariables(): List<VariableDescriptor> =
            variableList.mapWithDeserializer { loadVariable(it) }

        /**
         * 计算所有属性描述符列表
         *
         * 保持原始顺序反序列化所有属性声明
         *
         * @return 所有属性描述符列表，保持原始顺序
         */
        private fun computeProperties(): List<PropertyDescriptor> =
            propertyList.mapWithDeserializer { loadProperty(it) }

        /**
         * 计算所有类型别名描述符列表
         *
         * 保持原始顺序反序列化所有类型别名声明
         *
         * @return 所有类型别名描述符列表，保持原始顺序
         */
        private fun computeTypeAliases(): List<TypeAliasDescriptor> =
            typeAliasList.mapWithDeserializer { loadTypeAlias(it) }

        private fun computeClasss(): List<ClassDescriptor> =
            classList.mapWithDeserializer { loadClass(it) }

        private fun computeAllNonDeclaredFunctions(): List<SimpleFunctionDescriptor> =
            getNonDeclaredFunctionNames().flatMap { computeNonDeclaredFunctionsForName(it) }

        private fun computeAllNonDeclaredVariables(): List<VariableDescriptor> =
            getNonDeclaredVariableNames().flatMap { computeNonDeclaredVariablesForName(it) }

        private fun computeAllNonDeclaredProperties(): List<PropertyDescriptor> =
            getNonDeclaredPropertyNames().flatMap { computeNonDeclaredPropertiesForName(it) }

        /**
         * 计算指定名称的非声明函数描述符列表
         *
         * @param name 函数名称
         * @return 对应名称的非声明函数描述符列表
         */
        private fun computeNonDeclaredFunctionsForName(name: Name): List<SimpleFunctionDescriptor> =
            computeNonDeclaredDescriptors(name, declaredFunctions, ::computeNonDeclaredFunctions)

        /**
         * 计算指定名称的非声明变量描述符列表
         *
         * @param name 变量名称
         * @return 对应名称的非声明变量描述符列表
         */
        private fun computeNonDeclaredVariablesForName(name: Name): List<VariableDescriptor> =
            computeNonDeclaredDescriptors(name, declaredVariables, ::computeNonDeclaredVariables)

        /**
         * 计算指定名称的非声明属性描述符列表
         *
         * @param name 属性名称
         * @return 对应名称的非声明属性描述符列表
         */
        private fun computeNonDeclaredPropertiesForName(name: Name): List<PropertyDescriptor> =
            computeNonDeclaredDescriptors(name, declaredProperties, ::computeNonDeclaredProperties)

        override fun getContributedFunctions(
            name: Name,
            location: LookupLocation
        ): Collection<SimpleFunctionDescriptor> {
            if (name !in functionNames) return emptyList()
            return functionsByName[name].orEmpty()
        }

        override fun getClassByName(name: Name): ClassDescriptor? {
            return classByName[name]
        }

        override fun getTypeAliasByName(name: Name): TypeAliasDescriptor? {
            return typeAliasesByName[name]
        }

        override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
            if (name !in propertyNames) return emptyList()
            return propertiesByName[name].orEmpty()
        }

        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
            if (name !in variableNames) return emptyList()
            return variablesByName[name].orEmpty()
        }

        override fun addFunctionsAndPropertiesTo(
            result: MutableCollection<DeclarationDescriptor>,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            location: LookupLocation
        ) {
            if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
                allVariables.filterTo(result) { nameFilter(it.name) }
            }
            if (kindFilter.acceptsKinds(DescriptorKindFilter.PROPERTYS_MASK)) {
                allProperties.filterTo(result) { nameFilter(it.name) }
            }
            if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
                allFunctions.filterTo(result) { nameFilter(it.name) }
            }
        }

        /**
         * We have to collect non-declared properties in such non-pretty way because we don't want to change the contract of the
         * [computeNonDeclaredProperties] and [computeNonDeclaredFunctions] methods, because we do not want any performance penalties.
         *
         * [computeNonDeclared] may only add elements to the end of [MutableList], otherwise this function would not work properly.
         */
        /**
         * 计算非声明描述符列表
         *
         * 该方法用于计算特定名称的非声明描述符，保持原始顺序。
         * 首先过滤出具有相同名称的已声明描述符，然后调用computeNonDeclared函数添加非声明描述符，
         * 最后返回新添加的部分。
         *
         * @param name 要查找的名称
         * @param declaredDescriptors 已声明描述符列表
         * @param computeNonDeclared 计算非声明描述符的函数
         * @return 非声明描述符列表
         */
        private inline fun <T : DeclarationDescriptor> computeNonDeclaredDescriptors(
            name: Name,
            declaredDescriptors: List<T>,
            computeNonDeclared: (Name, MutableList<T>) -> Unit
        ): List<T> {
            val declaredDescriptorsWithSameName = declaredDescriptors.filterTo(mutableListOf()) { it.name == name }
            val nonDeclaredPropertiesStartIndex = declaredDescriptorsWithSameName.size

            computeNonDeclared(name, declaredDescriptorsWithSameName)

            return declaredDescriptorsWithSameName.subList(
                nonDeclaredPropertiesStartIndex,
                declaredDescriptorsWithSameName.size
            )
        }

        /**
         * 将列表中的元素映射为名称集合
         *
         * 保持原始顺序将列表元素转换为名称标识符集合
         * 使用LinkedHashSet保证顺序一致性
         *
         * @param getName 从元素中提取名称字符串的函数
         * @return 保持原始顺序的名称集合
         */
        private inline fun <T> List<T>.mapToNames(getName: (T) -> Name): Set<Name> {
            // `mutableSetOf` returns `LinkedHashSet`, it is important to preserve the order of the declarations.
            return mapTo(mutableSetOf()) {  getName(it)  }
        }

        /**
         * 使用反序列化器将声明列表映射为描述符列表
         *
         * 保持原始顺序将声明反序列化为对应的描述符
         *
         * @param deserialize 反序列化函数，将声明转换为描述符
         * @return 保持原始顺序的描述符列表
         */
        private inline fun <T,K : MemberDescriptor> List<T>.mapWithDeserializer(
            deserialize: DeclarationDeserializer.(T) -> K?
        ): List<K> {
            return mapNotNull { c.declDeserializer.deserialize(it) }
        }
    }

    /**
     * 根据名称创建类ID
     *
     * 子类需要实现此方法，为给定名称创建对应的ClassId。
     * ClassId用于唯一标识一个类，包含包名和类名信息。
     *
     * @param name 类名称
     * @return 对应的ClassId
     */
    protected abstract fun createClassId(name: Name): ClassId

    /**
     * 获取非声明函数名称集合
     *
     * 子类需要实现此方法，返回作用域中所有非声明函数的名称集合。
     * 非声明函数通常是继承或合成的函数，而非直接在当前作用域声明的函数。
     *
     * @return 非声明函数名称集合
     */
    protected abstract fun getNonDeclaredFunctionNames(): Set<Name>

    /**
     * 获取非声明变量名称集合
     *
     * 子类需要实现此方法，返回作用域中所有非声明变量的名称集合。
     * 非声明变量通常是继承或合成的变量，而非直接在当前作用域声明的变量。
     *
     * @return 非声明变量名称集合
     */
    protected abstract fun getNonDeclaredVariableNames(): Set<Name>

    /**
     * 获取非声明属性名称集合
     *
     * 子类需要实现此方法，返回作用域中所有非声明属性的名称集合。
     * 非声明属性通常是继承或合成的属性，而非直接在当前作用域声明的属性。
     *
     * @return 非声明属性名称集合
     */
    protected abstract fun getNonDeclaredPropertyNames(): Set<Name>

    /**
     * 获取非声明分类器名称集合
     *
     * 子类需要实现此方法，返回作用域中所有非声明分类器的名称集合。
     * 分类器包括类、接口、对象等类型声明。返回null表示没有非声明分类器。
     *
     * @return 非声明分类器名称集合，如果没有则返回null
     */
    protected abstract fun getNonDeclaredClassifierNames(): Set<Name>?


    /**
     * 分类器名称的懒加载属性
     *
     * 通过存储管理器创建的可空懒加载值，包含所有分类器名称（类名、类型别名名称和非声明分类器名称）。
     * 只有在首次访问时才会计算，避免不必要的计算开销。
     * 如果没有非声明分类器名称，则返回null。
     */
    private val classifierNamesLazy by c.storageManager.createNullableLazyValue {
        val nonDeclaredNames = getNonDeclaredClassifierNames() ?: return@createNullableLazyValue null
        impl.classNames + impl.typeAliasNames + nonDeclaredNames
    }

    /**
     * 获取指定名称的类型别名描述符
     *
     * 根据给定名称查找并返回对应的类型别名描述符。
     * 该方法委托给实现类的getTypeAliasByName方法。
     *
     * @param name 要查找的类型别名名称
     * @return 对应名称的类型别名描述符，如果不存在则返回null
     */
    private fun getTypeAliasByName(name: Name): TypeAliasDescriptor? {
        return impl.getTypeAliasByName(name)
    }

    /**
     * 获取指定名称的类描述符
     *
     * 根据给定名称创建类ID，然后通过组件的类反序列化器获取对应的类描述符。
     * 这是一个内部辅助方法，用于从类名获取完整的类描述符。
     *
     * @param name 要查找的类名称
     * @return 对应名称的类描述符，如果不存在则返回null
     */
    private fun getClassByName(name: Name): ClassDescriptor? {
        return impl.getClassByName(name)

    }

    /**
     * 获取所有变量名称集合
     *
     * 返回作用域中所有变量的名称集合，委托给实现类的variableNames属性。
     */
    override val variableNames get() = impl.variableNames

    /**
     * 获取所有属性名称集合
     *
     * 返回作用域中所有属性的名称集合，委托给实现类的propertyNames属性。
     */
    override val propertyNames get() = impl.propertyNames

    /**
     * 获取所有函数名称集合
     *
     * 返回作用域中所有函数的名称集合，委托给实现类的functionNames属性。
     */
    override val functionNames: Set<Name>
        get() = impl.functionNames

    /**
     * 获取所有分类器名称集合
     *
     * 返回作用域中所有分类器的名称集合，使用懒加载属性classifierNamesLazy。
     * 如果没有非声明分类器名称，则返回null。
     */
    override val classifierNames get(): Set<Name>? = classifierNamesLazy

    /**
     * 确定作用域是否绝对不包含指定名称
     *
     * 检查给定名称是否不在函数名称、变量名称、类名称和类型别名名称集合中。
     * 如果名称不在任何集合中，则返回true，表示作用域绝对不包含该名称。
     *
     * @param name 要检查的名称
     * @return 如果作用域绝对不包含该名称，则返回true；否则返回false
     */
    override fun definitelyDoesNotContainName(name: Name): Boolean {
        return name !in impl.functionNames && name !in impl.variableNames && name !in impl.classNames && name !in impl.typeAliasNames
    }

    private fun deserializeClass(name: Name): ClassDescriptor? =
        c.components.deserializeClass(createClassId(name))

    /**
     * 检查作用域是否包含指定名称的类
     *
     * 检查给定名称是否在类名称集合中。
     * 子类可以重写此方法以提供自定义的类检查逻辑。
     *
     * @param name 要检查的类名称
     * @return 如果作用域包含该名称的类，则返回true；否则返回false
     */
    protected open fun hasClass(name: Name): Boolean =
        name in impl.classNames

    /**
     * 获取指定名称的贡献分类器
     *
     * 根据给定名称和查找位置，返回对应的分类器描述符。
     * 首先检查是否有对应名称的类，然后检查是否有对应名称的类型别名。
     * 如果都没有，则返回null。
     *
     * @param name 要查找的分类器名称
     * @param location 查找位置
     * @return 对应名称的分类器描述符，如果不存在则返回null
     */
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
        when {
            hasClass(name) -> deserializeClass(name)
            name in impl.typeAliasNames -> getTypeAliasByName(name)
            else -> null
        }

    /**
     * 计算符合条件的描述符集合
     *
     * 根据给定的类型过滤器、名称过滤器和查找位置，计算并返回作用域中所有符合条件的描述符集合。
     * 描述符的顺序与它们在flatbuffers中序列化的顺序相同（参见MemberComparator）。
     *
     * 处理流程：
     * 1. 如果接受单例分类器，添加枚举条目描述符
     * 2. 添加函数和属性描述符
     * 3. 如果接受分类器，添加类描述符
     * 4. 如果接受类型别名，添加类型别名描述符
     *
     * @param kindFilter 类型过滤器，用于筛选特定类型的描述符
     * @param nameFilter 名称过滤器，用于筛选特定名称的描述符
     * @param location 查找位置
     * @return 所有符合条件的描述符集合
     */
    protected fun computeDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        location: LookupLocation
    ): Collection<DeclarationDescriptor> {
        //NOTE: descriptors should be in the same order they were serialized in flatbuffers
        // see MemberComparator
        val result = ArrayList<DeclarationDescriptor>(0)



        impl.addFunctionsAndPropertiesTo(result, kindFilter, nameFilter, location)

        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            for (className in impl.classNames) {
                if (nameFilter(className)) {
                    result.addIfNotNull(deserializeClass(className))

//                    result.addIfNotNull(getClassByName(className))
                }
            }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.TYPE_ALIASES_MASK)) {
            for (typeAliasName in impl.typeAliasNames) {
                if (nameFilter(typeAliasName)) {
                    result.addIfNotNull(impl.getTypeAliasByName(typeAliasName))
                }
            }
        }

        return result.compact()
    }


    /**
     * 打印作用域结构
     *
     * 以可读格式打印当前作用域的结构信息，包括类名和包含声明。
     * 这主要用于调试和日志记录目的，帮助开发者理解作用域的组成。
     *
     * @param p 打印器对象，用于格式化输出
     */
    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.println("containingDeclaration = " + c.containingDeclaration)

        p.popIndent()
        p.println("}")
    }

}


