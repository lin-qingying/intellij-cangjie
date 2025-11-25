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
 */

package org.cangnova.cangjie.protodebugger.memory.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorGutterComponentEx
import org.cangnova.cangjie.protodebugger.memory.state.MemoryStore

/**
 * 基础设置配置器
 *
 * 配置编辑器的基本显示选项，适用于内存视图场景。
 */
class BasicEditorSettingsConfigurator : EditorConfigurator {
    override fun configure(editor: Editor) {
        editor.settings.apply {
            isUseSoftWraps = false
            isRightMarginShown = false
        }
    }
}

/**
 * 地址行号转换配置器
 *
 * 将编辑器左侧的行号替换为内存地址显示。
 *
 * @property facade 内存视图外观，提供地址映射功能
 */
class AddressLineNumberConfigurator(
    private val store: MemoryStore<*>,
) : EditorConfigurator {
    override fun configure(editor: Editor) {
        val gutter = editor.gutter as? EditorGutterComponentEx ?: return
        gutter.setLineNumberConverter(MemoryAddressLineNumberConverter(store))
    }
}

/**
 * 内存Inlay提示配置器
 *
 * 添加内联提示，显示内存值的解释信息（如指针解引用、字符串预览等）。
 */
class MemoryInlayConfigurator : EditorConfigurator {
    override fun configure(editor: Editor) {
        // TODO: 实现Inlay提示功能
        // 这是新增功能，原实现中没有
    }
}

/**
 * 编辑器外观定制配置器
 *
 * 定制编辑器的颜色方案和视觉效果。
 */
class MemoryEditorAppearanceConfigurator : EditorConfigurator {
    override fun configure(editor: Editor) {
        // TODO: 自定义配色、字体等
        // 例如：使用等宽字体、高亮当前地址等
    }
}

/**
 * 快捷键绑定配置器
 *
 * 为内存视图编辑器绑定专用快捷键。
 */
class MemoryEditorKeybindingConfigurator : EditorConfigurator {
    override fun configure(editor: Editor) {
        // TODO: 注册快捷键
        // 例如：Ctrl+G跳转到地址、Ctrl+F查找字节序列等
    }
}

/**
 * 编辑器只读区域配置器
 *
 * 标记不可编辑的区域（如地址列、分隔符等）。
 */
class ReadOnlyRegionConfigurator : EditorConfigurator {
    override fun configure(editor: Editor) {
        // TODO: 设置只读区域
        // 使用 GuardedBlocks 保护地址和分隔符
    }
}