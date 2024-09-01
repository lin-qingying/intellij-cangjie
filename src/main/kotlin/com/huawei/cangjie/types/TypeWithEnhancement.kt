package com.huawei.cangjie.types

import com.huawei.cangjie.renderer.DescriptorRenderer
import com.huawei.cangjie.renderer.DescriptorRendererOptions
import com.huawei.cangjie.types.checker.CangJieTypeRefiner

interface TypeWithEnhancement {
    val origin: UnwrappedType
    val enhancement: CangJieType
}
fun UnwrappedType.inheritEnhancement(origin: CangJieType): UnwrappedType = wrapEnhancement(origin.getEnhancement())

fun UnwrappedType.inheritEnhancement(origin: CangJieType, transform: (CangJieType) -> CangJieType): UnwrappedType =
    wrapEnhancement(origin.getEnhancement()?.let(transform))
fun CangJieType.getEnhancement(): CangJieType? = when (this) {
//    is TypeWithEnhancement -> enhancement
    else -> null
}
fun UnwrappedType.wrapEnhancement(enhancement: CangJieType?): UnwrappedType {
    if (this is TypeWithEnhancement) {
        return origin.wrapEnhancement(enhancement)
    }
    if (enhancement == null || enhancement == this) {
        return this
    }
    return when (this) {
        is SimpleType -> SimpleTypeWithEnhancement(this, enhancement)
        is FlexibleType -> FlexibleTypeWithEnhancement(this, enhancement)
    }
}

class FlexibleTypeWithEnhancement(
    override val origin: FlexibleType,
    override val enhancement: CangJieType
) : FlexibleType(origin.lowerBound, origin.upperBound),
    TypeWithEnhancement {

    override fun replaceAttributes(newAttributes: TypeAttributes): UnwrappedType =
        origin.replaceAttributes(newAttributes).wrapEnhancement(enhancement)

    override fun makeOptionalAsSpecified(newNullability: Boolean): UnwrappedType =
        origin.makeOptionalAsSpecified(newNullability).wrapEnhancement(enhancement.unwrap().makeOptionalAsSpecified(newNullability))


    override fun render(renderer: DescriptorRenderer, options: DescriptorRendererOptions): String {
//        if (options.enhancedTypes) {
//            return renderer.renderType(enhancement)
//        }
        return origin.render(renderer, options)
    }

    override val delegate: SimpleType get() = origin.delegate

    @TypeRefinement
    @OptIn(TypeRefinement::class)
    override fun refine(cangjieTypeRefiner:CangJieTypeRefiner) =
        FlexibleTypeWithEnhancement(
            cangjieTypeRefiner.refineType(origin) as FlexibleType,
            cangjieTypeRefiner.refineType(enhancement)
        )

    override fun toString(): String =
        "[@EnhancedForWarnings($enhancement)] $origin"
}
class SimpleTypeWithEnhancement(
    override val delegate: SimpleType,
    override val enhancement: CangJieType
) : DelegatingSimpleType(),
    TypeWithEnhancement {

    override val origin get() = delegate

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType =
        origin.replaceAttributes(newAttributes).wrapEnhancement(enhancement) as SimpleType
//
    override fun makeOptionalAsSpecified(newNullability: Boolean): SimpleType = origin.makeOptionalAsSpecified(newNullability)
        .wrapEnhancement(enhancement.unwrap().makeOptionalAsSpecified(newNullability)) as SimpleType

    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) = SimpleTypeWithEnhancement(delegate, enhancement)
//
//    @TypeRefinement
//    @OptIn(TypeRefinement::class)
//    override fun refine(cangnjieTypeRefiner: CangJieTypeRefiner): SimpleTypeWithEnhancement =
//        SimpleTypeWithEnhancement(
//            cangnjieTypeRefiner.refineType(delegate) as SimpleType,
//            cangnjieTypeRefiner.refineType(enhancement)
//        )

    override fun toString(): String =
        "[@EnhancedForWarnings($enhancement)] $origin"
}
