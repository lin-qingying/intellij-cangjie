package com.linqingying.cangjie.test

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.application.ApplicationManager
import junit.framework.TestCase
import java.io.File
import java.nio.file.FileSystems
import java.util.zip.ZipFile

abstract class CangJieTestWithEnvironment : TestCase() {
    fun File.hasConfigFile(configFile: String): Boolean =
        if (isDirectory) File(this, "META-INF" + File.separator + configFile).exists()
        else try {
            ZipFile(this).use {
                it.getEntry("META-INF/$configFile") != null
            }
        } catch (e: Throwable) {
            false
        }

    init {


        CoreApplicationEnvironment.registerExtensionPointAndExtensions(
            FileSystems.getDefault().getPath(File("plugin/src/main/resources/META_INF/plugin.xml").path),
            "plugin.xml",
            ApplicationManager.getApplication().extensionArea
        )
    }

    val application = ApplicationManager.getApplication()

    val applicationEnvironment = CangJieCoreApplicationEnvironment.create({ }, true)

    val projectEnvironment = CangJieCoreProjectEnvironment({}, applicationEnvironment)

    val project = projectEnvironment.project
}
