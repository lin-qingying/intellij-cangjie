package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.builtins.UnsignedTypes
import com.linqingying.cangjie.builtins.isFunctionType
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.diagnostics.Errors.INVALID_TYPE_OF_ANNOTATION_MEMBER

import com.linqingying.cangjie.diagnostics.Errors.OPTIONAL_TYPE_OF_ANNOTATION_MEMBER
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.CjParameter
import com.linqingying.cangjie.psi.CjPsiUtil
import com.linqingying.cangjie.resolve.DescriptorUtils.isEnum
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall
import com.linqingying.cangjie.resolve.constants.BoolValue
import com.linqingying.cangjie.resolve.constants.TypedCompileTimeConstant
import com.linqingying.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.ErrorUtils.isError
import com.linqingying.cangjie.types.isError
import com.linqingying.cangjie.types.util.TypeUtils

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
