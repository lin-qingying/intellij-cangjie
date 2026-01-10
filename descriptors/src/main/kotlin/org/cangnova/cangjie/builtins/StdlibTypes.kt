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

package org.cangnova.cangjie.builtins

import org.cangnova.cangjie.builtins.StandardNames.COMPARABLE
import org.cangnova.cangjie.builtins.StandardNames.COUNTABLE
import org.cangnova.cangjie.builtins.StandardNames.EQUATABLE
import org.cangnova.cangjie.builtins.StandardNames.FUTURE
import org.cangnova.cangjie.builtins.StandardNames.FqNames.ast
import org.cangnova.cangjie.builtins.StandardNames.FqNames.core
import org.cangnova.cangjie.builtins.StandardNames.FqNames.sync
import org.cangnova.cangjie.builtins.StandardNames.ITERABLE
import org.cangnova.cangjie.builtins.StandardNames.RANGE
import org.cangnova.cangjie.builtins.StandardNames.RESOURCE
import org.cangnova.cangjie.descriptors.ClassAndEnumDescriptor
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.EnumDescriptor
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.CangJieTypeFactory
import org.cangnova.cangjie.types.SimpleType
import org.cangnova.cangjie.types.TypeArgumentImpl
import org.cangnova.cangjie.types.toDefaultAttributes

/**
 * 标准库类型访问器
 *
 * 提供对标准库（std.*）类型的便捷访问。与 [CangJieBuiltIns] 分离，
 * 确保编译器内置类型和标准库类型的职责分离。
 *
 * ## 架构分离
 *
 * ```
 * CangJieBuiltIns          StdlibTypes
 * ├─ builtInsModule        ├─ stdlibModule
 * ├─ Int8, Bool, Unit      ├─ Any, String, Array
 * └─ (编译器内置)          └─ (标准库)
 * ```
 *
 * ## 使用方式
 *
 * ```kotlin
 * val stdlibTypes = projectDescriptor.stdlibTypes
 * val anyType = stdlibTypes.anyType
 * val stringType = stdlibTypes.stringType
 * ```
 *
 * ## 创建时机
 *
 * StdlibTypes 在 stdlib 模块创建后立即创建，确保:
 * 1. stdlib 模块总是存在
 * 2. 不会出现空指针
 * 3. 生命周期与 stdlib 模块绑定
 *
 * @param stdlibModule 标准库模块描述符
 * @param storageManager 存储管理器，用于缓存
 *
 * @see CangJieBuiltIns
 */
