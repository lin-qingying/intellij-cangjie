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

package org.cangnova.cangjie.types

import com.intellij.util.containers.addIfNotNull
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.builtins.StandardNames.BUILT_INS_PACKAGE_NAME
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqNameUnsafe
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.DescriptorUtils.getContainingModule
import org.cangnova.cangjie.resolve.builtIns
import org.cangnova.cangjie.resolve.constants.StringValue
import org.cangnova.cangjie.types.functions.FunctionTypeKind
import org.cangnova.cangjie.types.functions.FunctionTypeKindExtractor
import org.cangnova.cangjie.utils.DFS

/**
 * 函数类型作为内置类型实现
 */

fun CangJieType.getValueParameterTypesFromCallableReflectionType(isCallableTypeWithExtension: Boolean): List<TypeArgument> {
//    assert(ReflectionTypes.isCCallableType(this)) { "Not a callable reflection type: $this" }
    val arguments = arguments
    val first = if (isCallableTypeWithExtension) 1 else 0
    val last = arguments.size - 1
    assert(first <= last) { "Not an exact function type: $this" }
    return arguments.subList(first, last)
}

fun getFunctionTypeArgumentProjections(
    receiverType: CangJieType?,
    parameterTypes: List<CangJieType>,
    parameterNames: List<Name>?,
    returnType: CangJieType,
    builtIns: CangJieBuiltIns
): List<TypeArgument> {
    val arguments =
        ArrayList<TypeArgument>(parameterTypes.size + (if (receiverType != null) 1 else 0) + 1)

    arguments.addIfNotNull(receiverType?.asTypeArgument())

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
        typeToUse.asTypeArgument()
    }

    arguments.add(returnType.asTypeArgument())

    return arguments
}

/**
 * 创建一个函数类型的简单类型。
 *
 * 该函数根据给定的参数构建一个函数类型，包括注解、接收者类型、参数类型、参数名称和返回类型。它使用内置类型系统获取相应的函数描述符和类型参数，最终创建一个简单的、非空的函数类型。
 *
 * @param builtIns 内置类型系统，提供基本类型信息的访问。
 * @param annotations 函数类型的注解，用于向类型添加元数据。
 * @param receiverType 函数的接收者类型，如果没有接收者则可以为 null。
 * @param contextReceiverTypes 上下文接收者类型的列表，支持多个接收者。
 * @param parameterTypes 函数的参数类型列表，确定参数的数量和类型。
 * @param parameterNames 参数名称的列表，可以为 null。
 * @param returnType 函数的返回类型。
 * @return 返回一个简单的、非空的函数类型。
 */

fun createFunctionType(
    builtIns: CangJieBuiltIns,
    annotations: Annotations,
    receiverType: CangJieType?,
    parameterTypes: List<CangJieType>,
    parameterNames: List<Name>?,
    returnType: CangJieType,
): SimpleType {
    // 获取函数类型的参数投影
    val arguments =
        getFunctionTypeArgumentProjections(
            receiverType,
            parameterTypes,
            parameterNames,
            returnType,
            builtIns
        )

    // 计算参数总数，包括上下文接收者和普通接收者
    val parameterCount = parameterTypes.size + if (receiverType == null) 0 else 1

    // 获取函数描述符
    val classDescriptor = getFunctionDescriptor(builtIns, parameterCount)

    // 创建并返回简单的、非空的函数类型
    // CangJie does not have extension function type annotations or context receivers
    return CangJieTypeFactory.simpleType(annotations.toDefaultAttributes(), classDescriptor, arguments)
}


/**
 * 扩展属性：获取声明描述符的不安全完全限定名（可能包含错误信息）。
 */
val DeclarationDescriptor.fqNameUnsafe: FqNameUnsafe
    get() = DescriptorUtils.getFqName(this)

fun getFunctionDescriptor(builtIns: CangJieBuiltIns, parameterCount: Int) =
    /*   if (isSuspendFunction) builtIns.getSuspendFunction(parameterCount) else*/ builtIns.getFunction(parameterCount)

fun DeclarationDescriptor.getFunctionTypeKind(): FunctionTypeKind? {
    if (this !is ClassDescriptor) return null
//    if (!CangJieBuiltIns.isUnderCangJiePackage(this)) return null

    return fqNameUnsafe.getFunctionTypeKind()
}

private fun FqNameUnsafe.getFunctionTypeKind(): FunctionTypeKind? {
    if (!isSafe || isRoot) return null

    return FunctionTypeKindExtractor.Default.getFunctionalClassKind(toSafe().parent(), shortName().asString())
}

val DeclarationDescriptor.isBuiltinFunctionalClassDescriptor: Boolean
    get() {

        val functionalClassKind = getFunctionTypeKind()
        return functionalClassKind == FunctionTypeKind.Function
    }

// CangJie does not have context function types, so this always returns 0
fun CangJieType.contextFunctionTypeParamsCount(): Int = 0


fun CangJieType.getValueParameterTypesFromFunctionType(): List<TypeArgument> {
    assert(isBuiltinFunctionalType) { "Not a function type: $this" }
    val arguments = arguments
    // CangJie does not have extension function types or context function types
    val first = 0
    val last = arguments.size - 1
    assert(first <= last) { "Not an exact function type: $this" }
    return arguments.subList(first, last)
}

val CangJieType.isBuiltinFunctionalType: Boolean
    get() = constructor.declarationDescriptor?.isBuiltinFunctionalClassDescriptor == true

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

fun CangJieType.extractParameterNameFromFunctionTypeArgument(): Name? {
    val annotation = annotations.findAnnotation(StandardNames.FqNames.parameterName) ?: return null
    val name = (annotation.allValueArguments.values.singleOrNull() as? StringValue)
        ?.value
        ?.takeIf { Name.isValidIdentifier(it) }
        ?: return null
    return Name.identifier(name)
}

val CangJieType.isFunctionTypeOrSubtype: Boolean
    get() = isTypeOrSubtypeOf { it.isFunctionType }

fun CangJieType.isFunctionTypeOrSubtype(predicate: (CangJieType) -> Boolean): Boolean =
    isTypeOrSubtypeOf { it.isFunctionType && predicate(it) }


fun isNumberedFunctionClassFqName(fqName: FqNameUnsafe): Boolean {
    return fqName.startsWith(BUILT_INS_PACKAGE_NAME) &&
            fqName.getFunctionTypeKind() == FunctionTypeKind.Function
}

fun FunctionDescriptor.toFunctionType(builtins: CangJieBuiltIns): CangJieType {
    this.returnType ?: this

    val valueTypes = this.valueParameters

    val parameterTypes = valueTypes.map { it.type }
    val parameterNames = valueTypes.map { it.name }

    return createFunctionType(
        builtins,
        this.annotations, null,
        parameterTypes, parameterNames, this.returnType!!
    )
}

fun FunctionDescriptor.toFunctionType(): CangJieType {
    this.returnType ?: this
 
    return toFunctionType(
        getContainingModule(this).builtIns

    )
}


fun isBuiltinFunctionClass(classId: ClassId): Boolean {
    if (!classId.startsWith(BUILT_INS_PACKAGE_NAME)) return false

    val kind = classId.asSingleFqName().toUnsafe().getFunctionTypeKind()
    return kind == FunctionTypeKind.Function
}
