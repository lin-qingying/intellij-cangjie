package com.linqingying.cangjie.serialization.builtins

import com.linqingying.cangjie.cli.messages.CompilerMessageSeverity
import com.linqingying.cangjie.config.*
import com.linqingying.cangjie.config.destDir
import com.linqingying.cangjie.metadata.builtins.BuiltInsBinaryVersion
import java.io.File


abstract class AbstractMetadataSerializer<T>(
    val configuration: CompilerConfiguration,
    val environment: CangJieCoreEnvironment,
    definedMetadataVersion: BuiltInsBinaryVersion? = null

) {
    protected val metadataVersion =
        definedMetadataVersion ?: configuration.get(CommonConfigurationKeys.METADATA_VERSION) as? BuiltInsBinaryVersion
        ?: BuiltInsBinaryVersion.INSTANCE

    fun analyzeAndSerialize(): OutputInfo? {
        val destDir = environment.destDir
        if (destDir == null) {
            val configuration = environment.configuration
            val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            messageCollector.report(CompilerMessageSeverity.ERROR, "Specify destination via -d")
            return null
        }

        val analysisResult = analyze() ?: return null
//        val performanceManager = environment.configuration.getNotNull(CLIConfigurationKeys.PERF_MANAGER)


        return serialize(analysisResult, destDir)
    }

    protected abstract fun analyze(): T?

    /**
     * @return number of written bytes and files
     * The return value is optional and might be omitted in implementations
     */
    protected abstract fun serialize(analysisResult: T, destDir: File): OutputInfo?

    data class OutputInfo(val totalSize: Int, val totalFiles: Int)
}
