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

package cn.cangnova.cangjie.resolve.calls.components

import cn.cangnova.cangjie.builtins.*
import cn.cangnova.cangjie.descriptors.ParameterDescriptor
import cn.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import cn.cangnova.cangjie.resolve.calls.inference.ConstraintSystemOperation
import cn.cangnova.cangjie.resolve.calls.model.*
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.UnwrappedType
import cn.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isNothing
import cn.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isUnit
import cn.cangnova.cangjie.types.isDynamic

//object SamTypeConversions : ParameterTypeConversion {
//    override fun conversionDefinitelyNotNeeded(
//        candidate: ResolutionCandidate,
//        argument: CangJieCallArgument,
//        expectedParameterType: UnwrappedType
//    ): Boolean {
//        val callComponents = candidate.callComponents
//
//        if (!callComponents.languageVersionSettings.supportsFeature(LanguageFeature.SamConversionPerArgument)) return true
//        if (expectedParameterType.isNothing()) return true
//        if (expectedParameterType.isFunctionType) return true
//
//        val samConversionOracle = callComponents.samConversionOracle
//        if (!callComponents.languageVersionSettings.supportsFeature(LanguageFeature.SamConversionForCangJieFunctions)) {
//            if (!samConversionOracle.shouldRunSamConversionForFunction(candidate.resolvedCall.candidateDescriptor)) return true
//        }
//
//        val declarationDescriptor = expectedParameterType.constructor.declarationDescriptor
//        if (declarationDescriptor is ClassDescriptor && declarationDescriptor.isDefinitelyNotSamInterface) return true
//
//        return false
//    }
//
//    override fun conversionIsNeededBeforeSubtypingCheck(
//        argument: CangJieCallArgument,
//        areSuspendOnlySamConversionsSupported: Boolean
//    ): Boolean {
//        return when (argument) {
//            is SubCangJieCallArgument -> {
//                val stableType = argument.receiver.stableType
//                if (
//                    stableType.isFunctionType
//                ) return true
//
//                hasNonAnalyzedLambdaAsReturnType(argument.callResult.subResolvedAtoms, stableType)
//            }
//            is SimpleCangJieCallArgument -> argument.receiver.stableType.run {
//                isFunctionType || (areSuspendOnlySamConversionsSupported  )
//            }
//            is LambdaCangJieCallArgument, is CallableReferenceCangJieCallArgument -> true
//            else -> false
//        }
//    }
//
//    private fun hasNonAnalyzedLambdaAsReturnType(subResolvedAtoms: List<ResolvedAtom>?, type: UnwrappedType): Boolean {
//        subResolvedAtoms?.forEach {
//            if (it is LambdaWithTypeVariableAsExpectedTypeAtom) {
//                if (it.expectedType.constructor == type.constructor) return true
//            }
//
//            val hasNonAnalyzedLambda = hasNonAnalyzedLambdaAsReturnType(it.subResolvedAtoms, type)
//            if (hasNonAnalyzedLambda) return true
//        }
//
//        return false
//    }
//
//    override fun conversionIsNeededAfterSubtypingCheck(argument: CangJieCallArgument): Boolean {
//        return argument is SimpleCangJieCallArgument && argument.receiver.stableType.isFunctionTypeOrSubtype
//    }
//
//    override fun convertParameterType(
//        candidate: ResolutionCandidate,
//        argument: CangJieCallArgument,
//        parameter: ParameterDescriptor,
//        expectedParameterType: UnwrappedType
//    ): UnwrappedType? {
//        val callComponents = candidate.callComponents
//        val originalExpectedType = argument.getExpectedType(parameter.original, callComponents.languageVersionSettings)
//
//        val convertedTypeByCandidate =
//            callComponents.samConversionResolver.getFunctionTypeForPossibleSamType(
//                expectedParameterType,
//                callComponents.samConversionOracle
//            ) ?: return null
//
//        val convertedTypeByOriginal =
//            if (expectedParameterType.constructor == originalExpectedType.constructor)
//                callComponents.samConversionResolver.getFunctionTypeForPossibleSamType(
//                    originalExpectedType,
//                    callComponents.samConversionOracle
//                )
//            else
//                convertedTypeByCandidate
//
//        assert(convertedTypeByCandidate.constructor == convertedTypeByOriginal?.constructor) {
//            "If original type is SAM type, then candidate should have same type constructor and corresponding function type\n" +
//                    "originalExpectType: $originalExpectedType, candidateExpectType: $expectedParameterType\n" +
//                    "functionTypeByOriginal: $convertedTypeByOriginal, functionTypeByCandidate: $convertedTypeByCandidate"
//        }
//
//        candidate.resolvedCall.registerArgumentWithSamConversion(
//            argument,
//            SamConversionDescription(convertedTypeByOriginal!!, convertedTypeByCandidate, expectedParameterType)
//        )
//
//        if (needCompatibilityResolveForSAM(candidate, expectedParameterType)) {
//            candidate.markCandidateForCompatibilityResolve()
//        }
//
//        val samDescriptor = originalExpectedType.constructor.declarationDescriptor
//        if (samDescriptor is ClassDescriptor) {
//            callComponents.lookupTracker.record(candidate.scopeTower.location, samDescriptor, SAM_LOOKUP_NAME)
//        }
//
//        return convertedTypeByCandidate
//    }
//
//    private fun needCompatibilityResolveForSAM(candidate: ResolutionCandidate, typeToConvert: UnwrappedType): Boolean {
//        // fun interfaces is a new feature with a new modifier, so no compatibility resolve is needed
//        val descriptor = typeToConvert.constructor.declarationDescriptor
//        if (descriptor is ClassDescriptor && descriptor.isFun) return false
//
//        // now conversions for CangJie candidates are possible, so we have to perform compatibility resolve
//        return !candidate.callComponents.samConversionOracle.isJavaApplicableCandidate(candidate.resolvedCall.candidateDescriptor)
//    }
//
//    fun isJavaParameterCanBeConverted(
//        candidate: ResolutionCandidate,
//        expectedParameterType: UnwrappedType
//    ): Boolean {
//        val callComponents = candidate.callComponents
//
//        val samConversionOracle = callComponents.samConversionOracle
//        if (!samConversionOracle.isJavaApplicableCandidate(candidate.resolvedCall.candidateDescriptor)) return false
//
//        val declarationDescriptor = expectedParameterType.constructor.declarationDescriptor
//        if (declarationDescriptor is ClassDescriptor && declarationDescriptor.isDefinitelyNotSamInterface) return false
//
//        val convertedType =
//            callComponents.samConversionResolver.getFunctionTypeForPossibleSamType(
//                expectedParameterType,
//                callComponents.samConversionOracle
//            )
//
//        return convertedType != null
//    }
//}
object UnitTypeConversions : ParameterTypeConversion {
    override fun conversionDefinitelyNotNeeded(
        candidate: ResolutionCandidate,
        argument: CangJieCallArgument,
        expectedParameterType: UnwrappedType
    ): Boolean {
        // for callable references and lambdas it already works
        if (argument !is SimpleCangJieCallArgument) return true

        val receiver = argument.receiver
        val csBuilder = candidate.getSystem().getBuilder()

        if (receiver.receiverValue.type.hasUnitOrSubtypeReturnType(csBuilder)) return true
        if (receiver.typesFromSmartCasts.any { it.hasUnitOrSubtypeReturnType(csBuilder) }) return true

        if (
            !expectedParameterType.isFunctionType ||
            !expectedParameterType.getReturnTypeFromFunctionType().isUnit()
        ) return true

        return false
    }

