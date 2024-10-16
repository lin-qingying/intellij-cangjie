package com.huawei.cangjie.resolve.scopes

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.resolve.lazy.descriptors.LazyEnumEntryDescriptor
import com.huawei.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.huawei.cangjie.resolve.scopes.MemberScope.Companion.ALL_NAME_FILTER
import com.huawei.cangjie.resolve.source.MemberScopeImpl
import com.huawei.cangjie.utils.Printer
import com.huawei.cangjie.utils.flatMapToNullable
import java.lang.reflect.Modifier

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

class InstanceMemberScope(private val memberScope: MemberScope) : MemberScope {
    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        return memberScope.getContributedVariables(name, location).filter { !it.isStatic }
    }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        return memberScope.getContributedPropertys(name, location).filter { !it.isStatic }
    }

    override fun getFunctionNames(): Set<Name> {
        return memberScope.getFunctionNames() // 返回所有函数名称

            .toSet()
    }

    override fun getVariableNames(): Set<Name> {
        return memberScope.getVariableNames() // 返回所有变量名称

            .toSet()
    }

    override fun getClassifierNames(): Set<Name>? {
        return memberScope.getClassifierNames() // 返回所有分类器名称
    }

    override fun getPropertyNames(): Set<Name> {
        return memberScope.getPropertyNames() // 返回所有属性名称

            .toSet()
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return memberScope.getContributedFunctions(name, location).filter { !it.isStatic } // 过滤非静态函数
    }

    override fun printScopeStructure(p: Printer) {
        p.println("InstanceMemberScope:") // 打印作用域结构
        memberScope.printScopeStructure(p) // 打印基础成员作用域的结构
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return memberScope.getContributedClassifier(name, location)?.takeIf { !it.isStatic } // 过滤非静态分类器
    }

    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {
        return memberScope.getExtendClass(name)
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return memberScope.getContributedDescriptors(kindFilter, nameFilter).filter { !it.isStatic } // 过滤非静态描述符
    }
}

