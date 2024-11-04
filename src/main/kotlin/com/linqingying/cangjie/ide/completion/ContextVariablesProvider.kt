package com.linqingying.cangjie.ide.completion

import com.linqingying.cangjie.descriptors.VariableDescriptor
import com.linqingying.cangjie.types.FuzzyType
import com.linqingying.cangjie.types.TypeSubstitutor
import java.util.HashSet

interface ContextVariablesProvider {
    fun functionTypeVariables(requiredType: FuzzyType): Collection<Pair<VariableDescriptor, TypeSubstitutor>>
}
class CollectRequiredTypesContextVariablesProvider : ContextVariablesProvider {
    private val _requiredTypes = HashSet<FuzzyType>()

    val requiredTypes: Set<FuzzyType>
        get() = _requiredTypes

    override fun functionTypeVariables(requiredType: FuzzyType): Collection<Pair<VariableDescriptor, TypeSubstitutor>> {
        _requiredTypes.add(requiredType)
        return emptyList()
    }
}
