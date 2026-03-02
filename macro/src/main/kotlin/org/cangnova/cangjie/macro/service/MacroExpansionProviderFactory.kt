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

package org.cangnova.cangjie.macro.service

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

/**
 * 宏展开提供者工厂扩展点接口
 *
 * 通过 IntelliJ 扩展点机制注册宏展开引擎，支持动态注册任意数量的引擎。
 * 每个实现对应一种宏展开引擎（如 `cjc-frontend` 或 `LSPMacroServer`）。
 *
 * ## 注册方式
 *
 * 在 plugin.xml（或所属模块的 XML 配置）中注册：
 *
 * ```xml
 * <extensions defaultExtensionNs="org.cangnova.cangjie">
 *     <macroExpansionProviderFactory
 *         implementation="com.example.MyMacroProviderFactory"/>
 * </extensions>
 * ```
 *
 * ## 设计原则
 *
 * - 工厂实例在**应用级**持有（无状态），每次 [createProvider] 返回**项目级**实例
 * - 实现类必须有无参构造函数（IntelliJ 扩展点要求）
 * - [engineId] 必须全局唯一，用于持久化用户的引擎选择
 * - 标记 [isDefault] = `true` 的工厂作为无配置时的回退引擎；
 *   如果多个工厂都标记了 default，则选择注册顺序中第一个可用的
 *
 * ## 内置实现
 *
 * - [org.cangnova.cangjie.macro.compiler.CompilerMacroExpansionProviderFactory]
 *   — `cjc-frontend --debug-macro`，默认引擎（isDefault = true）
 * - [org.cangnova.cangjie.macro.server.LspMacroServerProviderFactory]
 *   — `LSPMacroServer`，高性能常驻进程引擎
 */
interface MacroExpansionProviderFactory {

    companion object {
        val EP_NAME: ExtensionPointName<MacroExpansionProviderFactory> =
            ExtensionPointName.create("org.cangnova.cangjie.macroExpansionProviderFactory")
    }

    /**
     * 引擎唯一标识符
     *
     * 用于：
     * - 持久化用户选择的引擎（存储在 [org.cangnova.cangjie.macro.service.MacroExpansionSettings]）
     * - 日志和诊断信息
     * - 引擎切换时的查找
     *
     * 应与对应 [org.cangnova.cangjie.macro.engine.MacroExpansionEngine.id] 一致。
     */
    val engineId: String

    /**
     * 是否为默认引擎
     *
     * 当用户未配置引擎偏好，或所配置的引擎不可用时，优先选用标记了 `isDefault = true` 的工厂。
     * 通常只有 `cjc-frontend` 工厂应标记为 `true`。
     */
    val isDefault: Boolean get() = false

    /**
     * 创建项目级提供者实例
     *
     * 每个项目调用一次，返回的实例在项目生命周期内被 [org.cangnova.cangjie.macro.service.MacroExpansionServiceImpl] 缓存。
     * 实现可持有项目级状态（如进程句柄、连接池等）。
     *
     * @param project 目标项目
     * @return 对应该项目的 [org.cangnova.cangjie.macro.service.MacroExpansionProvider] 实例
     */
    fun createProvider(project: Project): MacroExpansionProvider
}