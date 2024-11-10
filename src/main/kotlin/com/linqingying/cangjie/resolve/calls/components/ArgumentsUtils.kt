package com.linqingying.cangjie.resolve.calls.components

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.ParameterDescriptor
import com.linqingying.cangjie.descriptors.ValueParameterDescriptor
import com.linqingying.cangjie.resolve.calls.model.CangJieCallArgument
import com.linqingying.cangjie.resolve.calls.model.ReceiverCangJieCallArgument
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.linqingying.cangjie.types.UnwrappedType
import com.linqingying.cangjie.types.checker.intersectWrappedTypes
import com.linqingying.cangjie.types.checker.prepareArgumentTypeRegardingCaptureTypes
import com.linqingying.cangjie.utils.DFS

val ValueParameterDescriptor.isVararg: Boolean get() = varargElementType != null
  fun CangJieCallArgument.isArrayType(): Boolean {
    if (this !is ReceiverCangJieCallArgument) return false

    if (receiver !is ReceiverValueWithSmartCastInfo) return false

    return CangJieBuiltIns.isArray((receiver as ReceiverValueWithSmartCastInfo).receiverValue.type)


}

internal fun CangJieCallArgument.getExpectedType(
    parameter: ParameterDescriptor,
    languageVersionSettings: LanguageVersionSettings
) =
    if (
        this.isSpread /*||
        this.isArrayAssignedAsNamedArgumentInAnnotation(parameter, languageVersionSettings) ||
        this.isArrayAssignedAsNamedArgumentInFunction(parameter, languageVersionSettings)*/
    ) {
        parameter.type.unwrap()
    } else {
        val varargType = (parameter as? ValueParameterDescriptor)?.varargElementType?.unwrap()
        if (isArrayType() && varargType != null) {
            parameter.type.unwrap()

        } else {
            varargType?: parameter.type.unwrap()

        }

    }

/**
 * @return `true` iff the parameter has a default value, i.e. declares it, inherits it by overriding a parameter which has a default value,
 * or is a parameter of an 'actual' declaration, such that the corresponding 'expect' parameter has a default value.
 */
fun ValueParameterDescriptor.hasDefaultValue(): Boolean {
    return DFS.ifAny(
        listOf(this),
        { current -> current.overriddenDescriptors.map(ValueParameterDescriptor::original) },
        { it.declaresDefaultValue() || it.isActualParameterWithCorrespondingExpectedDefault }
    )
}

/**
 * @see isActualParameterWithAnyExpectedDefault
 */
val ValueParameterDescriptor.isActualParameterWithCorrespondingExpectedDefault: Boolean
    get() = checkExpectedParameter { it.declaresDefaultValue() }

private fun ValueParameterDescriptor.checkExpectedParameter(checker: (ValueParameterDescriptor) -> Boolean): Boolean {
//    val function = containingDeclaration
//    if (function is FunctionDescriptor && function.isActual) {
//        val expected = function.findCompatibleExpectsForActual().firstOrNull()
//        return expected is FunctionDescriptor && checker(expected.valueParameters[index])
//    }
    return false
}

// with all smart casts if stable
val ReceiverValueWithSmartCastInfo.stableType: UnwrappedType
    get() {
        if (!isStable || !hasTypesFromSmartCasts())
            return receiverValue.type.unwrap()

        /*
         * We have to intersect types first as after capturing, subtyping relation may change and some type won't be excluded from intersection type.
         *
         * Example:
         *      allOriginalTypes = [Inv<out CharSequence>, Inv<String>]
         *      intersect(Inv<out CharSequence>, Inv<String>) = Inv<String>
         *      capture(Inv<String>) = Inv<String>
         * But with capturing first:
         *      capture(Inv<out CharSequence>) = Inv<CapturedType(out CharSequence)>
         *      capture(Inv<String>) = Inv<String>
         *      intersect(Inv<CapturedType(out CharSequence)>, Inv<String>) = Inv<CapturedType(out CharSequence)> & Inv<String>
         *
         * Such redundant type with captured argument may further lead to contradiction in constraint system or less exact solution.
         */
        val intersectionType = intersectWrappedTypes(allOriginalTypes)


//        if (intersectionType.isNullableNothing() && !intersectionType.isMarkedOption) {
//            return intersectionType.makeNullable().unwrap()
//        }

        return prepareArgumentTypeRegardingCaptureTypes(intersectionType) ?: intersectionType
    }

internal fun unexpectedArgument(argument: CangJieCallArgument): Nothing =
    error("Unexpected argument type: $argument, ${argument.javaClass.canonicalName}.")

internal val ReceiverValueWithSmartCastInfo.unstableType: UnwrappedType?
    get() {
        if (isStable || !hasTypesFromSmartCasts())
            return if (isStable) null else receiverValue.type.unwrap()

        val intersectionType = intersectWrappedTypes(allOriginalTypes)

        return prepareArgumentTypeRegardingCaptureTypes(intersectionType) ?: intersectionType
    }
