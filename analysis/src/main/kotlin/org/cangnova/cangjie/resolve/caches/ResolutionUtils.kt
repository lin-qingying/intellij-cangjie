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

@file:JvmName("ResolutionUtils")

package org.cangnova.cangjie.resolve.caches

import org.cangnova.cangjie.FrontendInternals
import org.cangnova.cangjie.resolve.AnalysisResult
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.*

import org.cangnova.cangjie.psi.psiUtil.getNonStrictParentOfType
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTraceContext
import org.cangnova.cangjie.resolve.binding.getType
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.util.getResolvedCall
import org.cangnova.cangjie.resolve.calls.util.safeAnalyze
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import org.cangnova.cangjie.resolve.lazy.NoDescriptorForDeclarationException
import org.cangnova.cangjie.resolve.qualified.QualifiedExpressionResolver
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.utils.actionUnderSafeAnalyzeBlock
import org.cangnova.cangjie.utils.returnIfNoDescriptorForDeclarationException

fun CjElement.analyzeWithAllCompilerChecks(): AnalysisResult = getResolutionFacade().analyzeWithAllCompilerChecks(this)

@JvmOverloads
fun CjElement.safeAnalyze(
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL,
): BindingContext = safeAnalyze(getResolutionFacade(), bodyResolveMode)

fun CjFile.resolveImportReference(fqName: FqName): Collection<DeclarationDescriptor> {
    val facade = getResolutionFacade()
    return facade.resolveImportReference(facade.moduleDescriptor, fqName)
}

fun CjElement.safeAnalyze(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL,
): BindingContext = try {
    analyze(resolutionFacade, bodyResolveMode)
} catch (e: Exception) {
    e.returnIfNoDescriptorForDeclarationException { BindingContext.EMPTY }
}

// this method don't check visibility and collect all descriptors with given fqName
@OptIn(FrontendInternals::class)
fun ResolutionFacade.resolveImportReference(
    moduleDescriptor: ModuleDescriptor,
    fqName: FqName,
): Collection<DeclarationDescriptor> {
    val importDirective = CjPsiFactory(project).createImportDirective(ImportPath(fqName, false))
    val qualifiedExpressionResolver = this.getFrontendService(QualifiedExpressionResolver::class.java)
    return importDirective.items.flatMap {
        qualifiedExpressionResolver.processImportReference(
            it,
            moduleDescriptor,
            BindingTraceContext(),
            excludedImportNames = emptyList(),
            packageFragmentForVisibilityCheck = null
        )?.getContributedDescriptors() ?: emptyList()
    }

}

/**
 * 将当前元素解析为调用，使用给定的解析器和解析模式。
 *
 * @param bodyResolveMode 解析模式，默认为 BodyResolveMode.PARTIAL。
 * @return 解析后的调用结果。
 *
 * 注意：为了获得后续调用的稳定结果，请使用带有提供 resolutionFacade 的重载方法。
 */
fun CjElement.resolveToCall(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL) =
    resolveToCall(getResolutionFacade(), bodyResolveMode)


fun CjElement.resolveToCall(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL,
): ResolvedCall<out CallableDescriptor>? = getResolvedCall(safeAnalyze(resolutionFacade, bodyResolveMode))


fun CjFile.analyzeWithAllCompilerChecks(vararg extraFiles: CjFile): AnalysisResult =
    this.analyzeWithAllCompilerChecks(null, *extraFiles)

fun CjFile.analyzeWithAllCompilerChecks(callback: ((Diagnostic) -> Unit)?, vararg extraFiles: CjFile): AnalysisResult {
    return if (extraFiles.isEmpty()) {
        CangJieCacheService.getInstance(project).getResolutionFacade(this)
            .analyzeWithAllCompilerChecks(this, callback)
    } else {
        CangJieCacheService.getInstance(project).getResolutionFacade(listOf(this) + extraFiles.toList())
            .analyzeWithAllCompilerChecks(this, callback)
    }
}

/**
 * **请使用带有提供 resolutionFacade 的重载方法以获得后续调用的稳定结果**
 *
 * @param bodyResolveMode 解析模式，默认为 BodyResolveMode.PARTIAL。
 * @return 返回解析到的描述符，如果不存在则返回 null。
 */
fun CjTypeStatement.resolveToDescriptorIfAny(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL) =
    resolveToDescriptorIfAny(getResolutionFacade(), bodyResolveMode)


fun CjTypeStatement.resolveToDescriptorIfAny(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL,
): ClassDescriptor? {
    return (this as CjDeclaration).resolveToDescriptorIfAny(resolutionFacade, bodyResolveMode) as? ClassDescriptor
}

/**
 * 该函数首先使用声明解析器来解析此声明及其附加声明（例如其父声明），
 * 然后从绑定上下文中获取相关的描述符。
 * 需要解析的具体声明集取决于 bodyResolveMode。
 *
 * **请注意，为了获得后续调用的稳定结果，请使用提供 resolutionFacade 的重载方法**
 *
 * @param bodyResolveMode 解析模式，默认为 BodyResolveMode.PARTIAL
 * @return 解析到的描述符，如果未解析到则返回 null
 */
fun CjDeclaration.resolveToDescriptorIfAny(
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL,
): DeclarationDescriptor? =
    resolveToDescriptorIfAny(getResolutionFacade(), bodyResolveMode)


