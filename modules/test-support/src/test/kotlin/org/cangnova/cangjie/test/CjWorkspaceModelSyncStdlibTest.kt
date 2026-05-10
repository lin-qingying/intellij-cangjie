package org.cangnova.cangjie.test

import com.intellij.openapi.components.service
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.InheritedSdkDependency
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryRoot
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.testFramework.registerServiceInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.cangnova.cangjie.projectStructure.CaSourceModuleKind
import org.cangnova.cangjie.projectStructure.CangJieProjectStructureProviderService
import org.cangnova.cangjie.projectStructure.toCaSourceModule
import org.cangnova.cangjie.project.model.CjDependency
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjPackage
import org.cangnova.cangjie.project.model.CjPackageMetadata
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.model.CjSourceSet
import org.cangnova.cangjie.project.model.CjVersion
import org.cangnova.cangjie.project.model.CjWorkspace
import org.cangnova.cangjie.project.model.PackageId
import org.cangnova.cangjie.project.model.ResolvedDependency
import org.cangnova.cangjie.project.model.ResolvedGraph
import org.cangnova.cangjie.project.model.SourceId
import org.cangnova.cangjie.project.model.cjSdk
import org.cangnova.cangjie.project.service.CjDependencyService
import org.cangnova.cangjie.project.workspace.CjWorkspaceModelSync
import org.cangnova.cangjie.toolchain.api.CANGJIE_PROJECT_SDK_CONFIG_TOPIC
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfigChangedEvent
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfigListener
import org.cangnova.cangjie.toolchain.api.CjSdk
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CjWorkspaceModelSyncStdlibTest : AbstractCangJieMultiModuleTest() {
    override val runTestInDispatchThread: Boolean = false

    override fun setUp() {
        super.setUp()
        registerToolchain("CangJie Test SDK", "std.cjo", "std/std.core.cjo", "std/std.objectpool.cjo")
    }

    fun testWorkspaceStdlibIsRegisteredAsSingleProjectLibrary() {
        project.registerServiceInstance(CjDependencyService::class.java, TestDependencyService(project))

        val cjProject = createWorkspaceProject()

        runBlocking(Dispatchers.Default) {
            project.service<CjWorkspaceModelSync>().syncProject(cjProject)
        }

        val sdk = requireNotNull(project.cjSdk)
        val snapshot = project.workspaceModel.currentSnapshot
        val stdlibLibraries = snapshot.entities(LibraryEntity::class.java)
            .filter(::isStdlibProjectLibrary)
            .toList()

        assertEquals(1, stdlibLibraries.size)
        assertTrue(stdlibLibraries.single().roots.any { root ->
            root.url.url.replace('\\', '/').contains(sdk.stdlibPath.toString().replace('\\', '/'))
        })

        val appModuleEntity = snapshot.entities(ModuleEntity::class.java).first { it.name == "sync-workspace.app" }
        val libModuleEntity = snapshot.entities(ModuleEntity::class.java).first { it.name == "sync-workspace.lib" }
        val appStdlibDependency = appModuleEntity.dependencies.filterIsInstance<LibraryDependency>().single(::isStdlibDependency)
        val libStdlibDependency = libModuleEntity.dependencies.filterIsInstance<LibraryDependency>().single(::isStdlibDependency)

        assertEquals(appStdlibDependency.library, libStdlibDependency.library)
    }

    fun testWorkspaceStdlibProjectLibraryIsVisibleThroughOpenApiLibraryTable() {
        project.registerServiceInstance(CjDependencyService::class.java, TestDependencyService(project))

        val cjProject = createWorkspaceProject()

        runBlocking(Dispatchers.Default) {
            project.service<CjWorkspaceModelSync>().syncProject(cjProject)
        }

        val stdlibName = currentStdlibLibraryName()
        val openApiLibrary = LibraryTablesRegistrar.getInstance()
            .getLibraryTable(project)
            .getLibraryByName(stdlibName)

        assertNotNull(openApiLibrary, "stdlib project library must be bridged into Project Library Table")
        assertTrue(openApiLibrary.getFiles(com.intellij.openapi.roots.OrderRootType.CLASSES).isNotEmpty())
    }

    fun testStaleStdlibProjectLibraryIsCollapsedToCurrentSdkLibrary() {
        project.registerServiceInstance(CjDependencyService::class.java, TestDependencyService(project))

        val cjProject = createWorkspaceProject()
        val workspaceSync = project.service<CjWorkspaceModelSync>()

        runBlocking(Dispatchers.Default) {
            workspaceSync.syncProject(cjProject)
        }

        injectStaleStdlibProjectLibrary()

        runBlocking(Dispatchers.Default) {
            workspaceSync.syncProject(cjProject)
        }

        val snapshot = project.workspaceModel.currentSnapshot
        val stdlibLibraries = snapshot.entities(LibraryEntity::class.java)
            .filter(::isStdlibProjectLibrary)
            .toList()

        assertEquals(1, stdlibLibraries.size)
        assertEquals(currentStdlibLibraryName(), stdlibLibraries.single().name)
    }

    fun testRemovingSdkClearsStdlibLibraryAndDependencies() {
        project.registerServiceInstance(CjDependencyService::class.java, TestDependencyService(project))

        val cjProject = createWorkspaceProject()
        val workspaceSync = project.service<CjWorkspaceModelSync>()

        runBlocking(Dispatchers.Default) {
            workspaceSync.syncProject(cjProject)
        }

        CjProjectSdkConfig.getInstance(project).setProjectSdkId(null)

        runBlocking(Dispatchers.Default) {
            workspaceSync.syncProject(cjProject)
        }

        val snapshot = project.workspaceModel.currentSnapshot
        val stdlibLibraries = snapshot.entities(LibraryEntity::class.java)
            .filter(::isStdlibProjectLibrary)
            .toList()

        assertTrue(stdlibLibraries.isEmpty())

        val moduleLibraryDependencies = snapshot.entities(ModuleEntity::class.java)
            .filter { it.name == "sync-workspace.app" || it.name == "sync-workspace.lib" }
            .flatMap { entity -> entity.dependencies.filterIsInstance<LibraryDependency>() }

        assertTrue(moduleLibraryDependencies.none(::isStdlibDependency))
    }

    fun testSwitchingSdkRefreshesSharedStdlibLibraryRoots() {
        project.registerServiceInstance(CjDependencyService::class.java, TestDependencyService(project))

        val cjProject = createWorkspaceProject()
        val workspaceSync = project.service<CjWorkspaceModelSync>()

        runBlocking(Dispatchers.Default) {
            workspaceSync.syncProject(cjProject)
        }

        val firstRoots = currentStdlibRoots()
        val switchedSdk = registerToolchain("switched-sdk", "std.cjo", "std/std.core.cjo", "std/std.objectpool.cjo")

        CjProjectSdkConfig.getInstance(project).setProjectSdkId(switchedSdk.id)

        runBlocking(Dispatchers.Default) {
            workspaceSync.syncProject(cjProject)
        }

        val stdlibLibrary = currentStdlibLibrary()
        val refreshedRoots = stdlibLibrary.roots.map { it.url.url }.toSet()

        assertEquals(1, project.workspaceModel.currentSnapshot.entities(LibraryEntity::class.java).count(::isStdlibProjectLibrary))
        assertTrue(refreshedRoots != firstRoots)
        assertTrue(refreshedRoots.any { it.replace('\\', '/').contains(switchedSdk.stdlibPath.toString().replace('\\', '/')) })
    }

    fun testUnusableSdkClearsStdlibLibraryAndDependencies() {
        project.registerServiceInstance(CjDependencyService::class.java, TestDependencyService(project))

        val cjProject = createWorkspaceProject()
        val workspaceSync = project.service<CjWorkspaceModelSync>()

        runBlocking(Dispatchers.Default) {
            workspaceSync.syncProject(cjProject)
        }

        val unusableSdkHome = Files.createTempDirectory("cangjie-plugin-broken-sdk").also { sdkHome ->
            Files.createDirectories(sdkHome.resolve("bin"))
        }
        val unusableSdk = CangJiePluginTestCaseBase.registerProjectToolchain(
            project = project,
            parentDisposable = testRootDisposable,
            sdkHome = unusableSdkHome,
            displayName = "Broken Test SDK",
        )

        CjProjectSdkConfig.getInstance(project).setProjectSdkId(unusableSdk.id)

        runBlocking(Dispatchers.Default) {
            workspaceSync.syncProject(cjProject)
        }

        val snapshot = project.workspaceModel.currentSnapshot
        val stdlibLibraries = snapshot.entities(LibraryEntity::class.java)
            .filter(::isStdlibProjectLibrary)
            .toList()

        assertTrue(stdlibLibraries.isEmpty())

        val moduleLibraryDependencies = snapshot.entities(ModuleEntity::class.java)
            .filter { it.name == "sync-workspace.app" || it.name == "sync-workspace.lib" }
            .flatMap { entity -> entity.dependencies.filterIsInstance<LibraryDependency>() }

        assertTrue(moduleLibraryDependencies.none(::isStdlibDependency))
    }

    fun testDanglingStdlibDependencyDoesNotBreakProjectStructureAndIsRebuilt() {
        project.registerServiceInstance(CjDependencyService::class.java, TestDependencyService(project))

        val cjProject = createWorkspaceProject()
        val workspaceSync = project.service<CjWorkspaceModelSync>()

        runBlocking(Dispatchers.Default) {
            workspaceSync.syncProject(cjProject)
        }

        injectDanglingStdlibDependencyOnAppModule()

        val danglingAppModule = project.workspaceModel.currentSnapshot.entities(ModuleEntity::class.java)
            .first { it.name == "sync-workspace.app" }

        runProjectStructureReadAction {
            val sourceModule = requireNotNull(danglingAppModule.toCaSourceModule(project, CaSourceModuleKind.PRODUCTION))
            sourceModule.directRegularDependencies.toList()
        }

        runBlocking(Dispatchers.Default) {
            workspaceSync.syncProject(cjProject)
        }

        val refreshedAppModule = project.workspaceModel.currentSnapshot.entities(ModuleEntity::class.java)
            .first { it.name == "sync-workspace.app" }
        val stdlibDependencies = refreshedAppModule.dependencies
            .filterIsInstance<LibraryDependency>()
            .filter(::isStdlibDependency)

        assertEquals(1, stdlibDependencies.size)
        assertEquals(currentStdlibLibraryName(), stdlibDependencies.single().library.name)
    }

    fun testProjectSdkConfigPublishesChangeEventsOnlyWhenSdkIdChanges() {
        val sdkConfig = CjProjectSdkConfig.getInstance(project)
        val initialSdkId = requireNotNull(sdkConfig.getProjectSdkId())
        val events = mutableListOf<CjProjectSdkConfigChangedEvent>()

        project.messageBus.connect(testRootDisposable).subscribe(
            CANGJIE_PROJECT_SDK_CONFIG_TOPIC,
            object : CjProjectSdkConfigListener {
                override fun projectSdkChanged(event: CjProjectSdkConfigChangedEvent) {
                    events += event
                }
            },
        )

        val switchedSdk = registerToolchain("event-sdk", "std.cjo")
        events.clear()

        sdkConfig.setProjectSdkId(initialSdkId)
        sdkConfig.setProjectSdkId(initialSdkId)
        sdkConfig.setProjectSdkId(null)

        assertEquals(
            listOf(
                CjProjectSdkConfigChangedEvent(switchedSdk.id, initialSdkId),
                CjProjectSdkConfigChangedEvent(initialSdkId, null),
            ),
            events,
        )
    }

    private fun createWorkspaceProject(): TestCjProject {
        val workspaceRoot = createLocalDirectory(Files.createTempDirectory("cangjie-sync-workspace"))
        val projectModel = TestCjProject(
            name = "sync-workspace",
            rootDir = workspaceRoot,
            intellijProject = project,
        )

        val appModule = createWorkspaceModule(projectModel, "app")
        val libModule = createWorkspaceModule(projectModel, "lib")
        val workspace = TestCjWorkspace(
            name = projectModel.name,
            rootDir = workspaceRoot,
            project = projectModel,
            modules = listOf(appModule, libModule),
        )

        projectModel.workspaceValue = workspace
        return projectModel
    }

    private fun createWorkspaceModule(projectModel: TestCjProject, moduleName: String): TestCjModule {
        val moduleRootPath = projectModel.rootDir.toNioPath().resolve(moduleName)
        val moduleRoot = createLocalDirectory(moduleRootPath)
        val sourceRoot = createLocalDirectory(moduleRootPath.resolve("src"))
        val module = TestCjModule(
            name = moduleName,
            rootDir = moduleRoot,
            project = projectModel,
            sourceSets = listOf(TestCjSourceSet(name = "main", sourceRoots = listOf(sourceRoot))),
        )
        module.declaredDependencies = listOf(
            CjDependency.Stdlib(
                name = "stdlib",
                versionReq = org.cangnova.cangjie.project.model.VersionRequirement.Exact(CjVersion("0.0.0")),
                sourceModule = module,
            )
        )
        return module
    }

    private fun isStdlibProjectLibrary(library: LibraryEntity): Boolean {
        return library.tableId == LibraryTableId.ProjectLibraryTableId &&
            library.name.startsWith("stdlib:") &&
            library.name.endsWith("@stdlib")
    }

    private fun isStdlibDependency(dependency: LibraryDependency): Boolean {
        return dependency.library.tableId == LibraryTableId.ProjectLibraryTableId &&
            dependency.library.presentableName.startsWith("stdlib:")
    }

    private fun currentStdlibLibrary(): LibraryEntity {
        return project.workspaceModel.currentSnapshot.entities(LibraryEntity::class.java)
            .first(::isStdlibProjectLibrary)
    }

    private fun currentStdlibLibraryName(): String = requireNotNull(project.cjSdk).let { sdk ->
        PackageId(
            name = "stdlib",
            version = CjVersion(sdk.version?.semver?.parsedVersion),
            sourceId = SourceId.Stdlib,
        ).toString()
    }

    private fun currentStdlibRoots(): Set<String> {
        return currentStdlibLibrary().roots.map { it.url.url }.toSet()
    }

    private fun injectStaleStdlibProjectLibrary() {
        val currentLibrary = currentStdlibLibrary()
        val urlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
        val staleRoot = createLocalDirectory(Files.createTempDirectory("cangjie-sync-stale-stdlib-root"))

        runBlocking(Dispatchers.Default) {
            project.workspaceModel.update("Inject stale stdlib project library") { builder ->
                builder.addEntity(
                    LibraryEntity(
                        name = "stdlib:9.9.9@stdlib",
                        tableId = LibraryTableId.ProjectLibraryTableId,
                        roots = listOf(LibraryRoot(urlManager.getOrCreateFromUrl(staleRoot.url), LibraryRootTypeId.COMPILED)),
                        entitySource = currentLibrary.entitySource,
                    )
                )
            }
        }
    }

    private fun injectDanglingStdlibDependencyOnAppModule() {
        val danglingDependency = LibraryDependency(
            library = LibraryId("stdlib:9.9.9@stdlib", LibraryTableId.ProjectLibraryTableId),
            exported = false,
            scope = DependencyScope.COMPILE,
        )

        runBlocking(Dispatchers.Default) {
            project.workspaceModel.update("Inject dangling stdlib dependency") { builder ->
                val appModule = builder.entities(ModuleEntity::class.java)
                    .first { it.name == "sync-workspace.app" }
                val contentRoots = appModule.contentRoots.map { contentRoot ->
                    val sourceRoots = contentRoot.sourceRoots.map { sourceRoot ->
                        SourceRootEntity(
                            url = sourceRoot.url,
                            rootTypeId = sourceRoot.rootTypeId,
                            entitySource = sourceRoot.entitySource,
                        )
                    }

                    ContentRootEntity(
                        url = contentRoot.url,
                        excludedPatterns = contentRoot.excludedPatterns,
                        entitySource = contentRoot.entitySource,
                    ) {
                        this.sourceRoots = sourceRoots.toMutableList()
                    }
                }
                val entitySource = appModule.entitySource

                builder.removeEntity(appModule)
                builder.addEntity(
                    ModuleEntity(
                        name = "sync-workspace.app",
                        dependencies = listOf(ModuleSourceDependency, InheritedSdkDependency, danglingDependency),
                        entitySource = entitySource,
                    ) {
                        this.contentRoots = contentRoots.toMutableList()
                    }
                )
            }
        }

        CangJieProjectStructureProviderService.getInstance(project).incOutOfBlockModificationCount()
    }

    private fun registerToolchain(displayName: String, vararg relativeStdlibPaths: String): CjSdk {
        val sdkHome = CangJiePluginTestCaseBase.createSlimToolchainHome(*relativeStdlibPaths)
        return CangJiePluginTestCaseBase.registerProjectToolchain(
            project = project,
            parentDisposable = testRootDisposable,
            sdkHome = sdkHome,
            displayName = displayName,
        )
    }

    private class TestDependencyService(
        private val project: Project,
    ) : CjDependencyService {
        override suspend fun resolveGraph(root: CjPackage): Result<ResolvedGraph> {
            val rootModule = root as? CjPackage.LocalModule
                ?: error("Expected local module root, but got ${root::class.simpleName}")
            return Result.success(buildResolvedGraph(rootModule))
        }

        override suspend fun resolveSingle(
            dependency: CjDependency,
            enabledFeatures: Set<String>,
        ): Result<CjPackage> {
            return when (dependency) {
                is CjDependency.Stdlib -> resolveStdlibPackage(dependency)?.let(Result.Companion::success)
                    ?: Result.success(
                        CjPackage.Failed(
                            id = PackageId(dependency.name, CjVersion.ZERO, SourceId.Stdlib),
                            errorMessage = "No SDK configured for project",
                        )
                    )

                else -> Result.failure(IllegalStateException("Unsupported dependency in stdlib sync test: ${dependency::class.simpleName}"))
            }
        }

        override fun clearCache() = Unit

        private fun buildResolvedGraph(rootModule: CjPackage.LocalModule): ResolvedGraph {
            val packages = linkedMapOf<PackageId, CjPackage>(rootModule.id to rootModule)
            val dependencies = mutableListOf<ResolvedDependency>()

            rootModule.dependencies.filterIsInstance<CjDependency.Stdlib>().forEach { dependency ->
                val resolvedStdlib = resolveStdlibPackage(dependency) ?: return@forEach
                packages[resolvedStdlib.id] = resolvedStdlib
                dependencies += ResolvedDependency(
                    declaration = dependency,
                    resolvedTo = resolvedStdlib.id,
                    enabledFeatures = emptySet(),
                    scope = dependency.scope,
                )
            }

            return ResolvedGraph(
                packages = packages,
                dependencies = mapOf(rootModule.id to dependencies),
                root = rootModule.id,
                enabledFeatures = emptyMap(),
            )
        }

        private fun resolveStdlibPackage(dependency: CjDependency.Stdlib): CjPackage.Stdlib? {
            val sdk = project.cjSdk ?: return null
            return CjPackage.Stdlib(
                id = PackageId(
                    name = dependency.name,
                    version = CjVersion(sdk.version?.semver?.parsedVersion),
                    sourceId = SourceId.Stdlib,
                ),
                path = sdk.stdlibPath,
            )
        }
    }

    private class TestCjProject(
        override val name: String,
        override val rootDir: VirtualFile,
        override val intellijProject: Project,
    ) : CjProject {
        lateinit var workspaceValue: CjWorkspace

        override val isWorkspace: Boolean = true
        override val module: CjModule? = null
        override val workspace: CjWorkspace
            get() = workspaceValue
        override val isValid: Boolean = true

        override fun refresh(onComplete: (() -> Unit)?) {
            onComplete?.invoke()
        }

        override fun findModule(name: String): CjModule? = workspace.modules.firstOrNull { it.name == name }
    }

    private class TestCjWorkspace(
        override val name: String,
        override val rootDir: VirtualFile,
        override val project: CjProject,
        override val modules: List<CjModule>,
    ) : CjWorkspace {
        override val configFile: VirtualFile? = null
        override val sourceSets: List<CjSourceSet> = emptyList()

        override fun findModule(name: String): CjModule? = modules.firstOrNull { it.name == name }
    }

    private class TestCjModule(
        override val name: String,
        override val rootDir: VirtualFile,
        override val project: CjProject,
        override val sourceSets: List<CjSourceSet>,
    ) : UserDataHolderBase(), CjModule {
        lateinit var declaredDependencies: List<CjDependency>

        override val configFile: VirtualFile? = null
        override val dependencies: List<CjDependency>
            get() = declaredDependencies
        override val metadata: CjPackageMetadata = object : CjPackageMetadata {
            override val name: String = this@TestCjModule.name
            override val group: String? = null
            override val version: CjVersion = CjVersion("0.0.1")
            override val description: String? = null
            override val authors: List<String> = emptyList()
            override val license: String? = null
            override val repositoryUrl: String? = null
            override val dependencies: List<CjDependency>
                get() = declaredDependencies
            override val localPath: java.nio.file.Path = rootDir.toNioPath()
            override val outputType: org.cangnova.cangjie.project.model.CjOutputType? = null
        }
    }

    private class TestCjSourceSet(
        override val name: String,
        override val sourceRoots: List<VirtualFile>,
    ) : CjSourceSet {
        override val resourceRoots: List<VirtualFile> = emptyList()
        override val outputDirectory: List<VirtualFile> = emptyList()
        override val isTest: Boolean = false
    }
}
