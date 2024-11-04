package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.scopes.MemberScope

interface PackageData : DeclarationDescriptor {
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

interface PackageViewDescriptor : PackageData {

    fun getContributedDescriptorsByReexportDirective(
        languageVersionSettings: LanguageVersionSettings,
        packageFragment: PackageFragmentDescriptor?,
        declaredName: Name,

        aliasName: Name
    ): Collection<DeclarationDescriptor> {

        return emptyList()
    }

    val reexportTop: Boolean get() =  true
    override val containingDeclaration: PackageViewDescriptor?

    val memberScope: MemberScope

    val module: ModuleDescriptor

    val fragments: List<PackageFragmentDescriptor>

    fun isEmpty(): Boolean = fragments.isEmpty()
}
