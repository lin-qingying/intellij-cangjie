package com.huawei.cangjie.resolve.calls.tasks

import com.huawei.cangjie.descriptors.CallableMemberDescriptor
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.resolve.classId
import com.huawei.cangjie.types.TypeSubstitutor
import com.huawei.cangjie.utils.OperatorNameConventions

// Creates a descriptor denoting an extension function for a collection of non-extension "invoke"s from function types.
// For example, `fun invoke(param: P): R` becomes `fun P.invoke(): R`
fun createSynthesizedInvokes(functions: Collection<FunctionDescriptor>): Collection<FunctionDescriptor> {
    val result = ArrayList<FunctionDescriptor>(1)

//    for (invoke in functions) {
//        if (invoke.name != OperatorNameConventions.INVOKE) continue
//
//        // "invoke" must have at least one parameter, which will become the receiver parameter of the synthesized "invoke"
//        if (invoke.valueParameters.isEmpty()) continue
//
//        val containerClassId = (invoke.containingDeclaration as ClassDescriptor).classId
//        val synthesized = if (containerClassId != null && isBuiltinFunctionClass(containerClassId)) {
//            createSynthesizedFunctionWithFirstParameterAsReceiver(invoke)
//        } else {
//            val invokeDeclaration = invoke.overriddenDescriptors.singleOrNull()
//                ?: error("No single overridden invoke for $invoke: ${invoke.overriddenDescriptors}")
//            val synthesizedSuperFun = createSynthesizedFunctionWithFirstParameterAsReceiver(invokeDeclaration)
//            val fakeOverride = synthesizedSuperFun.copy(
//                invoke.containingDeclaration,
//                synthesizedSuperFun.modality,
//                synthesizedSuperFun.visibility,
//                CallableMemberDescriptor.Kind.FAKE_OVERRIDE,
//                /* copyOverrides = */ false
//            )
//            fakeOverride.setSingleOverridden(synthesizedSuperFun)
//            fakeOverride
//        }
//
//        result.add(synthesized.substitute(TypeSubstitutor.create(invoke.dispatchReceiverParameter!!.type)) ?: continue)
//    }

    return result
}
