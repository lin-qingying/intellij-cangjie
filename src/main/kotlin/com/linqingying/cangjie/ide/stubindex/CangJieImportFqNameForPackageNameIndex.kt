package com.linqingying.cangjie.ide.stubindex

import com.linqingying.cangjie.descriptors.DescriptorVisibilities
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjImportDirective

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

class CangJieImportFqNameForPackageNameIndex internal constructor() : StringStubIndexExtension<CjImportDirective>() {
    companion object Helper : CangJieStringStubIndexHelper<CjImportDirective>(CjImportDirective::class.java) {
        override val indexKey: StubIndexKey<String, CjImportDirective> =
            StubIndexKey.createIndexKey("com.linqingying.cangjie.ide.stubindex.CangJieImportFqNameForPackageNameIndex")

        fun getReexport(
            key: String,
            project: Project,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        ): Collection<CjImportDirective> {

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

    override fun getKey(): StubIndexKey<String, CjImportDirective> = indexKey

    @Deprecated("Base method is deprecated", ReplaceWith("CangJieImportFqNameForPackageNameIndex[key, project, scope]"))
    override fun get(key: String, project: Project, scope: GlobalSearchScope): Collection<CjImportDirective> {
        return Helper[key, project, scope]
    }
}
