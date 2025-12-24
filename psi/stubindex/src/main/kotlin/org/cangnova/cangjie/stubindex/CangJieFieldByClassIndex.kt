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
import org.cangnova.cangjie.psi.CjFieldVariable

/**
 * 按所属类的完全限定名索引类成员字段
 *
 * 用于通过类的 FqName 查找该类的所有字段。
 * 例如：查找 "std.core.Person" 类的所有字段
 */
class CangJieFieldByClassIndex internal constructor() : StringStubIndexExtension<CjFieldVariable>() {
    companion object Helper : CangJieStringStubIndexHelper<CjFieldVariable>(CjFieldVariable::class.java) {
        override val indexKey: StubIndexKey<String, CjFieldVariable> =
            StubIndexKey.createIndexKey(CangJieFieldByClassIndex::class.java.simpleName)
    }

    override fun getKey(): StubIndexKey<String, CjFieldVariable> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieFieldByClassIndex[classFqName, project, scope]"))
    override fun get(classFqName: String, project: Project, scope: GlobalSearchScope): Collection<CjFieldVariable> {
        return Helper[classFqName, project, scope]
    }
}
