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

package org.cangnova.cangjie.resolve.calls.model

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.builtins.ReflectionTypes
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.incremental.components.LookupTracker
import org.cangnova.cangjie.resolve.calls.components.ArgumentsToParametersMapper
import org.cangnova.cangjie.resolve.calls.components.CallableReferenceArgumentResolver
import org.cangnova.cangjie.resolve.calls.components.CangJieResolutionStatelessCallbacks
import org.cangnova.cangjie.resolve.calls.components.TypeArgumentsToParametersMapper
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintInjector

import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.cangnova.cangjie.types.ComposableTypeSubstitutor
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner


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
    val cangjieTypeChecker: CangJieTypeChecker,
    val lookupTracker: LookupTracker,
    val cangjieTypeRefiner: CangJieTypeRefiner,
    val callableReferenceArgumentResolver: CallableReferenceArgumentResolver
)

class GivenCandidate(
    val descriptor: FunctionDescriptor,
    val dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    val knownTypeParametersResultingSubstitutor: ComposableTypeSubstitutor?
)
