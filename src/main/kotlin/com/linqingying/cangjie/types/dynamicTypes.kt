package com.linqingying.cangjie.types

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.container.PlatformSpecificExtension
import com.linqingying.cangjie.renderer.DescriptorRenderer
import com.linqingying.cangjie.renderer.DescriptorRendererOptions
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.model.DynamicTypeMarker
import com.linqingying.cangjie.types.util.builtIns


@DefaultImplementation(impl = DynamicTypesSettings::class)
open class DynamicTypesSettings : PlatformSpecificExtension<DynamicTypesSettings> {
    open val dynamicTypesAllowed: Boolean
        get() = false
}

class DynamicTypesAllowed : DynamicTypesSettings() {
    override val dynamicTypesAllowed: Boolean
        get() = true
}

fun CangJieType.isDynamic(): Boolean = unwrap() is DynamicType

fun createDynamicType(builtIns: CangJieBuiltIns) = DynamicType(builtIns, TypeAttributes.Empty)

class DynamicType(
    builtIns: CangJieBuiltIns,
    override val attributes: TypeAttributes
) : FlexibleType(builtIns.nothingType, builtIns.anyType), DynamicTypeMarker {
    override val delegate: SimpleType get() = upperBound


    override val isMarkedOption: Boolean get() = false

    override fun replaceAttributes(newAttributes: TypeAttributes): DynamicType =
        DynamicType(delegate.builtIns, newAttributes)


    // Nullability has no effect on dynamics
    override fun makeOptionalAsSpecified(newNullability: Boolean): DynamicType = this


    override fun render(renderer: DescriptorRenderer, options: DescriptorRendererOptions): String = "dynamic"

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) = this
}
