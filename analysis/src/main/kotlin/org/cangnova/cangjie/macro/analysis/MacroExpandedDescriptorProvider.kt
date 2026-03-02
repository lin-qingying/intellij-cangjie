/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.macro.analysis

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.data.CjClassInfoUtil
import org.cangnova.cangjie.descriptors.data.CjTypeStatementInfo
import org.cangnova.cangjie.descriptors.impl.PropertyDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.VariableDescriptorImpl
import org.cangnova.cangjie.macro.cache.MacroExpansionCache
import org.cangnova.cangjie.macro.service.MacroExpansionResult
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.source.getPsi
import java.util.concurrent.ConcurrentHashMap

/**
 * 宏展开描述符提供者
 *
 * 从展开后的 [CjFile] PSI 中提取顶层声明：
 * - class/struct/interface（CjClass/CjStruct/CjInterface）→ [CjTypeStatementInfo]（交由 LazyClassDescriptor 正常解析）
 * - enum（CjEnum）→ [CjTypeStatementInfo]（交由 LazyEnumDescriptor 正常解析）
 * - extend（CjExtend）→ 跳过（extend 不创建新命名类型，仅为现有类型扩展成员）
 * - 函数（CjNamedFunction）→ [SimpleFunctionDescriptor]
 * - 变量（CjPatternVariable）→ [VariableDescriptor]（顶层 let/var 声明）
 * - 属性（CjProperty）→ [PropertyDescriptor]（类成员属性）
 *
 * class/struct/interface/enum 不再创建描述符存根，而是保存 PSI 信息，
 * 由 [MacroExpansionSyntheticResolveExtension] 通过 [LazyClassContext] 创建
 * [LazyClassDescriptor]/[LazyEnumDescriptor]，走正常的分析管线。
 */
@Service(Service.Level.PROJECT)
class MacroExpandedDescriptorProvider(private val project: Project) : Disposable {

    private val log = Logger.getInstance(MacroExpandedDescriptorProvider::class.java)

    // 包级别缓存：key = filePath
    private val packageDescriptorCache = ConcurrentHashMap<String, FileDescriptors>()
    private val packageCacheTimestamps = ConcurrentHashMap<String, Long>()

    // 类级别缓存：key = "$filePath@$classStartOffset"
    private val classDescriptorCache = ConcurrentHashMap<String, FileDescriptors>()
    private val classCacheTimestamps = ConcurrentHashMap<String, Long>()

    /**
     * 宏展开产生的类型 PSI 信息及其对应的宏调用源元素
     *
     * @param typeInfo 类/枚举的 PSI 信息（交由 LazyClassDescriptor/LazyEnumDescriptor 正常解析）
     * @param sourceElement 指向原始宏调用的源元素（用于 Ctrl+Click 导航）
     */
    data class MacroTypeInfo(
        val typeInfo: CjTypeStatementInfo<*>,
        val sourceElement: SourceElement
    )

    /**
     * 文件中所有宏展开产生的描述符和 PSI 信息
     *
     * - functions：函数描述符
     * - variables：顶层变量描述符（let/var，包级别）
     * - properties：属性描述符（prop，类成员级别）
     * - classes：CjClass/CjStruct/CjInterface 的 PSI 信息及宏调用源元素（交由 LazyClassDescriptor 通过正常管线解析）
     * - enums：CjEnum 的 PSI 信息及宏调用源元素（交由 LazyEnumDescriptor 通过正常管线解析）
     * - CjExtend 不生成新命名类型，已跳过
     */
    data class FileDescriptors(
        val functions: Map<Name, List<SimpleFunctionDescriptor>>,
        val variables: Map<Name, List<VariableDescriptor>>,
        val properties: Map<Name, List<PropertyDescriptor>>,
        val classes: Map<Name, List<MacroTypeInfo>>,
        val enums: Map<Name, List<MacroTypeInfo>>,
        val functionNames: Set<Name>,
        val variableNames: Set<Name>,
        val propertyNames: Set<Name>,
        val classNames: Set<Name>,
        val enumNames: Set<Name>
    ) {
        companion object {
            val EMPTY = FileDescriptors(
                functions = emptyMap(),
                variables = emptyMap(),
                properties = emptyMap(),
                classes = emptyMap(),
                enums = emptyMap(),
                functionNames = emptySet(),
                variableNames = emptySet(),
                propertyNames = emptySet(),
                classNames = emptySet(),
                enumNames = emptySet()
            )
        }
    }

