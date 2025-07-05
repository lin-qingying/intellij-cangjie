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

package cn.cangnova.cangjie.serialization.builtins

import com.intellij.openapi.project.Project
import cn.cangnova.cangjie.resolve.AnalysisResult
import cn.cangnova.cangjie.resolve.AnalyzerWithCompilerReport
import cn.cangnova.cangjie.resolve.CommonResolverForModuleFactory
import cn.cangnova.cangjie.descriptors.ModuleInfo
import cn.cangnova.cangjie.builtins.StandardNames
import cn.cangnova.cangjie.config.*
import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.impl.EmptyPackageFragmentDescriptor
import cn.cangnova.cangjie.descriptors.impl.ModuleDescriptorImpl
import cn.cangnova.cangjie.metadata.ProtoBuf
import cn.cangnova.cangjie.metadata.builtins.BuiltInsBinaryVersion
import cn.cangnova.cangjie.name.ClassId
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.platform.CommonPlatforms
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.CompilerEnvironment
import cn.cangnova.cangjie.resolve.DescriptorUtils
import cn.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import cn.cangnova.cangjie.serialization.CangJieSerializerExtensionBase
import cn.cangnova.cangjie.serialization.DescriptorSerializer
import cn.cangnova.cangjie.serialization.MetadataSerializerExtension
import cn.cangnova.cangjie.serialization.deserialization.BuiltInSerializerProtocol
import cn.cangnova.cangjie.serialization.deserialization.CangJieBuiltInClassDescriptorFactory
import cn.cangnova.cangjie.storage.LockBasedStorageManager
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File

data class CommonAnalysisResult(val moduleDescriptor: ModuleDescriptor, val bindingContext: BindingContext)

class CangJieBuiltInsSerializer(
    configuration: CompilerConfiguration,
    environment: CangJieCoreEnvironment,
  val  dependOnOldBuiltIns: Boolean
) : AbstractMetadataSerializer<CommonAnalysisResult>(configuration, environment,   BuiltInsBinaryVersion.INSTANCE) {
    protected var totalSize = 0
    protected var totalFiles = 0



    override fun analyze(): CommonAnalysisResult? {
        return runCommonAnalysisForSerialization(environment, dependOnOldBuiltIns, dependencyContainerFactory = { null })

    }

    protected inner class PackageSerializer(
        private val classes: Collection<DeclarationDescriptor>,
        private val members: Collection<DeclarationDescriptor>,
        private val packageFqName: FqName,
        private val destFile: File,
        private val languageVersionSettings: LanguageVersionSettings,
        private val project: Project? = null
    ) {
        private val proto = ProtoBuf.PackageFragment.newBuilder()
        private val extension = createSerializerExtension()
        protected open fun createSerializerExtension(): CangJieSerializerExtensionBase = MetadataSerializerExtension(metadataVersion)

        fun run() {
            val serializer = DescriptorSerializer.createTopLevel(extension, languageVersionSettings, project)
            serializeClasses(classes, serializer, project)
            serializeMembers(members, serializer)
            serializeStringTable()
            serializeBuiltInsFile()
        }

        private fun serializeClasses(classes: Collection<DeclarationDescriptor>, parentSerializer: DescriptorSerializer, project: Project?) {
            for (descriptor in DescriptorSerializer.sort(classes)) {
                if (descriptor !is ClassDescriptor || descriptor.kind == ClassKind.ENUM_ENTRY) continue

                val serializer = DescriptorSerializer.create(descriptor, extension, parentSerializer, languageVersionSettings, project)
                serializeClasses(
                    descriptor.unsubstitutedInnerClassesScope.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS),
                    serializer,
                    project
                )

                proto.addClass_(serializer.classProto(descriptor).build())
            }
        }

        private fun serializeMembers(members: Collection<DeclarationDescriptor>, serializer: DescriptorSerializer) {
            proto.`package` = serializer.packagePartProto(packageFqName, members).build()
        }

        private fun serializeStringTable() {
            val (strings, qualifiedNames) = extension.stringTable.buildProto()
            proto.strings = strings
            proto.qualifiedNames = qualifiedNames
        }

        private fun serializeBuiltInsFile() {
            val stream = ByteArrayOutputStream()
            with(DataOutputStream(stream)) {
                val version = extension.metadataVersion.toArray()
                writeInt(version.size)
                version.forEach { writeInt(it) }
            }
            proto.build().writeTo(stream)
            write(stream)
        }

        private fun write(stream: ByteArrayOutputStream) {
            totalSize += stream.size()
            totalFiles++
            assert(!destFile.isDirectory) { "Cannot write because output destination is a directory: $destFile" }
            destFile.parentFile.mkdirs()
            destFile.writeBytes(stream.toByteArray())
        }
    }
    private fun createCloneable(module: ModuleDescriptor): ClassDescriptor {
        val factory = CangJieBuiltInClassDescriptorFactory(LockBasedStorageManager.NO_LOCKS, module) {
            EmptyPackageFragmentDescriptor(module, StandardNames.BUILT_INS_PACKAGE_FQ_NAME)
        }
        return factory.createClass(ClassId.topLevel(StandardNames.FqNames.cloneable.toSafe()))
            ?: error("Could not create kotlin.Cloneable in $module")
    }
    override fun serialize(analysisResult: CommonAnalysisResult, destDir: File): OutputInfo {
        val files = environment.getSourceFiles()
        val module = analysisResult.moduleDescriptor

        destDir.deleteRecursively()
        if (!destDir.mkdirs()) {
            error("Could not make directories: $destDir")
        }

        files.map { it.packageFqName }.toSet().forEach { fqName ->
            val packageView = module.getPackage(fqName)
            PackageSerializer(
                packageView.fragments.flatMap { fragment -> fragment.getMemberScope().getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS) },
//                packageView.memberScope.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS) + createCloneable(module),
                packageView.fragments.flatMap { fragment -> DescriptorUtils.getAllDescriptors(fragment.getMemberScope()) },
                packageView.fqName,
                File(destDir, BuiltInSerializerProtocol.getBuiltInsFilePath(packageView.fqName)),
                environment.configuration.languageVersionSettings,
            ).run()
        }
        return OutputInfo(totalSize, totalFiles)
    }


}
private data class AnalysisResultWithHasErrors(val result: AnalysisResult, val hasErrors: Boolean)

