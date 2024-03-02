package com.huawei.cangjie.idea.newProject

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.icon.CangJieIcons
import com.intellij.openapi.util.NlsContexts
import org.jetbrains.annotations.Nls
import javax.swing.Icon


sealed class CjProjectTemplate(
    @Suppress("UnstableApiUsage") @NlsContexts.ListItem val name: String,
    val isBinary: Boolean,
    val icon: Icon
) {
    @Nls
    fun validateProjectName(crateName: String): String? = CjPackageNameValidator.validate(crateName, isBinary)
}

sealed class CjGenericTemplate(@Suppress("UnstableApiUsage") @NlsContexts.ListItem name: String, isBinary: Boolean) :
    CjProjectTemplate(name, isBinary, CangJieIcons.CANGJIE_FILE) {
    object CjpmBinaryTemplate : CjGenericTemplate(CangJieBundle.message("list.item.binary.application"), true)
    object CjpmLibraryTemplate : CjGenericTemplate(CangJieBundle.message("list.item.library"), false)
}

open class CjCustomTemplate(
    @Suppress("UnstableApiUsage") @NlsContexts.ListItem name: String,
    val url: String
) : CjProjectTemplate(name, false, CangJieIcons.CANGJIE_FILE) {


    val shortLink: String
        get() = url.substringAfter("//")
}
