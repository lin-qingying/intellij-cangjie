package com.huawei.cangjie.resolve.scopes

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.huawei.cangjie.resolve.scopes.MemberScope.Companion.ALL_NAME_FILTER
import com.huawei.cangjie.resolve.source.MemberScopeImpl
import com.huawei.cangjie.utils.Printer
import com.huawei.cangjie.utils.flatMapToNullable

fun MemberScope.computeAllNames() = getClassifierNames()?.let { classifierNames ->
    getFunctionNames().toMutableSet().also {
        it.addAll(getVariableNames())
        it.addAll(classifierNames)
    }
}

/**
 * The same as getDescriptors(kindFilter, nameFilter) but the result is guaranteed to be filtered by kind and name.
 */
fun MemberScope.getDescriptorsFiltered(
    kindFilter: DescriptorKindFilter = DescriptorKindFilter.ALL,
    nameFilter: (Name) -> Boolean = ALL_NAME_FILTER
): Collection<DeclarationDescriptor> {
    if (kindFilter.kindMask == 0) return listOf()
    return getContributedDescriptors(kindFilter, nameFilter).filter { kindFilter.accepts(it) && nameFilter(it.name) }
}

fun Iterable<MemberScope>.flatMapClassifierNamesOrNull(): MutableSet<Name>? =
    flatMapToNullable(hashSetOf(), MemberScope::getClassifierNames)

interface MemberScope : ResolutionScope {
    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor>

    override fun getContributedPropertys(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard PropertyDescriptor>

    /**
     * These methods may return a superset of an actual names' set
     */
    fun getFunctionNames(): Set<Name>
    fun getVariableNames(): Set<Name>
    fun getClassifierNames(): Set<Name>?
    fun getPropertyNames(): Set<Name>

    override fun getContributedFunctions(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard SimpleFunctionDescriptor>

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
        override fun getPropertyNames(): Set<Name> = emptySet<Name>()

        override fun getFunctionNames() = emptySet<Name>()
        override fun getVariableNames() = emptySet<Name>()
        override fun getClassifierNames() = emptySet<Name>()
    }
}


abstract class DescriptorKindExclude {
    abstract fun excludes(descriptor: DeclarationDescriptor): Boolean
    object Extensions : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor)
                = descriptor is CallableDescriptor && descriptor.extensionReceiverParameter != null

