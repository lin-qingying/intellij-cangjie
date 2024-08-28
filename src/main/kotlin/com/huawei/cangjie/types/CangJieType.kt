package com.huawei.cangjie.types

import com.huawei.cangjie.descriptors.annotations.Annotated
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.renderer.DescriptorRenderer
import com.huawei.cangjie.renderer.DescriptorRendererOptions
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.error.ErrorType
import com.huawei.cangjie.types.model.CangJieTypeMarker
import com.huawei.cangjie.types.model.FlexibleTypeMarker
import com.huawei.cangjie.types.model.SimpleTypeMarker
import com.huawei.cangjie.types.model.TypeArgumentListMarker

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
    abstract fun makeNullableAsSpecified(newNullability: Boolean): UnwrappedType

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

    override fun makeNullableAsSpecified(newNullability: Boolean) = this

    override fun replaceAttributes(newAttributes: TypeAttributes) = this
    override fun hashCode(): Int {
        var result = constructor.hashCode()
        result = 31 * result + memberScope.hashCode()
        result = 31 * result + typeName.hashCode()
        return result
    }
}

abstract class SimpleType : UnwrappedType(), SimpleTypeMarker, TypeArgumentListMarker {
    abstract override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType
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


