package com.linqingying.cangjie.toml

import com.intellij.openapi.fileTypes.LanguageFileType
import org.toml.TomlBundle.message
import org.toml.TomlIcons
import org.toml.lang.TomlLanguage
import javax.swing.Icon

object CangJieTomlFileType : LanguageFileType(TomlLanguage) {
    override fun getName(): String {
        return "TOML"

    }

    override fun getDescription(): String {
        return message("filetype.toml.description")

    }

    override fun getDefaultExtension(): String {
        return "toml"

    }

    override fun getIcon(): Icon {
        return TomlIcons.TomlFile
    }
}
