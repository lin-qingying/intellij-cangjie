package com.huawei.cangjie.builtins

import com.huawei.cangjie.builtins.functions.AllowedToUsedOnlyInK1
import com.huawei.cangjie.builtins.functions.FunctionTypeKind
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.constants.IntValue
import com.huawei.cangjie.resolve.constants.StringValue
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.util.asTypeProjection
import com.huawei.cangjie.types.util.replaceAnnotations
import com.intellij.util.containers.addIfNotNull


fun getFunctionTypeArgumentProjections(
    receiverType: CangJieType?,
    contextReceiverTypes: List<CangJieType>,
    parameterTypes: List<CangJieType>,
    parameterNames: List<Name>?,
    returnType: CangJieType,
    builtIns: CangJieBuiltIns
): List<TypeProjection> {
    val arguments = ArrayList<TypeProjection>(parameterTypes.size + contextReceiverTypes.size + (if (receiverType != null) 1 else 0) + 1)

    arguments.addAll(contextReceiverTypes.map { it.asTypeProjection() })
    arguments.addIfNotNull(receiverType?.asTypeProjection())

    parameterTypes.mapIndexedTo(arguments) { index, type ->
        val name = parameterNames?.get(index)?.takeUnless { it.isSpecial }
        val typeToUse = if (name != null) {
            val parameterNameAnnotation = BuiltInAnnotationDescriptor(
                builtIns,
                StandardNames.FqNames.parameterName,
                mapOf(StandardNames.NAME to StringValue(name.asString()))
            )
            type.replaceAnnotations(Annotations.create(type.annotations + parameterNameAnnotation))
        }
        else {
            type
        }
        typeToUse.asTypeProjection()
    }

    arguments.add(returnType.asTypeProjection())

    return arguments
}
@JvmOverloads
fun createFunctionType(
    builtIns: CangJieBuiltIns,
    annotations: Annotations,
    receiverType: CangJieType?,
    contextReceiverTypes: List<CangJieType>,
    parameterTypes: List<CangJieType>,
    parameterNames: List<Name>?,
    returnType: CangJieType,
    suspendFunction: Boolean = false
): SimpleType {
    val arguments =
        getFunctionTypeArgumentProjections(receiverType, contextReceiverTypes, parameterTypes, parameterNames, returnType, builtIns)
    val parameterCount = parameterTypes.size + contextReceiverTypes.size + if (receiverType == null) 0 else 1
    val classDescriptor = getFunctionDescriptor(builtIns, parameterCount, suspendFunction)

    // TODO: preserve laziness of given annotations
    var typeAnnotations = annotations
    if (receiverType != null) typeAnnotations = typeAnnotations.withExtensionFunctionAnnotation(builtIns)
    if (contextReceiverTypes.isNotEmpty()) typeAnnotations =
        typeAnnotations.withContextReceiversFunctionAnnotation(builtIns, contextReceiverTypes.size)

    return CangJieTypeFactory.simpleNotNullType(typeAnnotations.toDefaultAttributes(), classDescriptor, arguments)
}

fun Annotations.withContextReceiversFunctionAnnotation(builtIns: CangJieBuiltIns, contextReceiversCount: Int) =
    if (hasAnnotation(StandardNames.FqNames.contextFunctionTypeParams)) {
        this
    } else {
        Annotations.create(
            this + BuiltInAnnotationDescriptor(
                builtIns, StandardNames.FqNames.contextFunctionTypeParams, mapOf(
                    StandardNames.CONTEXT_FUNCTION_TYPE_PARAMETER_COUNT_NAME to IntValue(contextReceiversCount)
                )
            )
        )
    }
fun Annotations.withExtensionFunctionAnnotation(builtIns: CangJieBuiltIns) =
    if (hasAnnotation(StandardNames.FqNames.extensionFunctionType)) {
        this
    } else {
        Annotations.create(this + BuiltInAnnotationDescriptor(builtIns, StandardNames.FqNames.extensionFunctionType, emptyMap()))
    }
val DeclarationDescriptor.fqNameUnsafe: FqNameUnsafe
    get() = DescriptorUtils.getFqName(this)
fun getFunctionDescriptor(builtIns: CangJieBuiltIns, parameterCount: Int, isSuspendFunction: Boolean = false) =
 /*   if (isSuspendFunction) builtIns.getSuspendFunction(parameterCount) else*/ builtIns.getFunction(parameterCount)

fun DeclarationDescriptor.getFunctionTypeKind(): FunctionTypeKind? {
    if (this !is ClassDescriptor) return null
//    if (!CangJieBuiltIns.isUnderCangJiePackage(this)) return null

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
