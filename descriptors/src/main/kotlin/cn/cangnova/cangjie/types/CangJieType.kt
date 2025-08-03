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

package cn.cangnova.cangjie.types

import cn.cangnova.cangjie.descriptors.annotations.Annotated
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.renderer.DescriptorRenderer
import cn.cangnova.cangjie.renderer.DescriptorRendererOptions
import cn.cangnova.cangjie.resolve.scopes.InstanceMemberScope
import cn.cangnova.cangjie.resolve.scopes.MemberScope
import cn.cangnova.cangjie.resolve.scopes.StaticMemberScope
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.types.error.ErrorType
import cn.cangnova.cangjie.types.model.CangJieTypeMarker
import cn.cangnova.cangjie.types.model.FlexibleTypeMarker
import cn.cangnova.cangjie.types.model.SimpleTypeMarker
import cn.cangnova.cangjie.types.model.TypeArgumentListMarker

fun CangJieType.isNullable(): Boolean = TypeUtils.isNullableType(this)

val CangJieType.isError: Boolean
    get() = unwrap().let { unwrapped ->
        unwrapped is ErrorType
//                ||
//                (unwrapped is FlexibleType && unwrapped.delegate is ErrorType)
    }

interface SubtypingRepresentatives {
    val subTypeRepresentative: CangJieType
    val superTypeRepresentative: CangJieType

    fun sameTypeConstructor(type: CangJieType): Boolean
}

sealed class CangJieType : Annotated, CangJieTypeMarker {
    abstract fun unwrap(): UnwrappedType
    abstract val constructor: TypeConstructor
    abstract val arguments: List<TypeProjection>
    abstract val attributes: TypeAttributes
    override val annotations: Annotations
        get() = attributes.annotations

    //    Option 枚举语法糖
    abstract val isMarkedOption: Boolean
    abstract val memberScope: MemberScope

    //    静态
    val staticMemberScope: MemberScope get() = StaticMemberScope(memberScope)

    //    实例
    val instanceMemberScope: MemberScope get() = InstanceMemberScope(memberScope)

    //    是否为扩展类型的原父类型
    var isExtensionType: Boolean = false


    /**
     * Returns refined type using passed CangJieTypeRefiner
     *
     * Refined type has its member scope refined
     *
     * Note #1: supertypes and type arguments ARE NOT refined!
     *
     * Note #2: Correct subtyping or equality for refined types from different Refiners *is not guaranteed*
     *
     * Implementation notice:
     * Basically, this is a simple form of double-dispatching, used to incapsulate
     * structure of specific type-implementations, which means that compound types most probably would like
     * to implement it by recursively calling [refine] on components.
     * A very few "basic" types (like [SimpleTypeImpl]) implement it by actually adjusting
     * content using passed refiner and other low-level methods
     */
    @TypeRefinement
    abstract fun refine(cangjieTypeRefiner: CangJieTypeRefiner): CangJieType

}

sealed class UnwrappedType : CangJieType() {
    final override fun unwrap(): UnwrappedType = this
    abstract fun replaceAttributes(newAttributes: TypeAttributes): UnwrappedType
    abstract fun makeOptionalAsSpecified(newNullability: Boolean): UnwrappedType

    @TypeRefinement
    abstract override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): UnwrappedType
}

class BasicType(

    override val constructor: TypeConstructor,
    override val memberScope: MemberScope
) : SimpleType() {

    override val arguments: List<TypeProjection>
        get() = listOf()
    override val attributes: TypeAttributes
        get() = TypeAttributes.Empty
    override val isMarkedOption: Boolean
        get() = false


    override fun equals(other: Any?): Boolean {
        if (other !is BasicType) return false
        if (this === other) return true

        return typeName == other.typeName
    }

    val typeName = constructor.declarationDescriptor?.name ?: ""


    override fun toString(): String {
        return "$typeName"
    }

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): UnwrappedType = this

    override fun makeOptionalAsSpecified(newNullability: Boolean) = when {
        newNullability == isMarkedOption -> this
//        newNullability -> OptionalSimpleType(this.toOptionalType() as SimpleType)
        else -> NotNullSimpleType(this)
    }
    override fun replaceAttributes(newAttributes: TypeAttributes) = this
    override fun hashCode(): Int {
        var result = constructor.hashCode()
        result = 31 * result + memberScope.hashCode()
        result = 31 * result + typeName.hashCode()
        return result
    }
}

