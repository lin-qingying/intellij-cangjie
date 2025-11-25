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

package org.cangnova.cangjie.protodebugger.memory

/**
 * 内存相关扩展函数和工具
 */

/**
 * 将地址转换为单地址范围
 */
fun Address.toRange(): AddressRange = this.rangeTo(this)

/**
 * 从Long创建地址
 */
fun Long.toAddress(): Address = Address.Companion.Factory.fromLong(this)

/**
 * 从ULong创建地址
 */
fun ULong.toAddress(): Address = Address(this)

/**
 * 将字节数组转换为十六进制字符串
 */
fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

/**
 * 将字节转换为无符号整数
 */
fun Byte.toUnsignedInt(): Int = this.toInt() and 0xFF

/**
 * 格式化地址为固定宽度的十六进制字符串
 *
 * @param width 输出的字符宽度（不包括0x前缀）
 * @return 格式化的地址字符串
 */
fun Address.formatHex(width: Int = 16): String {
    val hex = value.toString(16)
    return "0x" + hex.padStart(width, '0')
}

/**
 * 对齐地址到指定边界
 *
 * @param alignment 对齐边界（必须是2的幂）
 * @return 对齐后的地址
 */
fun Address.alignDown(alignment: ULong): Address {
    return this.alignDown(alignment)
}

/**
 * 向上对齐地址到指定边界
 *
 * @param alignment 对齐边界（必须是2的幂）
 * @return 对齐后的地址
 */
fun Address.alignUp(alignment: ULong): Address {
    return this.alignUp(alignment)
}

/**
 * 检查地址是否对齐到指定边界
 *
 * @param alignment 对齐边界
 * @return 如果对齐返回true
 */
fun Address.isAligned(alignment: ULong): Boolean {
    return this.isAlignedTo(alignment)
}

/**
 * 扩展地址范围
 *
 * @param factor 扩展因子（例如0.5表示前后各扩展50%）
 * @return 扩展后的范围
 */
fun AddressRange.expand(factor: Double): AddressRange {
    val expandSize = (size.toDouble() * factor).toULong()
    val newStart = if (start.value >= expandSize) {
        start - expandSize.toLong()
    } else {
        Address.MIN_VALUE
    }
    val newEnd = start + (size.toLong() + expandSize.toLong() * 2)
    return newStart.rangeTo(newEnd)
}