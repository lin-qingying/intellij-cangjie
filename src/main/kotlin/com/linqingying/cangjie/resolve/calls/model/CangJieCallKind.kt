package com.linqingying.cangjie.resolve.calls.model

import com.linqingying.cangjie.resolve.calls.components.*
import com.linqingying.cangjie.resolve.calls.components.ArgumentsToCandidateParameterDescriptor
import com.linqingying.cangjie.resolve.calls.components.CheckArgumentsInParenthesis
import com.linqingying.cangjie.resolve.calls.components.CreateFreshVariablesSubstitutor
import com.linqingying.cangjie.resolve.calls.components.MapArguments
import com.linqingying.cangjie.resolve.calls.components.NoArguments


/**
 * 枚举类CangJieCallKind表示仓颉调用的种类，每种调用类型都关联一系列的解析部分（ResolutionPart），
 * 这些解析部分定义了在处理该调用类型时需要执行的检查和解析步骤。
 *
 * @param resolutionPart 可变参数，表示该调用类型所需的解析部分列表。
 */
enum class CangJieCallKind(vararg resolutionPart: ResolutionPart) {
    /**
     * 变量调用类型，关联一系列解析部分，如检查静态调用、检查可见性等。
     * 这些解析部分是处理变量调用时需要执行的步骤。
     */
    VARIABLE(
        CheckStaticCall,
        CheckVisibility,
        CheckExtensionPrivateVisibility,
        CheckSuperExpressionCallPart,
        NoTypeArguments,
        MapTypeArguments,
        NoArguments,
        CreateFreshVariablesSubstitutor,
//        CollectionTypeVariableUsagesInfo,
//        CheckExplicitReceiverKindConsistency,
        CheckReceivers,

        PostponedVariablesInitializerResolutionPart,
//        CheckContextReceiversResolutionPart,
        CheckIncompatibleTypeVariableUpperBounds
    ),
    /**
     * 函数调用类型，关联一系列解析部分，如检查扩展私有可见性、检查操作符调用部分等。
     * 这些解析部分是处理函数调用时需要执行的步骤。
     */
    FUNCTION(
        CheckExtensionPrivateVisibility,
        CheckOperatorCallPart,
        CheckVisibility,
//        CheckInfixResolutionPart,
//        CheckOperatorResolutionPart,
        CheckSuperExpressionCallPart,
        MapTypeArguments,
        MapArguments,
        ArgumentsToCandidateParameterDescriptor,
        CreateFreshVariablesSubstitutor,
//        CollectionTypeVariableUsagesInfo,
//        CheckExplicitReceiverKindConsistency,
        CheckReceivers,
        CheckArgumentsInParenthesis,
        CheckExternalArgument,
//        EagerResolveOfCallableReferences,
//        CompatibilityOfPartiallyApplicableSamConversion,
        PostponedVariablesInitializerResolutionPart,
//        CheckContextReceiversResolutionPart,
        CheckIncompatibleTypeVariableUpperBounds,
        CheckStaticCall,
    ),
    /**
     * 调用调用类型，继承自函数调用类型，但可能有额外的解析需求。
     */
    INVOKE(*FUNCTION.resolutionSequence.toTypedArray()),
    /**
     * 枚举调用类型，关联一系列特定的解析部分，如映射类型参数、映射参数等。
     * 这些解析部分是处理枚举调用时需要执行的步骤。
     */
    ENUM(/**FUNCTION.resolutionSequence.toTypedArray(),CheckEnumCall */
        MapTypeArguments,
        MapArguments,
        ArgumentsToCandidateParameterDescriptor,
        CreateFreshVariablesSubstitutor,
        CheckArgumentsInParenthesis
    ),
    /**
     * 可调用引用调用类型，关联一系列解析部分，如检查可见性、检查接收者等。
     * 这些解析部分是处理可调用引用调用时需要执行的步骤。
     */
    CALLABLE_REFERENCE(
        CheckVisibility,
        NoTypeArguments,
        NoArguments,
        CreateFreshVariablesSubstitutor,
//        CollectionTypeVariableUsagesInfo,
        CheckReceivers,
//        CheckCallableReference,
        CheckIncompatibleTypeVariableUpperBounds
    ),
    /**
     * 不支持的调用类型，用于标识尚未支持或未定义解析步骤的调用类型。
     */
    UNSUPPORTED();

    /**
     * 解析序列属性，存储该调用类型关联的解析部分列表。
     */
    val resolutionSequence = resolutionPart.asList()
}
