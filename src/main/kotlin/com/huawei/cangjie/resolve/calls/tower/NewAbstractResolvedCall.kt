package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.psi.Call
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.model.CangJieCall
import com.huawei.cangjie.resolve.calls.model.CangJieCallDiagnostic
import com.huawei.cangjie.resolve.calls.model.ResolvedCall
import com.huawei.cangjie.resolve.calls.model.ResolvedCallAtom
import com.huawei.cangjie.types.CangJieType


sealed class NewAbstractResolvedCall<D : CallableDescriptor> : ResolvedCall<D>
{
    abstract val psiCangJieCall: PSICangJieCall
    abstract val cangjieCall: CangJieCall?

    abstract val resolvedCallAtom: ResolvedCallAtom?
    abstract val diagnostics: Collection<CangJieCallDiagnostic>
    abstract fun updateExtensionReceiverType(newType: CangJieType)

    abstract fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?)
    abstract fun updateDispatchReceiverType(newType: CangJieType)

    override fun getCall(): Call = psiCangJieCall.psiCall
}
