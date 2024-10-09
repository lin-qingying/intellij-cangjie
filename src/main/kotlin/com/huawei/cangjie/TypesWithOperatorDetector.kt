package com.huawei.cangjie

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.ide.CangJieIndicesHelper
import com.huawei.cangjie.ide.search.isValidOperator
import com.huawei.cangjie.ide.util.fuzzyExtensionReceiverType
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.scopes.collectFunctions
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.util.TypeNullability
import com.huawei.cangjie.utils.OperatorNameConventions
import com.huawei.cangjie.utils.addIfNotNull
import java.util.ArrayList
import java.util.HashMap

class TypesWithContainsDetector(
    scope: LexicalScope,
    indicesHelper: CangJieIndicesHelper?,
    private val argumentType: CangJieType
) : TypesWithOperatorDetector(OperatorNameConventions.CONTAINS, scope, indicesHelper) {

    override fun checkIsSuitableByType(
        operator: FunctionDescriptor,
        freeTypeParams: Collection<TypeParameterDescriptor>
    ): TypeSubstitutor? {
        val parameter = operator.valueParameters.single()
        val fuzzyParameterType = parameter.type.toFuzzyType(operator.typeParameters + freeTypeParams)
        return fuzzyParameterType.checkIsSuperTypeOf(argumentType)
    }
}

abstract class TypesWithOperatorDetector(
    private val name: Name,
    private val scope: LexicalScope,
    private val indicesHelper: CangJieIndicesHelper?
) {
    protected abstract fun checkIsSuitableByType(
        operator: FunctionDescriptor,
        freeTypeParams: Collection<TypeParameterDescriptor>
    ): TypeSubstitutor?

    private val cache = HashMap<FuzzyType, Pair<FunctionDescriptor, TypeSubstitutor>?>()

    val extensionOperators: Collection<FunctionDescriptor> by lazy {
        val result = ArrayList<FunctionDescriptor>()

        val extensionsFromScope = scope
            .collectFunctions(name, NoLookupLocation.FROM_IDE)
            .filter { it.extensionReceiverParameter != null }
        result.addSuitableOperators(extensionsFromScope)

        indicesHelper?.getTopLevelExtensionOperatorsByName(name.asString())?.let { result.addSuitableOperators(it) }

        result.distinctBy { it.original }
    }

    val classesWithMemberOperators: Collection<ClassDescriptor> by lazy {
        if (indicesHelper == null) return@lazy emptyList<ClassDescriptor>()
        val operators = ArrayList<FunctionDescriptor>().addSuitableOperators(indicesHelper.getMemberOperatorsByName(name.asString()))
        operators.map { it.containingDeclaration as ClassDescriptor }.distinct()
    }

    private fun MutableCollection<FunctionDescriptor>.addSuitableOperators(functions: Collection<FunctionDescriptor>): MutableCollection<FunctionDescriptor> {
        for (function in functions) {
            if (!function.isValidOperator()) continue

            val freeParameters = function.typeParameters
            val containingClass = function.containingDeclaration as? ClassDescriptor
            if (containingClass != null) {
                freeParameters += containingClass.typeConstructor.parameters
            }

            val substitutor = checkIsSuitableByType(function, freeParameters) ?: continue
            addIfNotNull(function.substitute(substitutor))
        }
        return this
    }

    fun findOperator(type: FuzzyType): Pair<FunctionDescriptor, TypeSubstitutor>? = if (cache.containsKey(type)) {
        cache[type]
    } else {
        val result = findOperatorNoCache(type)
        cache[type] = result
        result
    }

    private fun findOperatorNoCache(type: FuzzyType): Pair<FunctionDescriptor, TypeSubstitutor>? {
        if (type.nullability() != TypeNullability.NULLABLE) {
            for (memberFunction in type.type.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_IDE)) {
                if (memberFunction.isValidOperator()) {
                    val substitutor = checkIsSuitableByType(memberFunction, type.freeParameters) ?: continue
                    val substituted = memberFunction.substitute(substitutor) ?: continue
                    return substituted to substitutor
                }
            }
        }

        for (operator in extensionOperators) {
            val substitutor = type.checkIsSubtypeOf(operator.fuzzyExtensionReceiverType()!!) ?: continue
            val substituted = operator.substitute(substitutor) ?: continue
            return substituted to substitutor
        }

        return null
    }
}
