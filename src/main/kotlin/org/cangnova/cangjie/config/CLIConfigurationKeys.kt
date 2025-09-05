package org.cangnova.cangjie.config

import java.io.File

object CLIConfigurationKeys {
    val METADATA_DESTINATION_DIRECTORY: CompilerConfigurationKey<File> =
        CompilerConfigurationKey.create("metadata destination directory")

    @JvmField
    val CONTENT_ROOTS: CompilerConfigurationKey<List<ContentRoot>> = CompilerConfigurationKey.create("content roots")

    @JvmField
    val PERF_MANAGER: CompilerConfigurationKey<CommonCompilerPerformanceManager> =
        CompilerConfigurationKey.create("performance manager")

    @JvmField
    val RENDER_DIAGNOSTIC_INTERNAL_NAME: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("render diagnostic internal name")


}