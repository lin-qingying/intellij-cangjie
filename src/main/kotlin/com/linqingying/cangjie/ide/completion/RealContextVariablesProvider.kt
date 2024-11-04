package com.linqingying.cangjie.ide.completion

import com.linqingying.cangjie.builtins.isBuiltinFunctionalTypeOrSubtype
import com.linqingying.cangjie.descriptors.VariableDescriptor
import com.linqingying.cangjie.ide.codeinsight.ReferenceVariantsHelper
import com.linqingying.cangjie.resolve.scopes.DescriptorKindExclude
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.types.FuzzyType
import com.linqingying.cangjie.types.TypeSubstitutor
import com.linqingying.cangjie.types.fuzzyReturnType
import com.linqingying.cangjie.utils.CallTypeAndReceiver
import com.intellij.psi.PsiElement
import com.intellij.util.SmartList
import java.util.HashSet



class RealContextVariablesProvider(
    private val referenceVariantsHelper: ReferenceVariantsHelper,
    private val contextElement: PsiElement
) : ContextVariablesProvider {

    val allFunctionTypeVariables by lazy {
        collectVariables().filter { it.type.isBuiltinFunctionalTypeOrSubtype }
    }

    /*
    * The reason for using `nameFilter = MemberScope.ALL_NAME_FILTER` here is that we have
    * functionality that allows to complete arguments for a completing call like here:
    * class C {
    *   companion object {
    *     fun create(p: (Int) -> Unit) {}
    *   }
    * }
    *
    * val handler: (Int) -> Unit = {}
    *
    * val v: C = cr<caret>
    *
    * And here at <caret> it's possible to complete the full line: C.create(handler) !!!
    * */
    private fun collectVariables(): Collection<VariableDescriptor> {
        val descriptorFilter =
            DescriptorKindFilter.VARIABLES exclude DescriptorKindExclude.Extensions // we exclude extensions by performance reasons
        return referenceVariantsHelper.getReferenceVariants(
            contextElement,
            CallTypeAndReceiver.DEFAULT,
            descriptorFilter,
            nameFilter = MemberScope.ALL_NAME_FILTER).map { it as VariableDescriptor }
    }

    override fun functionTypeVariables(requiredType: FuzzyType): Collection<Pair<VariableDescriptor, TypeSubstitutor>> {
        val result = SmartList<Pair<VariableDescriptor, TypeSubstitutor>>()
        for (variable in allFunctionTypeVariables) {
            val substitutor = variable.fuzzyReturnType()?.checkIsSubtypeOf(requiredType) ?: continue
            result.add(variable to substitutor)
        }
        return result
    }
}

