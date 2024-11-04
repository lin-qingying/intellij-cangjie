package com.linqingying.cangjie.resolve.calls.checkers

import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.descriptors.SimpleFunctionDescriptor
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjConstantExpression
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.CjPsiUtil
import com.linqingying.cangjie.psi.CjUnaryExpression
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall
import com.linqingying.cangjie.resolve.calls.util.getResolvedCall
import com.linqingying.cangjie.resolve.constants.ErrorValue
import com.linqingying.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import com.linqingying.cangjie.resolve.constants.IntegerValueTypeConstant
import com.linqingying.cangjie.resolve.constants.TypedCompileTimeConstant
import com.linqingying.cangjie.resolve.descriptorUtil.fqNameSafe
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.SimpleType
import com.linqingying.cangjie.types.lowerIfFlexible
import com.linqingying.cangjie.types.util.TypeUtils
import com.linqingying.cangjie.types.util.isPrimitiveNumber
import com.intellij.psi.PsiElement

object NewSchemeOfIntegerOperatorResolutionChecker : CallChecker{

    fun needToCheck(expectedType: CangJieType): Boolean {
        if (TypeUtils.noExpectedType(expectedType)) return false
        return expectedType.lowerIfFlexible().isPrimitiveNumber()
    }
    private fun checkArgumentImpl(
        expectedType: SimpleType,
        argumentExpression: CjExpression,
        trace: BindingTrace,
        moduleDescriptor: ModuleDescriptor
    ) {
        val bindingContext = trace.bindingContext
        val callForArgument = argumentExpression.getResolvedCall(bindingContext) ?: return
//        if (!callForArgument.isIntOperator()) return
        val callElement = callForArgument.call.callElement as? CjExpression ?: return
        val deparenthesizedElement = CjPsiUtil.deparenthesize(callElement)!!
        if (deparenthesizedElement is CjConstantExpression) return
        if (deparenthesizedElement is CjUnaryExpression) {
            val token = deparenthesizedElement.operationToken
            if (token == CjTokens.PLUS || token == CjTokens.MINUS) return
        }

        val compileTimeValue = bindingContext[BindingContext.COMPILE_TIME_VALUE, argumentExpression] ?: return

        val newExpressionType = when (compileTimeValue) {
            is IntegerValueTypeConstant -> {
                val currentExpressionType = compileTimeValue.unknownIntegerType
                val valueTypeConstructor = currentExpressionType.constructor as? IntegerLiteralTypeConstructor ?: return
                valueTypeConstructor.getApproximatedType()
            }
            is TypedCompileTimeConstant<*> -> {
                val typeFromCall = callForArgument.resultingDescriptor.returnType?.lowerIfFlexible()
                if (typeFromCall != null) {
                    typeFromCall
                } else {
                    val constantValue = compileTimeValue.constantValue
                    if (constantValue is ErrorValue) return
                    // Values of all numeric constants are held in Long value
                    val value = constantValue.value as? Long ?: return
                    IntegerLiteralTypeConstructor(value, moduleDescriptor, compileTimeValue.parameters).getApproximatedType()
                }
            }
            else -> return
        }
//        if (newExpressionType.constructor != expectedType.constructor) {
//            val willBeConversion = newExpressionType.isInt() && expectedType.makeNotNullable().isLong()
//            if (!willBeConversion) {
//                trace.report(Errors.INTEGER_OPERATOR_RESOLVE_WILL_CHANGE.on(argumentExpression, newExpressionType))
//            }
//        }
    }


    @JvmStatic
    fun checkArgument(
        expectedType: CangJieType,
        argument: CjExpression,
        trace: BindingTrace,
        moduleDescriptor: ModuleDescriptor
    ) {
        if (needToCheck(expectedType)) {
            checkArgumentImpl(expectedType.lowerIfFlexible(), CjPsiUtil.deparenthesize(argument)!!, trace, moduleDescriptor)
        }
    }

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        for ((valueParameter, arguments) in resolvedCall.valueArguments) {
            val expectedType =/* if (valueParameter.isVararg) {
                valueParameter.varargElementType ?: continue
            } else {*/
                valueParameter.type
           /* }*/.unwrap().lowerIfFlexible()
            if (!needToCheck(expectedType)) {
                continue
            }
            for (argument in arguments.arguments) {
                val expression = CjPsiUtil.deparenthesize(argument.getArgumentExpression()) ?: continue
                checkArgumentImpl(expectedType, expression, context.trace, context.moduleDescriptor)
            }
        }
    }
}
