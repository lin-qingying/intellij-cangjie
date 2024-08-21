package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.ide.stubindex.CangJieExactPackagesIndex
import com.huawei.cangjie.ide.stubindex.CangJieImportFqNameForPackageNameIndex
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjImportInfo
import com.huawei.cangjie.psi.CjPackageDirective
import com.huawei.cangjie.resolve.LazyExplicitImportScope
import com.huawei.cangjie.resolve.lazy.declarations.impl.ReexportPackageFragment
import com.huawei.cangjie.resolve.scopes.ChainedMemberScope
import com.huawei.cangjie.resolve.scopes.LazyScopeAdapter
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.storage.getValue
import com.huawei.cangjie.utils.CallOnceFunction
import com.intellij.openapi.application.runReadAction

class LazyReexportAgent
    (

    override val visibility: DescriptorVisibility,
    val proxied: DeclarationDescriptor,
    val packageFragmentDescriptor: PackageFragmentDescriptor,

    ) : DeclarationDescriptor {
    override val original: DeclarationDescriptor = this
    override val containingDeclaration: DeclarationDescriptor = packageFragmentDescriptor
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return proxied.accept(visitor, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        return proxied.acceptVoid(visitor)

    }

    var isTop: Boolean = true


    override val name: Name = proxied.name
}

class LazyReexportPackage(

    override val fqName: FqName,

    ) : PackageData {
    val packageProjection: PackageData
        get() {
            return this

        }
    override val original: DeclarationDescriptor = packageProjection.original
    override val containingDeclaration: DeclarationDescriptor? = packageProjection.containingDeclaration

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        TODO("Not yet implemented")
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        TODO("Not yet implemented")
    }

    override val name: Name
        get() = TODO("Not yet implemented")
}

