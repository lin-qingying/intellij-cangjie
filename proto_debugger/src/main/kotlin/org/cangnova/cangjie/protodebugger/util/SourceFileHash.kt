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

package org.cangnova.cangjie.protodebugger.util
/**
 * 调试器源文件哈希类
 *
 * 该类封装了源文件的哈希值和哈希算法类型，用于验证源文件的完整性和一致性。
 * 在调试过程中，通过比较源文件的哈希值来确保调试信息与源代码的匹配。
 *
 * 使用场景：
 * - 验证调试器中的源文件与实际文件的一致性
 * - 检测源文件的修改和更新
 * - 支持源码级别的调试和断点设置
 * - 确保调试信息的准确性
 *
 * 主要功能：
 * - 支持多种哈希算法（MD5、SHA1、SHA256）
 * - 提供源文件完整性的验证机制
 * - 支持哈希值的比较和匹配
 * - 封装哈希算法和值的组合
 *
 * @param type 哈希算法类型
 * @param hash 哈希值的字符串表示
 */
class SourceFileHash(
    /**
     * 哈希算法类型
     *
     * 指定用于计算哈希值的算法类型。
     */
    val type: Type,

    /**
     * 哈希值
     *
     * 源文件计算得到的哈希值的十六进制字符串表示。
     */
    val hash: String
) {
    /**
     * 哈希算法类型枚举
     *
     * 定义了支持的哈希算法类型，不同的算法提供不同的安全性和性能特征。
     */
    enum class Type {
        /**
         * MD5哈希算法
         *
         * 128位哈希值，计算速度快，但安全性相对较低。
         * 适用于一般性的文件完整性检查。
         */
        MD5,

        /**
         * SHA1哈希算法
         *
         * 160位哈希值，安全性比MD5高，计算速度适中。
         * 在调试器中是常用的哈希算法选择。
         */
        SHA1,

        /**
         * SHA256哈希算法
         *
         * 256位哈希值，安全性最高，但计算相对较慢。
         * 适用于对安全性要求较高的场景。
         */
        SHA256
    }

    /**
     * 返回哈希信息的字符串表示
     *
     * 格式为"算法类型: 哈希值"，便于日志记录和调试显示。
     *
     * @return 格式化的哈希信息字符串
     */
    override fun toString(): String {
        return String.format("%s: %s", type, hash)
    }

    /**
     * 比较两个源文件哈希是否相等
     *
     * 两个哈希相等需要算法类型和哈希值都完全相同。
     *
     * @param other 要比较的对象
     * @return 如果哈希相等返回true，否则返回false
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SourceFileHash) return false

        if (type != other.type) return false
        return hash == other.hash
    }

    /**
     * 计算哈希的哈希码
     *
     * 基于算法类型和哈希值计算哈希码。
     *
     * @return 哈希的哈希码
     */
    override fun hashCode(): Int {
        return 17 * type.hashCode() + hash.hashCode()
    }
}
