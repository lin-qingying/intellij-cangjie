/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.lazy


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
 */



import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.context.GlobalContext
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.incremental.components.LookupTracker
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.calls.components.InferenceSession
import org.cangnova.cangjie.resolve.extensions.SyntheticResolveExtension
import org.cangnova.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
import org.cangnova.cangjie.resolve.lazy.declarations.LazyPackageDescriptor
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyAnnotations
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyAnnotationsContextImpl
import org.cangnova.cangjie.storage.*
import org.cangnova.cangjie.types.WrappedTypeFactory
import org.cangnova.cangjie.types.checker.NewCangJieTypeChecker
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import jakarta.inject.Inject
import org.cangnova.cangjie.ExceptionTracker
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace


/**
 * 解析会话 - 负责延迟解析和描述符创建
 */
class ResolveSession @Deprecated("Only calls from injectors expected") constructor(
    private val project: Project,
    globalContext: GlobalContext,
    private val module: ModuleDescriptor,
    override val declarationProviderFactory: DeclarationProviderFactory,
    delegationTrace: BindingTrace,
    private val cangjieTypeChecker: NewCangJieTypeChecker
) : CangJieCodeAnalyzer, LazyClassContext {

    // 核心组件
    override val storageManager: LazyResolveStorageManager =
        LockBasedLazyResolveStorageManager(globalContext.storageManager)

      val exceptionTracker: ExceptionTracker = globalContext.exceptionTracker

    override val trace: BindingTrace = storageManager.createSafeTrace(delegationTrace)

    private val packages: CacheWithNotNullValues<FqName, LazyPackageDescriptor> =
        storageManager.createCacheWithNotNullValues()

    override val syntheticResolveExtension: SyntheticResolveExtension =
        SyntheticResolveExtension.getInstance(project)

    private val fileAnnotations: MemoizedFunctionToNotNull<CjFile, LazyAnnotations> =
        storageManager.createMemoizedFunction { file ->
            createAnnotations(file, emptyList())
        }

    // 可注入的依赖项 - 使用lateinit和属性委托
    @set:Inject
    lateinit var controlFlowAnalyzer: ControlFlowAnalyzer


    @set:Inject
    lateinit var lazyDeclarationResolver: LazyDeclarationResolver


    @set:Inject
    lateinit var localDescriptorResolver: LocalDescriptorResolver


    @set:Inject
    override lateinit var delegationFilter: DelegationFilter


    @set:Inject
    override lateinit var wrappedTypeFactory: WrappedTypeFactory


    @set:Inject
    override lateinit var descriptorResolver: DescriptorResolver


    @set:Inject
    lateinit var supertypeLoopsResolver: SupertypeLoopChecker


    @set:Inject
    lateinit var extendDescriptorResolver: ExtendDescriptorResolver


    @set:Inject
    override lateinit var functionDescriptorResolver: FunctionDescriptorResolver


    @set:Inject
    lateinit var fileScopeProvider: FileScopeProvider


    @set:Inject
    override lateinit var declarationScopeProvider: DeclarationScopeProvider


    @set:Inject
    override lateinit var lookupTracker: LookupTracker


    @set:Inject
    override lateinit var languageVersionSettings: LanguageVersionSettings


    @set:Inject
    override lateinit var typeResolver: TypeResolver


    @set:Inject
    override lateinit var sealedClassInheritorsProvider: SealedClassInheritorsProvider


    @set:Inject
    override lateinit var overloadChecker: OverloadChecker


    @set:Inject
    override lateinit var overloadResolver: OverloadResolver


    @set:Inject
    lateinit var annotationResolver: AnnotationResolver


    @set:Inject
    override lateinit var enumDescriptorResolver: EnumDescriptorResolver


    override fun getPackageFragmentProvider(): PackageFragmentProvider {
        return object : PackageFragmentProviderOptimized {
            override fun collectPackageFragments(
                fqName: FqName,
                packageFragments: MutableCollection<PackageFragmentDescriptor>
            ) {
                getPackageFragment(fqName)?.let { packageFragments.add(it) }
            }

            override fun isEmpty(fqName: FqName): Boolean =
                declarationProviderFactory.getPackageMemberDeclarationProvider(fqName) == null

            override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> =
                listOfNotNull(getPackageFragment(fqName))

            override fun getSubPackagesOf(
                fqName: FqName,
                nameFilter: (Name) -> Boolean
            ): Collection<FqName> {
                val packageDescriptor = getPackageFragment(fqName) ?: return emptyList()
                return packageDescriptor.declarationProvider.getAllDeclaredSubPackages(nameFilter)
            }
        }

    }
    // PackageFragmentProvider 实现

    // CangJieCodeAnalyzer 实现
    override val moduleDescriptor: ModuleDescriptor
        get() = module

    override val bindingContext: BindingContext
        get() = trace.bindingContext

    override val inferenceSession: InferenceSession?
        get() = null


    // LazyClassContext 实现
    override val cangjieTypeCheckerOfOwnerModule: NewCangJieTypeChecker
        get() = cangjieTypeChecker

    override val supertypeLoopChecker: SupertypeLoopChecker
        get() = supertypeLoopsResolver

    // 公共方法
    fun getFileAnnotations(file: CjFile): Annotations = fileAnnotations(file)

    override fun resolveToDescriptor(declaration: CjDeclaration): DeclarationDescriptor {
        assertValid()

        require(areDescriptorsCreatedForDeclaration(declaration)) {
            "No descriptors are created for declarations of type ${declaration::class.simpleName}. " +
                    "Change the caller accordingly"
        }

        val isLocal = ReadAction.compute<Boolean, Nothing> { CjPsiUtil.isLocal(declaration) }

        return if (isLocal) {
            localDescriptorResolver.resolveLocalDeclaration(declaration)
        } else {
            lazyDeclarationResolver.resolveToDescriptor(declaration)
        }
    }

    override fun getPackageFragmentOrDiagnoseFailure(
        fqName: FqName,
        from: CjFile?
    ): LazyPackageDescriptor {
        return getPackageFragment(fqName)
            ?: run {
                declarationProviderFactory.diagnoseMissingPackageFragment(fqName, from)
                error("diagnoseMissingPackageFragment should throw!")
            }
    }

    override fun getPackageFragment(fqName: FqName): LazyPackageDescriptor? {
        val provider = declarationProviderFactory.getPackageMemberDeclarationProvider(fqName)
            ?: return null

        return packages.computeIfAbsent(fqName) {
            LazyPackageDescriptor(module, fqName, this, provider)
        }
    }

    override fun assertValid() {
        module.assertValid()
    }

    override fun getClassDescriptor(
        typeStatement: CjTypeStatement,
        location: LookupLocation
    ): ClassDescriptor = lazyDeclarationResolver.getClassDescriptor(typeStatement, location)

    override fun getTopLevelClassifierDescriptors(
        fqName: FqName,
        location: LookupLocation
    ): Collection<ClassifierDescriptor> {
        if (fqName.isRoot) return emptyList()

        val provider = declarationProviderFactory.getPackageMemberDeclarationProvider(fqName.parent())
            ?: return emptyList()

        return buildList {
            // 添加类描述符
            provider.getTypeStatementDeclarations(fqName.shortName()).mapNotNullTo(this) { info ->
                getClassDescriptor(info.correspondingClass, location)
            }

            // 添加类型别名描述符
            provider.getTypeAliasDeclarations(fqName.shortName()).mapTo(this) { alias ->
                lazyDeclarationResolver.resolveToDescriptor(alias) as ClassifierDescriptor
            }
        }
    }

    private fun createAnnotations(
        file: CjFile,
        annotationEntries: List<CjAnnotation>
    ): LazyAnnotations {
        val scope = fileScopeProvider.getFileResolutionScope(file)
        val lazyAnnotationContext = LazyAnnotationsContextImpl(
            annotationResolver,
            storageManager,
            trace,
            scope
        )
        return LazyAnnotations(lazyAnnotationContext, annotationEntries)
    }

    companion object {

        fun areDescriptorsCreatedForDeclaration(declaration: CjDeclaration): Boolean =
            true
    }
}