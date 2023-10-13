package com.huawei.cangjie.lang.core.psi

import com.huawei.cangjie.lang.CjFileType
import com.huawei.cangjie.lang.CjLanguage
import com.huawei.cangjie.lang.core.completion.getOriginalOrSelf
import com.huawei.cangjie.lang.core.psi.ext.CjMod
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker


class CjFile(
    fileViewProvider: FileViewProvider
)  : CjFileBase(fileViewProvider)  {
//    override val containingMod: CjMod get() = getOriginalOrSelf()

//    override val crateRelativePath: String? get() = RsPsiImplUtil.modCrateRelativePath(this)

//    override val crateRoot: CjMod? get() = cachedData.crateRoot

//    override val crateRelativePath: String? get() = RsPsiImplUtil.modCrateRelativePath(this)
}
abstract class CjFileBase(fileViewProvider: FileViewProvider) : PsiFileBase(fileViewProvider, CjLanguage)    {

//    override fun getReference(): RsReference? = null

    override fun getOriginalFile(): CjFileBase = super.getOriginalFile() as CjFileBase

    override fun getFileType(): FileType = CjFileType

//    override fun getStub(): RsFileStub? = super.getStub() as RsFileStub?
}