    /**
     * 获取包级别宏展开产生的描述符（顶层 class/enum/function/property）
     *
     * 缓存键为 [filePath]，所有描述符的 [containingDeclaration] = [packageDescriptor]。
     */
    fun getPackageLevelDescriptors(
        sourceFile: VirtualFile,
        packageDescriptor: PackageFragmentDescriptor
    ): FileDescriptors {
        val filePath = sourceFile.path

        val cachedTimestamp = packageCacheTimestamps[filePath]
        if (cachedTimestamp != null && cachedTimestamp == sourceFile.modificationStamp) {
            packageDescriptorCache[filePath]?.let { return it }
        }

        val results = loadExpansionResults(sourceFile) ?: return FileDescriptors.EMPTY
        val psiResults = loadPsiResults(results, sourceFile)
        if (psiResults.isEmpty()) return FileDescriptors.EMPTY

        val descriptors = extractDescriptors(psiResults, packageDescriptor)

        packageDescriptorCache[filePath] = descriptors
        packageCacheTimestamps[filePath] = sourceFile.modificationStamp

        return descriptors
    }

    /**
     * 获取类级别宏展开产生的描述符（成员 function/property）
     *
     * 只处理宏调用位置落在 [classDescriptor] PSI 文本范围内的展开结果，
     * 避免把其他类的展开成员注入到当前类。
     *
     * 缓存键为 "$filePath@$classStartOffset"，所有描述符的 [containingDeclaration] = [classDescriptor]。
     */
    fun getClassLevelDescriptors(
        sourceFile: VirtualFile,
        classDescriptor: ClassDescriptor
    ): FileDescriptors {
        val filePath = sourceFile.path
        val classPsi = classDescriptor.source.getPsi() ?: return FileDescriptors.EMPTY
        val classStartOffset = classPsi.textRange?.startOffset ?: return FileDescriptors.EMPTY
        val cacheKey = "$filePath@$classStartOffset"

        val cachedTimestamp = classCacheTimestamps[cacheKey]
        if (cachedTimestamp != null && cachedTimestamp == sourceFile.modificationStamp) {
            classDescriptorCache[cacheKey]?.let { return it }
        }

        val allResults = loadExpansionResults(sourceFile) ?: return FileDescriptors.EMPTY

        // 过滤：只保留宏调用位于该类文本范围内的展开结果
        val classRange = classPsi.textRange
        val classResults = allResults.filter { result ->
            result.startOffset >= classRange.startOffset && result.startOffset < classRange.endOffset
        }
        if (classResults.isEmpty()) return FileDescriptors.EMPTY

        val psiResults = loadPsiResults(classResults, sourceFile)
        if (psiResults.isEmpty()) return FileDescriptors.EMPTY

        val descriptors = extractDescriptors(psiResults, classDescriptor)

        classDescriptorCache[cacheKey] = descriptors
        classCacheTimestamps[cacheKey] = sourceFile.modificationStamp

        return descriptors
    }

    private fun loadExpansionResults(sourceFile: VirtualFile): List<MacroExpansionResult>? {
        val expansionCache = MacroExpansionCache.getInstance(project)
        val results = expansionCache.getForFile(sourceFile) ?: return null
        return if (results.isEmpty()) null else results
    }

    private fun loadPsiResults(
        results: List<MacroExpansionResult>,
        sourceFile: VirtualFile
    ): List<Pair<MacroExpansionResult, CjFile>> {
        val psiCache = MacroExpandedPsiCache.getInstance(project)
        return psiCache.getOrCreateAllPsi(results, sourceFile)
    }

