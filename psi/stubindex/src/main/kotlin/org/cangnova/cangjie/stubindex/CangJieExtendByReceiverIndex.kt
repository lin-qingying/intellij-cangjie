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

package org.cangnova.cangjie.stubindex

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.cangnova.cangjie.psi.CjExtend

/**
 * 按被扩展类型的短名称索引扩展声明
 *
 * 该索引允许通过被扩展类型的名称（如 "Int64"、"String"）快速查找所有对应的 extend 声明。
 * 这在方法解析时非常有用，可以找到为特定类型添加的扩展成员。
 *
 * 索引键: 被扩展类型的短名称（如 "Int64"）
 * 索引值: 对应的 CjExtend PSI 元素
 *
 * 示例:
 * ```cangjie
 * extend Int64 <: Printable { ... }
 * ```
 * 上述声明会被索引为: "Int64" -> CjExtend
 */
class CangJieExtendByReceiverIndex internal constructor() : StringStubIndexExtension<CjExtend>() {
    companion object Helper : CangJieStringStubIndexHelper<CjExtend>(CjExtend::class.java) {
        @JvmStatic
        @Suppress("DeprecatedCallableAddReplaceWith")
        @Deprecated("Use the Helper object instead", level = DeprecationLevel.ERROR)
        fun getInstance(): CangJieExtendByReceiverIndex {
            return CangJieExtendByReceiverIndex()
        }

        @JvmField
        @Deprecated("Use the Helper object instead", level = DeprecationLevel.ERROR)
        val INSTANCE: CangJieExtendByReceiverIndex = CangJieExtendByReceiverIndex()

        override val indexKey: StubIndexKey<String, CjExtend> =
            StubIndexKey.createIndexKey(CangJieExtendByReceiverIndex::class.java.simpleName)
    }

    override fun getKey(): StubIndexKey<String, CjExtend> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieExtendByReceiverIndex[receiverTypeName, project, scope]"))
    override fun get(receiverTypeName: String, project: Project, scope: GlobalSearchScope): Collection<CjExtend> {
        return Helper[receiverTypeName, project, scope]
    }
}
