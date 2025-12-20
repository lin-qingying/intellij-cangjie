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

package org.cangnova.cangjie.types.checker

import org.cangnova.cangjie.container.DefaultImplementation
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.model.CangJieTypeMarker

/**
 * 类型精化支持配置
 *
 * 用于控制模块级别的类型精化行为
 *
 * @param isEnabled 是否启用类型精化
 */
sealed class TypeRefinementSupport(val isEnabled: Boolean) {
    /** 禁用类型精化 */
    object Disabled : TypeRefinementSupport(isEnabled = false)

    /** 启用但未初始化 */
    object EnabledUninitialized : TypeRefinementSupport(isEnabled = true)

    /** 已启用并初始化的类型精化 */
    class Enabled(val typeRefiner: CangJieTypeRefiner) : TypeRefinementSupport(isEnabled = true)
}

/** 类型精化器的模块能力键 */
val REFINER_CAPABILITY = ModuleCapability<Ref<TypeRefinementSupport>>("CangJieTypeRefiner")

/**
 * 可变引用包装器
 *
 * 用于存储可变的类型精化支持配置
 */
class Ref<T : Any>(var value: T)

/**
 * 批量精化类型列表
 *
 * @param types 要精化的类型集合
 * @return 精化后的类型列表
 */
fun CangJieTypeRefiner.refineTypes(types: Iterable<CangJieType>): List<CangJieType> = types.map { refineType(it) }

/**
 * 仓颉类型精化器
 *
 * 类型精化（Type Refinement）用于在跨模块边界时细化类型信息。
 * 这对于处理增量编译、多模块项目和类型别名等场景非常重要。
 *
 * 主要功能：
 * - 精化类型定义
 * - 精化父类型列表
 * - 精化描述符
 * - 缓存精化后的作用域
 *
 * 默认实现不执行任何精化操作（直接返回原始类型）
 */
@DefaultImplementation(impl = AbstractTypeRefiner.Default::class)
abstract class CangJieTypeRefiner : AbstractTypeRefiner(){

    /**
     * 精化类型
     *
     * @param type 要精化的类型
     * @return 精化后的类型
     */
    abstract override fun refineType(type: CangJieTypeMarker): CangJieType

    /**
     * 精化类的父类型列表
     *
     * @param classDescriptor 类描述符
     * @return 精化后的父类型集合
     */
    abstract fun refineSupertypes(classDescriptor: ClassifierDescriptorWithTypeConstructor): Collection<CangJieType>

    /**
     * 精化描述符
     *
     * 将声明描述符精化为分类器描述符（如果适用）
     *
     * @param descriptor 要精化的描述符
     * @return 精化后的分类器描述符，如果不适用则返回 null
     */
    abstract fun refineDescriptor(descriptor: DeclarationDescriptor): ClassifierDescriptor?

    /**
     * 检查模块是否需要类型精化
     *
     * @param moduleDescriptor 模块描述符
     * @return 如果需要精化则返回 true
     */
    abstract fun isRefinementNeededForModule(moduleDescriptor: ModuleDescriptor): Boolean

    /**
     * 检查类型构造器是否需要精化
     *
     * @param typeConstructor 类型构造器
     * @return 如果需要精化则返回 true
     */
    abstract fun isRefinementNeededForTypeConstructor(typeConstructor: TypeConstructor): Boolean

    /**
     * 获取或创建类的缓存作用域
     *
     * 用于避免重复计算类的成员作用域
     *
     * @param classDescriptor 类描述符
     * @param compute 作用域计算函数
     * @return 缓存的或新计算的作用域
     */
    abstract fun <S : MemberScope> getOrPutScopeForClass(classDescriptor: ClassifierDescriptor, compute: () -> S): S

    /**
     * 精化枚举类型
     *
     * @param enumType 要精化的枚举类型
     * @return 精化后的枚举类型
     */
    abstract fun refineEnumType(enumType: EnumType): EnumType

    /**
     * 默认的类型精化器实现
     *
     * 不执行任何精化操作，直接返回原始值
     */
    object Default : CangJieTypeRefiner() {

        /** 直接返回原始类型，不进行精化 */
        override fun refineType(type: CangJieTypeMarker): CangJieType = type as CangJieType

        /** 返回 null，表示不需要精化描述符 */
        override fun refineDescriptor(descriptor: DeclarationDescriptor): ClassifierDescriptor? {
            return null

        }

        /** 直接返回类的原始父类型列表 */
        override fun refineSupertypes(classDescriptor: ClassifierDescriptorWithTypeConstructor): Collection<CangJieType> {
            return classDescriptor.typeConstructor.supertypes
        }

        /** 始终返回 false，表示不需要对模块进行精化 */
        override fun isRefinementNeededForModule(moduleDescriptor: ModuleDescriptor): Boolean {
            return false
        }

        /** 始终返回 false，表示不需要对类型构造器进行精化 */
        override fun isRefinementNeededForTypeConstructor(typeConstructor: TypeConstructor): Boolean {
            return false
        }

        /** 不使用缓存，直接计算作用域 */
        override fun <S : MemberScope> getOrPutScopeForClass(classDescriptor: ClassifierDescriptor, compute: () -> S): S {
            return compute()
        }

        /** 直接返回原始枚举类型，不进行精化 */
        override fun refineEnumType(enumType: EnumType): EnumType {
            return enumType
        }
    }
}
