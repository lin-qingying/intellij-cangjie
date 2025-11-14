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

import com.intellij.util.system.CpuArch

/**
 * 系统架构类型枚举
 *
 * 该枚举定义了调试器支持的各种CPU架构类型，包括x86、ARM、MIPS等主流架构。
 * 每个架构类型都包含了位数、标识符前缀、ELF机器码和PE机器码等信息。
 *
 * 使用场景：
 * - 调试器识别目标程序的CPU架构
 * - 选择合适的调试器和调试工具
 * - 处理不同架构的二进制文件
 * - 配置架构特定的调试选项
 *
 * 主要功能：
 * - 架构识别和分类
 * - 支持多种二进制文件格式（ELF、PE）
 * - 提供架构标识符和机器码映射
 * - 支持架构的自动检测
 *
 * 技术特点：
 * - 支持32位和64位架构
 * - 兼容多种操作系统和文件格式
 * - 提供灵活的架构匹配算法
 * - 支持架构别名的识别
 *
 * @param myBits 架构的位数（32或64）
 * @param myIdPrefixArray 架构的标识符前缀数组，用于识别不同格式的架构名称
 * @param myElfMachineCode ELF文件格式中的机器码，可能为null
 * @param myPeMachineCode PE文件格式中的机器码，可能为null
 */
enum class ArchitectureType(
    /**
     * 架构的位数
     *
     * 表示CPU架构的地址总线宽度：
     * - 32：32位架构，支持4GB地址空间
     * - 64：64位架构，支持更大的地址空间
     * - 0：未知架构
     */
    private val myBits: Byte,

    /**
     * 架构标识符前缀数组
     *
     * 包含该架构的各种可能名称前缀，用于架构识别。
     * 例如x86_64架构可能被识别为"x86_64"、"x64"或"amd64"。
     */
    private val myIdPrefixArray: Array<String>,

    /**
     * ELF机器码
     *
     * 在ELF（Executable and Linkable Format）文件格式中，
     * 用于标识目标CPU架构的机器码。主要用于Unix/Linux系统。
     */
    private val myElfMachineCode: Int?,

    /**
     * PE机器码
     *
     * 在PE（Portable Executable）文件格式中，
     * 用于标识目标CPU架构的机器码。主要用于Windows系统。
     */
    private val myPeMachineCode: Int?
) {
    I386(32, arrayOf("i386", "i486", "i586", "i686", "x86"), 3, 332),
    X86_64(64, arrayOf("x86_64", "x64", "amd64"), 62, 34404),
    ARM(32, arrayOf("arm", "xscale", "arm64_32"), 40, 448),
    ARM64(64, arrayOf("arm64", "armv8", "aarch64"), 183, 43620),
    MIPS(32, arrayOf("mips"), 8, null),
    MIPS64(64, arrayOf("mips64"), 8, null),
    PPC(32, arrayOf("ppc", "powerpc"), 20, null),
    PPC64(64, arrayOf("powerpc64", "ppc970-64"), 21, null),
    SPARC(32, arrayOf("sparc"), 2, null),
    SPARCV9(64, arrayOf("sparcv9"), 43, null),
    RISCV32(32, arrayOf("riscv32"), 243, 20530),
    RISCV64(64, arrayOf("riscv64"), 243, 20580),
    UNKNOWN(0, arrayOf("unknown"), null, null);

    /**
     * 获取架构的主要标识符
     *
     * 返回该架构的第一个（主要）标识符，通常是最常用的名称。
     *
     * @return 架构的主要标识符字符串
     */
    fun getId(): String {
        return myIdPrefixArray[0]
    }

    /**
     * 获取架构的位数
     *
     * 返回该架构的地址总线位数。
     *
     * @return 架构的位数（32、64或0）
     */
    fun getBits(): Int {
        return myBits.toInt()
    }

    companion object {
        /**
         * 根据架构字符串查找对应的架构类型
         *
         * 使用最长匹配算法查找最匹配的架构类型。
         * 该方法会尝试匹配所有可能的前缀，返回匹配最长的架构。
         *
         * 使用场景：
         * - 从系统信息或配置文件中解析架构
         * - 处理用户输入的架构名称
         * - 识别不同格式的架构字符串
         *
         * @param arch 要查找的架构字符串，可以为null
         * @return 匹配的架构类型，未找到时返回UNKNOWN
         */
        fun forArchitecture(arch: String?): ArchitectureType {
            var found = UNKNOWN
            arch?.let { nonNullArch ->
                var foundPrefixLength = 0
                for (value in entries) {
                    for (prefix in value.myIdPrefixArray) {
                        if (foundPrefixLength < prefix.length && nonNullArch.startsWith(prefix)) {
                            foundPrefixLength = prefix.length
                            found = value
                            if (foundPrefixLength == nonNullArch.length) {
                                return value
                            }
                        }
                    }
                }
            }
            return found
        }

        /**
         * 根据IntelliJ平台的CPU架构查找对应的架构类型
         *
         * 将IntelliJ平台识别的CPU架构转换为调试器的架构类型。
         *
         * 使用场景：
         * - 与IntelliJ平台的架构检测集成
         * - 根据当前运行环境选择调试器
         * - 处理IDE的架构相关信息
         *
         * @param cpuArch IntelliJ平台的CPU架构
         * @return 对应的架构类型，不支持时返回UNKNOWN
         */
        fun forVmCpuArch(cpuArch: CpuArch): ArchitectureType {
            return when (cpuArch) {
                CpuArch.X86 -> I386
                CpuArch.X86_64 -> X86_64
                CpuArch.ARM32 -> ARM
                CpuArch.ARM64 -> ARM64
                else -> UNKNOWN
            }
        }

        /**
         * 根据PE文件机器码查找对应的架构类型
         *
         * 通过PE文件格式中的机器码识别目标程序的架构类型。
         * 主要用于Windows平台的可执行文件分析。
         *
         * 使用场景：
         * - 分析Windows可执行文件的架构
         * - 选择合适的Windows调试器
         * - 处理PE文件的架构信息
         *
         * @param peCode PE文件中的机器码
         * @return 对应的架构类型，未找到时返回UNKNOWN
         */
        fun forPeMachineType(peCode: Short): ArchitectureType {
            for (value in entries) {
                if (value.myPeMachineCode == peCode.toInt()) {
                    return value
                }
            }
            return UNKNOWN
        }

        /**
         * 根据ELF文件机器码查找对应的架构类型
         *
         * 通过ELF文件格式中的机器码识别目标程序的架构类型。
         * 主要用于Unix/Linux平台的可执行文件分析。
         *
         * 使用场景：
         * - 分析Unix/Linux可执行文件的架构
         * - 选择合适的Linux调试器
         * - 处理ELF文件的架构信息
         *
         * @param elfCode ELF文件中的机器码
         * @return 对应的架构类型，未找到时返回UNKNOWN
         */
        fun forElfMachineType(elfCode: Short): ArchitectureType {
            for (value in entries) {
                if (value.myElfMachineCode == elfCode.toInt()) {
                    return value
                }
            }
            return UNKNOWN
        }
    }
}