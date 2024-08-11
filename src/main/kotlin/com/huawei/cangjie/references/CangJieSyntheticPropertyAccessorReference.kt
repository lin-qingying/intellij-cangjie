package com.huawei.cangjie.references

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjNameReferenceExpression
import com.huawei.cangjie.resolve.BindingContext
import com.intellij.util.SmartList
import com.intellij.util.containers.addIfNotNull


internal class CangJieSyntheticPropertyAccessorReference(
    expression: CjNameReferenceExpression,
    getter: Boolean
) : SyntheticPropertyAccessorReference(expression, getter), CjReference {
    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
//        val descriptors = expression.getReferenceTargets(context)

        val result = SmartList<FunctionDescriptor>()
//        for (descriptor in descriptors) {
//            if (descriptor is SyntheticJavaPropertyDescriptor) {
//                if (getter) {
//                    result.add(descriptor.getMethod)
//                } else {
//                    if (descriptor.setMethod == null) result.addIfNotNull(descriptor.getMethod)
//                    else result.addIfNotNull(descriptor.setMethod)
//                }
//            }
//        }
        return result
    }

}
