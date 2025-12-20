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

package org.cangnova.cangjie.resolve.caches


import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import org.cangnova.cangjie.FrontendInternals

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.diagnostics.DiagnosticSink
import org.cangnova.cangjie.moduleinfo.IdeaModuleInfo
import org.cangnova.cangjie.moduleinfo.ModuleInfo
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.AnalysisResult
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.resolve.ResolverForProject
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.findModuleDescriptor
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import org.cangnova.cangjie.utils.exceptions.CangJieExceptionWithAttachmentsImpl

private class ResolutionFacadeWithDebugInfo(
    private val delegate: ResolutionFacade,
    private val creationPlace: CreationPlace
) : ResolutionFacade, ResolutionFacadeModuleDescriptorProvider {
    override fun findModuleDescriptor(ideaModuleInfo: IdeaModuleInfo): ModuleDescriptor {
        return delegate.findModuleDescriptor(ideaModuleInfo)
    }

    override val project: Project
        get() = delegate.project

    override fun analyze(element: CjElement, bodyResolveMode: BodyResolveMode): BindingContext {
        return wrapExceptions({ ResolvingWhat(element, bodyResolveMode = bodyResolveMode) }) {
            delegate.analyze(element, bodyResolveMode)
        }
    }

    override fun resolveToDescriptor(
        declaration: CjDeclaration,
        bodyResolveMode: BodyResolveMode
    ): DeclarationDescriptor {
        return wrapExceptions({ ResolvingWhat(declaration, bodyResolveMode = bodyResolveMode) }) {
            delegate.resolveToDescriptor(declaration, bodyResolveMode)
        }
    }


    override fun analyzeWithAllCompilerChecks(
        element: CjElement,
        callback: DiagnosticSink.DiagnosticsCallback?
    ): AnalysisResult {
        return wrapExceptions({ ResolvingWhat(element) }) {
            delegate.analyzeWithAllCompilerChecks(element, callback)
        }
    }

    override fun analyzeWithAllCompilerChecks(
        elements: Collection<CjElement>,
        callback: DiagnosticSink.DiagnosticsCallback?
    ): AnalysisResult {
        return wrapExceptions({ ResolvingWhat(elements = elements) }) {
            delegate.analyzeWithAllCompilerChecks(elements, callback)
        }
    }

    override fun analyze(elements: Collection<CjElement>, bodyResolveMode: BodyResolveMode): BindingContext {
        return wrapExceptions({ ResolvingWhat(elements = elements, bodyResolveMode = bodyResolveMode) }) {
            delegate.analyze(elements, bodyResolveMode)
        }
    }

    override fun fetchWithAllCompilerChecks(element: CjElement): AnalysisResult? {
        return wrapExceptions({ ResolvingWhat(element = element) }) {
            delegate.fetchWithAllCompilerChecks(element)
        }
    }


//    override fun resolveToDescriptor(declaration: CjDeclaration, bodyResolveMode: BodyResolveMode): DeclarationDescriptor {
//        return wrapExceptions({ ResolvingWhat(declaration, bodyResolveMode = bodyResolveMode) }) {
//            delegate.resolveToDescriptor(declaration, bodyResolveMode)
//        }
//    }

    override val moduleDescriptor: ModuleDescriptor
        get() = delegate.moduleDescriptor

    @FrontendInternals
    override fun <T : Any> getFrontendService(serviceClass: Class<T>): T {
        return wrapExceptions({ ResolvingWhat(serviceClass = serviceClass) }) {
            delegate.getFrontendService(serviceClass)
        }
    }

    override fun <T : Any> getIdeService(serviceClass: Class<T>): T {
        return wrapExceptions({ ResolvingWhat(serviceClass = serviceClass) }) {
            delegate.getIdeService(serviceClass)
        }
    }
//    @FrontendInternals
//    override fun <T : Any> getFrontendService(serviceClass: Class<T>): T {
//        return wrapExceptions({ ResolvingWhat(serviceClass = serviceClass) }) {
//            delegate.getFrontendService(serviceClass)
//        }
//    }
//
//    override fun <T : Any> getIdeService(serviceClass: Class<T>): T {
//        return wrapExceptions({ ResolvingWhat(serviceClass = serviceClass) }) {
//            delegate.getIdeService(serviceClass)
//        }
//    }

    @FrontendInternals
    override fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T {
        return wrapExceptions({ ResolvingWhat(element, serviceClass = serviceClass) }) {
            delegate.getFrontendService(element, serviceClass)
        }
    }


//
//    @FrontendInternals
//    override fun <T : Any> tryGetFrontendService(element: PsiElement, serviceClass: Class<T>): T? {
//        return wrapExceptions({ ResolvingWhat(element, serviceClass = serviceClass) }) {
//            delegate.tryGetFrontendService(element, serviceClass)
//        }
//    }

    override fun getResolverForProject(): ResolverForProject<out ModuleInfo> {
        return delegate.getResolverForProject()
    }
//
//    @FrontendInternals
//    override fun <T : Any> getFrontendService(moduleDescriptor: ModuleDescriptor, serviceClass: Class<T>): T {
//        return wrapExceptions({ ResolvingWhat(serviceClass = serviceClass, moduleDescriptor = moduleDescriptor) }) {
//            delegate.getFrontendService(moduleDescriptor, serviceClass)
//        }
//    }

    private inline fun <R> wrapExceptions(resolvingWhat: () -> ResolvingWhat, body: () -> R): R {
        try {
            return body()
        } catch (e: Throwable) {
            if (e is ControlFlowException || e is IndexNotReadyException) {
                throw e
            }
            throw CangJieIdeaResolutionException(e, resolvingWhat(), creationPlace)
        }
    }
}

