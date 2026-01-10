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

package org.cangnova.cangjie.psi.stubs

import org.cangnova.cangjie.name.FqName

/**
 * 表示仓颉文件 Stub 的类型。
 *
 * 不同类型的文件（源文件、Facade 文件、多文件类等）有不同的 Stub 结构。
 */
sealed interface CangJieFileStubKind {

    /**
     * 包含包名信息的文件 Stub 类型。
     */
    sealed interface WithPackage : CangJieFileStubKind {
        val packageFqName: FqName

        /**
         * 普通源文件。
         */
        interface File : WithPackage

        /**
         * 脚本文件。
         */
        interface Script : WithPackage

        /**
         * Facade 文件类型。
         */
        sealed interface Facade : WithPackage {
            val facadeFqName: FqName

            /**
             * 简单的 Facade 文件。
             */
            interface Simple : Facade {
                val partSimpleName: String
            }

            /**
             * 多文件类的 Facade。
             */
            interface MultifileClass : Facade {
                val facadePartSimpleNames: List<String>
            }
        }
    }

    /**
     * 无效的文件 Stub（解析失败等情况）。
     */
    interface Invalid : CangJieFileStubKind {
        val errorMessage: String
    }
}