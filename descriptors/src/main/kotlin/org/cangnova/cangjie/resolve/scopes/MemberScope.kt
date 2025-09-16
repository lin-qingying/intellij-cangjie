/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.resolve.scopes

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.MemberScope.Companion.ALL_NAME_FILTER
import org.cangnova.cangjie.utils.Printer
import org.cangnova.cangjie.utils.flatMapToNullable
import java.lang.reflect.Modifier

fun MemberScope.computeAllNames() = classifierNames?.let { classifierNames ->
    functionNames.toMutableSet().also {
        it.addAll(variableNames)
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
    flatMapToNullable(hashSetOf(), MemberScope::classifierNames)


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
    val functionNames: Set<Name>
    val variableNames: Set<Name>
    val classifierNames: Set<Name>?
    val propertyNames: Set<Name>

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
        override val propertyNames: Set<Name>
            get() = emptySet<Name>()
        override val functionNames: Set<Name>
            get() = emptySet<Name>()

        override val classifierNames: Set<Name>?
            get() = emptySet()
        override val variableNames = emptySet<Name>()

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
