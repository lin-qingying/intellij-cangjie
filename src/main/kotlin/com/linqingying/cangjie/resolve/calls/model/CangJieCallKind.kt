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

package com.linqingying.cangjie.resolve.calls.model

import com.linqingying.cangjie.resolve.calls.components.*


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
        CheckIncompatibleTypeVariableUpperBounds,
        CheckStaticCall,
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
        EagerResolveOfCallableReferences,
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
    ENUM(
        /**FUNCTION.resolutionSequence.toTypedArray(),CheckEnumCall */
        CheckDesiredEnumType,
        MapTypeArguments,
        MapArguments,
        ArgumentsToCandidateParameterDescriptor,
        CreateFreshVariablesSubstitutor,
        CheckArgumentsInParenthesis
    ),
    CASE_ENUM(
        CheckDesiredEnumType,
        MapTypeArguments,
        MapArguments,
        ArgumentsToCandidateParameterDescriptor,
        CreateFreshVariablesSubstitutor,
        CheckCaseEnumArgumentSize,

//        CheckArgumentsInParenthesis
    ),

    /**
     * 可调用引用调用类型，关联一系列解析部分，如检查可见性、检查接收者等。
     * 这些解析部分是处理可调用引用调用时需要执行的步骤。
     */
    CALLABLE_REFERENCE(
        CheckVisibility,
        MapTypeArguments,

        NoArguments,
        CreateFreshVariablesSubstitutor,
        CollectionTypeVariableUsagesInfo,
        CheckReceivers,
        CheckCallableReference,
        CheckIncompatibleTypeVariableUpperBounds,
        CheckStaticCall,

    ),

    /**
     * 不支持的调用类型，用于标识尚未支持或未定义解析步骤的调用类型。
     */
    UNSUPPORTED;

    /**
     * 解析序列属性，存储该调用类型关联的解析部分列表。
     */
    val resolutionSequence = resolutionPart.asList()
}
