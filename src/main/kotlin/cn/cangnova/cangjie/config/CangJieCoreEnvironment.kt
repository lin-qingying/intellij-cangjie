/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.config

import com.intellij.codeInsight.ContainerProvider
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreProjectEnvironment
import com.intellij.lang.MetaLanguage
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.TransactionGuardImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.PersistentFSConstants
import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.psi.FileContextProvider
import com.intellij.psi.impl.smartPointers.SmartPointerAnchorProvider
import com.intellij.psi.search.GlobalSearchScope
import cn.cangnova.cangjie.analyzer.CommonCompilerPerformanceManager
import cn.cangnova.cangjie.cli.messages.CompilerSystemProperties
import cn.cangnova.cangjie.cli.messages.MessageCollector
import cn.cangnova.cangjie.cli.messages.toBooleanLenient
import cn.cangnova.cangjie.extensions.CollectAdditionalSourcesExtension
import cn.cangnova.cangjie.extensions.ProcessSourcesBeforeCompilingExtension
import cn.cangnova.cangjie.lang.CangJieFileType
import cn.cangnova.cangjie.lang.declarations.CangJieDeclarationsFileType
import cn.cangnova.cangjie.metadata.deserialization.BinaryVersion
import cn.cangnova.cangjie.parsing.CangJieParserDefinition
import cn.cangnova.cangjie.psi.CangJiePsiFacade
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.psi.DefaultCangJiePsiFacade
import cn.cangnova.cangjie.resolve.lazy.declarations.CliDeclarationProviderFactoryService
import cn.cangnova.cangjie.resolve.lazy.declarations.DeclarationProviderFactoryService
import cn.cangnova.cangjie.serialization.builtins.languageVersionSettings
import java.io.File

enum class EnvironmentConfigFiles {

    NATIVE_CONFIG_FILES,

}


sealed interface CangJieCoreApplicationEnvironmentMode {
    object Production : CangJieCoreApplicationEnvironmentMode

    object UnitTest : CangJieCoreApplicationEnvironmentMode

    companion object {
        fun fromUnitTestModeFlag(isUnitTestMode: Boolean): CangJieCoreApplicationEnvironmentMode =
            if (isUnitTestMode) UnitTest else Production
    }
}

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


fun setupIdeaStandaloneExecution() = IdeaStandaloneExecutionSetup.doSetup()

object IdeaStandaloneExecutionSetup {
    private val LOG: Logger = Logger.getInstance(IdeaStandaloneExecutionSetup::class.java)
    private const val FALLBACK_IDEA_BUILD_NUMBER = "999.SNAPSHOT"

    private fun checkInHeadlessMode() {
        // if  application is null it means that we are in progress of set-up application environment i.e. we are not in the running IDEA
        val application = ApplicationManager.getApplication() ?: return
        if (!application.isHeadlessEnvironment) {
            LOG.error(Throwable("IdeaStandaloneExecutionSetup should be called only in headless environment"))
        }
    }

    /**
     * A workaround for the problem introduced in https://github.com/JetBrains/intellij-community/commit/58409581337c8d0a74a748977843fd1a80adf1c8
     *
     * e: java.lang.ExceptionInInitializerError
     * 	 at cn.cangnova.cangjie.com.intellij.ide.plugins.PluginEnabler.<clinit>(PluginEnabler.java:22)
     *   ...
     * Caused by: java.lang.RuntimeException: Could not find installation home path. Please reinstall the software.
     *   at cn.cangnova.cangjie.com.intellij.openapi.application.PathManager.getHomePath(PathManager.java:98)
     *   ...
     */
    private fun workaroundEarlyAccessRegistryQueryProblem() {
        val key = "idea.config.path"
        if (System.getProperty(key) == null) {
            System.setProperty(key, "some/non/existent/path")
        }
    }

    fun doSetup() {
        checkInHeadlessMode()

        System.getProperties().setProperty("project.structure.add.tools.jar.to.new.jdk", "false")
        System.getProperties().setProperty("psi.track.invalidation", "true")
        System.getProperties().setProperty("psi.incremental.reparse.depth.limit", "1000")
        System.getProperties().setProperty("psi.sleep.in.validity.check", "false")
        System.getProperties().setProperty("ide.hide.excluded.files", "false")
        System.getProperties().setProperty("ast.loading.filter", "false")
        System.getProperties().setProperty("idea.ignore.disabled.plugins", "true")
        System.getProperties().setProperty("platform.random.idempotence.check.rate", "1000")
        workaroundEarlyAccessRegistryQueryProblem()
        // Setting the build number explicitly avoids the command-line compiler
        // reading /tmp/build.txt in an attempt to get a build number from there.
        // See intellij platform PluginManagerCore.getBuildNumber.
        System.getProperties().setProperty("idea.plugins.compatible.build", FALLBACK_IDEA_BUILD_NUMBER)
    }

}