interface CommonDependenciesContainer {
    val moduleInfos: List<ModuleInfo>

    fun moduleDescriptorForModuleInfo(moduleInfo: ModuleInfo): ModuleDescriptor

    fun registerDependencyForAllModules(
        moduleInfo: ModuleInfo,
        descriptorForModule: ModuleDescriptorImpl
    )

    fun packageFragmentProviderForModuleInfo(moduleInfo: ModuleInfo): PackageFragmentProvider?
    val friendModuleInfos: List<ModuleInfo>
    val refinesModuleInfos: List<ModuleInfo>
}
internal fun runCommonAnalysisForSerialization(
    environment: CangJieCoreEnvironment,
    dependOnBuiltins: Boolean,
    dependencyContainerFactory: () -> CommonDependenciesContainer?
): CommonAnalysisResult? {
    val performanceManager = environment.configuration.getNotNull(CLIConfigurationKeys.PERF_MANAGER)

    var analysisResultWithHasErrors: AnalysisResultWithHasErrors
    do {
        performanceManager.notifyAnalysisStarted()
        analysisResultWithHasErrors = runCommonAnalysisIteration(environment, dependOnBuiltins, dependencyContainerFactory())
        val result = analysisResultWithHasErrors.result
        if (result is AnalysisResult.RetryWithAdditionalRoots) {
            environment.addCangJieSourceRoots(result.additionalCangJieRoots)
        }
        performanceManager.notifyAnalysisFinished()
    } while (result is AnalysisResult.RetryWithAdditionalRoots)

    val analysisResult = analysisResultWithHasErrors.result
    return if (analysisResult.shouldGenerateCode && !analysisResultWithHasErrors.hasErrors)
        CommonAnalysisResult(analysisResult.moduleDescriptor, analysisResult.bindingContext)
    else
        null
}
var CompilerConfiguration.languageVersionSettings: LanguageVersionSettings
    get() = get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, LanguageVersionSettingsImpl.DEFAULT)
    set(value) = put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, value)

private fun runCommonAnalysisIteration(
    environment: CangJieCoreEnvironment,
    dependOnBuiltins: Boolean,
    dependencyContainer: CommonDependenciesContainer?
): AnalysisResultWithHasErrors {
    val configuration = environment.configuration
    val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
    val files = environment.getSourceFiles()
    val moduleName = Name.special("<${configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)}>")

    val analyzer = AnalyzerWithCompilerReport(
        messageCollector,
        configuration.languageVersionSettings,
        configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    )

    analyzer.analyzeAndReport(files) {
        CommonResolverForModuleFactory.analyzeFiles(
            files, moduleName, dependOnBuiltins, configuration.languageVersionSettings,
            CommonPlatforms.defaultCommonPlatform, CompilerEnvironment,
            dependenciesContainer = dependencyContainer
        ) { content ->
            environment.createPackagePartProvider(content.moduleContentScope)
        }
    }

    return AnalysisResultWithHasErrors(analyzer.analysisResult, analyzer.hasErrors())
}
