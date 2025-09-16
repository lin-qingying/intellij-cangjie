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

package org.cangnova.cangjie.descriptors.annotations

import org.cangnova.cangjie.name.FqName

/**
 * 可注解接口，表示可以被注解修饰的声明
 * 提供默认的注解集合实现（空注解）
 */
interface Annotated {
    /**
     * 获取该声明的注解集合
     * 默认返回空注解集合
     */
    val annotations: Annotations get() = Annotations.EMPTY
}

/**
 * 组合注解类，将多个注解集合组合成一个统一的注解视图
 * 用于处理继承、组合或其他需要合并注解的场景
 */
class CompositeAnnotations(
    private val delegates: List<Annotations>
) : Annotations {
    constructor(vararg delegates: Annotations) : this(delegates.toList())

    override fun isEmpty() = delegates.all { it.isEmpty() }

    override fun hasAnnotation(fqName: FqName) = delegates.asSequence().any { it.hasAnnotation(fqName) }

    override fun findAnnotation(fqName: FqName) =
        delegates.asSequence().mapNotNull { it.findAnnotation(fqName) }.firstOrNull()

    @Suppress("DEPRECATION", "OverridingDeprecatedMember", "OVERRIDE_DEPRECATION")
    override fun getUseSiteTargetedAnnotations() = delegates.flatMap { it.getUseSiteTargetedAnnotations() }

    override fun iterator() = delegates.asSequence().flatMap { it.asSequence() }.iterator()
}


/**
 * 基于谓词过滤的注解类
 * 根据指定的过滤条件筛选注解集合
 */
class FilteredByPredicateAnnotations(
    private val delegate: Annotations,
    private val filter: (AnnotationDescriptor) -> Boolean
) : Annotations {
    override fun isEmpty(): Boolean {
        return !iterator().hasNext()
    }

    override fun iterator(): Iterator<AnnotationDescriptor> {
        return delegate.filter(filter).iterator()
    }

    override fun findAnnotation(fqName: FqName): AnnotationDescriptor? {
        return super.findAnnotation(fqName)?.takeIf(filter)
    }
}

/**
 * 注解集合接口，表示一组注解描述符
 * 提供了注解的查找、判断和遍历功能
 */
interface Annotations : Iterable<AnnotationDescriptor> {
    fun isEmpty(): Boolean

    fun findAnnotation(fqName: FqName): AnnotationDescriptor? = firstOrNull { it.fqName == fqName }

    fun hasAnnotation(fqName: FqName): Boolean = findAnnotation(fqName) != null

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("This method should only be used in frontend where we split annotations according to their use-site targets.")
    fun getUseSiteTargetedAnnotations(): List<AnnotationWithTarget> = emptyList()

    companion object {
        /**
         * 空的注解集合实例
         */
        @JvmField
        val EMPTY: Annotations = object : Annotations {
            override fun isEmpty() = true

            override fun findAnnotation(fqName: FqName) = null

            override fun iterator() = emptyList<AnnotationDescriptor>().iterator()

            override fun toString() = "EMPTY"
        }

        /**
         * 创建注解集合的工厂方法
         * @param annotations 注解描述符列表
         * @return 如果列表为空返回EMPTY，否则创建新的AnnotationsImpl实例
         */
        fun create(annotations: List<AnnotationDescriptor>): Annotations =
            if (annotations.isEmpty()) EMPTY else AnnotationsImpl(annotations)
    }
}

/**
 * 基于完全限定名过滤的注解类
 * 根据注解的完全限定名进行过滤
 * @param isDefinitelyNewInference 是否使用新的类型推断逻辑
 */
class FilteredAnnotations(
    private val delegate: Annotations,
    private val isDefinitelyNewInference: Boolean,
    private val fqNameFilter: (FqName) -> Boolean
) : Annotations {

    constructor(delegate: Annotations, fqNameFilter: (FqName) -> Boolean) : this(delegate, false, fqNameFilter)

    override fun hasAnnotation(fqName: FqName) =
        if (fqNameFilter(fqName)) delegate.hasAnnotation(fqName)
        else false

    override fun findAnnotation(fqName: FqName) =
        if (fqNameFilter(fqName)) delegate.findAnnotation(fqName)
        else null

    override fun iterator() = delegate.filter(this::shouldBeReturned).iterator()

    override fun isEmpty(): Boolean {
        val condition = delegate.any(this::shouldBeReturned)

        return if (isDefinitelyNewInference) !condition else condition
    }

    private fun shouldBeReturned(annotation: AnnotationDescriptor): Boolean =
        annotation.fqName.let { fqName ->
            fqName != null && fqNameFilter(fqName)
        }
}

/**
 * 组合两个注解集合
 * 如果其中一个为空，则返回另一个
 * 如果两个都非空，则创建组合注解
 */
fun composeAnnotations(first: Annotations, second: Annotations) =
    when {
        first.isEmpty() -> second
        second.isEmpty() -> first
        else -> CompositeAnnotations(first, second)
    }
