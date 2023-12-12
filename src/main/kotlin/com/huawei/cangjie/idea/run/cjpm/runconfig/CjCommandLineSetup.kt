package com.huawei.cangjie.idea.run.cjpm.runconfig

import com.intellij.execution.target.*
import com.intellij.execution.target.local.LocalTargetEnvironment
import com.intellij.execution.target.value.DeferredTargetValue
import com.intellij.execution.target.value.TargetValue
import com.intellij.execution.target.value.getUploadRootForLocalPath
import com.intellij.lang.LangCoreBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.isDirectory
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.nio.file.Path
import java.nio.file.Paths

val TargetEnvironmentConfiguration.languageRuntime: CjLanguageRuntimeConfiguration?
    get() = runtimes.findByType()
fun String.toPath(): Path = Paths.get(this)
@Suppress("UnstableApiUsage")
class CjCommandLineSetup(val request: TargetEnvironmentRequest) {
    private val languageRuntime: CjLanguageRuntimeConfiguration? = request.configuration?.languageRuntime
    private val environmentPromise = AsyncPromise<Pair<TargetEnvironment, TargetProgressIndicator>>()
    private val dependingOnEnvironmentPromise: MutableList<Promise<Unit>> = mutableListOf()
    private val uploads: MutableList<Upload> = mutableListOf()
    private val projectHomeOnTarget: LanguageRuntimeType.VolumeDescriptor = LanguageRuntimeType.VolumeDescriptor(
        CjCommandLineSetup::class.java.simpleName + ":projectHomeOnTarget",
        "",
        "",
        "",
        request.projectPathOnTarget
    )

    fun requestUploadIntoTarget(uploadPathString: String): TargetValue<String> {
        val uploadPath = FileUtil.toSystemDependentName(uploadPathString).toPath()
        val isDir = uploadPath.isDirectory()
        val localRootPath = if (isDir) uploadPath else (uploadPath.parent ?: Paths.get("."))
        val (uploadRoot, pathToRoot) = request.getUploadRootForLocalPath(localRootPath)
            ?: createUploadRoot(projectHomeOnTarget, localRootPath).let { uploadRoot ->
                request.uploadVolumes += uploadRoot
                uploadRoot to "."
            }
        val result = DeferredTargetValue(uploadPathString)
        dependingOnEnvironmentPromise += environmentPromise.then { (environment, targetProgressIndicator) ->
            if (targetProgressIndicator.isCanceled || targetProgressIndicator.isStopped) {
                result.stopProceeding()
                return@then
            }
            val volume = environment.uploadVolumes.getValue(uploadRoot)
            try {
                val relativePath = if (isDir) {
                    pathToRoot
                } else {
                    uploadPath.fileName.toString()
                        .let { if (pathToRoot == ".") it else joinPath(pathToRoot, it) }
                }
                val resolvedTargetPath = volume.resolveTargetPath(relativePath)
                uploads.add(Upload(volume, relativePath))
                result.resolve(resolvedTargetPath)
            } catch (t: Throwable) {
                LOG.warn(t)
                targetProgressIndicator.stopWithErrorMessage(
                    LangCoreBundle.message(
                        "progress.message.failed.to.resolve.0.1",
                        volume.localRoot, t.localizedMessage
                    )
                )
                result.resolveFailure(t)
            }
        }
        return result
    }

    private fun createUploadRoot(
        volumeDescriptor: LanguageRuntimeType.VolumeDescriptor,
        localRootPath: Path
    ): TargetEnvironment.UploadRoot =
        languageRuntime?.createUploadRoot(volumeDescriptor, localRootPath)
            ?: TargetEnvironment.UploadRoot(localRootPath, TargetEnvironment.TargetPath.Temporary())

    private fun joinPath(vararg segments: String): String =
        segments.joinToString(request.targetPlatform.platform.fileSeparator.toString())

    fun provideEnvironment(environment: TargetEnvironment, targetProgressIndicator: TargetProgressIndicator) {
        val application = ApplicationManager.getApplication()
        LOG.assertTrue(
            environment is LocalTargetEnvironment ||
                    uploads.isEmpty() ||
                    !application.isDispatchThread ||
                    application.isUnitTestMode,
            "Preparation of environment shouldn't be performed on EDT."
        )
        environmentPromise.setResult(environment to targetProgressIndicator)
        uploads.asSequence()
            .sortedBy { it.relativePath.length }
            .groupBy({ it.volume }, { it.relativePath })
            .forEach { (volume, relativePaths) ->
                volume.upload(relativePaths.first(), targetProgressIndicator)
            }
        for (promise in dependingOnEnvironmentPromise) {
            promise.blockingGet(0) // Just rethrows errors
        }
    }

    private class Upload(val volume: TargetEnvironment.UploadableVolume, val relativePath: String)

    companion object {
        private val LOG: Logger = logger<CjCommandLineSetup>()
    }
}
