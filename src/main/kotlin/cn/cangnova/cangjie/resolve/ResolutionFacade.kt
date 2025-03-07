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

package cn.cangnova.cangjie.resolve

import cn.cangnova.cangjie.analyzer.AnalysisResult
import cn.cangnova.cangjie.analyzer.ModuleInfo
import cn.cangnova.cangjie.analyzer.ResolverForProject
import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.diagnostics.DiagnosticSink
import cn.cangnova.cangjie.descriptors.ModuleDescriptor
import cn.cangnova.cangjie.ide.FrontendInternals
import cn.cangnova.cangjie.psi.CjDeclaration
import cn.cangnova.cangjie.psi.CjElement
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import cn.cangnova.cangjie.resolve.lazy.BodyResolveMode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

inline fun <reified T : Any> ResolutionFacade.ideService(): T = this.getIdeService(T::class.java)

interface ResolutionFacade{
    val project: Project

    fun fetchWithAllCompilerChecks(element: CjElement): AnalysisResult? = null
    val moduleDescriptor: ModuleDescriptor
    // get service for the module this resolution was created for
    @FrontendInternals
    fun <T : Any> getFrontendService(serviceClass: Class<T>): T
    fun <T : Any> getIdeService(serviceClass: Class<T>): T

    fun analyzeWithAllCompilerChecks(element: CjElement, callback: DiagnosticSink.DiagnosticsCallback? = null): AnalysisResult
            = analyzeWithAllCompilerChecks(listOf(element), callback)
    fun analyze(elements: Collection<CjElement>, bodyResolveMode: BodyResolveMode): BindingContext
    fun resolveToDescriptor(declaration: CjDeclaration, bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): DeclarationDescriptor

    fun analyzeWithAllCompilerChecks(elements: Collection<CjElement>, callback: DiagnosticSink.DiagnosticsCallback? = null): AnalysisResult
    fun analyze(element: CjElement, bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): BindingContext
    // get service for the module defined by PsiElement/ModuleDescriptor passed as parameter
    @FrontendInternals
    fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T
    fun getResolverForProject(): ResolverForProject<out ModuleInfo>

}
@FrontendInternals
inline fun <reified T : Any> ResolutionFacade.frontendService(): T = this.getFrontendService(T::class.java)
val ResolutionFacade.languageVersionSettings: LanguageVersionSettings
    get() = @OptIn(FrontendInternals::class) frontendService()
val ResolutionFacade.dataFlowValueFactory: DataFlowValueFactory
    get() = @OptIn(FrontendInternals::class) frontendService()
