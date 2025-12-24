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

package org.cangnova.cangjie.stubindex

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.cangnova.cangjie.psi.CjField

/**
 * 按短名称索引类成员字段
 *
 * 用于通过字段名称快速查找 CjField 元素。
 * 例如：查找所有名为 "name" 的字段
 */
class CangJieFieldShortNameIndex internal constructor() : StringStubIndexExtension<CjField>() {
    companion object Helper : CangJieStringStubIndexHelper<CjField>(CjField::class.java) {
        override val indexKey: StubIndexKey<String, CjField> =
            StubIndexKey.createIndexKey(CangJieFieldShortNameIndex::class.java.simpleName)
    }

    override fun getKey(): StubIndexKey<String, CjField> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieFieldShortNameIndex[shortName, project, scope]"))
    override fun get(shortName: String, project: Project, scope: GlobalSearchScope): Collection<CjField> {
        return Helper[shortName, project, scope]
    }
}
