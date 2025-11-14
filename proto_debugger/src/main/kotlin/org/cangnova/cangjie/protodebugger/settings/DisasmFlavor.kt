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

package org.cangnova.cangjie.protodebugger.settings

import java.util.Objects

/**
 * 反汇编语法风格类
 *
 * 该数据类表示反汇编器的语法风格，主要用于调试器中显示汇编代码时的格式选择。
 * 不同的语法风格会影响汇编指令的显示格式，特别是操作数的顺序和寄存器的表示方式。
 *
 * 使用场景：
 * - 配置调试器中汇编代码的显示风格
 * - 适应不同开发者的汇编语法偏好
 * - 支持多种汇编器生成的代码格式
 * - 提供一致的反汇编显示体验
 *
 * 主要功能：
 * - 定义Intel和AT&T两种主要的x86汇编语法
 * - 支持语法风格的识别和验证
 * - 提供语法风格之间的转换支持
 * - 封装语法风格的配置信息
 *
 * 语法风格说明：
 * - Intel语法：操作数在前，寄存器在后（如 mov eax, ebx）
 * - AT&T语法：寄存器在前，操作数在后（如 movl %ebx, %eax）
 *
 * @param name 语法风格的名称，可能为null
 */
data class DisasmFlavor(val name: String?) {
        companion object {
        /**
         * Intel语法风格
         *
         * Intel语法是最常见的x86汇编语法，特点是：
         * - 操作数在前，寄存器在后（如 mov eax, ebx）
         * - 寄存器不使用%前缀
         * - 立即数不使用$前缀
         * - 广泛用于Windows平台和Intel文档
         */
        val INTEL = DisasmFlavor("intel")

        /**
         * AT&T语法风格
         *
         * AT&T语法是Unix/Linux系统中常用的汇编语法，特点是：
         * - 寄存器在前，操作数在后（如 movl %ebx, %eax）
         * - 寄存器使用%前缀
         * - 立即数使用$前缀
         * - 广泛用于GCC、GDB等工具
         */
        val ATT = DisasmFlavor("att")

        /**
         * 检查是否为x86架构的反汇编语法风格
         *
         * 判断给定的语法风格是否为Intel或AT&T语法。
         * 这两种语法主要用于x86/x64架构的汇编代码。
         *
         * 使用场景：
         * - 验证用户选择的语法风格
         * - 过滤支持的语法风格
         * - 语法风格的兼容性检查
         *
         * @param flavor 要检查的语法风格，可能为null
         * @return 如果是Intel或AT&T语法返回true，否则返回false
         */
        fun isX86DisasmFlavor(flavor: DisasmFlavor?): Boolean {
            return INTEL == flavor || ATT == flavor
        }

        /**
         * 根据布尔值获取x86反汇编语法风格
         *
         * 根据是否使用Intel语法的布尔值返回对应的语法风格对象。
         * 这是一个便捷的转换方法，用于从配置或用户选择中获取语法风格。
         *
         * 使用场景：
         * - 从配置文件读取语法风格设置
         * - 根据用户偏好选择语法风格
         * - 提供默认的语法风格选择
         *
         * @param useIntelSyntax 是否使用Intel语法，true返回Intel，false返回AT&T
         * @return 对应的语法风格对象
         */
        fun getX86DisasmFlavor(useIntelSyntax: Boolean): DisasmFlavor {
            return if (useIntelSyntax) INTEL else ATT
        }
    }

    /**
     * 比较两个反汇编语法风格是否相等
     *
     * 两个语法风格相等需要它们的名称完全相同。
     *
     * @param other 要比较的对象
     * @return 如果语法风格相等返回true，否则返回false
     */
    override fun equals(other: Any?): Boolean {
        return other is DisasmFlavor && Objects.equals(name, other.name)
    }

    /**
     * 返回语法风格的字符串表示
     *
     * @return 语法风格的名称
     */
    override fun toString(): String {
        return name.toString()
    }

    /**
     * 计算语法风格的哈希码
     *
     * @return 基于名称的哈希码
     */
    override fun hashCode(): Int {
        return name?.hashCode() ?: 0
    }
    }
