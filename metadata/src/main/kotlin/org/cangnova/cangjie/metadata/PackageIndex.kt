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

package org.cangnova.cangjie.metadata

/**
 * 表示仓颉编译器中用于AST序列化和包引用处理的包索引值的Kotlin枚举类。
 */
enum class PackageIndex(val value: Int) {
    /**
     * 无效的包引用 (-1)
     */
    INVALID_PACKAGE_INDEX(-1),
    
    /**
     * 当前包索引 (-2)  
     * 用于当前包内的声明
     */
    CURRENT_PKG_INDEX(-2),
    
    /**
     * 包引用索引 (-3)
     * 用于引用整个包时
     */
    PKG_REFERENCE_INDEX(-3),


    ;

    companion object {
        /**
         * 将整数值转换为PackageIndex枚举
         * @param value 整数值
         * @return PackageIndex枚举，如果值表示导入的包（≥0）则返回null
         */
        fun fromValue(value: Int): PackageIndex? {
            return values().find { it.value == value }
        }
        
        /**
         * 检查给定的值是否表示导入的包索引
         * @param value 要检查的整数值
         * @return 如果值 ≥ 0 则返回true，表示导入的包
         */
        fun isImportedPackage(value: Int): Boolean {
            return value >= 0
        }
    }
}