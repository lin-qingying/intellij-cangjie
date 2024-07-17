package com.huawei.cangjie.resolve.constants

import com.huawei.cangjie.builtins.StandardNames
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.findClassAcrossModuleDependencies

fun hasUnsignedTypesInModuleDependencies(module: ModuleDescriptor): Boolean {
    return module.findClassAcrossModuleDependencies(StandardNames.FqNames.uInt32ClassId) != null
}
interface CompileTimeConstant<out T>{
    val isError: Boolean
        get() = false
    val parameters: Parameters

    data class Parameters(
        val canBeUsedInAnnotation: Boolean,
        val isPure: Boolean,
        // `isUnsignedNumberLiteral` means that this constant represents simple number literal with `u` suffix (123u, 0xFEu)
        val isUnsignedNumberLiteral: Boolean,
        // `isUnsignedLongNumberLiteral` means that this constant represents simple number literal with `{uU}{lL}` suffix (123uL, 0xFEUL)
        val isUnsignedLongNumberLiteral: Boolean,
        val usesVariableAsConstant: Boolean,
        val usesNonConstValAsConstant: Boolean,
        // `isConvertableConstVal` means that this is `const val` that can participate in signed to unsigned conversion
        // see LanguageFeature.ImplicitSignedToUnsignedIntegerConversion
        val isConvertableConstVal: Boolean
    )
}
