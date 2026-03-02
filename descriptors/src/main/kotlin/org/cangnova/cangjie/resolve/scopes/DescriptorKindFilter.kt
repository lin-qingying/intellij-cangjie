/*
 * Copyright 2026 LinQingYing. and contributors.
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

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.PackageFragmentDescriptor
import org.cangnova.cangjie.descriptors.PackageViewDescriptor
import org.cangnova.cangjie.descriptors.PropertyDescriptor
import org.cangnova.cangjie.descriptors.TypeAliasDescriptor
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.descriptors.extend.ExtendDescriptor
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import java.lang.reflect.Modifier

/**
 * 描述符类型过滤器
 * 用于根据类型掩码和排除规则过滤描述符
 */
class DescriptorKindFilter(
    kindMask: Int,
    val excludes: List<DescriptorKindExclude> = listOf()
) {
    companion object {
        // 掩码到名称的映射类
        private class MaskToName(val mask: Int, val name: String)


        // 用于生成下一个掩码值的变量和函数
        private var nextMaskValue: Int = 0x01
        private fun nextMask() = nextMaskValue.apply { nextMaskValue = nextMaskValue shl 1 }

        // 各种描述符类型的掩码常量
        val CLASSES_MASK: Int = nextMask()  // 类和分类器掩码


        val TYPE_ALIASES_MASK: Int = nextMask()               // 类型别名掩码
        val PACKAGES_MASK: Int = nextMask()                   // 包掩码
        val MODULES_MASK: Int = nextMask()                    // 模块掩码
        val FUNCTIONS_MASK: Int = nextMask()                  // 函数掩码
        val MACROS_MASK: Int = nextMask()                      // 宏掩码
        val VARIABLES_MASK: Int = nextMask()                  // 变量掩码
        val PROPERTYS_MASK: Int = nextMask()                  // 属性掩码
        val EXTENDS_MASK: Int = nextMask()                    // 扩展掩码

        val REEXPORT_MASK: Int = nextMask()                   // 重导出掩码

        // 组合掩码常量
        val ALL_KINDS_MASK: Int = nextMask() - 1              // 所有类型掩码
        val CLASSIFIERS_MASK: Int =
            CLASSES_MASK or TYPE_ALIASES_MASK  // 分类器掩码（包括类和类型别名）

        val VALUES_MASK: Int   =FUNCTIONS_MASK or VARIABLES_MASK or PROPERTYS_MASK  // 值掩码
        val CALLABLES_MASK: Int = FUNCTIONS_MASK or VARIABLES_MASK or PROPERTYS_MASK  // 可调用对象掩码

        // 预定义的过滤器实例
        @JvmField
        val ALL: DescriptorKindFilter = DescriptorKindFilter(ALL_KINDS_MASK)  // 所有类型过滤器

        @JvmField
        val CALLABLES: DescriptorKindFilter = DescriptorKindFilter(CALLABLES_MASK)  // 可调用对象过滤器


        @JvmField
        val TYPE_ALIASES: DescriptorKindFilter = DescriptorKindFilter(TYPE_ALIASES_MASK)  // 类型别名过滤器

        @JvmField
        val CLASSIFIERS: DescriptorKindFilter = DescriptorKindFilter(CLASSIFIERS_MASK)  // 分类器过滤器（包括类和类型别名）

        @JvmField
        val PACKAGES: DescriptorKindFilter = DescriptorKindFilter(PACKAGES_MASK)  // 包过滤器

        @JvmField
        val MODULES: DescriptorKindFilter = DescriptorKindFilter(MODULES_MASK)  // 模块过滤器

        @JvmField
        val REEXPORT: DescriptorKindFilter = DescriptorKindFilter(REEXPORT_MASK)  // 重导出过滤器

        @JvmField
        val FUNCTIONS: DescriptorKindFilter = DescriptorKindFilter(FUNCTIONS_MASK)  // 函数过滤器
        @JvmField
        val MACROS: DescriptorKindFilter = DescriptorKindFilter(MACROS_MASK)  // 函数过滤器

        @JvmField
        val VARIABLES: DescriptorKindFilter = DescriptorKindFilter(VARIABLES_MASK)  // 变量过滤器

        @JvmField
        val PROPERTYS: DescriptorKindFilter = DescriptorKindFilter(PROPERTYS_MASK)  // 属性过滤器

        @JvmField
        val EXTENDS: DescriptorKindFilter = DescriptorKindFilter(EXTENDS_MASK)  // 扩展过滤器

        @JvmField
        val VALUES: DescriptorKindFilter = DescriptorKindFilter(VALUES_MASK)  // 值过滤器


        // 调试用的掩码位名称映射
        private val DEBUG_MASK_BIT_NAMES = staticFields<DescriptorKindFilter>()
            .filter { it.type == Integer.TYPE }
            .mapNotNull { field ->
                val mask = field.get(null) as Int
                val isOneBitMask = mask == (mask and (-mask))
                if (isOneBitMask) MaskToName(mask, field.name) else null
            }

        // 调试用的预定义过滤器掩码名称映射
        private val DEBUG_PREDEFINED_FILTERS_MASK_NAMES = staticFields<DescriptorKindFilter>()
            .mapNotNull { field ->
                val filter = field.get(null) as? DescriptorKindFilter
                if (filter != null) MaskToName(filter.kindMask, field.name) else null
            }

        // 获取静态字段的内联函数
        private inline fun <reified T : Any> staticFields() =
            T::class.java.fields.filter { Modifier.isStatic(it.modifiers) }

    }

    val kindMask: Int  // 类型掩码

    init {
        // 应用排除规则，从掩码中移除被完全排除的类型
        var mask = kindMask
        excludes.forEach { mask = mask and it.fullyExcludedDescriptorKinds.inv() }
        this.kindMask = mask
    }

    /**
     * 与另一个过滤器求交集
     */
    fun intersect(other: DescriptorKindFilter) =
        DescriptorKindFilter(kindMask and other.kindMask, excludes + other.excludes)

    /**
     * 判断是否接受指定类型
     */
    fun acceptsKinds(kinds: Int): Boolean = kindMask and kinds != 0

    /**
     * 创建排除指定类型的新过滤器
     */
    fun withoutKinds(kinds: Int): DescriptorKindFilter = DescriptorKindFilter(kindMask and kinds.inv(), excludes)

    /**
     * 创建仅限于指定类型的过滤器，如果没有匹配的类型则返回 null
     */
    fun restrictedToKindsOrNull(kinds: Int): DescriptorKindFilter? {
        val mask = kindMask and kinds
        if (mask == 0) return null
        return DescriptorKindFilter(mask, excludes)
    }

    /**
     * 获取声明描述符对应的类型掩码
     */
    private fun DeclarationDescriptor.kind(): Int {
        return when (this) {
            is ClassDescriptor,is ClassifierDescriptor ->   CLASSES_MASK
            is TypeAliasDescriptor -> TYPE_ALIASES_MASK
            is PackageFragmentDescriptor, is PackageViewDescriptor -> PACKAGES_MASK
            is ModuleDescriptor -> MODULES_MASK
            is MacroDescriptor -> MACROS_MASK
            is FunctionDescriptor -> FUNCTIONS_MASK
            is PropertyDescriptor -> PROPERTYS_MASK
            is VariableDescriptor -> VARIABLES_MASK
            is ExtendDescriptor -> EXTENDS_MASK

            else -> 0
        }
    }

    /**
     * 判断是否接受指定的声明描述符
     */
    fun accepts(descriptor: DeclarationDescriptor): Boolean =
        kindMask and descriptor.kind() != 0 && excludes.all { !it.excludes(descriptor) }

    /**
     * 添加排除规则并返回新的过滤器
     */
    infix fun exclude(exclude: DescriptorKindExclude): DescriptorKindFilter =
        DescriptorKindFilter(kindMask, excludes + listOf(exclude))

    /**
     * 添加指定类型并返回新的过滤器
     */
    fun withKinds(kinds: Int): DescriptorKindFilter = DescriptorKindFilter(kindMask or kinds, excludes)
    override fun toString(): String {
        val predefinedFilterName = DEBUG_PREDEFINED_FILTERS_MASK_NAMES.firstOrNull { it.mask == kindMask }?.name
        val kindString = predefinedFilterName ?: DEBUG_MASK_BIT_NAMES
            .mapNotNull { if (acceptsKinds(it.mask)) it.name else null }
            .joinToString(separator = " | ")

        return "DescriptorKindFilter($kindString, $excludes)"
    }


}

