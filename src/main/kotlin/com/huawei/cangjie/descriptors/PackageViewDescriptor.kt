package com.huawei.cangjie.descriptors

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.resolve.scopes.MemberScope
interface PackageData: DeclarationDescriptor{
    val fqName: FqName


    fun shouldSeeInternalsOf(whatPackage: PackageData): Boolean {
//判断自己是不是 whatPackage 的子包或本包


//        val f1 = FqName.topLevel(Name.identifier("a"))
//        val f2 = f1.child(Name.identifier("b"))

        if (this.fqName == whatPackage.fqName.parent()) return true
        return this.fqName.startsWith(whatPackage.fqName)

    }

    fun shouldSeeInternalsOf(whatPackage: PackageFragmentDescriptor): Boolean {
//判断自己是不是 whatPackage 的子包或本包


//        val f1 = FqName.topLevel(Name.identifier("a"))
//        val f2 = f1.child(Name.identifier("b"))

        return this.fqName.startsWith(whatPackage.fqName)

    }

    fun shouldProtectedsOf(whatPackage: PackageData): Boolean {
// 判断模块名是否相同
        return this.fqName.moduleName == whatPackage.fqName.moduleName

    }
}
interface PackageViewDescriptor :PackageData {



    override val containingDeclaration: PackageViewDescriptor?

    val memberScope: MemberScope

    val module: ModuleDescriptor

    val fragments: List<PackageFragmentDescriptor>

    fun isEmpty(): Boolean = fragments.isEmpty()
}
