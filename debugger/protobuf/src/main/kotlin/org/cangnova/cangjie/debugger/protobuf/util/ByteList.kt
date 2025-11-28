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

package org.cangnova.cangjie.debugger.protobuf.util

/**
 * 字节列表类
 *
 * 该类是对ByteArray的List接口封装，提供了不可变的字节列表操作。
 * 它继承自AbstractList，支持标准的集合操作，同时保持对底层字节数组的直接访问。
 *
 * 使用场景：
 * - 调试器中的指令字节序列表示
 * - 内存数据的字节级操作
 * - 网络通信和协议处理
 * - 二进制数据的结构化处理
 *
 * 主要功能：
 * - 提供List接口的字节序列操作
 * - 支持从Iterable创建ByteList
 * - 提供高效的字节访问和子列表操作
 * - 保持与原生ByteArray的兼容性
 *
 * 技术特点：
 * - 不可变的字节列表实现
 * - 高效的内存使用
 * - 支持标准的集合操作
 * - 与Kotlin集合框架完全兼容
 *
 * @paramByteArray 底层的字节数组
 */
class ByteList(val byteArray: ByteArray) :
    AbstractList<Byte>() {
    companion object {
        /**
         * 从Iterable创建ByteList
         *
         * 将任意的字节迭代器转换为ByteList实例。
         * 这是一个便捷的工厂方法，用于从各种字节源创建ByteList。
         *
         * 使用场景：
         * - 从集合或流创建字节列表
         * - 转换动态生成的字节序列
         * - 协议数据的封装和传输
         *
         * @param byteList 字节的迭代器
         * @return 新的ByteList实例
         */
        fun fromIterable(byteList: Iterable<Byte>): ByteList {
            return ByteList(byteList.toList().toByteArray())
        }
    }

    /**
     * 获取列表大小
     *
     * @return 字节列表的长度
     */
    override val size: Int = byteArray.size

    /**
     * 比较两个ByteList是否相等
     *
     * 两个ByteList相等当且仅当它们的字节数组内容完全相同。
     *
     * @param other 要比较的对象
     * @return 如果内容相同返回true，否则返回false
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ByteList) return false

        return byteArray.contentEquals(other.byteArray)
    }

    /**
     * 获取指定索引处的字节
     *
     * @param index 字节的索引位置
     * @return 指定位置的字节值
     */
    override operator fun get(index: Int): Byte = byteArray[index]

    /**
     * 计算ByteList的哈希码
     *
     * 使用类似Java ArrayList的哈希算法，基于所有字节的内容计算。
     *
     * @return ByteList的哈希码
     */
    override fun hashCode(): Int =
        byteArray.fold(1) { accumulator, element ->
            accumulator * 31 + element.hashCode()
        }

    /**
     * 获取子列表
     *
     * 返回指定范围内的字节子列表，创建新的ByteList实例。
     *
     * @param fromIndex 起始索引（包含）
     * @param toIndex 结束索引（不包含）
     * @return 新的ByteList子列表
     */
    override fun subList(
        fromIndex: Int,
        toIndex: Int
    ): ByteList = ByteList(byteArray.sliceArray(fromIndex.until(toIndex)))
}