/**
 * 描述符类型排除抽象类
 * 用于定义哪些类型的描述符应该被排除
 */
abstract class DescriptorKindExclude {
    /**
     * 判断指定的描述符是否应该被排除
     */
    abstract fun excludes(descriptor: DeclarationDescriptor): Boolean

    /**
     * 排除扩展函数的对象
     */
    object Extensions : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor) =
            descriptor is CallableDescriptor

        override val fullyExcludedDescriptorKinds: Int get() = 0
    }

    override fun toString() = this::class.java.simpleName

    /**
     * 排除非扩展函数的对象
     */
    object NonExtensions : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor) =
            descriptor !is CallableDescriptor

        override val fullyExcludedDescriptorKinds =
            DescriptorKindFilter.ALL_KINDS_MASK and (DescriptorKindFilter.FUNCTIONS_MASK or DescriptorKindFilter.VARIABLES_MASK).inv()
    }

    /**
     * 排除模块对象
     */
    object Module : DescriptorKindExclude() {
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



    /**
     * 被此 [DescriptorKindExclude] 完全排除的描述符类型的位掩码
     * 即，[excludes] 对这些类型的所有描述符都返回 true
     * Bit-mask of descriptor kind's that are fully excluded by this [DescriptorKindExclude].
     * That is, [excludes] returns true for all descriptor of these kinds.
     */
    abstract val fullyExcludedDescriptorKinds: Int
}

