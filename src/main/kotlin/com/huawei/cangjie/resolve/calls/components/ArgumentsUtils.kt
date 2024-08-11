package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.descriptors.ParameterDescriptor
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.resolve.calls.model.CangJieCallArgument
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
