package com.huawei.cangjie.ide.util

import com.huawei.cangjie.descriptors.ClassKind
import com.huawei.cangjie.descriptors.Modality
import com.huawei.cangjie.descriptors.Visibility
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjFile
import com.intellij.openapi.extensions.ExtensionPointName

/** Filter that can block classes from being automatically imported as a side-effect of other actions. */
fun interface ClassImportFilter {
    /**
     * This class holds information that implementations of ClassImportFilter might need to decide whether to import a given class.
     * By packaging this information in a data class, we avoid the need to change the API when we need to add more/different information.
     */
    data class ClassInfo(val fqName: FqName, val classKind: ClassKind, val modality: Modality, val visibility: Visibility, val isNested: Boolean)

    /** Returns whether to allow this class to be imported. */
    fun allowClassImport(classInfo: ClassInfo, contextFile: CjFile) : Boolean

    companion object {
        val EP_NAME = ExtensionPointName.create<ClassImportFilter>("com.huawei.cangjie.classImportFilter")
        fun allowClassImport(classInfo: ClassInfo, contextFile: CjFile) =
            EP_NAME.extensions.all { it.allowClassImport(classInfo, contextFile) }
    }
}
