package com.huawei.cangjie.ide.imports

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.getLineCount
import com.huawei.cangjie.psi.psiUtil.isMultiLine
import com.huawei.cangjie.psi.psiUtil.nextLeaf
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.ImportPath
import com.huawei.cangjie.resolve.descriptorUtil.fqNameSafe
import com.huawei.cangjie.resolve.descriptorUtil.getImportableDescriptor
import com.huawei.cangjie.resolve.getReferenceTargets
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiWhiteSpace


val DeclarationDescriptor.importableFqName: FqName?
    get() {
        if (!canBeReferencedViaImport()) return null
        return getImportableDescriptor().fqNameSafe
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
                val directivePath = it.importPath
                directivePath != null && importPathComparator.compare(directivePath, importPath) <= 0
            }

            if (insertAfter != null && newDirective.importPath == insertAfter.importPath) return insertAfter

            (importList.addAfter(newDirective, insertAfter) as CjImportDirective).also {
                importList.addBefore(psiFactory.createNewLine(1), it)
            }
        }
    } else {
        error("Trying to insert import $fqName into a file $name of type ${this::class.java} with no import list.")
    }
}