    private fun CangJieType.hasUnitOrSubtypeReturnType(c: ConstraintSystemOperation): Boolean =
        isFunctionType && arguments.last().type.isUnitOrSubtypeOrVariable(c)

    private fun CangJieType.isUnitOrSubtypeOrVariable(c: ConstraintSystemOperation): Boolean =
        isUnitOrSubtype() || c.isTypeVariable(this)

    private fun CangJieType.isUnitOrSubtype(): Boolean =
        isUnit() || isDynamic() || isNothing()


    override fun conversionIsNeededBeforeSubtypingCheck(
        argument: CangJieCallArgument,
        areSuspendOnlySamConversionsSupported: Boolean
    ): Boolean =
        argument is SimpleCangJieCallArgument && argument.receiver.stableType.isFunctionType

    override fun conversionIsNeededAfterSubtypingCheck(argument: CangJieCallArgument): Boolean {
        if (argument !is SimpleCangJieCallArgument) return false

        var isFunctionTypeOrSubtype = false
        val hasReturnTypeInSubtypes = argument.receiver.stableType.isFunctionTypeOrSubtype {
            isFunctionTypeOrSubtype = true
            it.getReturnTypeFromFunctionType().isUnitOrSubtype() // there is no need to check for variable as it was done earlier
        }

        if (!isFunctionTypeOrSubtype) return false

        return !hasReturnTypeInSubtypes
    }