/**
 * 解析当前命名函数的描述符，如果可能的话。
 *
 * @param bodyResolveMode 解析模式，默认为 BodyResolveMode.PARTIAL。
 * @return 解析后的函数描述符，如果无法解析则返回 null。
 *
 * 注意：为了获得后续调用的稳定结果，请使用提供 resolutionFacade 的重载方法。
 */
fun CjNamedFunction.resolveToDescriptorIfAny(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL) =
    resolveToDescriptorIfAny(getResolutionFacade(), bodyResolveMode)


val CjTypeReference.type: CangJieType?
    get() {


        val descriptorPsi = getNonStrictParentOfType<CjDeclaration>()
        val result = this.safeAnalyze(bodyResolveMode = BodyResolveMode.PARTIAL)
        val descriptor = descriptorPsi?.descriptor



        return this.getType(result)


    }
val CjDeclaration.descriptor: DeclarationDescriptor?
    get() = if (this is CjParameter) this.descriptor else this.resolveToDescriptorIfAny(BodyResolveMode.FULL)
val CjParameter.descriptor: ValueParameterDescriptor?
    get() = this.resolveToParameterDescriptorIfAny(BodyResolveMode.FULL)

/**
 * **Please, use overload with providing resolutionFacade for stable results of subsequent calls**
 */
fun CjParameter.resolveToParameterDescriptorIfAny(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL) =
    resolveToParameterDescriptorIfAny(getResolutionFacade(), bodyResolveMode)

fun CjParameter.resolveToParameterDescriptorIfAny(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL,
): ValueParameterDescriptor? {
    val context = safeAnalyze(resolutionFacade, bodyResolveMode)
    return context[BindingContext.VALUE_PARAMETER, this] as? ValueParameterDescriptor
}

/**
 * 解析当前声明及其父声明，并从绑定上下文中获取相关描述符。
 * 具体解析哪些声明取决于 `bodyResolveMode`。
 *
 * @param resolutionFacade 用于解析声明的解析器 facade。
 * @param bodyResolveMode 指定解析模式，默认为 `BodyResolveMode.PARTIAL`。
 * @return 返回解析到的描述符，如果未解析到则返回 null。
 */
fun CjDeclaration.resolveToDescriptorIfAny(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL,
): DeclarationDescriptor? {
    // 使用指定的解析模式进行安全分析
    val context = safeAnalyze(resolutionFacade, bodyResolveMode)

    // 如果当前声明是参数且有 `let` 或 `var` 关键字
    return if (this is CjParameter && hasLetOrVar()) {
        // 尝试从主构造函数参数中获取描述符
        context[BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, this]
        // 如果在主构造函数参数中未找到，则尝试从声明到描述符映射中获取
            ?: context[BindingContext.DECLARATION_TO_DESCRIPTOR, this]
    } else {
        // 直接从声明到描述符映射中获取描述符
        context[BindingContext.DECLARATION_TO_DESCRIPTOR, this]
    }
}


fun CjNamedFunction.resolveToDescriptorIfAny(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL,
): FunctionDescriptor? {
    return (this as CjDeclaration).resolveToDescriptorIfAny(resolutionFacade, bodyResolveMode) as? FunctionDescriptor
}

@JvmOverloads
fun CjElement.analyze(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL,
): BindingContext {


    return resolutionFacade.analyze(this, bodyResolveMode)
}

@JvmOverloads
fun CjElement.analyze(
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL,
): BindingContext = analyze(getResolutionFacade(), bodyResolveMode)

fun CjElement.safeAnalyzeNonSourceRootCode(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL,
): BindingContext =
    actionUnderSafeAnalyzeBlock({ analyze(resolutionFacade, bodyResolveMode) }, { BindingContext.EMPTY })

fun CjElement.getResolutionFacade(): ResolutionFacade =
    CangJieCacheService.getInstance(project).getResolutionFacade(this)

@JvmOverloads
fun CjElement.safeAnalyzeNonSourceRootCode(
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL,
): BindingContext {

    return safeAnalyzeNonSourceRootCode(getResolutionFacade(), bodyResolveMode)
}

/**
 * 当 resolveToDescriptorIfAny 返回 null 时，此函数会抛出异常，否则其行为与 resolveToDescriptorIfAny 相同。
 *
 * **请注意，为了后续调用的稳定性，请使用提供 resolutionFacade 的重载方法**
 *
 * @param bodyResolveMode 解析模式，默认为 BodyResolveMode.FULL
 * @return 解析后的声明描述符 [DeclarationDescriptor]
 */
fun CjDeclaration.unsafeResolveToDescriptor(
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL,
): DeclarationDescriptor =
    unsafeResolveToDescriptor(getResolutionFacade(), bodyResolveMode)


/**
 * 当 [resolveToDescriptorIfAny] 返回 null 时，此函数抛出异常；否则，其行为与 [resolveToDescriptorIfAny] 相同。
 *
 * @param resolutionFacade 用于解析声明的上下文对象。
 * @param bodyResolveMode 指定解析模式，默认为 [BodyResolveMode.FULL]。
 * @return 解析后的 [DeclarationDescriptor]。
 */
fun CjDeclaration.unsafeResolveToDescriptor(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL,
): DeclarationDescriptor =
    resolveToDescriptorIfAny(resolutionFacade, bodyResolveMode) ?: throw NoDescriptorForDeclarationException(this)


