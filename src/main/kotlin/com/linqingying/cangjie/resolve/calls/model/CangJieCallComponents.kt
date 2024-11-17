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

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.builtins.ReflectionTypes
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.incremental.components.LookupTracker
import com.linqingying.cangjie.resolve.calls.components.ArgumentsToParametersMapper
import com.linqingying.cangjie.resolve.calls.components.CallableReferenceArgumentResolver
import com.linqingying.cangjie.resolve.calls.components.CangJieResolutionStatelessCallbacks
import com.linqingying.cangjie.resolve.calls.components.TypeArgumentsToParametersMapper
import com.linqingying.cangjie.resolve.calls.inference.components.ConstraintInjector

import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.linqingying.cangjie.types.TypeSubstitutor
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.checker.NewCangJieTypeChecker


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
