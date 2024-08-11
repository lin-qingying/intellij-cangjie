package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.psi.CjSuperExpression
import com.huawei.cangjie.resolve.DescriptorToSourceUtils
import com.huawei.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import com.huawei.cangjie.resolve.calls.components.CangJieResolutionStatelessCallbacks
import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.inference.components.ConstraintInjector
import com.huawei.cangjie.resolve.calls.inference.components.SimpleConstraintSystemImpl
import com.huawei.cangjie.resolve.calls.inference.isBuilderInferenceCall
import com.huawei.cangjie.resolve.calls.model.CallableReferenceCangJieCallArgument
import com.huawei.cangjie.resolve.calls.model.CangJieCall
import com.huawei.cangjie.resolve.calls.model.CangJieCallArgument
import com.huawei.cangjie.resolve.calls.model.SimpleCangJieCallArgument
import com.huawei.cangjie.resolve.calls.results.SimpleConstraintSystem
import com.huawei.cangjie.resolve.calls.util.isInfixCall
import com.huawei.cangjie.resolve.calls.util.isSuperOrDelegatingConstructorCall
import com.huawei.cangjie.resolve.deprecation.DeprecationResolver
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.checker.CangJieTypeRefiner

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
