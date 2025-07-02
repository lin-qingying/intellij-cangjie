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

package cn.cangnova.cangjie.resolve.calls.tower

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.descriptors.ValueParameterDescriptor
import cn.cangnova.cangjie.psi.CjSuperExpression
import cn.cangnova.cangjie.resolve.DescriptorToSourceUtils
import cn.cangnova.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import cn.cangnova.cangjie.resolve.calls.components.CangJieResolutionStatelessCallbacks
import cn.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import cn.cangnova.cangjie.resolve.calls.inference.components.ConstraintInjector
import cn.cangnova.cangjie.resolve.calls.inference.components.SimpleConstraintSystemImpl
import cn.cangnova.cangjie.resolve.calls.inference.isBuilderInferenceCall
import cn.cangnova.cangjie.resolve.calls.model.CallableReferenceCangJieCallArgument
import cn.cangnova.cangjie.resolve.calls.model.CangJieCall
import cn.cangnova.cangjie.resolve.calls.model.CangJieCallArgument
import cn.cangnova.cangjie.resolve.calls.model.SimpleCangJieCallArgument
import cn.cangnova.cangjie.resolve.calls.results.SimpleConstraintSystem
import cn.cangnova.cangjie.resolve.calls.util.isInfixCall
import cn.cangnova.cangjie.resolve.calls.util.isSuperOrDelegatingConstructorCall
import cn.cangnova.cangjie.resolve.deprecation.DeprecationResolver
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner

class CangJieResolutionStatelessCallbacksImpl(
    private val deprecationResolver: DeprecationResolver,
    private val languageVersionSettings: LanguageVersionSettings,
    private val CangJieTypeRefiner: CangJieTypeRefiner
) : CangJieResolutionStatelessCallbacks {
    override fun isDescriptorFromSource(descriptor: CallableDescriptor) =
        DescriptorToSourceUtils.descriptorToDeclaration(descriptor) != null

    override fun isInfixCall(cangjieCall: CangJieCall) =
        cangjieCall is PSICangJieCallImpl && isInfixCall(cangjieCall.psiCall)

    override fun isOperatorCall(cangjieCall: CangJieCall) =
        (cangjieCall is PSICangJieCallForInvoke) ||
                (cangjieCall is PSICangJieCallImpl
//                        && isConventionCall(CangJieCall.psiCall)
                        )

    override fun isSuperOrDelegatingConstructorCall(cangjieCall: CangJieCall) =
        cangjieCall is PSICangJieCallImpl && isSuperOrDelegatingConstructorCall(
            cangjieCall.psiCall
        )

    override fun isHiddenInResolution(
        descriptor: DeclarationDescriptor, cangjieCall: CangJieCall, resolutionCallbacks: CangJieResolutionCallbacks,
    ): Boolean {
        return false
//        deprecationResolver.isHiddenInResolution(
//            descriptor,
//            (cangjieCall as? PSICangJieCall)?.psiCall,
//            (resolutionCallbacks as? CangJieResolutionCallbacksImpl)?.trace?.bindingContext,
//            cangjieCall is PSICangJieCallImpl && cangjieCall.psiCall.isCallWithSuperReceiver(),
//        )
    }

    override fun isHiddenInResolution(
        descriptor: DeclarationDescriptor,
        cangjieCallArgument: CangJieCallArgument,
        resolutionCallbacks: CangJieResolutionCallbacks
    ): Boolean {
        return false
//       return  deprecationResolver.isHiddenInResolution(
//            descriptor,
//            cangjieCallArgument.psiCallArgument.psiExpression,
//            (resolutionCallbacks as? CangJieResolutionCallbacksImpl)?.trace?.bindingContext,
//            isSuperCall = false,
//            fromImportingScope = false
//        )

    }


    override fun isSuperExpression(receiver: SimpleCangJieCallArgument?): Boolean =
        receiver?.psiExpression is CjSuperExpression

    override fun getScopeTowerForCallableReferenceArgument(argument: CallableReferenceCangJieCallArgument): ImplicitScopeTower =
        (argument as CallableReferenceCangJieCallArgumentImpl).scopeTowerForResolution

    override fun getVariableCandidateIfInvoke(functionCall: CangJieCall): ResolutionCandidate? =
        (functionCall as? PSICangJieCallForInvoke)?.variableCall

    override fun isBuilderInferenceCall(argument: CangJieCallArgument, parameter: ValueParameterDescriptor): Boolean =
        isBuilderInferenceCall(parameter, argument.psiCallArgument.valueArgument/*, languageVersionSettings*/)

//    override fun isApplicableCallForBuilderInference(
//        descriptor: CallableDescriptor,
//        languageVersionSettings: LanguageVersionSettings,
//    ): Boolean {
//        return isApplicableCallForBuilderInference(
//            descriptor,
//            languageVersionSettings
//        )
//    }

    override fun isOldIntersectionIsEmpty(types: Collection<CangJieType>): Boolean {
        return false
//        return TypeIntersector.intersectTypes(types) == null
    }



    override fun createConstraintSystemForOverloadResolution(
        constraintInjector: ConstraintInjector, builtIns: CangJieBuiltIns
    ): SimpleConstraintSystem =
        SimpleConstraintSystemImpl(constraintInjector, builtIns, CangJieTypeRefiner, languageVersionSettings)

}