class StdlibTypes(
    private val stdlibModule: ModuleDescriptor,
    private val storageManager: StorageManager
) {

    // ============================== Scopes ==============================


    /**
     * std.core 包的作用域
     */
    val STD_CORE_SCOPE get() = stdlibModule.getPackage(core).memberScope

    /**
     * std.sync 包的作用域
     */
    val STD_SYNC_SCOPE get() = stdlibModule.getPackage(sync).memberScope

    /**
     * std.ast 包的作用域
     */
    val STD_AST_SCOPE get() = stdlibModule.getPackage(ast).memberScope

    // ============================== Memoized Class Lookup ==============================

    /**
     * std.core 包中的类查找缓存
     */
    private val myStdCoreBuiltInClassesByName = storageManager.createMemoizedFunction { name: Name ->
        val classifier = STD_CORE_SCOPE.getContributedClassifier(
            name,
            NoLookupLocation.FROM_BUILTINS
        )
        if (classifier == null) {
            throw AssertionError("Stdlib class std.core.$name is not found")
        }
        if (classifier !is ClassDescriptor) {
            throw AssertionError("Must be a class descriptor $name, but was $classifier")
        }
        classifier
    }

    /**
     * std.sync 包中的类查找缓存
     */
    private val myStdSyncBuiltInClassesByName = storageManager.createMemoizedFunction { name: Name ->
        val classifier = STD_SYNC_SCOPE.getContributedClassifier(
            name,
            NoLookupLocation.FROM_BUILTINS
        )
        if (classifier == null) {
            throw AssertionError("Stdlib class std.sync.$name is not found")
        }
        if (classifier !is ClassDescriptor) {
            throw AssertionError("Must be a class descriptor $name, but was $classifier")
        }
        classifier
    }

    /**
     * std.ast 包中的类查找缓存
     */
    private val myStdAstBuiltInClassesByName = storageManager.createMemoizedFunction { name: Name ->
        val classifier = STD_AST_SCOPE.getContributedClassifier(
            name,
            NoLookupLocation.FROM_BUILTINS
        )
        if (classifier == null) {
            throw AssertionError("Stdlib class std.ast.$name is not found")
        }
        if (classifier !is ClassDescriptor) {
            throw AssertionError("Must be a class descriptor $name, but was $classifier")
        }
        classifier
    }

    // ============================== Helper Methods ==============================

    private fun getStdCoreClassByName(simpleName: Name): ClassDescriptor {
        return myStdCoreBuiltInClassesByName.invoke(simpleName)
    }

    private fun getStdCoreClassByName(simpleName: String): ClassAndEnumDescriptor {
        return myStdCoreBuiltInClassesByName.invoke(Name.identifier(simpleName))
    }

    private fun getStdSyncClassByName(simpleName: Name): ClassDescriptor {
        return myStdSyncBuiltInClassesByName.invoke(simpleName)
    }

    private fun getStdSyncClassByName(simpleName: String): ClassDescriptor {
        return myStdSyncBuiltInClassesByName.invoke(Name.identifier(simpleName))
    }

    private fun getStdAstClassByName(simpleName: String): ClassDescriptor {
        return myStdAstBuiltInClassesByName.invoke(Name.identifier(simpleName))
    }

    // ============================== std.core Types ==============================

    /**
     * std.core.Any 类描述符
     */
    val any: ClassDescriptor
        get() = getStdCoreClassByName("Any") as ClassDescriptor

    /**
     * std.core.Any 类型
     */
    val anyType: SimpleType
        get() = any.defaultType

    /**
     * std.core.String 类描述符
     *
     * 注意: string 属性名实际返回的是 "Any" 类，这看起来是个 bug
     * 保持原有行为以兼容现有代码
     */
    val string: ClassDescriptor
        get() = getStdCoreClassByName("String")as ClassDescriptor

    /**
     * std.core.String 类型
     */
    val stringType: SimpleType
        get() = string.defaultType

    /**
     * std.core.Object 类描述符
     */
    val `object`: ClassDescriptor
        get() = getStdCoreClassByName("Object")as ClassDescriptor

    /**
     * std.core.Object 类型
     */
    val objectType: CangJieType
        get() = `object`.defaultType
    /**
     * std.core.Option 枚举描述符
     */
    val option: EnumDescriptor
        get() = getStdCoreClassByName("Option") as EnumDescriptor

    /**
     * std.core.Option 类型
     */
    val optionType: SimpleType
        get() = option.defaultType
    /**
     * std.core.Array 类描述符
     */
    val array: ClassDescriptor
        get() = getStdCoreClassByName("Array")as ClassDescriptor

    /**
     * std.core.Array 类型
     */
    val arrayType: SimpleType
        get() = array.defaultType

    /**
     * std.core.Throwable 类描述符
     */
    val exception: ClassDescriptor
        get() = getStdCoreClassByName("Exception")as ClassDescriptor

    /**
     * std.core.Throwable 类型
     */
    val exceptionType: CangJieType
        get() = exception.defaultType

    /**
     * std.core.CType 类描述符
     */
    val ctype: ClassDescriptor
        get() = getStdCoreClassByName("CType")as ClassDescriptor

    /**
     * std.core.CType 类型
     */
    val ctypeType: SimpleType
        get() = ctype.defaultType

    /**
     * std.core.CPointer 类描述符
     */
    val cpointer: ClassDescriptor
        get() = getStdCoreClassByName("CPointer")as ClassDescriptor

    /**
     * std.core.CPointer 类型
     */
    val cpointerType: SimpleType
        get() = cpointer.defaultType

    /**
     * std.core.CFunc 类描述符
     */
    val cfunc: ClassDescriptor
        get() = getStdCoreClassByName("CFunc")as ClassDescriptor

    /**
     * std.core.CFunc 类型
     */
    val cfuncType: SimpleType
        get() = cfunc.defaultType

    /**
     * std.core.Range 类描述符
     */
    val range: ClassDescriptor
        get() = getStdCoreClassByName(RANGE)

    /**
     * std.core.Range 类型
     */
    val rangeType: SimpleType
        get() = range.defaultType

    /**
     * std.core.Resource 类描述符
     */
    val resource: ClassDescriptor
        get() = getStdCoreClassByName(RESOURCE)

    /**
     * std.core.Equatable 类描述符
     */
    val equatable: ClassDescriptor
        get() = getStdCoreClassByName(EQUATABLE)

    /**
     * std.core.Equatable 类型
     */
    val equatableType: SimpleType
        get() = equatable.defaultType

    /**
     * std.core.Comparable 类描述符
     */
    val comparable: ClassDescriptor
        get() = getStdCoreClassByName(COMPARABLE)

    /**
     * std.core.Comparable 类型
     */
    val comparableType: SimpleType
        get() = comparable.defaultType

    /**
     * std.core.Countable 类描述符
     */
    val countable: ClassDescriptor
        get() = getStdCoreClassByName(COUNTABLE)

    /**
     * std.core.Countable 类型
     */
    val countableType: SimpleType
        get() = countable.defaultType

    val iterable get() = getStdCoreClassByName(ITERABLE)
    val iterableType: SimpleType
        get() = iterable.defaultType

    // ============================== std.sync Types ==============================

    /**
     * std.sync.ReentrantMutex 类描述符
     */
    val reentrantMutex: ClassDescriptor
        get() = getStdSyncClassByName("ReentrantMutex")

    /**
     * std.sync.ReentrantMutex 类型
     */
    val reentrantMutexType: SimpleType
        get() = reentrantMutex.defaultType

    /**
     * std.sync.Future 类描述符
     */
    val future: ClassDescriptor
        get() = getStdSyncClassByName(FUTURE)

    /**
     * std.sync.Future 类型
     */
    val futureType: SimpleType
        get() = future.defaultType

    // ============================== std.ast Types ==============================

    /**
     * std.ast.Tokens 类描述符
     */
    val tokens: ClassDescriptor
        get() = getStdAstClassByName("Tokens")

    /**
     * std.ast.Tokens 类型
     */
    val tokensType: SimpleType
        get() = tokens.defaultType

    // ============================== Factory ==============================
    fun getArrayType(

        argument: CangJieType,
        annotations: Annotations
    ): SimpleType {
        val types =
            listOf(
                TypeArgumentImpl(

                    argument
                )
            )
        return CangJieTypeFactory.simpleNonOptionType(
            annotations.toDefaultAttributes(),
            array,
            types
        )
    }

    fun getArrayType(

        argument: CangJieType
    ): SimpleType {
        return getArrayType(

            argument,
            Annotations.EMPTY
        )
    }

    companion object {
        /**
         * 从 ModuleDescriptor 创建 StdlibTypes
         *
         * @param stdlibModule 标准库模块描述符
         * @param storageManager 存储管理器
         * @return StdlibTypes 实例
         * @throws IllegalArgumentException 如果 module 不是 stdlib 模块
         */
        fun create(
            stdlibModule: ModuleDescriptor,
            storageManager: StorageManager
        ): StdlibTypes {
            require(stdlibModule.name == StandardNames.STD_PACKAGE_NAME) {
                "Expected stdlib module, but got ${stdlibModule.name}"
            }
            return StdlibTypes(stdlibModule, storageManager)
        }
    }
}