private class CangJieIdeaResolutionException(
    cause: Throwable,
    resolvingWhat: ResolvingWhat,
    creationPlace: CreationPlace
) : CangJieExceptionWithAttachmentsImpl(
    "CangJie resolution encountered a problem while ${resolvingWhat.shortDescription()}${cause.message?.let { ":\n$it" } ?: ""}",
    cause
) {
    init {
        withAttachment("info.txt", buildString {
            append(resolvingWhat.description())
            appendLine("---------------------------------------------")
            append(creationPlace.description())
        })
        for (element in resolvingWhat.elements.withIndex()) {
            withPsiAttachment("element${element.index}.cj", element.value)
            withPsiAttachment(
                "file${element.index}.cj",
                element.value.containingFile
            )
        }
    }
}


private class CreationPlace(
    private val elements: Collection<CjElement>,
    private val context: ModuleInfo?,
//    private val settings: PlatformAnalysisSettings?
) {
    fun description() = buildString {
        appendLine("Resolver created for:")
        for (element in elements) {
            appendElement(element)
        }
        if (context != null) {
            appendLine("Provided module info: $context")
        }
//        if (settings != null) {
//            appendLine("Provided settings: $settings")
//        }
    }
}

private class ResolvingWhat(
    val element: PsiElement? = null,
    val elements: Collection<PsiElement> = emptyList(),
    private val bodyResolveMode: BodyResolveMode? = null,
    private val serviceClass: Class<*>? = null,
    private val moduleDescriptor: ModuleDescriptor? = null
) {
    fun shortDescription() = serviceClass?.let { "getting service ${serviceClass.simpleName}" }
        ?: "analyzing ${(element ?: elements.firstOrNull())?.javaClass?.simpleName ?: ""}"

    fun description(): String {
        return buildString {
            appendLine("Failed performing task:")
            if (serviceClass != null) {
                appendLine("Getting service: ${serviceClass.name}")
            } else {
                append("Analyzing code")
                if (bodyResolveMode != null) {
                    append(" in BodyResolveMode.$bodyResolveMode")
                }
                appendLine()
            }
            appendLine("Elements:")
            element?.let { appendElement(it) }
            for (element in elements) {
                appendElement(element)
            }
//            if (moduleDescriptor != null) {
//                appendLine("Provided module descriptor for module ${moduleDescriptor.getCapability(AnalysisContext.Capability)}")
//            }
        }
    }
}

private fun StringBuilder.appendElement(element: PsiElement) {
    fun info(key: String, value: String?) {
        appendLine("  $key = $value")
    }

    appendLine("Element of type: ${element.javaClass.simpleName}:")
    if (element is PsiNamedElement) {
        info("name", element.name)
    }
    info("isValid", element.isValid.toString())
    info("isPhysical", element.isPhysical.toString())
    if (element is PsiFile) {
        info("containingFile.name", element.containingFile.name)
    }
//    val moduleInfoResult = ifIndexReady { element.moduleInfoOrNull }
//    info("moduleInfo", moduleInfoResult?.let { it.result?.toString() ?: "null" } ?: "<index not ready>")

//    val moduleInfo = moduleInfoResult?.result
//    if (moduleInfo != null) {
//        info("moduleInfo.platform", moduleInfo.platform.toString())
//    }

    val virtualFile = element.containingFile?.virtualFile
    info("virtualFile", virtualFile?.name)

    if (virtualFile != null) {
        val moduleName =
            ifIndexReady { ModuleUtil.findModuleForFile(virtualFile, element.project)?.name ?: "null" }?.result
        info(
            "ideaModule",
            moduleName ?: "<index not ready>"
        )
    }
}

private class IndexResult<T>(val result: T)

private fun <T> ifIndexReady(body: () -> T): IndexResult<T>? = try {
    IndexResult(body())
} catch (e: IndexNotReadyException) {
    null
}

internal fun ResolutionFacade.createdFor(
    files: Collection<CjFile>,
    context: ModuleInfo?,
//    settings: PlatformAnalysisSettings
): ResolutionFacade {
    return ResolutionFacadeWithDebugInfo(this, CreationPlace(files, context/*, settings*/))
}
