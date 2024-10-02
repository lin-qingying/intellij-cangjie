package com.huawei.cangjie.resolve

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.UnsignedTypes
import com.huawei.cangjie.builtins.isFunctionType
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.diagnostics.Errors.INVALID_TYPE_OF_ANNOTATION_MEMBER

import com.huawei.cangjie.diagnostics.Errors.OPTIONAL_TYPE_OF_ANNOTATION_MEMBER
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjParameter
import com.huawei.cangjie.psi.CjPsiUtil
import com.huawei.cangjie.resolve.DescriptorUtils.isEnum
import com.huawei.cangjie.resolve.calls.model.ResolvedCall
import com.huawei.cangjie.resolve.constants.BoolValue
import com.huawei.cangjie.resolve.constants.TypedCompileTimeConstant
import com.huawei.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.ErrorUtils.isError
import com.huawei.cangjie.types.isError
import com.huawei.cangjie.types.util.TypeUtils

object CompileTimeConstantUtils   {


        fun checkConstructorParametersType(parameters: List<CjParameter>, trace: BindingTrace) {
            for (parameter in parameters) {
                val parameterDescriptor = trace.bindingContext[BindingContext.VALUE_PARAMETER, parameter] ?: continue
                val parameterType = parameterDescriptor.type
                val typeReference = parameter.typeReference
                if (typeReference != null) {
                    when {
                        parameterType.isFunctionType -> trace.report(OPTIONAL_TYPE_OF_ANNOTATION_MEMBER.on(typeReference))
                        !isAcceptableTypeForAnnotationParameter(parameterType) -> trace.report(INVALID_TYPE_OF_ANNOTATION_MEMBER.on(typeReference))
                    }
                }
            }
        }

        private fun isAcceptableTypeForAnnotationParameter(parameterType: CangJieType): Boolean {
            if ( parameterType.isError) return true

            val typeDescriptor = TypeUtils.getClassDescriptor(parameterType) ?: return false

            if (isEnum (typeDescriptor) ||
//                isAnnotationClass(typeDescriptor) ||

                CangJieBuiltIns.isPrimitiveArray(parameterType) ||
                CangJieBuiltIns.isPrimitiveType(parameterType) ||
                CangJieBuiltIns.isString(parameterType) ||
                UnsignedTypes.isUnsignedType(parameterType)
//                || UnsignedTypes.isUnsignedArrayType(parameterType)
                ) {
                return true
            }

            if (CangJieBuiltIns.isArray(parameterType)) {
                val arguments = parameterType.arguments
                if (arguments.size == 1) {
                    val arrayType = arguments[0].type
                    if (arrayType.isMarkedOption) return false
                    val arrayTypeDescriptor = TypeUtils.getClassDescriptor(arrayType)
                    if (arrayTypeDescriptor != null) {
                        return isEnum (arrayTypeDescriptor) ||
//                                isAnnotationClass(arrayTypeDescriptor) ||

                                CangJieBuiltIns.isString(arrayType)
                    }
                }
            }

            return false
        }

//        fun isArrayFunctionCall(resolvedCall: ResolvedCall<*>): Boolean {
//            val unsafe = DescriptorUtils.getFqName(resolvedCall.candidateDescriptor)
//            return unsafe.isSafe && ARRAY_CALL_FQ_NAMES.contains(unsafe.toSafe())
//        }

        fun canBeReducedToBooleanConstant(
            expression: CjExpression?,
            context: BindingContext,
            expectedValue: Boolean?
        ): Boolean {
            val effectiveExpression = CjPsiUtil.deparenthesize(expression) ?: return false

            val compileTimeConstant = ConstantExpressionEvaluator.getConstant(effectiveExpression, context)
            if (compileTimeConstant !is TypedCompileTimeConstant || compileTimeConstant.usesVariableAsConstant) return false

            val constantValue = compileTimeConstant.constantValue

            if (constantValue !is BoolValue) return false

            val value = constantValue.value
            return expectedValue == null || expectedValue == value
        }

}
