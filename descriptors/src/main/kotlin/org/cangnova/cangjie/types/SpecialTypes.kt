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

/*
<html>None of the following candidates is applicable:<br/>constructor(captureStatus: CaptureStatus, constructor: CapturedTypeConstructor, lowerType: UnwrappedType?, attributes: TypeAttributes = ..., isOption: Boolean = ..., isProjectionNotNull: Boolean = ...): CapturedType<br/>constructor(captureStatus: CaptureStatus, lowerType: UnwrappedType?, argument: TypeArgument, typeParameter: TypeParameterDescriptor): CapturedType * 仓颉类型系统特殊类型定义
 *
 * 本文件定义了仓颉语言类型系统中的特殊类型，包括：
 * - 类型缩写（AbbreviatedType）
 * - 明确非Option类型（DefinitelyNonOptionType）
 * - 各种类型包装与委托类型
 *
 * 主要用于类型推断、类型检查、类型属性替换、Option判定等场景。
 *
 * 仓颉语言无null/nullable概念，所有“可选”相关逻辑均用Option表达。
 */

package org.cangnova.cangjie.types

import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.impl.TypeParameterDescriptorImpl
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.checker.CapturedType
import org.cangnova.cangjie.types.checker.TypeVariableConstructor
import org.cangnova.cangjie.types.checker.OptionChecker


/**
 * 类型缩写（如类型别名）
 * 用于表示类型的缩写形式和展开形式
 * 例如：类型别名、简化类型显示等
 */
fun SimpleType.withAbbreviation(abbreviatedType: SimpleType): SimpleType {
    if (isError) return this
    return AbbreviatedType(this, abbreviatedType)
}

 /**
 * 获取类型的缩写形式
 */
fun CangJieType.getAbbreviation(): SimpleType? = getAbbreviatedType()?.abbreviation
/**
 * 获取类型的缩写类型对象
 */
fun CangJieType.getAbbreviatedType(): AbbreviatedType? = unwrap() as? AbbreviatedType

/**
 * 类型缩写类型
 * @property delegate 展开后的类型
 * @property abbreviation 缩写形式
 */
class AbbreviatedType(override val delegate: SimpleType, val abbreviation: SimpleType) : DelegatingSimpleType() {
    val expandedType: SimpleType get() = delegate

    /**
     * 替换类型属性，返回新的缩写类型
     */
    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType =
        AbbreviatedType(delegate.replaceAttributes(newAttributes), abbreviation)

    /**
     * 按指定Option状态转换类型
     */
    override fun makeOptionAsSpecified(isOption: Boolean) =
        AbbreviatedType(
            delegate.makeOptionAsSpecified(isOption),
            abbreviation.makeOptionAsSpecified(isOption)
        )
    /**
     * 替换委托类型
     */
    
    override fun replaceDelegate(delegate: SimpleType) = AbbreviatedType(delegate, abbreviation)
}

/**
 * 委托简单类型基类
 * 用于实现类型包装、属性委托等
 */
abstract class DelegatingSimpleType : SimpleType() {
    protected abstract val delegate: SimpleType
    override val constructor: TypeConstructor get() = delegate.constructor
    override val arguments: List<TypeArgument> get() = delegate.arguments
    override val isOption: Boolean get() = delegate.isOption
    override val memberScope: MemberScope get() = delegate.memberScope
    override val attributes: TypeAttributes get() = delegate.attributes

    /**
     * 替换委托类型
     */
    
    abstract fun replaceDelegate(delegate: SimpleType): DelegatingSimpleType

    /**
     * 类型精化，返回新的委托类型
     */
    
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): SimpleType =
        replaceDelegate(cangjieTypeRefiner.refineType(delegate) as SimpleType)
}

/**
 * 类型包装基类
 * 用于实现类型的延迟计算、属性委托等
 */
abstract class WrappedType : CangJieType() {
    open fun isComputed(): Boolean = true
    protected abstract val delegate: CangJieType
    override val constructor: TypeConstructor get() = delegate.constructor
    override val arguments: List<TypeArgument> get() = delegate.arguments
    override val isOption: Boolean get() = delegate.isOption
    override val memberScope: MemberScope get() = delegate.memberScope
    override val attributes: TypeAttributes get() = delegate.attributes

    /**
     * 解包类型，返回最内层未包装类型
     */
    final override fun unwrap(): UnwrappedType {
        var result = delegate
        while (result is WrappedType) {
            result = result.delegate
        }
        return result as UnwrappedType
    }

    override fun toString(): String {
        return if (isComputed()) {
            delegate.toString()
        } else {
            "<Not computed yet>"
        }
    }
}

/**
 * 延迟类型包装
 * 用于实现类型的惰性计算
 */
class LazyWrappedType(
    private val storageManager: StorageManager,
    private val computation: () -> CangJieType
) : WrappedType() {
    private val lazyValue = storageManager.createLazyValue(computation)
    override val delegate: CangJieType get() = lazyValue()
    override fun isComputed(): Boolean = lazyValue.isComputed()
    /**
     * 类型精化，返回新的延迟包装类型
     */
    
    
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) = LazyWrappedType(storageManager) {
        cangjieTypeRefiner.refineType(computation())
    }
}







