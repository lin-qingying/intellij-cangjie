package com.huawei.cangjie.resolve.scopes

import com.huawei.cangjie.descriptors.ClassifierDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.Substitutable
import com.huawei.cangjie.descriptors.VariableDescriptor
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.psiUtil.sure
import com.huawei.cangjie.resolve.calls.inference.wrapWithCapturingSubstitution
import com.huawei.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeSubstitutor
import com.huawei.cangjie.types.checker.SimpleClassicTypeSystemContext.safeSubstitute
import com.huawei.cangjie.utils.Printer
import com.huawei.cangjie.utils.newLinkedHashSetWithExpectedSize

class SubstitutingScope(private val workerScope: MemberScope, givenSubstitutor: TypeSubstitutor) : MemberScope {
    val substitutor by lazy { givenSubstitutor.substitution.buildSubstitutor() }

    private val capturingSubstitutor = givenSubstitutor.substitution.wrapWithCapturingSubstitution().buildSubstitutor()
    private var substitutedDescriptors: MutableMap<DeclarationDescriptor, DeclarationDescriptor>? = null

    private val _allDescriptors by lazy { substitute(workerScope.getContributedDescriptors()) }
    override fun getFunctionNames() = workerScope.getFunctionNames()
    override fun getVariableNames() = workerScope.getVariableNames()
    override fun getClassifierNames() = workerScope.getClassifierNames()
    override fun getPropertyNames() = workerScope.getPropertyNames()
    fun substitute(type: CangJieType): CangJieType {
        if (capturingSubstitutor.isEmpty) return type
        return capturingSubstitutor.safeSubstitute(type) as CangJieType
    }

    private fun <D : DeclarationDescriptor> substitute(descriptor: D): D {
        if (capturingSubstitutor.isEmpty) return descriptor

        if (substitutedDescriptors == null) {
            substitutedDescriptors = HashMap<DeclarationDescriptor, DeclarationDescriptor>()
        }

        val substituted = substitutedDescriptors!!.getOrPut(descriptor) {
            when (descriptor) {
                is Substitutable<*> -> descriptor.substitute(capturingSubstitutor).sure {
                    "We expect that no conflict should happen while substitution is guaranteed to generate invariant projection, " +
                            "but $descriptor substitution fails"
                }

                else -> error("Unknown descriptor in scope: $descriptor")
            }
        }

        @Suppress("UNCHECKED_CAST")
        return substituted as D
    }

    private fun <D : DeclarationDescriptor> substitute(descriptors: Collection<D>): Collection<D> {
        if (capturingSubstitutor.isEmpty) return descriptors
        if (descriptors.isEmpty()) return descriptors

        val result = newLinkedHashSetWithExpectedSize<D>(descriptors.size)
        for (descriptor in descriptors) {
            val substitute = substitute(descriptor)
            result.add(substitute)
        }

        return result
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor> =
        substitute(workerScope.getContributedVariables(name, location))

    override fun getContributedPropertys(name: Name, location: LookupLocation) =
        substitute(workerScope.getContributedPropertys(name, location))
    override fun getContributedClassifiers( name: Name, location: LookupLocation): List<ClassifierDescriptor> =
        workerScope.getContributedClassifiers(name, location) .map { substitute(it) }

    override fun getContributedClassifier(name: Name, location: LookupLocation) =
        workerScope.getContributedClassifier(name, location)?.let { substitute(it) }

    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {
        return substitute(workerScope.getExtendClass(name)).toList()
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation) =
        substitute(workerScope.getContributedFunctions(name, location))


    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return _allDescriptors.filter {
            nameFilter.invoke(it.name)
        }
    }

//    override fun getFunctionNames() = workerScope.getFunctionNames()
//    override fun getVariableNames() = workerScope.getVariableNames()
//    override fun getClassifierNames() = workerScope.getClassifierNames()

    override fun definitelyDoesNotContainName(name: Name) = workerScope.definitelyDoesNotContainName(name)

    //
    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.println("substitutor = ")
        p.pushIndent()
        p.println(capturingSubstitutor)
        p.popIndent()

        p.print("workerScope = ")
        workerScope.printScopeStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }
}
