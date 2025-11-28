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

package org.cangnova.cangjie.debugger.protobuf.memory.editor

import com.intellij.openapi.editor.Editor

/**
 * 编辑器配置器接口
 *
 * 定义单一职责的编辑器配置行为。
 * 每个配置器专注于一个特定的编辑器增强功能。
 */
interface EditorConfigurator {
    /**
     * 应用配置到编辑器
     *
     * @param editor 待配置的编辑器实例
     */
    fun configure(editor: Editor)
}

/**
 * 编辑器配置管道
 *
 * 使用装饰器链模式组织多个编辑器配置器。
 * 提供流式API按顺序应用配置。
 *
 * 使用示例：
 * ```kotlin
 * EditorPipeline()
 *     .with(BasicSettingsConfigurator())
 *     .with(AddressLineNumberConfigurator(facade))
 *     .with(MemoryInlayConfigurator())
 *     .applyTo(editor)
 * ```
 */
class EditorPipeline {
    private val configurators = mutableListOf<EditorConfigurator>()

    /**
     * 添加配置器到管道
     *
     * @param configurator 要添加的配置器
     * @return 当前管道实例，支持链式调用
     */
    fun with(configurator: EditorConfigurator): EditorPipeline {
        configurators.add(configurator)
        return this
    }

    /**
     * 将所有配置器应用到编辑器
     *
     * 按添加顺序依次执行每个配置器。
     *
     * @param editor 目标编辑器
     */
    fun applyTo(editor: Editor) {
        configurators.forEach { it.configure(editor) }
    }

    /**
     * 清空管道中的所有配置器
     */
    fun clear() {
        configurators.clear()
    }

    /**
     * 获取配置器数量
     */
    val size: Int get() = configurators.size
}

/**
 * 编辑器配置器构建器
 *
 * 提供DSL风格的配置器创建方式。
 */
object EditorConfiguratorBuilder {
    /**
     * 创建Lambda配置器
     *
     * 允许直接使用lambda表达式定义配置逻辑，
     * 适用于简单的一次性配置。
     */
    fun from(block: (Editor) -> Unit): EditorConfigurator {
        return object : EditorConfigurator {
            override fun configure(editor: Editor) {
                block(editor)
            }
        }
    }
}