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

package com.linqingying.cangjie.psi

import com.linqingying.cangjie.lang.CangJieFileType
import com.linqingying.cangjie.psi.psiUtil.getElementTextWithContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.impl.source.tree.FileElement
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.LightVirtualFile
import java.util.LinkedHashSet


abstract class CjCodeFragment(
    private val myProject: Project,
    name: String,
    text: CharSequence,
    imports: String?, // Should be separated by CjCodeFragment.IMPORT_SEPARATOR
    elementType: IElementType,
    private val context: PsiElement?
) : CjFile(
    run {
        val psiManager = PsiManager.getInstance(myProject) as PsiManagerEx
        psiManager.fileManager.createFileViewProvider(LightVirtualFile(name, CangJieFileType.INSTANCE, text), true)
    }, false
), CjCodeFragmentBase {
    private var viewProvider = super.getViewProvider() as SingleRootFileViewProvider
    private var imports = LinkedHashSet<String>()

    private val fakeContextForJavaFile: PsiElement? by lazy {
        this.getCopyableUserData(FAKE_CONTEXT_FOR_JAVA_FILE)?.invoke()
    }

     fun addImportsFromString(imports: String?) {
        if (imports == null || imports.isEmpty()) return

        imports.split(IMPORT_SEPARATOR).forEach {
            addImport(it)
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

    final override fun init(elementType: IElementType, contentElementType: IElementType?) {
        super.init(elementType, contentElementType)
    }


    private var isPhysical = true

    abstract fun getContentElement(): CjElement?


    override fun isPhysical() = isPhysical

    override fun isValid() = true

    override fun getContext(): PsiElement? {
        if (fakeContextForJavaFile != null) return fakeContextForJavaFile
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
            imports = this@CjCodeFragment.imports
            viewProvider = SingleRootFileViewProvider(
                PsiManager.getInstance(myProject),
                LightVirtualFile(name, CangJieFileType.INSTANCE, text),
                false
            )
            viewProvider.forceCachedPsi(this)
        }
    }

    final override fun getViewProvider() = viewProvider


    fun addImport(import: String) {
        val contextFile = getContextContainingFile()
        if (contextFile != null) {
            if (contextFile.importDirectives.find { it.text == import } == null) {
                imports.add(import)
            }
        }
    }

    fun importsAsImportList(): CjImportList? {
        if (imports.isNotEmpty() && context != null) {
            return CjPsiFactory.contextual(context)
                .createFile("imports_for_codeFragment.cj", imports.joinToString("\n")).importList
        }
        return null
    }

    override val importDirectives: List<CjImportDirectiveItem>
        get() = importsAsImportList()?.importItems ?: emptyList()


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
                addImport(it)
            }
        }
    }

    companion object {
        const val IMPORT_SEPARATOR: String = ","

        val FAKE_CONTEXT_FOR_JAVA_FILE: Key<Function0<CjElement>> = Key.create("FAKE_CONTEXT_FOR_JAVA_FILE")

        private val LOG = Logger.getInstance(CjCodeFragment::class.java)
    }
}
