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
import org.cangnova.cangjie.psi.CjEnumConstructor

/**
 * 枚举构造器按枚举类型索引
 *
 * 用于查找某个枚举类型的所有构造器。
 * 索引键是枚举类型的完全限定名（FQN）。
 *
 * 例如：对于 `com.example.Color.Red` 构造器，
 * 索引键是 `com.example.Color`（父枚举类型的 FQN）
 */
class CangJieEnumConstructorByEnumTypeIndex internal constructor() : StringStubIndexExtension<CjEnumConstructor>() {
    companion object Helper : CangJieStringStubIndexHelper<CjEnumConstructor>(CjEnumConstructor::class.java) {
        override val indexKey: StubIndexKey<String, CjEnumConstructor> =
            StubIndexKey.createIndexKey(CangJieEnumConstructorByEnumTypeIndex::class.java.simpleName)
    }

    override fun getKey(): StubIndexKey<String, CjEnumConstructor> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieEnumConstructorByEnumTypeIndex[key, project, scope]"))
    override fun get(enumTypeFqn: String, project: Project, scope: GlobalSearchScope): Collection<CjEnumConstructor> {
        return Helper[enumTypeFqn, project, scope]
    }
}