internal object IdeaExtensionPoints

internal val CangJieCoreEnvironment.destDir: File?
    get() = configuration.get(CLIConfigurationKeys.METADATA_DESTINATION_DIRECTORY)

class CangJieCoreApplicationEnvironment private constructor(
    parentDisposable: Disposable,
    environmentMode: CangJieCoreApplicationEnvironmentMode,
) : CoreApplicationEnvironment(parentDisposable, environmentMode == CangJieCoreApplicationEnvironmentMode.UnitTest) {
    fun idleCleanup() {

    }

    companion object {
        private fun registerExtensionPoints() {
//            registerApplicationExtensionPoint(
//                DynamicBundle.LanguageBundleEP.EP_NAME,
//                DynamicBundle.LanguageBundleEP::class.java
//            )
            registerApplicationExtensionPoint(FileContextProvider.EP_NAME, FileContextProvider::class.java)
//            registerApplicationExtensionPoint(MetaDataContributor.EP_NAME, MetaDataContributor::class.java)
            registerApplicationExtensionPoint(ContainerProvider.EP_NAME, ContainerProvider::class.java)
            registerApplicationExtensionPoint(MetaLanguage.EP_NAME, MetaLanguage::class.java)
            registerApplicationExtensionPoint(
                SmartPointerAnchorProvider.EP_NAME,
                SmartPointerAnchorProvider::class.java
            )

        }

        fun create(
            parentDisposable: Disposable,
            environmentMode: CangJieCoreApplicationEnvironmentMode,
        ): CangJieCoreApplicationEnvironment {
            val environment = CangJieCoreApplicationEnvironment(parentDisposable, environmentMode)
            registerExtensionPoints()
            return environment
        }
    }
}

