package com.huawei.cangjie.resolve

import com.huawei.cangjie.psi.CjProperty
import com.huawei.cangjie.psi.CjPropertyAccessor
import com.huawei.cangjie.psi.CjVariable
import com.huawei.cangjie.types.CangJieType

class VariableAsPropertyInfo(
    val propertyGetter: CjPropertyAccessor?,
    val propertySetter: CjPropertyAccessor?,
    val variableType: CangJieType?,
    val hasBody: Boolean,
//    val hasDelegate: Boolean
) {
    companion object {
        fun createFromDestructuringDeclarationEntry(type: CangJieType): VariableAsPropertyInfo {
            return VariableAsPropertyInfo(null, null, type, false/*, false*/)
        }
@JvmStatic
        fun createFromProperty(property: CjVariable): VariableAsPropertyInfo {
            return VariableAsPropertyInfo(null, null, null, false/*, property.hasDelegate()*/)
        }
        @JvmStatic

        fun createFromProperty(property: CjProperty): VariableAsPropertyInfo {
            return VariableAsPropertyInfo(
                property.getter,
                property.setter,
                null,
                property.hasBody()/*, property.hasDelegate()*/
            )
        }
    }
}