    private fun extractDescriptors(
        psiResults: List<Pair<MacroExpansionResult, CjFile>>,
        containingDescriptor: DeclarationDescriptor
    ): FileDescriptors {
        val functions = mutableMapOf<Name, MutableList<SimpleFunctionDescriptor>>()
        val variables = mutableMapOf<Name, MutableList<VariableDescriptor>>()
        val properties = mutableMapOf<Name, MutableList<PropertyDescriptor>>()
        val classes = mutableMapOf<Name, MutableList<MacroTypeInfo>>()
        val enums = mutableMapOf<Name, MutableList<MacroTypeInfo>>()

        for ((result, psiFile) in psiResults) {
            try {
                extractDeclarationsFromFile(psiFile, result, containingDescriptor, functions, variables, properties, classes, enums)
            } catch (e: Exception) {
                log.warn("宏展开描述符提取失败 [${result.macroName}]: ${e.message}", e)
            }
        }

        return FileDescriptors(
            functions = functions,
            variables = variables,
            properties = properties,
            classes = classes,
            enums = enums,
            functionNames = functions.keys.toSet(),
            variableNames = variables.keys.toSet(),
            propertyNames = properties.keys.toSet(),
            classNames = classes.keys.toSet(),
            enumNames = enums.keys.toSet()
        )
    }

    /**
     * 遍历展开后 CjFile 的顶层声明，按类型分类提取。
     *
     * 类型分类策略：
     * - CjNamedFunction → SimpleFunctionDescriptor
     * - CjPatternVariable → VariableDescriptor（顶层 let/var 声明）
     * - CjProperty → PropertyDescriptor（类成员 prop 声明）
     * - CjClass / CjStruct / CjInterface → MacroTypeInfo（交由 LazyClassDescriptor 正常解析）
     * - CjEnum → MacroTypeInfo（交由 LazyEnumDescriptor 正常解析）
     * - CjExtend → 跳过（extend 不创建新命名类型）
     */
    private fun extractDeclarationsFromFile(
        psiFile: CjFile,
        result: MacroExpansionResult,
        containingDescriptor: DeclarationDescriptor,
        functions: MutableMap<Name, MutableList<SimpleFunctionDescriptor>>,
        variables: MutableMap<Name, MutableList<VariableDescriptor>>,
        properties: MutableMap<Name, MutableList<PropertyDescriptor>>,
        classes: MutableMap<Name, MutableList<MacroTypeInfo>>,
        enums: MutableMap<Name, MutableList<MacroTypeInfo>>
    ) {
        val sourceElement = buildSourceElement(result)

        for (declaration in psiFile.declarations) {
            when (declaration) {
                is CjNamedFunction -> {
                    val name = declaration.nameAsName ?: continue
                    createFunctionDescriptor(declaration, containingDescriptor, sourceElement)
                        ?.let { functions.getOrPut(name) { mutableListOf() }.add(it) }
                }

                is CjProperty -> {
                    val name = declaration.nameAsName ?: continue
                    createPropertyDescriptor(declaration, containingDescriptor, sourceElement)
                        ?.let { properties.getOrPut(name) { mutableListOf() }.add(it) }
                }

                // 顶层变量（let/var）→ VariableDescriptor
                is CjPatternVariable -> {
                    val name = declaration.nameAsName ?: continue
                    createVariableDescriptor(declaration, name, containingDescriptor, sourceElement)
                        ?.let { variables.getOrPut(name) { mutableListOf() }.add(it) }
                }

                // class / struct / interface → MacroTypeInfo（通过正常管线解析，同时保存宏调用源元素）
                is CjClass, is CjStruct, is CjInterface -> {
                    val typeStatement = declaration as CjTypeStatement
                    val name = typeStatement.nameAsName ?: continue
                    val classInfo = CjClassInfoUtil.createTypeStatementInfo(typeStatement)
                    classes.getOrPut(name) { mutableListOf() }.add(MacroTypeInfo(classInfo, sourceElement))
                }

                // enum → MacroTypeInfo（通过正常管线解析，同时保存宏调用源元素）
                is CjEnum -> {
                    val name = declaration.nameAsName ?: continue
                    val enumInfo = CjClassInfoUtil.createTypeStatementInfo(declaration)
                    enums.getOrPut(name) { mutableListOf() }.add(MacroTypeInfo(enumInfo, sourceElement))
                }

                // extend → 跳过（extend 不创建新命名类型，仅为现有类型扩展成员）
                is CjExtend -> { /* intentionally skipped */ }
            }
        }
    }