    override fun convertParameterType(
        candidate: ResolutionCandidate,
        argument: CangJieCallArgument,
        parameter: ParameterDescriptor,
        expectedParameterType: UnwrappedType
    ): UnwrappedType {
        val nonUnitReturnedParameterType = createFunctionType(
            candidate.callComponents.builtIns,
            expectedParameterType.annotations,
            expectedParameterType.getReceiverTypeFromFunctionType(),
            expectedParameterType.getContextReceiverTypesFromFunctionType(),
            expectedParameterType.getValueParameterTypesFromFunctionType().map { it.type },
            parameterNames = null,
            candidate.callComponents.builtIns.anyType,

        )

        candidate.resolvedCall.registerArgumentWithUnitConversion(argument, nonUnitReturnedParameterType)

        candidate.markCandidateForCompatibilityResolve()

        return nonUnitReturnedParameterType
    }

}
//object SuspendTypeConversions : ParameterTypeConversion {
//    override fun conversionDefinitelyNotNeeded(
//        candidate: ResolutionCandidate,
//        argument: CangJieCallArgument,
//        expectedParameterType: UnwrappedType
//    ): Boolean {
//        if (argument !is SimpleCangJieCallArgument) return true
//
//        val argumentType = argument.receiver.stableType
//        if (argumentType.isSuspendFunctionType) return true
//
//        if (!expectedParameterType.isSuspendFunctionType) return true
//
//        return false
//    }
//
//    override fun conversionIsNeededBeforeSubtypingCheck(
//        argument: CangJieCallArgument,
//        areSuspendOnlySamConversionsSupported: Boolean
//    ): Boolean =
//        argument is SimpleCangJieCallArgument &&
//                (argument.receiver.stableType.isFunctionType  )
//
//    override fun conversionIsNeededAfterSubtypingCheck(argument: CangJieCallArgument): Boolean =
//        argument is SimpleCangJieCallArgument && argument.receiver.stableType.isFunctionTypeOrSubtype
//
//    override fun convertParameterType(
//        candidate: ResolutionCandidate,
//        argument: CangJieCallArgument,
//        parameter: ParameterDescriptor,
//        expectedParameterType: UnwrappedType
//    ): UnwrappedType {
//        val nonSuspendParameterType = createFunctionType(
//            candidate.callComponents.builtIns,
//            expectedParameterType.annotations,
//            expectedParameterType.getReceiverTypeFromFunctionType(),
//            expectedParameterType.getContextReceiverTypesFromFunctionType(),
//            expectedParameterType.getValueParameterTypesFromFunctionType().map { it.type },
//            parameterNames = null,
//            expectedParameterType.getReturnTypeFromFunctionType(),
//            suspendFunction = false
//        )
//
//        candidate.resolvedCall.registerArgumentWithSuspendConversion(argument, nonSuspendParameterType)
//
//        candidate.markCandidateForCompatibilityResolve()
//
//        return nonSuspendParameterType
//    }
//}

interface ParameterTypeConversion {
    fun conversionDefinitelyNotNeeded(
        candidate: ResolutionCandidate,
        argument: CangJieCallArgument,
        expectedParameterType: UnwrappedType
    ): Boolean

    fun conversionIsNeededBeforeSubtypingCheck(argument: CangJieCallArgument, areSuspendOnlySamConversionsSupported: Boolean): Boolean
    fun conversionIsNeededAfterSubtypingCheck(argument: CangJieCallArgument): Boolean

    fun convertParameterType(
        candidate: ResolutionCandidate,
        argument: CangJieCallArgument,
        parameter: ParameterDescriptor,
        expectedParameterType: UnwrappedType
    ): UnwrappedType?
}

