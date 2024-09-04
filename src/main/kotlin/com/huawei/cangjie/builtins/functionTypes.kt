package com.huawei.cangjie.builtins

import com.huawei.cangjie.builtins.functions.AllowedToUsedOnlyInK1
import com.huawei.cangjie.builtins.functions.FunctionTypeKind
import com.huawei.cangjie.builtins.functions.FunctionTypeKindExtractor
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.constants.Int32Value
import com.huawei.cangjie.resolve.constants.StringValue
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.util.asTypeProjection
import com.huawei.cangjie.types.util.replaceAnnotations
import com.huawei.cangjie.types.util.supertypes
import com.huawei.cangjie.utils.DFS
import com.intellij.util.containers.addIfNotNull


/**
 * 函数类型作为内置类型实现
 */



val CangJieType.isExtensionFunctionType: Boolean
    get() = isFunctionType && isTypeAnnotatedWithExtensionFunctionType

fun CangJieType.getValueParameterTypesFromCallableReflectionType(isCallableTypeWithExtension: Boolean): List<TypeProjection> {
//    assert(ReflectionTypes.isCCallableType(this)) { "Not a callable reflection type: $this" }
    val arguments = arguments
    val first = if (isCallableTypeWithExtension) 1 else 0
    val last = arguments.size - 1
    assert(first <= last) { "Not an exact function type: $this" }
    return arguments.subList(first, last)
}

fun getFunctionTypeArgumentProjections(
    receiverType: CangJieType?,
    contextReceiverTypes: List<CangJieType>,
    parameterTypes: List<CangJieType>,
    parameterNames: List<Name>?,
    returnType: CangJieType,
    builtIns: CangJieBuiltIns
): List<TypeProjection> {
    val arguments =
        ArrayList<TypeProjection>(parameterTypes.size + contextReceiverTypes.size + (if (receiverType != null) 1 else 0) + 1)

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
        } else {
            type
        }
        typeToUse.asTypeProjection()
    }

    arguments.add(returnType.asTypeProjection())

    return arguments
}

@JvmOverloads
fun createFunctionType(
//    memberScope: MemberScope,

    builtIns: CangJieBuiltIns,
    annotations: Annotations,
    receiverType: CangJieType?,
    contextReceiverTypes: List<CangJieType>,
    parameterTypes: List<CangJieType>,
    parameterNames: List<Name>?,
    returnType: CangJieType,

    ): SimpleType {
    val arguments =
        getFunctionTypeArgumentProjections(
            receiverType,
            contextReceiverTypes,
            parameterTypes,
            parameterNames,
            returnType,
            builtIns
        )
    val parameterCount = parameterTypes.size + contextReceiverTypes.size + if (receiverType == null) 0 else 1
    val classDescriptor = getFunctionDescriptor(builtIns, parameterCount)
//    val classDescriptor = c.scope.getFunctionClassDescriptor(parameterCount)!!


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
                    StandardNames.CONTEXT_FUNCTION_TYPE_PARAMETER_COUNT_NAME to Int32Value(contextReceiversCount)
                )
            )
        )
    }

fun Annotations.withExtensionFunctionAnnotation(builtIns: CangJieBuiltIns) =
    if (hasAnnotation(StandardNames.FqNames.extensionFunctionType)) {
        this
    } else {
        Annotations.create(
            this + BuiltInAnnotationDescriptor(
                builtIns,
                StandardNames.FqNames.extensionFunctionType,
                emptyMap()
            )
        )
    }

val DeclarationDescriptor.fqNameUnsafe: FqNameUnsafe
    get() = DescriptorUtils.getFqName(this)
//fun getFunctionDescriptor( builtIns: CangJieBuiltIns,parameterCount: Int) =
// FunctionClassDescriptor.create(builtIns ,parameterCount )

fun getFunctionDescriptor(builtIns: CangJieBuiltIns, parameterCount: Int) =
    /*   if (isSuspendFunction) builtIns.getSuspendFunction(parameterCount) else*/ builtIns.getFunction(parameterCount)

fun DeclarationDescriptor.getFunctionTypeKind(): FunctionTypeKind? {
    if (this !is ClassDescriptor) return null
//    if (!CangJieBuiltIns.isUnderCangJiePackage(this)) return null

    return fqNameUnsafe.getFunctionTypeKind()
}

@OptIn(AllowedToUsedOnlyInK1::class)
private fun FqNameUnsafe.getFunctionTypeKind(): FunctionTypeKind? {
    if (!isSafe || isRoot) return null

    return FunctionTypeKindExtractor.Default.getFunctionalClassKind(toSafe().parent(), shortName().asString())
}

val DeclarationDescriptor.isBuiltinFunctionalClassDescriptor: Boolean
    get() {

        val functionalClassKind = getFunctionTypeKind()
        return functionalClassKind == FunctionTypeKind.Function
    }

fun CangJieType.contextFunctionTypeParamsCount(): Int {
    val annotationDescriptor = annotations.findAnnotation(StandardNames.FqNames.contextFunctionTypeParams) ?: return 0
    val constantValue =
        annotationDescriptor.allValueArguments.getValue(StandardNames.CONTEXT_FUNCTION_TYPE_PARAMETER_COUNT_NAME)
    return (constantValue as Int32Value).value


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

private fun CangJieType.isTypeOrSubtypeOf(predicate: (CangJieType) -> Boolean): Boolean =
    predicate(this) ||
            DFS.dfsFromNode(
                this,
                DFS.Neighbors { it.constructor.supertypes },
                DFS.VisitedWithSet(),
                object : DFS.AbstractNodeHandler<CangJieType, Boolean>() {
                    private var result = false

                    override fun beforeChildren(current: CangJieType): Boolean {
                        if (predicate(current)) {
                            result = true
                        }
                        return !result
                    }

                    override fun result() = result
                }
            )
val CangJieType.isBuiltinFunctionalTypeOrSubtype: Boolean
    get() = isTypeOrSubtypeOf { it.isBuiltinFunctionalType }

fun CangJieType.getReturnTypeFromFunctionType(): CangJieType {
    assert(isBuiltinFunctionalType) { "Not a function type: $this" }
    return arguments.last().type
}
fun CangJieType.getPureArgumentsForFunctionalTypeOrSubtype(): List<CangJieType> {
    assert(isBuiltinFunctionalTypeOrSubtype) { "Not a function type or subtype: $this" }
    return extractFunctionalTypeFromSupertypes().arguments.dropLast(1).map { it.type }
}
fun CangJieType.extractFunctionalTypeFromSupertypes(): CangJieType {
    assert(isBuiltinFunctionalTypeOrSubtype) { "Not a function type or subtype: $this" }
    return if (isBuiltinFunctionalType) this else supertypes().first { it.isBuiltinFunctionalType }
}

val CangJieType.functionTypeKind: FunctionTypeKind?
    get() = constructor.declarationDescriptor?.getFunctionTypeKind()

val CangJieType.isFunctionType: Boolean
    get() = functionTypeKind == FunctionTypeKind.Function

val CangJieType.isNonExtensionFunctionType: Boolean
    get() = isFunctionType && !isTypeAnnotatedWithExtensionFunctionType

fun CangJieType.extractParameterNameFromFunctionTypeArgument(): Name? {
    val annotation = annotations.findAnnotation(StandardNames.FqNames.parameterName) ?: return null
    val name = (annotation.allValueArguments.values.singleOrNull() as? StringValue)
        ?.value
        ?.takeIf { Name.isValidIdentifier(it) }
        ?: return null
    return Name.identifier(name)
}