class CangJieCoreEnvironment private constructor(


    val configuration: CompilerConfiguration,
    configFiles: EnvironmentConfigFiles,
    projectEnvironment: ProjectEnvironment? = null,
    _project: Project? = null
) {

    companion object {
        private val LOG = Logger.getInstance(CangJieCoreEnvironment::class.java)

        @PublishedApi
        internal val APPLICATION_LOCK = Object()
        private var ourApplicationEnvironment: CangJieCoreApplicationEnvironment? = null
        private var ourProjectCount = 0

        // made public for Upsource
        @Suppress("MemberVisibilityCanBePrivate")
        @JvmStatic
        fun registerApplicationServices(applicationEnvironment: CangJieCoreApplicationEnvironment) {
            with(applicationEnvironment) {
                registerFileType(CangJieFileType.INSTANCE, "cj")
                registerFileType(CangJieDeclarationsFileType, "cjd")

                registerParserDefinition(CangJieParserDefinition())
//                application.registerService(CangJieBinaryClassCache::class.java, CangJieBinaryClassCache())
//                application.registerService(JavaClassSupers::class.java, JavaClassSupersImpl::class.java)
                application.registerService(TransactionGuard::class.java, TransactionGuardImpl::class.java)
//                application.registerService(VirtualFileSetFactory::class.java, getCompactVirtualFileSetFactory())
//                application.registerService(InternalPersistentJavaLanguageLevelReaderService::class.java, InternalPersistentJavaLanguageLevelReaderService.DefaultImpl())
            }
        }

        fun getOrCreateApplicationEnvironmentForProduction(
            projectDisposable: Disposable,
            configuration: CompilerConfiguration,
        ): CangJieCoreApplicationEnvironment = getOrCreateApplicationEnvironment(
            projectDisposable,
            configuration,
            CangJieCoreApplicationEnvironmentMode.Production,
        )

        private fun createApplicationEnvironment(
            parentDisposable: Disposable,
            configuration: CompilerConfiguration,
            environmentMode: CangJieCoreApplicationEnvironmentMode,
        ): CangJieCoreApplicationEnvironment {
            val applicationEnvironment = CangJieCoreApplicationEnvironment.create(parentDisposable, environmentMode)

//            registerApplicationExtensionPointsAndExtensionsFrom(configuration, "extensions/compiler.xml")
//
//            registerApplicationServicesForCLI(applicationEnvironment)
            registerApplicationServices(applicationEnvironment)

            return applicationEnvironment
        }

        /**
         * This method is also used in Gradle after configuration phase finished.
         */
        @JvmStatic
        fun disposeApplicationEnvironment() {
            synchronized(APPLICATION_LOCK) {
                val environment = ourApplicationEnvironment ?: return
                ourApplicationEnvironment = null
                Disposer.dispose(environment.parentDisposable)
                resetApplicationManager(environment.application)
                ZipHandler.clearFileAccessorCache()
            }
        }


        @JvmStatic
        fun resetApplicationManager(applicationToReset: Application? = null) {
            val currentApplication = ApplicationManager.getApplication() ?: return
            if (applicationToReset != null && applicationToReset != currentApplication) {
                return
            }

            try {
                val ourApplicationField = ApplicationManager::class.java.getDeclaredField("ourApplication")
                ourApplicationField.isAccessible = true
                ourApplicationField.set(null, null)
            } catch (exception: Exception) {
                // Resetting the application manager is not critical in a production context. If the reflective access fails, we shouldn't
                // expose the user to the failure.
                if (currentApplication.isUnitTestMode) {
                    throw exception
                }
            }
        }

        fun getOrCreateApplicationEnvironment(
            projectDisposable: Disposable,
            configuration: CompilerConfiguration,
            environmentMode: CangJieCoreApplicationEnvironmentMode,
        ): CangJieCoreApplicationEnvironment {
            synchronized(APPLICATION_LOCK) {
                if (ourApplicationEnvironment == null) {
                    val disposable = Disposer.newDisposable("Disposable for the CangJieCoreApplicationEnvironment")
                    ourApplicationEnvironment =
                        createApplicationEnvironment(
                            disposable,
                            configuration,
                            environmentMode,
                        )
                    ourProjectCount = 0
                    Disposer.register(disposable, Disposable {
                        synchronized(APPLICATION_LOCK) {
                            ourApplicationEnvironment = null
                        }
                    })
                }
                try {
                    val disposeAppEnv =
                        CompilerSystemProperties.CANGJIE_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY.value.toBooleanLenient() != true
                    // Disposer uses identity of passed object to deduplicate registered disposables
                    // We should everytime pass new instance to avoid un-registering from previous one
                    @Suppress("ObjectLiteralToLambda")
                    Disposer.register(projectDisposable, object : Disposable {
                        override fun dispose() {
                            synchronized(APPLICATION_LOCK) {
                                // Build-systems may run many instances of the compiler in parallel
                                // All projects share the same ApplicationEnvironment, and when the last project is disposed,
                                // the ApplicationEnvironment is disposed as well
                                if (--ourProjectCount <= 0) {
                                    // Do not use this property unless you sure need it, causes Application to MEMORY LEAK
                                    // Only valid use-case is when Application should be cached to avoid
                                    // initialization costs
                                    if (disposeAppEnv) {
                                        disposeApplicationEnvironment()
                                    } else {
                                        ourApplicationEnvironment?.idleCleanup()
                                    }
                                }
                            }
                        }
                    })
                } finally {
                    ourProjectCount++
                }

                return ourApplicationEnvironment!!
            }
        }

        @JvmStatic
        fun createForProduction(
            projectDisposable: Disposable,
            configuration: CompilerConfiguration,
            configFiles: EnvironmentConfigFiles,
            project: Project? = null
        ): CangJieCoreEnvironment {


            if (project == null) {
                setupIdeaStandaloneExecution()
                val appEnv = getOrCreateApplicationEnvironmentForProduction(projectDisposable, configuration)
                val projectEnv = ProjectEnvironment(projectDisposable, appEnv, configuration)
                val environment = CangJieCoreEnvironment(configuration, configFiles, projectEnv, project)
                return environment
            } else {
                val environment = CangJieCoreEnvironment(configuration, configFiles, null, project)
                return environment

            }


        }

        @JvmStatic
        fun registerProjectServices(project: MockProject) {
            with(project) {

                registerService(CangJiePsiFacade::class.java, DefaultCangJiePsiFacade(this))
            }
        }

        @JvmStatic
        fun ProjectEnvironment.configureProjectEnvironment(
            configuration: CompilerConfiguration,
            configFiles: EnvironmentConfigFiles
        ) {
            PersistentFSConstants::class.java.getDeclaredField("ourMaxIntellisenseFileSize")
                .apply { isAccessible = true }
                .setInt(null, FileUtilRt.LARGE_FOR_CONTENT_LOADING)

            registerExtensionsFromPlugins(configuration)
            // otherwise consider that project environment is properly configured before passing to the environment
            // TODO: consider some asserts to check important extension points


            registerProjectServices(project)

            for (extension in CompilerConfigurationExtension.getInstances(project)) {
                extension.updateConfiguration(configuration)
            }
        }

    }

    val project = _project ?: projectEnvironment?.project ?: error("Project and projectEnvironment cannot be all null")

    private val sourceFiles = mutableListOf<CjFile>()

    init {

        projectEnvironment?.configureProjectEnvironment(configuration, configFiles)

        if (project is MockProject) {
            project.registerService(
                DeclarationProviderFactoryService::class.java,
                CliDeclarationProviderFactoryService(sourceFiles)
            )
            collectAdditionalSources(project)

        }


        sourceFiles += createSourceFilesFromSourceRoots(
            configuration, project,
            getSourceRootsCheckingForDuplicates(
                configuration,
                configuration[CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY]
            )
        )

        sourceFiles.sortBy { it.virtualFile.path }
//        val messageCollector = configuration.get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
//        val contentRoots = configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS)


    }

    private fun collectAdditionalSources(project: MockProject) {
        var unprocessedSources: Collection<CjFile> = sourceFiles
        val processedSources = HashSet<CjFile>()
        val processedSourcesByExtension = HashMap<CollectAdditionalSourcesExtension, Collection<CjFile>>()
        // repeat feeding extensions with sources while new sources a being added
        var sourceCollectionIterations = 0
        while (unprocessedSources.isNotEmpty()) {
            if (sourceCollectionIterations++ > 10) { // TODO: consider using some appropriate global constant
                throw IllegalStateException("Unable to collect additional sources in reasonable number of iterations")
            }
            processedSources.addAll(unprocessedSources)
            val allNewSources = ArrayList<CjFile>()
            for (extension in CollectAdditionalSourcesExtension.getInstances(project)) {
                // do not feed the extension with the sources it returned on the previous iteration
                val sourcesToProcess = unprocessedSources - (processedSourcesByExtension[extension] ?: emptyList())
                val newSources =
                    extension.collectAdditionalSourcesAndUpdateConfiguration(sourcesToProcess, configuration, project)
                if (newSources.isNotEmpty()) {
                    allNewSources.addAll(newSources)
                    processedSourcesByExtension[extension] = newSources
                }
            }
            unprocessedSources = allNewSources.filterNot { processedSources.contains(it) }.distinct()
            sourceFiles += unprocessedSources
        }
    }

    fun getSourceFiles(): List<CjFile> =
        ProcessSourcesBeforeCompilingExtension.getInstances(project)
            .fold(sourceFiles as Collection<CjFile>) { files, extension ->
                extension.processSources(files, configuration)
            }.toList()

    fun addCangJieSourceRoots(rootDirs: List<File>) {
        val roots = rootDirs.map { CangJieSourceRoot(it.absolutePath, isCommon = false, hmppModuleName = null) }
        sourceFiles += createSourceFilesFromSourceRoots(configuration, project, roots).toSet() - sourceFiles
    }

    fun createPackagePartProvider(scope: GlobalSearchScope): CangJiePackagePartProvider {
        return CangJiePackagePartProvider(configuration.languageVersionSettings, scope).apply {
//            addRoots(initialRoots, configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY))
//            packagePartProviders += this
        }
    }

    class ProjectEnvironment(
        disposable: Disposable,
        applicationEnvironment: CoreApplicationEnvironment,
        configuration: CompilerConfiguration
    ) : CoreProjectEnvironment(disposable, applicationEnvironment) {


        private var extensionRegistered = false

        override fun preregisterServices() {
//            registerProjectExtensionPoints(project.extensionArea)
        }

        internal fun registerExtensionsFromPlugins(project: MockProject, configuration: CompilerConfiguration) {

        }

        fun registerExtensionsFromPlugins(configuration: CompilerConfiguration) {
            if (!extensionRegistered) {
//                registerPluginExtensionPoints(project)
                registerExtensionsFromPlugins(project, configuration)
                extensionRegistered = true
            }
        }


    }

}
