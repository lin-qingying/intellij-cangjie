package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.descriptors.PackageViewDescriptor
import com.linqingying.cangjie.diagnostics.Errors.PACKAGE_ACCESS_VIOLATION
import com.linqingying.cangjie.psi.CjFile

class PackagerResolver(
    private val trace: BindingTrace,
    private val moduleDescriptor: ModuleDescriptor
) {
    fun check(c: TopDownAnalysisContext) {
        c.files.firstOrNull()?.let {
            val packageView = moduleDescriptor.getPackage(it.packageFqName)


            checkPackageLevel(it)
        }
//       packageViews


    }

    private fun checkPackageLevel(file: CjFile) {

        val packageView = moduleDescriptor.getPackage(file.packageFqName)
        val currentLevel = packageView.visibility.toAccessControlLevel()
        if (currentLevel == 0) {
            return
        }
        if (packageView.fqName.isModuleName) {
            return
        }
        val parentLevel = packageView.containingDeclaration?.visibility?.toAccessControlLevel() ?: return
        if (parentLevel < currentLevel) {

            trace.report(
                PACKAGE_ACCESS_VIOLATION.on(
                    file.packageDirective,

                    file.packageFqName,                 packageView.containingDeclaration?.fqName,
                )
            )

        }
    }
}