abstract class SimpleType : UnwrappedType(), SimpleTypeMarker, TypeArgumentListMarker {
    abstract override fun makeOptionalAsSpecified(newNullability: Boolean): SimpleType
    abstract override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType
    override fun toString(): String {
        return buildString {
            for (annotation in annotations) {
                append("[", DescriptorRenderer.DEBUG_TEXT.renderAnnotation(annotation), "] ")
            }

            append(constructor)
            if (arguments.isNotEmpty()) arguments.joinTo(this, separator = ", ", prefix = "<", postfix = ">")
//            if (isMarkedOption) append("?")
        }
    }
}


class ThisType(private val otype: SimpleType) : SimpleType() {
    override fun makeOptionalAsSpecified(newNullability: Boolean): SimpleType {
        return otype.makeOptionalAsSpecified(newNullability)

    }

    fun getType(): CangJieType {
        return otype.arguments[0].type
    }

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType {
        return otype.replaceAttributes(newAttributes)

    }

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): UnwrappedType {
        return otype.refine(cangjieTypeRefiner)
    }

    override val constructor: TypeConstructor
        get() = otype.constructor
    override val arguments: List<TypeProjection>
        get() = otype.arguments
    override val attributes: TypeAttributes
        get() = otype.attributes
    override val isMarkedOption: Boolean
        get() = false
    override val memberScope: MemberScope
        get() = otype.memberScope
}


//使用语法糖声明的Option类型   表现为  ?Int  只在声明时才会实例化此类型

//使用语法糖声明的Option类型   表现为  ?Int  只在声明时才会实例化此类型
class OptionType(  val otype: SimpleType) : SimpleType() {
    override fun makeOptionalAsSpecified(newNullability: Boolean): SimpleType {
        return otype.makeOptionalAsSpecified(newNullability)

    }

    fun getType(): CangJieType {
        return otype.arguments[0].type
    }

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType {
        return OptionType(otype.replaceAttributes(newAttributes))

    }

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): UnwrappedType {
        return otype.refine(cangjieTypeRefiner)
    }

    override val constructor: TypeConstructor
        get() = otype.constructor
    override val arguments: List<TypeProjection>
        get() = otype.arguments
    override val attributes: TypeAttributes
        get() = otype.attributes
    override val isMarkedOption: Boolean
        get() = true
    override val memberScope: MemberScope
        get() = otype.memberScope

}


// lowerBound is a subtype of upperBound
abstract class FlexibleType(val lowerBound: SimpleType, val upperBound: SimpleType) :
    UnwrappedType(), SubtypingRepresentatives, FlexibleTypeMarker {

    abstract val delegate: SimpleType

    override val subTypeRepresentative: CangJieType
        get() = lowerBound
    override val superTypeRepresentative: CangJieType
        get() = upperBound

    override fun sameTypeConstructor(type: CangJieType) = false

    abstract fun render(renderer: DescriptorRenderer, options: DescriptorRendererOptions): String

    override val attributes: TypeAttributes get() = delegate.attributes
    override val constructor: TypeConstructor get() = delegate.constructor
    override val arguments: List<TypeProjection> get() = delegate.arguments
    override val isMarkedOption: Boolean get() = delegate.isMarkedOption
    override val memberScope: MemberScope get() = delegate.memberScope

    override fun toString(): String = DescriptorRenderer.DEBUG_TEXT.renderType(this)

//    @TypeRefinement
//    abstract override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): FlexibleType
}


// This method used for transform type to simple type afler substitution
fun CangJieType.asSimpleType(): SimpleType {
    return unwrap() as? SimpleType ?: error("This is should be simple type: $this")
}


