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

package cn.cangnova.cangjie.resolve.calls.model

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.builtins.ReflectionTypes
import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.descriptors.FunctionDescriptor
import cn.cangnova.cangjie.incremental.components.LookupTracker
import cn.cangnova.cangjie.resolve.calls.components.ArgumentsToParametersMapper
import cn.cangnova.cangjie.resolve.calls.components.CallableReferenceArgumentResolver
import cn.cangnova.cangjie.resolve.calls.components.CangJieResolutionStatelessCallbacks
import cn.cangnova.cangjie.resolve.calls.components.TypeArgumentsToParametersMapper
import cn.cangnova.cangjie.resolve.calls.inference.components.ConstraintInjector

import cn.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import cn.cangnova.cangjie.types.TypeSubstitutor
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.types.checker.NewCangJieTypeChecker


class CangJieCallComponents(
    val statelessCallbacks: CangJieResolutionStatelessCallbacks,
    val argumentsToParametersMapper: ArgumentsToParametersMapper,
    val typeArgumentsToParametersMapper: TypeArgumentsToParametersMapper,
    val constraintInjector: ConstraintInjector,
    val reflectionTypes: ReflectionTypes,
    val builtIns: CangJieBuiltIns,
    val languageVersionSettings: LanguageVersionSettings,
//    val samConversionOracle: SamConversionOracle,
//    val samConversionResolver: SamConversionResolver,
    val cangjieTypeChecker: NewCangJieTypeChecker,
    val lookupTracker: LookupTracker,
    val cangjieTypeRefiner: CangJieTypeRefiner,
    val callableReferenceArgumentResolver: CallableReferenceArgumentResolver
)

class GivenCandidate(
    val descriptor: FunctionDescriptor,
    val dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    val knownTypeParametersResultingSubstitutor: TypeSubstitutor?
)