class LazyPackageViewDescriptorImpl(
    override val module: ModuleDescriptorImpl, override val fqName: FqName, val storageManager: StorageManager
) : DeclarationDescriptorImpl(Annotations.EMPTY, fqName.shortNameOrSpecial()), PackageViewDescriptor {
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R =
        visitor.visitPackageViewDescriptor(this, data!!)


    protected val empty: Boolean by storageManager.createLazyValue {
        module.packageFragmentProvider.isEmpty(fqName)
    }

    override fun isEmpty(): Boolean = empty


    private val reexportDirectives = runReadAction {
        module.project?.let {
            CangJieImportFqNameForPackageNameIndex.getReexport(fqName.asString(), it)
        }?.toSet()
    } ?: emptySet()


    private val reexportPackageView: MutableMap<CjImportInfo, PackageViewDescriptor> = mutableMapOf()
    var packageDirectives: MutableList<CjPackageDirective> = mutableListOf(


    )

    //    是否为重导出顶层
    override var reexportTop = true

    //    是否需要报告包名修饰符不一致错误
    var isReported: Boolean = false

    private var _visibility: DescriptorVisibility? = null

    init {
        if (!isEmpty()) {
            initVisibility()
        }

    }

    //是否报告宏包声明不一致错误
    var isReportMacroPackage: Boolean = false
    var isMacroPackage: Boolean = false

    private fun initVisibility() {

        reexportDirectives.forEach { cjImportDirective ->


            val fqname = if (cjImportDirective.isAllUnder) {
                cjImportDirective.importedFqName
            } else {
                cjImportDirective.importedFqName?.parent()
            }

            if (fqname != this.fqName) {
                fqname?.let {
                    reexportPackageView[cjImportDirective] = module.getPackage(it).apply {
                        this@LazyPackageViewDescriptorImpl.reexportTop = false
                    }
                }
            }


        }


        if (module.project != null) {
            val filelist = CangJieExactPackagesIndex.get(fqName.asString(), module.project)
            val visibilitys = mutableListOf<DescriptorVisibility>()
            val macroPackagesIS = mutableListOf<Boolean>()
            filelist.forEach {
                if (it.packageDirective != null) {
                    packageDirectives.add(it.packageDirective!!)


                    macroPackagesIS.add(it.packageDirective!!.isMacroPackage)

                    visibilitys.add(it.packageDirective!!.modifierVisibility)
                }
                it.packageDirective?.getModifierVisibility()
            }

//            如果macroPackagesIS全部为true 或者其中有true
            if (macroPackagesIS.all { it } || macroPackagesIS.any { it }) {
                isMacroPackage = true
            } else {
                isMacroPackage = false
            }
//            如果macroPackagesIS既有true也有false ，报告错误
            if (macroPackagesIS.any { it } && macroPackagesIS.any { !it }) {
                isReportMacroPackage = true
            }

            if (visibilitys.isNotEmpty()) {
                if (visibilitys.all { it == visibilitys.first() }) {
                    _visibility = visibilitys.first()
                } else {
                    _visibility = visibilitys.first()
                    isReported = true
                }
            }

        }

    }

    override val visibility: DescriptorVisibility
        get() {
            if (_visibility != null) {
                return _visibility!!
            }

            return DescriptorVisibilities.PUBLIC
        }

    private val importScope = mutableMapOf<PackageViewDescriptor, MutableMap<Name, LazyExplicitImportScope>>()

    override fun getContributedDescriptorsByReexportDirective(
        languageVersionSettings: LanguageVersionSettings,
        packageFragment: PackageFragmentDescriptor?,
        declaredName: Name,

        aliasName: Name
    ): Collection<DeclarationDescriptor> {

        val declarationList = mutableListOf<DeclarationDescriptor>()
        for (key in reexportPackageView.keys) {
            val packageViewDescriptor = reexportPackageView[key] ?: continue

            if (!key.isAllUnder && key.importedName == aliasName) {
                val declaration = packageViewDescriptor.memberScope.getContributedClassifier(
                    aliasName, NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS
                )
                declaration?.let {
                    declarationList.add(
                        LazyReexportAgent(
                            key.modifierVisibility,
                            it,
                            ReexportPackageFragment(module, this.fqName, memberScope)
                        )
                    )
                }

            } else {
                val scope =
                    if (importScope[packageViewDescriptor] != null && importScope[packageViewDescriptor]!![aliasName] != null) {

                        importScope[packageViewDescriptor]!![aliasName]!!
                    } else {
                        if (importScope[packageViewDescriptor] == null) {
                            importScope[packageViewDescriptor] = mutableMapOf()
                        }
                        importScope[packageViewDescriptor]!![aliasName] = LazyExplicitImportScope(
                            languageVersionSettings,
                            packageViewDescriptor,
                            packageFragment,
                            declaredName,
                            aliasName,
                            CallOnceFunction(Unit) { candidates ->
                                println()
                            })
                        importScope[packageViewDescriptor]!![aliasName]!!
                    }
                val declarations = scope.getContributedDescriptors().map {
                    LazyReexportAgent(key.modifierVisibility, it,    ReexportPackageFragment(module, this.fqName, memberScope))
                }

                declarationList.addAll(declarations)

            }


        }

        return declarationList
    }

    override val containingDeclaration: PackageViewDescriptor?
        get() = if (fqName.isRoot) null else module.getPackage(fqName.parent())
    override val memberScope: MemberScope = LazyScopeAdapter(storageManager) {
        if (isEmpty()) {
            MemberScope.Empty
        } else {
            // Packages from SubpackagesScope are got via getContributedDescriptors(DescriptorKindFilter.PACKAGES, MemberScope.ALL_NAME_FILTER)
            val scopes = fragments.map { it.getMemberScope() } + SubpackagesScope(module, fqName)
            ChainedMemberScope.create("package view scope for $fqName in ${module.name}", scopes)
        }
    }
    override val fragments: List<PackageFragmentDescriptor> by storageManager.createLazyValue {
        module.packageFragmentProvider.packageFragments(fqName)
    }

}




