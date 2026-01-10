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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.psi.psiUtil.getElementTextWithContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.impl.source.tree.FileElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.LightVirtualFile
import java.util.LinkedHashSet

abstract class CjCodeFragment(
    viewProvider: FileViewProvider,
    imports: String?, // Should be separated by KtCodeFragment.IMPORT_SEPARATOR
    elementType: IElementType,
    private val context: PsiElement?,
) : CjFile(
    viewProvider,
    false,
),
    CjCodeFragmentBase {
    private var viewProvider = super.getViewProvider() as SingleRootFileViewProvider

    constructor(
        project: Project,
        name: String,
        text: CharSequence,
        imports: String?,
        elementType: IElementType,
        context: PsiElement?,
    ) : this(
        createFileViewProviderForLightFile(project, name, text),
        imports,
        elementType,
        context,
    )

    /**
     * Parses raw [rawImports] and appends them to the list of code fragment imports.
     *
     * Import strings must be separated by the [IMPORT_SEPARATOR].
     * Each import must be either a qualified name to import (e.g., 'foo.bar'), or a complete text representation of an import directive
     * (e.g., 'import foo.bar as baz').
     *
     * Note that already present import directives will be ignored.
     *
     * @return `true` if new import directives were added.
     */
    private fun appendImports(rawImports: String): Boolean {
        if (rawImports.isEmpty()) {
            return false
        }

        var hasNewImports = false

        for (rawImport in rawImports.split(IMPORT_SEPARATOR)) {
            val importDirectiveString = if (rawImport.startsWith("import ")) rawImport else "import $rawImport"
            if (importDirectiveStrings.add(importDirectiveString) && !hasNewImports) {
                hasNewImports = true
            }
        }

        return hasNewImports
    }

    fun addImportsFromString(imports: String?) {
        if (imports.isNullOrEmpty()) return

        imports.split(IMPORT_SEPARATOR).forEach {
            addImportsFromString(it)
        }

        // we need this code to force re-highlighting, otherwise it does not work by some reason
        val tempElement = CjPsiFactory(project).createColon()
        add(tempElement).delete()
    }

    init {
        @Suppress("LeakingThis")
        getViewProvider().forceCachedPsi(this)
        init(TokenType.CODE_FRAGMENT, elementType)
        if (context != null) {
            initImports(imports)
        }
    }

    private var importDirectiveStrings = LinkedHashSet<String>()

    private var forcedResolveScope: GlobalSearchScope? = null
    override fun getForcedResolveScope(): GlobalSearchScope? = forcedResolveScope



    override fun forceResolveScope(scope: GlobalSearchScope?) {
        forcedResolveScope = scope
    }

    private var isPhysical = true

    abstract fun getContentElement(): CjElement?

    override fun isPhysical() = isPhysical

    override fun isValid() = true

    override fun getContext(): PsiElement? {
        if (context != null && context !is CjElement) {
            val logInfoForContextElement =
                (context as? PsiFile)?.virtualFile?.path ?: context.getElementTextWithContext()
            LOG.warn("CodeFragment with non-cangjie context should have fakeContextForJavaFile set: \noriginalContext = $logInfoForContextElement")
            return null
        }

        return context
    }

    override fun getResolveScope() = context?.resolveScope ?: super.getResolveScope()

    override fun clone(): CjCodeFragment {
        val elementClone = calcTreeElement().clone() as FileElement
        return (cloneImpl(elementClone) as CjCodeFragment).apply {
            isPhysical = false
            myOriginalFile = this@CjCodeFragment
            importDirectiveStrings = LinkedHashSet(this@CjCodeFragment.importDirectiveStrings)
            viewProvider = SingleRootFileViewProvider(
                PsiManager.getInstance(project),
                LightVirtualFile(name, CangJieFileType.INSTANCE, text),
                false,
            )
            viewProvider.forceCachedPsi(this)
        }
        return (cloneImpl(elementClone) as CjCodeFragment).apply {
            isPhysical = false
            myOriginalFile = this@CjCodeFragment
            importDirectiveStrings = LinkedHashSet(this@CjCodeFragment.importDirectiveStrings)

            viewProvider = SingleRootFileViewProvider(
                PsiManager.getInstance(project),
                LightVirtualFile(name, CangJieFileType.INSTANCE, text),
                false,
            )
            viewProvider.forceCachedPsi(this)
        }
    }

    fun importsToString(): String {
        return importDirectiveStrings.joinToString(IMPORT_SEPARATOR)
    }

    final override fun getViewProvider() = viewProvider

    @Deprecated(
        "Use 'addImportsFromString()w' instead",
        ReplaceWith("addImportsFromString(import)"),
        level = DeprecationLevel.WARNING,
    )
    fun addImport(import: String) {
        addImportsFromString(import)
    }

    fun importsAsImportList(): CjImportList? {
        if (importDirectiveStrings.isNotEmpty() && context != null) {
            val ktPsiFactory = CjPsiFactory.contextual(context)
            val fileText = importDirectiveStrings.joinToString("\n")
            return ktPsiFactory.createFile("imports_for_codeFragment.kt", fileText).importList
        }

        return null
    }

    override val importDirectivesItem: List<CjImportInfo>
        get() = importsAsImportList()?.imports?.flatMap { it.importItems } ?: emptyList()

    fun getContextContainingFile(): CjFile? {
        return getOriginalContext()?.takeIf { it.isValid }?.getContainingCjFile()
    }

    fun getOriginalContext(): CjElement? {
        val contextElement = getContext() as? CjElement
        val contextFile = contextElement?.containingFile as? CjFile
        if (contextFile is CjCodeFragment) {
            return contextFile.getOriginalContext()
        }
        return contextElement
    }

    private fun initImports(imports: String?) {
        if (imports != null && imports.isNotEmpty()) {
            val importsWithPrefix =
                imports.split(IMPORT_SEPARATOR).map { it.takeIf { it.startsWith("import ") } ?: "import ${it.trim()}" }
            importsWithPrefix.forEach {
                addImportsFromString(it)
            }
        }
    }

    companion object {
        const val IMPORT_SEPARATOR: String = ","

        val FAKE_CONTEXT_FOR_JAVA_FILE: Key<Function0<CjElement>> = Key.create("FAKE_CONTEXT_FOR_JAVA_FILE")

        private val LOG = Logger.getInstance(CjCodeFragment::class.java)
        fun createFileViewProviderForLightFile(
            project: Project,
            name: String,
            text: CharSequence,
        ): FileViewProvider {
            val psiManager = PsiManager.getInstance(project) as PsiManagerEx
            return psiManager.fileManager.createFileViewProvider(
                LightVirtualFile(name, CangJieFileType.INSTANCE, text),
                /* eventSystemEnabled = */
                true,
            )
        }
    }
}
