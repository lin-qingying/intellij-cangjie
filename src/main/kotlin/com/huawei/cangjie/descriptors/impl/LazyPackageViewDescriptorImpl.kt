package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.ide.stubindex.CangJieExactPackagesIndex
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjPackageDirective
import com.huawei.cangjie.resolve.scopes.ChainedMemberScope
import com.huawei.cangjie.resolve.scopes.LazyScopeAdapter
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.storage.getValue

class LazyPackageViewDescriptorImpl(
    override val module: ModuleDescriptorImpl,
    override val fqName: FqName,
    val storageManager: StorageManager
) : DeclarationDescriptorImpl(Annotations.EMPTY, fqName.shortNameOrSpecial()), PackageViewDescriptor {
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R =
        visitor.visitPackageViewDescriptor(this, data!!)


    protected val empty: Boolean by storageManager.createLazyValue {
        module.packageFragmentProvider.isEmpty(fqName)
    }

    override fun isEmpty(): Boolean = empty


    var packageDirectives: MutableList<CjPackageDirective> = mutableListOf(


    )

    //    是否需要报告包名修饰符不一致错误
    var isReported: Boolean = false

    var _visibility: DescriptorVisibility? = null

    init {
        if (!isEmpty()) {
            initVisibility()
        }

    }

    fun initVisibility(){
        if (module.project != null) {
            val filelist = CangJieExactPackagesIndex.get(fqName.asString(), module.project)
            val visibilitys = mutableListOf<DescriptorVisibility>()
            filelist.forEach {
                if (it.packageDirective != null) {
                    packageDirectives.add(it.packageDirective!!)

                    visibilitys.add(it.packageDirective!!.modifierVisibility)
                }
                it.packageDirective?.getModifierVisibility()
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




