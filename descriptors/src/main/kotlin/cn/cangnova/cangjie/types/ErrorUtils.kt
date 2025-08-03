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

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.types.error.*
import cn.cangnova.cangjie.types.util.isUnresolvedType

object ErrorUtils {

    val errorModule: ModuleDescriptor = ErrorModuleDescriptor

    private val errorProperty: PropertyDescriptor = ErrorPropertyDescriptor()
    val errorPropertyGroup: Set<PropertyDescriptor> = setOf(errorProperty)

      val errorVariable: VariableDescriptor = ErrorVariableDescriptor()
    val errorVariableGroup: Set<VariableDescriptor> = setOf(errorVariable)

    val errorPropertyType: CangJieType get() = createErrorType(ErrorTypeKind.ERROR_PROPERTY_TYPE)
    val errorVariableType: CangJieType get() = createErrorType(ErrorTypeKind.ERROR_VARIABLE_TYPE)

    @JvmStatic
    val invalidType: ErrorType get() = createErrorType(ErrorTypeKind.INVALID_TYPE)
    fun containsUninferredTypeVariable(type: CangJieType): Boolean = type.contains(::isUninferredTypeVariable)

    val errorTypeForLoopInSupertypes: CangJieType = createErrorType(ErrorTypeKind.CYCLIC_SUPERTYPES)

    val errorClass: ErrorClassDescriptor
        get() =
            ErrorClassDescriptor(Name.special(ErrorEntity.ERROR_CLASS.debugText.format("unknown class")))

    //    private fun isErrorClass(candidate: DeclarationDescriptor?): Boolean = candidate is ErrorClassDescriptor
    @JvmStatic
    fun createErrorType(kind: ErrorTypeKind, vararg formatParams: String): ErrorType =
        createErrorTypeWithArguments(kind, emptyList(), *formatParams)

    @JvmStatic
    fun isUninferredTypeVariable(type: CangJieType?): Boolean {
        if (type == null) return false
        val constructor = type.constructor
        return constructor is ErrorTypeConstructor && constructor.kind == ErrorTypeKind.UNINFERRED_TYPE_VARIABLE
    }

    /**
     * 检查给定的类型是否包含错误类型
     *
     * @param type 可能包含错误类型的CangJieType对象，可以为空
     * @return 如果类型为空，则返回false；如果类型本身或其任何参数包含错误类型，则返回true
     */
    fun containsErrorType(type: CangJieType?): Boolean {
        // 如果类型为空，则直接返回false，表示没有错误类型
        if (type == null) return false
        // 如果类型本身标记为错误类型，则返回true
        if (type.isError) return true
        // 遍历类型的参数，检查是否包含错误类型
        for (projection in type.arguments) {
            // 只要发现第一个错误类型，就立即返回true
            if ( containsErrorType(projection.type))
                return true
        }
        // 如果所有参数都没有错误类型，则返回false
        return false
    }

    fun unresolvedTypeAsItIs(type: CangJieType): String {
        assert(isUnresolvedType(type))
        return (type.constructor as ErrorTypeConstructor).getParam(0)
    }

    @JvmStatic
    fun createErrorType(kind: ErrorTypeKind, typeConstructor: TypeConstructor, vararg formatParams: String): ErrorType =
        createErrorTypeWithArguments(kind, emptyList(), typeConstructor, *formatParams)

    fun createErrorTypeWithArguments(
        kind: ErrorTypeKind,
        arguments: List<TypeProjection>,
        vararg formatParams: String
    ): ErrorType =
        createErrorTypeWithArguments(kind, arguments, createErrorTypeConstructor(kind, *formatParams), *formatParams)

    @JvmStatic
    fun createErrorScope(kind: ErrorScopeKind, vararg formatParams: String): ErrorScope =
        createErrorScope(kind, throwExceptions = false, *formatParams)

    @JvmStatic
    fun createErrorScope(kind: ErrorScopeKind, throwExceptions: Boolean, vararg formatParams: String): ErrorScope =
//        if (throwExceptions) ThrowingScope(kind, *formatParams) else
        ErrorScope(kind, *formatParams)

    fun createErrorTypeConstructor(kind: ErrorTypeKind, vararg formatParams: String): ErrorTypeConstructor =
        ErrorTypeConstructor(kind, *formatParams)

    fun createErrorTypeWithArguments(
        kind: ErrorTypeKind,
        arguments: List<TypeProjection>,
        typeConstructor: TypeConstructor,
        vararg formatParams: String
    ): ErrorType = ErrorType(
        typeConstructor, createErrorScope(ErrorScopeKind.ERROR_TYPE_SCOPE, typeConstructor.toString()),
        kind, arguments, isMarkedOption = false, *formatParams
    )

    private fun isErrorClass(candidate: DeclarationDescriptor?): Boolean = candidate is ErrorClassDescriptor

    /**
     * 检查函数的参数类型（包括类型参数和扩展接收器）中是否包含错误类型。不检查函数的返回类型。
     *
     * @param function 函数描述符，用于获取函数的参数信息
     * @return 如果函数的参数类型中包含错误类型，则返回 true；否则返回 false
     */
    fun containsErrorTypeInParameters(function: FunctionDescriptor): Boolean {
        // 检查扩展接收器参数是否包含错误类型
        val receiverParameter = function.extensionReceiverParameter
        if (receiverParameter != null && containsErrorType(receiverParameter.type))
            return true

        // 检查值参数是否包含错误类型
        for (parameter in function.valueParameters) {
            if (containsErrorType(parameter.type))
                return true
        }

        // 检查类型参数的上界是否包含错误类型
        for (parameter in function.typeParameters) {
            for (upperBound in parameter.upperBounds) {
                if (containsErrorType(upperBound))
                    return true
            }
        }
        return false
    }

    @JvmStatic
    fun isError(candidate: DeclarationDescriptor?): Boolean =
        candidate != null
                && (isErrorClass(candidate) || isErrorClass(candidate.containingDeclaration)
                || candidate === errorModule
                )

}
