package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.descriptors.ParameterDescriptor
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.resolve.calls.model.CangJieCallArgument
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.huawei.cangjie.types.UnwrappedType
import com.huawei.cangjie.types.checker.SimpleClassicTypeSystemContext.isNullableNothing
import com.huawei.cangjie.types.checker.intersectWrappedTypes
import com.huawei.cangjie.types.checker.prepareArgumentTypeRegardingCaptureTypes
import com.huawei.cangjie.utils.DFS


internal fun CangJieCallArgument.getExpectedType(parameter: ParameterDescriptor, languageVersionSettings: LanguageVersionSettings) =
    if (
        this.isSpread /*||
        this.isArrayAssignedAsNamedArgumentInAnnotation(parameter, languageVersionSettings) ||
        this.isArrayAssignedAsNamedArgumentInFunction(parameter, languageVersionSettings)*/
    ) {
        parameter.type.unwrap()
    } else {
        /*(parameter as? ValueParameterDescriptor)?.varargElementType?.unwrap() ?: */parameter.type.unwrap()
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