        override val fullyExcludedDescriptorKinds: Int get() = 0
    }
    object NonExtensions : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor)
                = descriptor !is CallableDescriptor || descriptor.extensionReceiverParameter == null

        override val fullyExcludedDescriptorKinds
                = DescriptorKindFilter.ALL_KINDS_MASK and (DescriptorKindFilter.FUNCTIONS_MASK or DescriptorKindFilter.VARIABLES_MASK).inv()
    }
    object TopLevelPackages : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor): Boolean {
            val fqName = when (descriptor) {
                is PackageFragmentDescriptor -> descriptor.fqName
                is PackageViewDescriptor -> descriptor.fqName
                else -> return false
            }
            return fqName.parent().isRoot
        }

        override val fullyExcludedDescriptorKinds: Int get() = 0
    }

    object EnumEntry : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor) =
            descriptor is ClassDescriptor && descriptor.kind == ClassKind.ENUM_ENTRY

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
) {

    val kindMask: Int

    init {
        var mask = kindMask
        excludes.forEach { mask = mask and it.fullyExcludedDescriptorKinds.inv() }
        this.kindMask = mask
    }

    fun acceptsKinds(kinds: Int): Boolean = kindMask and kinds != 0
    fun withoutKinds(kinds: Int): DescriptorKindFilter = DescriptorKindFilter(kindMask and kinds.inv(), excludes)
    fun restrictedToKindsOrNull(kinds: Int): DescriptorKindFilter? {
        val mask = kindMask and kinds
        if (mask == 0) return null
        return DescriptorKindFilter(mask, excludes)
    }

    private fun DeclarationDescriptor.kind(): Int {
        return when (this) {
            is ClassDescriptor -> if (this.kind.isObject) SINGLETON_CLASSIFIERS_MASK else NON_SINGLETON_CLASSIFIERS_MASK
            is TypeAliasDescriptor -> TYPE_ALIASES_MASK
            is ClassifierDescriptor -> NON_SINGLETON_CLASSIFIERS_MASK
            is PackageFragmentDescriptor, is PackageViewDescriptor -> PACKAGES_MASK
            is FunctionDescriptor -> FUNCTIONS_MASK
            is VariableDescriptor -> VARIABLES_MASK
            else -> 0
        }
    }

    fun accepts(descriptor: DeclarationDescriptor): Boolean =
        kindMask and descriptor.kind() != 0 && excludes.all { !it.excludes(descriptor) }

    infix fun exclude(exclude: DescriptorKindExclude): DescriptorKindFilter =
        DescriptorKindFilter(kindMask, excludes + listOf(exclude))

    companion object {

        private var nextMaskValue: Int = 0x01
        val SINGLETON_CLASSIFIERS_MASK: Int = nextMask()
        val TYPE_ALIASES_MASK: Int = nextMask()
        val FUNCTIONS_MASK: Int = nextMask()
        val VARIABLES_MASK: Int = nextMask()
        val PROPERTYS_MASK: Int = nextMask()

        val PACKAGES_MASK: Int = nextMask()

        //        重导出语句
        val REEXPORT_MASK: Int = nextMask()
        val ALL_KINDS_MASK: Int = nextMask() - 1
        val CALLABLES_MASK: Int = FUNCTIONS_MASK or VARIABLES_MASK

        @JvmField
        val PACKAGES: DescriptorKindFilter = DescriptorKindFilter(PACKAGES_MASK)

        @JvmField
        val FUNCTIONS: DescriptorKindFilter = DescriptorKindFilter(FUNCTIONS_MASK)
        @JvmField val CALLABLES: DescriptorKindFilter = DescriptorKindFilter(CALLABLES_MASK)

        @JvmField
        val VARIABLES: DescriptorKindFilter = DescriptorKindFilter(VARIABLES_MASK)

        @JvmField
        val PROPERTYS: DescriptorKindFilter = DescriptorKindFilter(PROPERTYS_MASK)

        @JvmField
        val REEXPORT: DescriptorKindFilter = DescriptorKindFilter(REEXPORT_MASK)

        @JvmField
        val ALL: DescriptorKindFilter = DescriptorKindFilter(ALL_KINDS_MASK)

        private fun nextMask() = nextMaskValue.apply { nextMaskValue = nextMaskValue shl 1 }

        val NON_SINGLETON_CLASSIFIERS_MASK: Int = nextMask()

        val CLASSIFIERS_MASK: Int = NON_SINGLETON_CLASSIFIERS_MASK or SINGLETON_CLASSIFIERS_MASK or TYPE_ALIASES_MASK

        @JvmField
        val CLASSIFIERS: DescriptorKindFilter = DescriptorKindFilter(CLASSIFIERS_MASK)

    }
}

fun CjFile.getScope(): FileScope {
    return FileScope(this)
}

class FileScope(val file: CjFile) : MemberScope {
    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor> {
        TODO("Not yet implemented")
    }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        TODO("Not yet implemented")
    }

    override fun getFunctionNames(): Set<Name> {
        TODO("Not yet implemented")
    }

    override fun getVariableNames(): Set<Name> {
        TODO("Not yet implemented")
    }

    override fun getClassifierNames(): Set<Name>? {
        TODO("Not yet implemented")
    }

    override fun getPropertyNames(): Set<Name> {
        TODO("Not yet implemented")
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        TODO("Not yet implemented")
    }

    override fun printScopeStructure(p: Printer) {
        TODO("Not yet implemented")
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        TODO("Not yet implemented")
    }

    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {
        TODO("Not yet implemented")
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        TODO("Not yet implemented")
    }
}