    private fun createFunctionDescriptor(
        function: CjNamedFunction,
        containingDescriptor: DeclarationDescriptor,
        sourceElement: SourceElement
    ): SimpleFunctionDescriptor? {
        val name = function.nameAsName ?: return null
        val descriptor = SimpleFunctionDescriptorImpl.create(
            containingDescriptor,
            Annotations.EMPTY,
            name,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            sourceElement
        )
        descriptor.initialize(
            null,           // extensionReceiverParameter
            emptyList(),    // typeParameters
            emptyList(),    // valueParameters
            null,           // returnType（占位）
            Modality.FINAL,
            DescriptorVisibilities.PUBLIC
        )
        return descriptor
    }

    private fun createPropertyDescriptor(
        property: CjProperty,
        containingDescriptor: DeclarationDescriptor,
        sourceElement: SourceElement
    ): PropertyDescriptor? {
        val name = property.nameAsName ?: return null
        return PropertyDescriptorImpl.create(
            containingDescriptor,
            Annotations.EMPTY,
            Modality.FINAL,
            DescriptorVisibilities.PUBLIC,
            property.isVar,
            name,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            sourceElement
        )
    }

    private fun createVariableDescriptor(
        variable: CjPatternVariable,
        name: Name,
        containingDescriptor: DeclarationDescriptor,
        sourceElement: SourceElement
    ): VariableDescriptor? {
        return VariableDescriptorImpl.create(
            containingDescriptor,
            name,
            DescriptorVisibilities.PUBLIC,
            variable.isVar,
            sourceElement
        )
    }

    /**
     * 构建指向原始宏调用的 SourceElement
     */
    private fun buildSourceElement(result: MacroExpansionResult): SourceElement {
        val macroExpr = findOriginalMacroExpression(result)
        return if (macroExpr != null) MacroExpandedSourceElement(macroExpr) else SourceElement.NO_SOURCE
    }

    private fun findOriginalMacroExpression(result: MacroExpansionResult): CjMacroExpression? {
        return try {
            val psiManager = PsiManager.getInstance(project)
            val virtualFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                .findFileByPath(result.filePath) ?: return null
            val psiFile = psiManager.findFile(virtualFile) as? CjFile ?: return null
            val element = psiFile.findElementAt(result.startOffset) ?: return null
            PsiTreeUtil.getParentOfType(element, CjMacroExpression::class.java)
        } catch (e: Exception) {
            log.debug("查找原始宏表达式失败 ${result.filePath}:${result.startOffset}", e)
            null
        }
    }

    fun invalidateForFile(filePath: String) {
        packageDescriptorCache.remove(filePath)
        packageCacheTimestamps.remove(filePath)
        classDescriptorCache.keys.removeIf { it.startsWith("$filePath@") }
        classCacheTimestamps.keys.removeIf { it.startsWith("$filePath@") }
    }

    fun clearAll() {
        packageDescriptorCache.clear()
        packageCacheTimestamps.clear()
        classDescriptorCache.clear()
        classCacheTimestamps.clear()
    }

    override fun dispose() {
        clearAll()
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): MacroExpandedDescriptorProvider {
            return project.getService(MacroExpandedDescriptorProvider::class.java)
        }
    }
}
