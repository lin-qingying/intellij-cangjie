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

package org.cangnova.cangjie.utils

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile

interface ImportComparablePriority : Comparable<ImportComparablePriority>

class ImportableFqNameClassifier(private val file: CjFile, private val isImportedByDefault: (FqName) -> Boolean) {
    private val preciseImports = HashSet<FqName>()
    private val preciseImportPackages = HashSet<FqName>()
    private val allUnderImports = HashSet<FqName>()
    private val excludedImports = HashSet<FqName>()

    init {
        for (import in file.importDirectivesItem) {
            val importPath = import.importPath ?: continue
            val fqName = importPath.fqName
            when {
                importPath.isAllUnder -> allUnderImports.add(fqName)
                !importPath.hasAlias() -> {
                    preciseImports.add(fqName)
                    preciseImportPackages.add(fqName.parent())
                }
                else -> excludedImports.add(fqName)
                // TODO: support aliased imports in completion
            }
        }
    }

    enum class Classification {
        fromCurrentPackage,
        topLevelPackage,
        preciseImport,
        defaultImport,
        allUnderImport,
        siblingImported,
        notImported,
        notToBeUsedInCangJie
    }

    fun classify(fqName: FqName, isPackage: Boolean): Classification {
        if (isPackage) {
            return when {
                isImportedWithPreciseImport(fqName) -> Classification.preciseImport
                fqName.parent().isRoot -> Classification.topLevelPackage
                else -> Classification.notImported
            }
        }

        return when {

            fqName.parent() == file.packageFqName -> Classification.fromCurrentPackage

            isImportedByDefault(fqName) -> Classification.defaultImport

            isImportedWithPreciseImport(fqName) -> Classification.preciseImport

            isImportedWithAllUnderImport(fqName) -> Classification.allUnderImport

            hasPreciseImportFromPackage(fqName.parent()) -> Classification.siblingImported

            else -> Classification.notImported
        }
    }

    private fun isImportedWithPreciseImport(name: FqName) = name in preciseImports
    private fun isImportedWithAllUnderImport(name: FqName) = name.parent() in allUnderImports && name !in excludedImports
    private fun hasPreciseImportFromPackage(packageName: FqName) = packageName in preciseImportPackages
}