object TypeConversions {
    fun performCompositeConversionBeforeSubtyping(
        candidate: ResolutionCandidate,
        argument: CangJieCallArgument,
        candidateParameter: ParameterDescriptor,
        candidateExpectedType: UnwrappedType,
    ): ConversionData {
//        val samConversionData = performConversionBeforeSubtyping(
//            candidate, argument, candidateParameter, candidateExpectedType, SamTypeConversions
//        )

//        val suspendConversionData = performConversionBeforeSubtyping(
//            candidate, argument, candidateParameter,
//            candidateExpectedType = samConversionData.convertedType ?: candidateExpectedType,
//            SuspendTypeConversions
//        )

        val unitConversionData = performConversionBeforeSubtyping(
            candidate, argument, candidateParameter,
            candidateExpectedType =  candidateExpectedType,
            UnitTypeConversions
        )

        return ConversionData(
            convertedType = unitConversionData.convertedType /*?: suspendConversionData.convertedType ?: samConversionData.convertedType*/,
            wasConversion = /*samConversionData.wasConversion || suspendConversionData.wasConversion ||*/ unitConversionData.wasConversion,
            conversionDefinitelyNotNeeded = /*samConversionData.conversionDefinitelyNotNeeded &&
                    suspendConversionData.conversionDefinitelyNotNeeded &&*/
                    unitConversionData.conversionDefinitelyNotNeeded
        )
    }

    fun performCompositeConversionAfterSubtyping(
        candidate: ResolutionCandidate,
        argument: CangJieCallArgument,
        candidateParameter: ParameterDescriptor,
        candidateExpectedType: UnwrappedType,
    ): UnwrappedType? {
//        val samConvertedType = performConversionAfterSubtyping(
//            candidate, argument, candidateParameter, candidateExpectedType, SamTypeConversions
//        )
//
//        val suspendConvertedType = performConversionAfterSubtyping(
//            candidate, argument, candidateParameter,
//            candidateExpectedType = samConvertedType ?: candidateExpectedType,
//            SuspendTypeConversions
//        )

        val unitConvertedType = performConversionAfterSubtyping(
            candidate, argument, candidateParameter,
            candidateExpectedType = /*suspendConvertedType ?: samConvertedType ?: */candidateExpectedType,
            UnitTypeConversions
        )

        return unitConvertedType /*?: suspendConvertedType ?: samConvertedType*/
    }

    private fun performConversionAfterSubtyping(
        candidate: ResolutionCandidate,
        argument: CangJieCallArgument,
        candidateParameter: ParameterDescriptor,
        candidateExpectedType: UnwrappedType,
        conversion: ParameterTypeConversion
    ): UnwrappedType? {
        return if (
            conversion.conversionIsNeededAfterSubtypingCheck(argument) &&
            !conversion.conversionDefinitelyNotNeeded(candidate, argument, candidateExpectedType)
        ) {
            conversion.convertParameterType(candidate, argument, candidateParameter, candidateExpectedType)
        } else {
            null
        }
    }

    private fun performConversionBeforeSubtyping(
        candidate: ResolutionCandidate,
        argument: CangJieCallArgument,
        candidateParameter: ParameterDescriptor,
        candidateExpectedType: UnwrappedType,
        conversion: ParameterTypeConversion
    ): ConversionData {
        val conversionDefinitelyNotNeeded = conversion.conversionDefinitelyNotNeeded(candidate, argument, candidateExpectedType)
//        val areSuspendOnlySamConversionsSupported =
//            candidate.callComponents.languageVersionSettings.supportsFeature(LanguageFeature.SuspendOnlySamConversions)
        return if (
            !conversionDefinitelyNotNeeded
//            &&
//            conversion.conversionIsNeededBeforeSubtypingCheck(argument, areSuspendOnlySamConversionsSupported)
        ) {
            ConversionData(
                conversion.convertParameterType(candidate, argument, candidateParameter, candidateExpectedType),
                wasConversion = true,
                conversionDefinitelyNotNeeded
            )
        } else {
            ConversionData(convertedType = null, wasConversion = false, conversionDefinitelyNotNeeded)
        }
    }

    class ConversionData(val convertedType: UnwrappedType?, val wasConversion: Boolean, val conversionDefinitelyNotNeeded: Boolean)

}
