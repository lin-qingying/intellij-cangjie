package com.huawei.cangjie.resolve.scopes;

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.source.MemberScopeImpl
import com.huawei.cangjie.utils.Printer
import com.huawei.cangjie.utils.flatMapToNullable

fun MemberScope.computeAllNames() = getClassifierNames()?.let { classifierNames ->
    getFunctionNames().toMutableSet().also {
        it.addAll(getVariableNames())
        it.addAll(classifierNames)
    }
}
fun Iterable<MemberScope>.flatMapClassifierNamesOrNull(): MutableSet<Name>? =
    flatMapToNullable(hashSetOf(), MemberScope::getClassifierNames)

interface MemberScope : ResolutionScope{
       override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard PropertyDescriptor>

    /**
     * These methods may return a superset of an actual names' set
     */
    fun getFunctionNames(): Set<Name>
    fun getVariableNames(): Set<Name>
    fun getClassifierNames(): Set<Name>?

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<@JvmWildcard SimpleFunctionDescriptor>
    companion object {
        val ALL_NAME_FILTER: (Name) -> Boolean = { true }
    }
    /**
     * Is supposed to be used in tests and debug only
     */
    fun printScopeStructure(p: Printer)
    object Empty : MemberScopeImpl() {
        override fun printScopeStructure(p: Printer) {
            p.println("Empty member scope")
        }

        override fun definitelyDoesNotContainName(name: Name): Boolean = true

        override fun getFunctionNames() = emptySet<Name>()
        override fun getVariableNames() = emptySet<Name>()
        override fun getClassifierNames() = emptySet<Name>()
    }
}


abstract class DescriptorKindExclude{
    object TopLevelPackages : DescriptorKindExclude() {
//        override fun excludes(descriptor: DeclarationDescriptor): Boolean {
//            val fqName = when (descriptor) {
//                is PackageFragmentDescriptor -> descriptor.fqName
//                is PackageViewDescriptor -> descriptor.fqName
//                else -> return false
//            }
//            return fqName.parent().isRoot
//        }

        override val fullyExcludedDescriptorKinds: Int get() = 0
    }
    /**
     * Bit-mask of descriptor kind's that are fully excluded by this [DescriptorKindExclude].
     * That is, [excludes] returns true for all descriptor of these kinds.
     */
    abstract val fullyExcludedDescriptorKinds: Int
}
class DescriptorKindFilter(
    kindMask: Int,
    val excludes: List<DescriptorKindExclude> = listOf()
){

    val kindMask: Int
    init {
        var mask = kindMask
        excludes.forEach { mask = mask and it.fullyExcludedDescriptorKinds.inv() }
        this.kindMask = mask
    }
    fun acceptsKinds(kinds: Int): Boolean
            = kindMask and kinds != 0
    fun withoutKinds(kinds: Int): DescriptorKindFilter
            = DescriptorKindFilter(kindMask and kinds.inv(), excludes)

    companion object{

        private var nextMaskValue: Int = 0x01
        val SINGLETON_CLASSIFIERS_MASK: Int = nextMask()
        val TYPE_ALIASES_MASK: Int = nextMask()
        val FUNCTIONS_MASK: Int = nextMask()
        val VARIABLES_MASK: Int = nextMask()

        val ALL_KINDS_MASK: Int = nextMask() - 1
        val PACKAGES_MASK: Int = nextMask()
        @JvmField val PACKAGES: DescriptorKindFilter = DescriptorKindFilter(PACKAGES_MASK)
        @JvmField val FUNCTIONS: DescriptorKindFilter = DescriptorKindFilter(FUNCTIONS_MASK)
        @JvmField val VARIABLES: DescriptorKindFilter = DescriptorKindFilter(VARIABLES_MASK)

        @JvmField val ALL: DescriptorKindFilter = DescriptorKindFilter(ALL_KINDS_MASK)

        private fun nextMask() = nextMaskValue.apply { nextMaskValue = nextMaskValue shl 1 }

        val NON_SINGLETON_CLASSIFIERS_MASK: Int = nextMask()

        val CLASSIFIERS_MASK: Int = NON_SINGLETON_CLASSIFIERS_MASK or SINGLETON_CLASSIFIERS_MASK or TYPE_ALIASES_MASK

    }
}
