/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.ide.stubindex

import com.linqingying.cangjie.descriptors.DescriptorVisibilities
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjImportDirectiveItem

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

class CangJieImportFqNameForPackageNameIndex internal constructor() : StringStubIndexExtension<CjImportDirectiveItem>() {
    companion object Helper : CangJieStringStubIndexHelper<CjImportDirectiveItem>(CjImportDirectiveItem::class.java) {
        override val indexKey: StubIndexKey<String, CjImportDirectiveItem> =
            StubIndexKey.createIndexKey("com.linqingying.cangjie.ide.stubindex.CangJieImportFqNameForPackageNameIndex")

        fun getReexport(
            key: String,
            project: Project,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        ): Collection<CjImportDirectiveItem> {

            return get(key, project, scope).filter {
                it.modifierVisibility != DescriptorVisibilities.PRIVATE
            }


        }


        fun contains(
            targetPackageName: FqName, currentPackageName: FqName, project: Project,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        ): Pair<Boolean, FqName> {
            //        判断一个FqName是否是包
            fun isPackage(fqName: FqName?): Boolean {
                fqName ?: return false

                return CangJieExactPackagesIndex.get(fqName.asString(), project, scope).isNotEmpty()

            }
            // 实现逻辑以检查目标包名是否包含在当前包名中
            // 查找当前包名的导入指令
            val directives = get(currentPackageName.asString(), project, scope).toMutableList().apply {
                if (isEmpty()) {
//                    该语句可能导入的不是包使用parent
                    addAll(get(currentPackageName.parent().asString(), project, scope))
                }
            }
            val fqName: FqName = if (isPackage(currentPackageName)) {
                currentPackageName
            } else {
                currentPackageName.parent()
            }
            // 查找匹配的导入指令
            val matchingDirective =
                directives.find {
                    (if (isPackage(it.importedFqName)) {

                        it.importedFqName
                    } else {


                        it.importedFqName?.parent()
                    }) == targetPackageName
                }
            // 返回是否找到匹配的导入指令及其引用
            return Pair(matchingDirective != null, fqName)
        }

        @JvmField
        @Deprecated("Use the Helper object instead", level = DeprecationLevel.ERROR)
        val INSTANCE: CangJieImportFqNameForPackageNameIndex = CangJieImportFqNameForPackageNameIndex()
    }

    override fun getKey(): StubIndexKey<String, CjImportDirectiveItem> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieImportFqNameForPackageNameIndex[key, project, scope]"))
    override fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<CjImportDirectiveItem> {
        return Helper[key, project, scope]
    }
}