class StaticMemberScope(val memberScope: MemberScope) : MemberScope {
    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        return memberScope.getContributedVariables(name, location).filter { it.isStatic }
    }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        return memberScope.getContributedPropertys(name, location).filter { it.isStatic }
    }

    override fun getFunctionNames(): Set<Name> {
        return memberScope.getFunctionNames() // 返回所有函数名称

            .toSet()
    }

    override fun getVariableNames(): Set<Name> {
        return memberScope.getVariableNames() // 返回所有变量名称

            .toSet()
    }

    override fun getClassifierNames(): Set<Name>? {
        return memberScope.getClassifierNames() // 返回所有分类器名称
    }

    override fun getPropertyNames(): Set<Name> {
        return memberScope.getPropertyNames() // 返回所有属性名称

            .toSet()
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return memberScope.getContributedFunctions(name, location).filter { it.isStatic } // 过滤非静态函数
    }

    override fun printScopeStructure(p: Printer) {
        p.println("InstanceMemberScope:") // 打印作用域结构
        memberScope.printScopeStructure(p) // 打印基础成员作用域的结构
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return memberScope.getContributedClassifier(name, location)?.takeIf { it.isStatic   || it  is LazyEnumEntryDescriptor} // 过滤非静态分类器
    }

    override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {
        return memberScope.getExtendClass(name)
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return memberScope.getContributedDescriptors(kindFilter, nameFilter).filter { it.isStatic } // 过滤非静态描述符
    }
}

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
        override fun excludes(descriptor: DeclarationDescriptor) =
            descriptor is CallableDescriptor && descriptor.extensionReceiverParameter != null

        override val fullyExcludedDescriptorKinds: Int get() = 0
    }

    override fun toString() = this::class.java.simpleName

    object NonExtensions : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor) =
            descriptor !is CallableDescriptor || descriptor.extensionReceiverParameter == null

        override val fullyExcludedDescriptorKinds =
            DescriptorKindFilter.ALL_KINDS_MASK and (DescriptorKindFilter.FUNCTIONS_MASK or DescriptorKindFilter.VARIABLES_MASK).inv()
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
    companion object {
        private class MaskToName(val mask: Int, val name: String)


        private var nextMaskValue: Int = 0x01
        private fun nextMask() = nextMaskValue.apply { nextMaskValue = nextMaskValue shl 1 }

        val NON_SINGLETON_CLASSIFIERS_MASK: Int = nextMask()
        val SINGLETON_CLASSIFIERS_MASK: Int = nextMask()
        val TYPE_ALIASES_MASK: Int = nextMask()
        val PACKAGES_MASK: Int = nextMask()
        val FUNCTIONS_MASK: Int = nextMask()
        val VARIABLES_MASK: Int = nextMask()
        val PROPERTYS_MASK: Int = nextMask()

        val REEXPORT_MASK: Int = nextMask()

        val ALL_KINDS_MASK: Int = nextMask() - 1
        val CLASSIFIERS_MASK: Int = NON_SINGLETON_CLASSIFIERS_MASK or SINGLETON_CLASSIFIERS_MASK or TYPE_ALIASES_MASK


        val VALUES_MASK: Int = SINGLETON_CLASSIFIERS_MASK or FUNCTIONS_MASK or VARIABLES_MASK or PROPERTYS_MASK
        val CALLABLES_MASK: Int = FUNCTIONS_MASK or VARIABLES_MASK or PROPERTYS_MASK

        @JvmField
        val ALL: DescriptorKindFilter = DescriptorKindFilter(ALL_KINDS_MASK)

        @JvmField
        val CALLABLES: DescriptorKindFilter = DescriptorKindFilter(CALLABLES_MASK)

        @JvmField
        val NON_SINGLETON_CLASSIFIERS: DescriptorKindFilter = DescriptorKindFilter(NON_SINGLETON_CLASSIFIERS_MASK)

        @JvmField
        val SINGLETON_CLASSIFIERS: DescriptorKindFilter = DescriptorKindFilter(SINGLETON_CLASSIFIERS_MASK)

        @JvmField
        val TYPE_ALIASES: DescriptorKindFilter = DescriptorKindFilter(TYPE_ALIASES_MASK)

        @JvmField
        val CLASSIFIERS: DescriptorKindFilter = DescriptorKindFilter(CLASSIFIERS_MASK)

        @JvmField
        val PACKAGES: DescriptorKindFilter = DescriptorKindFilter(PACKAGES_MASK)

        @JvmField
        val REEXPORT: DescriptorKindFilter = DescriptorKindFilter(REEXPORT_MASK)

        @JvmField
        val FUNCTIONS: DescriptorKindFilter = DescriptorKindFilter(FUNCTIONS_MASK)

        @JvmField
        val VARIABLES: DescriptorKindFilter = DescriptorKindFilter(VARIABLES_MASK)

        @JvmField
        val PROPERTYS: DescriptorKindFilter = DescriptorKindFilter(PROPERTYS_MASK)

        @JvmField
        val VALUES: DescriptorKindFilter = DescriptorKindFilter(VALUES_MASK)


        private val DEBUG_MASK_BIT_NAMES = staticFields<DescriptorKindFilter>()
            .filter { it.type == Integer.TYPE }
            .mapNotNull { field ->
                val mask = field.get(null) as Int
                val isOneBitMask = mask == (mask and (-mask))
                if (isOneBitMask) MaskToName(mask, field.name) else null
            }


        private val DEBUG_PREDEFINED_FILTERS_MASK_NAMES = staticFields<DescriptorKindFilter>()
            .mapNotNull { field ->
                val filter = field.get(null) as? DescriptorKindFilter
                if (filter != null) MaskToName(filter.kindMask, field.name) else null
            }

        private inline fun <reified T : Any> staticFields() =
            T::class.java.fields.filter { Modifier.isStatic(it.modifiers) }

    }

    val kindMask: Int

    init {
        var mask = kindMask
        excludes.forEach { mask = mask and it.fullyExcludedDescriptorKinds.inv() }
        this.kindMask = mask
    }

    fun intersect(other: DescriptorKindFilter) =
        DescriptorKindFilter(kindMask and other.kindMask, excludes + other.excludes)

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
            is PropertyDescriptor -> PROPERTYS_MASK
            is VariableDescriptor -> VARIABLES_MASK
            else -> 0
        }
    }

    fun accepts(descriptor: DeclarationDescriptor): Boolean =
        kindMask and descriptor.kind() != 0 && excludes.all { !it.excludes(descriptor) }

    infix fun exclude(exclude: DescriptorKindExclude): DescriptorKindFilter =
        DescriptorKindFilter(kindMask, excludes + listOf(exclude))

    fun withKinds(kinds: Int): DescriptorKindFilter = DescriptorKindFilter(kindMask or kinds, excludes)
    override fun toString(): String {
        val predefinedFilterName = DEBUG_PREDEFINED_FILTERS_MASK_NAMES.firstOrNull { it.mask == kindMask }?.name
        val kindString = predefinedFilterName ?: DEBUG_MASK_BIT_NAMES
            .mapNotNull { if (acceptsKinds(it.mask)) it.name else null }
            .joinToString(separator = " | ")

        return "DescriptorKindFilter($kindString, $excludes)"
    }


}

fun CjFile.getScope(): FileScope {
    return FileScope(this)
}

class FileScope(val file: CjFile) : MemberScope {
    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> {
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
