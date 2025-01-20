package com.linqingying.cangjie.toml

import com.intellij.notification.NotificationType
import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.ide.notifications.showBalloonWithoutProject
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlTableHeader

fun tomlPluginIsAbiCompatible(): Boolean = computeOnce
private inline fun <reified T : Any> load(): String = T::class.java.name

private val computeOnce: Boolean by lazy {
    try {
        load<TomlKeySegment>()
        true
    } catch (e: LinkageError) {
        showBalloonWithoutProject(
            CangJieBundle.message("notification.content.incompatible.toml.plugin.version.code.completion.for.cjpm.toml.not.available"),
            NotificationType.WARNING
        )
        false
    }
}

val TomlTableHeader.isDependencyListHeader: Boolean
    get() = key?.segments?.lastOrNull()?.isDependencyKey == true
