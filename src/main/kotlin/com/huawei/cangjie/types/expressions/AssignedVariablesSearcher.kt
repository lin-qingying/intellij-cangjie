package com.huawei.cangjie.types.expressions

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.SetMultimap
import com.huawei.cangjie.descriptors.VariableDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjBinaryExpression
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjTreeVisitorVoid

abstract class AssignedVariablesSearcher : CjTreeVisitorVoid()
{

    data class Writer(val assignment: CjBinaryExpression, val declaration: CjDeclaration?)
    private val assignedNames: SetMultimap<Name, Writer> = LinkedHashMultimap.create()

    fun hasWriters(variableDescriptor: VariableDescriptor) = writers(variableDescriptor).isNotEmpty()
    open fun writers(variableDescriptor: VariableDescriptor): MutableSet<Writer> = assignedNames[variableDescriptor.name]

}
