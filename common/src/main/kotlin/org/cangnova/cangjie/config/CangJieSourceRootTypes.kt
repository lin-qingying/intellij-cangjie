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

package org.cangnova.cangjie.config

import org.jetbrains.jps.model.module.JpsModuleSourceRootType

/**
 * 仓颉语言源根类型常量
 *
 * 定义了仓颉项目中使用的各种源根类型标识符。
 * 这些常量用于工作空间模型中的 SourceRootEntity.rootTypeId。
 *
 * ## 使用场景
 * - 创建 SourceRootEntity 时指定 rootTypeId
 * - 从 SourceRootEntity 判断源根类型
 * - 区分生产代码和测试代码
 * - 从 typeId 获取对应的 JpsModuleSourceRootType
 *
 * ## 类型说明
 * - SOURCE: 生产源代码目录
 * - TEST: 测试源代码目录
 * - RESOURCE: 生产资源目录
 * - TEST_RESOURCE: 测试资源目录
 */
object CangJieSourceRootTypes {
    /** 仓颉生产源代码根类型 */
    const val SOURCE = "cangjie-source"

    /** 仓颉测试源代码根类型 */
    const val TEST = "cangjie-test"

    /** 仓颉生产资源根类型 */
    const val RESOURCE = "cangjie-resource"

    /** 仓颉测试资源根类型 */
    const val TEST_RESOURCE = "cangjie-test-resource"

    /** 所有仓颉源代码根类型（不含资源） */
    val ALL_SOURCE_TYPES = setOf(SOURCE, TEST)

    /** 所有仓颉资源根类型 */
    val ALL_RESOURCE_TYPES = setOf(RESOURCE, TEST_RESOURCE)

    /** 所有仓颉根类型 */
    val ALL_TYPES = ALL_SOURCE_TYPES + ALL_RESOURCE_TYPES

    /** 所有测试相关的根类型 */
    val TEST_TYPES = setOf(TEST, TEST_RESOURCE)

    /** 所有生产相关的根类型 */
    val PRODUCTION_TYPES = setOf(SOURCE, RESOURCE)

    /**
     * typeId 到 JpsModuleSourceRootType 的映射
     */
    private val typeById: Map<String, JpsModuleSourceRootType<*>> = mapOf(
        SOURCE to CangJieSourceRootType,
        TEST to CangJieTestSourceRootType,
        RESOURCE to CangJieResourceRootType,
        TEST_RESOURCE to CangJieTestResourceRootType
    )

    /**
     * JpsModuleSourceRootType 到 typeId 的映射
     */
    private val idByType: Map<JpsModuleSourceRootType<*>, String> = typeById.entries.associate { it.value to it.key }

    /**
     * 根据 typeId 获取对应的 JpsModuleSourceRootType
     *
     * @param typeId 源根类型标识符
     * @return 对应的 JpsModuleSourceRootType，如果找不到则返回 null
     */
    fun findTypeById(typeId: String?): JpsModuleSourceRootType<*>? = typeId?.let { typeById[it] }

    /**
     * 根据 JpsModuleSourceRootType 获取对应的 typeId
     *
     * @param type JpsModuleSourceRootType 实例
     * @return 对应的 typeId，如果找不到则返回 null
     */
    fun findIdByType(type: JpsModuleSourceRootType<*>?): String? = type?.let { idByType[it] }

    /**
     * 判断是否为测试源代码类型
     */
    fun isTestSource(rootTypeId: String?): Boolean = rootTypeId == TEST

    /**
     * 判断是否为生产源代码类型
     */
    fun isProductionSource(rootTypeId: String?): Boolean = rootTypeId == SOURCE

    /**
     * 判断是否为仓颉源代码类型（生产或测试）
     */
    fun isSourceType(rootTypeId: String?): Boolean = rootTypeId in ALL_SOURCE_TYPES

    /**
     * 判断是否为仓颉资源类型
     */
    fun isResourceType(rootTypeId: String?): Boolean = rootTypeId in ALL_RESOURCE_TYPES

    /**
     * 判断是否为仓颉相关的根类型
     */
    fun isCangJieType(rootTypeId: String?): Boolean = rootTypeId in ALL_TYPES

    /**
     * 判断是否为测试相关类型（测试源代码或测试资源）
     */
    fun isTestType(rootTypeId: String?): Boolean = rootTypeId in TEST_TYPES

    /**
     * 判断是否为生产相关类型（生产源代码或生产资源）
     */
    fun isProductionType(rootTypeId: String?): Boolean = rootTypeId in PRODUCTION_TYPES

    /**
     * 根据是否为测试获取对应的源代码根类型
     *
     * @param forTests 是否为测试代码
     * @return 对应的源代码根类型标识符
     */
    fun getSourceRootTypeId(forTests: Boolean): String = if (forTests) TEST else SOURCE

    /**
     * 根据是否为测试获取对应的资源根类型
     *
     * @param forTests 是否为测试资源
     * @return 对应的资源根类型标识符
     */
    fun getResourceRootTypeId(forTests: Boolean): String = if (forTests) TEST_RESOURCE else RESOURCE

    /**
     * 根据是否为测试获取对应的源代码 JpsModuleSourceRootType
     *
     * @param forTests 是否为测试代码
     * @return 对应的 JpsModuleSourceRootType
     */
    fun getSourceRootType(forTests: Boolean): JpsModuleSourceRootType<*> =
        if (forTests) CangJieTestSourceRootType else CangJieSourceRootType

    /**
     * 根据是否为测试获取对应的资源 JpsModuleSourceRootType
     *
     * @param forTests 是否为测试资源
     * @return 对应的 JpsModuleSourceRootType
     */
    fun getResourceRootType(forTests: Boolean): JpsModuleSourceRootType<*> =
        if (forTests) CangJieTestResourceRootType else CangJieResourceRootType
}
