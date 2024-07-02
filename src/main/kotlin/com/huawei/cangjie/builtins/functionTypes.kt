package com.huawei.cangjie.builtins

import com.huawei.cangjie.builtins.functions.AllowedToUsedOnlyInK1
import com.huawei.cangjie.builtins.functions.FunctionTypeKind
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.constants.IntValue
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeProjection

val DeclarationDescriptor.fqNameUnsafe: FqNameUnsafe
    get() = DescriptorUtils.getFqName(this)

fun DeclarationDescriptor.getFunctionTypeKind(): FunctionTypeKind? {
    if (this !is ClassDescriptor) return null
//    if (!CangJieBuiltIns.isUnderKotlinPackage(this)) return null

    return fqNameUnsafe.getFunctionTypeKind()
}

@OptIn(AllowedToUsedOnlyInK1::class)
private fun FqNameUnsafe.getFunctionTypeKind(): FunctionTypeKind? {
    if (!isSafe || isRoot) return null
    TODO()
//    return FunctionTypeKindExtractor.Default.getFunctionalClassKind(toSafe().parent(), shortName().asString())
}

val DeclarationDescriptor.isBuiltinFunctionalClassDescriptor: Boolean
    get() {
        return false
//        val functionalClassKind = getFunctionTypeKind()
//        return functionalClassKind == FunctionTypeKind.Function ||
//                functionalClassKind == FunctionTypeKind.SuspendFunction
    }

fun CangJieType.contextFunctionTypeParamsCount(): Int {
    val annotationDescriptor = annotations.findAnnotation(StandardNames.FqNames.contextFunctionTypeParams) ?: return 0
    val constantValue =
        annotationDescriptor.allValueArguments.getValue(StandardNames.CONTEXT_FUNCTION_TYPE_PARAMETER_COUNT_NAME)
    return (constantValue as IntValue).value


}

fun CangJieType.getContextReceiverTypesFromFunctionType(): List<CangJieType> {
    assert(isBuiltinFunctionalType) { "Not a function type: $this" }
    val contextReceiversCount = contextFunctionTypeParamsCount()
    return if (contextReceiversCount == 0) {
        emptyList()
    } else {
        arguments.subList(0, contextReceiversCount).map { it.type }
    }
}
fun CangJieType.getValueParameterTypesFromFunctionType(): List<TypeProjection> {
    assert(isBuiltinFunctionalType) { "Not a function type: $this" }
    val arguments = arguments
    val first = contextFunctionTypeParamsCount() + if (isBuiltinExtensionFunctionalType) 1 else 0
    val last = arguments.size - 1
    assert(first <= last) { "Not an exact function type: $this" }
    return arguments.subList(first, last)
}

private val CangJieType.isTypeAnnotatedWithExtensionFunctionType: Boolean
    get() = annotations.findAnnotation(StandardNames.FqNames.extensionFunctionType) != null
val CangJieType.isBuiltinExtensionFunctionalType: Boolean
    get() = isBuiltinFunctionalType && isTypeAnnotatedWithExtensionFunctionType

val CangJieType.isBuiltinFunctionalType: Boolean
    get() = constructor.declarationDescriptor?.isBuiltinFunctionalClassDescriptor == true

fun CangJieType.getReceiverTypeFromFunctionType(): CangJieType? {
//    assert(isBuiltinFunctionalType) { "Not a function type: $this" }
    if (!isTypeAnnotatedWithExtensionFunctionType) {
        return null
    }
    val index = contextFunctionTypeParamsCount()
    return arguments[index].type
}

val CangJieType.functionTypeKind: FunctionTypeKind?
    get() = constructor.declarationDescriptor?.getFunctionTypeKind()

val CangJieType.isFunctionType: Boolean
    get() = functionTypeKind == FunctionTypeKind.Function

val CangJieType.isNonExtensionFunctionType: Boolean
    get() = isFunctionType && !isTypeAnnotatedWithExtensionFunctionType
