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

package org.cangnova.cangjie.utils

import cn.cangnova.cangjie.imports.CangJieImportPathComparator
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getLineCount
import org.cangnova.cangjie.psi.psiUtil.isMultiLine
import org.cangnova.cangjie.psi.psiUtil.nextLeaf
import org.cangnova.cangjie.resolve.DescriptorUtils
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiWhiteSpace
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportDirective
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.ImportPath
import org.cangnova.cangjie.psi.psiUtil.getLineCount
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.getReferenceTargets
import org.cangnova.cangjie.resolve.fqNameSafe
import org.cangnova.cangjie.types.CangJieType

fun ClassifierDescriptor.getConstructors(): Collection<ConstructorDescriptor> = when (this) {
    is ClassDescriptor -> constructors
    is TypeAliasDescriptor -> constructors
    else -> emptyList()
}

fun CangJieType.canBeReferencedViaImport(): Boolean {
    val descriptor = constructor.declarationDescriptor
    return descriptor != null && descriptor.canBeReferencedViaImport()
}

val DeclarationDescriptor.importableFqName: FqName?
    get() {
        if (!canBeReferencedViaImport()) return null
        return getImportableDescriptor().fqNameSafe
    }


fun CjReferenceExpression.getImportableTargets(bindingContext: BindingContext): Collection<DeclarationDescriptor> {
    val targets = bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, this]?.let { listOf(it) }
        ?: getReferenceTargets(bindingContext)
    return targets.map { it.getImportableDescriptor() }.toSet()
}

fun ImportPath.isImported(imports: Iterable<ImportPath>, excludedFqNames: Iterable<FqName>): Boolean {
    return isImported(imports) && (isAllUnder || this.fqName !in excludedFqNames)
}

private fun ImportPath.isImported(imports: Iterable<ImportPath>): Boolean = imports.any { isImported(it) }
fun ImportPath.isImported(alreadyImported: ImportPath): Boolean {
    return if (isAllUnder || hasAlias()) this == alreadyImported else fqName.isImported(alreadyImported)
}

fun FqName.isImported(importPath: ImportPath, skipAliasedImports: Boolean = true): Boolean {
    return when {
        skipAliasedImports && importPath.hasAlias() -> false
        importPath.isAllUnder && !isRoot -> importPath.fqName == this.parent()
        else -> importPath.fqName == this
    }
}


/**
 * @return newly added import if it's not present in the file, and already existing import, otherwise.
 */
fun CjFile.addImport(
    fqName: FqName,
    allUnder: Boolean = false,
    alias: Name? = null,
    project: Project = this.project,
    isPackageSplit: Boolean = false
): CjImportDirective {
    val importPath = ImportPath(if (isPackageSplit) fqName.parent() else fqName, allUnder, alias)

    val psiFactory = CjPsiFactory(project)
    if (this is CjCodeFragment) {
        val newDirective = psiFactory.createImportDirective(importPath)
        addImportsFromString(newDirective.text)
        return newDirective
    }

    val importList = importList
    if (importList != null) {
        val newDirective = psiFactory.createImportDirective(importPath)
        val imports = importList.imports
        return if (imports.isEmpty()) {
            val packageDirective = packageDirective?.takeIf { it.packageKeyword != null }
            packageDirective?.let {
                val elemAfterPkg = packageDirective.nextSibling
                val linesAfterPkg = elemAfterPkg.getLineCount() - 1
                val missingLines = 2 - linesAfterPkg
                if (missingLines > 0) addAfter(psiFactory.createNewLine(missingLines), it)
            }

            (importList.add(newDirective) as CjImportDirective).also {
                if (packageDirective == null) {
                    val whiteSpace = importList.nextLeaf(true)
                    if (whiteSpace is PsiWhiteSpace) {
                        val newLineBreak = if (whiteSpace.isMultiLine()) {
                            psiFactory.createWhiteSpace("\n" + whiteSpace.text)
                        } else {
                            psiFactory.createWhiteSpace("\n\n" + whiteSpace.text)
                        }

                        whiteSpace.replace(newLineBreak)
                    } else {
                        addAfter(psiFactory.createNewLine(2), importList)
                    }
                }
            }
        } else {


            val importPathComparator = CangJieImportPathComparator.create(this)
            val insertAfter = imports.lastOrNull {
                val directivePath = it.firstImportPath
                directivePath != null && importPathComparator.compare(directivePath, importPath) <= 0
            }

            if (insertAfter is CjImportDirective && newDirective.firstImportPath == insertAfter.firstImportPath) return insertAfter

            (importList.addAfter(newDirective, insertAfter) as CjImportDirective ).also {
                importList.addBefore(psiFactory.createNewLine(1), it)
            }
        }
    } else {
        error("Trying to insert import $fqName into a file $name of type ${this::class.java} with no import list.")
    }
}


fun DeclarationDescriptor.canBeReferencedViaImport(): Boolean {
    if (this is PackageViewDescriptor ||
        DescriptorUtils.isTopLevelDeclaration(this) /*||
        this is CallableDescriptor && DescriptorUtils.isStaticDeclaration(this)*/
    ) {
        return !name.isSpecial
    }

    //Both TypeAliasDescriptor and ClassDescriptor
    val parentClassifier = containingDeclaration as? ClassifierDescriptorWithTypeParameters ?: return false
    if (!parentClassifier.canBeReferencedViaImport()) return false

    return when (this) {
        is ConstructorDescriptor -> true // inner class constructors can't be referenced via import
        is ClassDescriptor, is TypeAliasDescriptor -> true
        else -> parentClassifier is ClassDescriptor
    }
}

fun DeclarationDescriptor.getImportableDescriptor(): DeclarationDescriptor =
    when (this) {
        is DescriptorDerivedFromTypeAlias -> typeAliasDescriptor
        is ConstructorDescriptor -> containingDeclaration
//        is PropertyAccessorDescriptor -> correspondingProperty
        else -> this
    }



