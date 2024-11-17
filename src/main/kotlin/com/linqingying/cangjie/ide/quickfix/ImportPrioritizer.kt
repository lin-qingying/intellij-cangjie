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

package com.linqingying.cangjie.ide.quickfix

import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.utils.fqname.ImportComparablePriority
import com.linqingying.cangjie.utils.fqname.ImportableFqNameClassifier
import com.linqingying.cangjie.utils.module
import com.intellij.psi.PsiElement
import com.intellij.psi.WeighingComparable
import com.intellij.psi.WeighingService
import com.intellij.psi.statistics.StatisticsInfo
import com.intellij.psi.statistics.StatisticsManager
import com.intellij.psi.util.ProximityLocation
import com.intellij.psi.util.proximity.PsiProximityComparator

class ImportPrioritizer(
    val file: CjFile,
    isImportedByDefault: (FqName) -> Boolean,
    private val compareNames: Boolean = true
){
    private val classifier = ImportableFqNameClassifier(file, isImportedByDefault)
    private val statsManager = StatisticsManager.getInstance()
    private val proximityLocation = ProximityLocation(file, file.module)

    inner class Priority(
        declaration: PsiElement?,
        val statisticsInfo: StatisticsInfo,
        private val isDeprecated: Boolean,
        private val fqName: FqName,
        private val expressionWeight: Int,
    ) : ImportComparablePriority {
        private val classification: ImportableFqNameClassifier.Classification = classifier.classify(fqName, false)
        private val lastUseRecency: Int = statsManager.getLastUseRecency(statisticsInfo)
        private val proximityWeight: WeighingComparable<PsiElement, ProximityLocation> =
            WeighingService.weigh(PsiProximityComparator.WEIGHER_KEY, declaration, proximityLocation)

        override fun compareTo(other: ImportComparablePriority): Int {
            other as Priority

            if (isDeprecated != other.isDeprecated) {
                return if (isDeprecated) +1 else -1
            }

            val c1 = expressionWeight.compareTo(other.expressionWeight)
            if (c1 != 0) {
                // callExpressionWeigh is non-negative number
                // Comparator contract says that if `a.compareTo(b)` returns -1 then `a` appears earlier than `b`
                return -c1
            }

            val c2 = classification.compareTo(other.classification)
            if (c2 != 0) return c2

            val c3 = lastUseRecency.compareTo(other.lastUseRecency)
            if (c3 != 0) return c3

            val c4 = proximityWeight.compareTo(other.proximityWeight)
            if (c4 != 0) return -c4 // n.b. reversed

            return if (compareNames) {
                fqName.asString().compareTo(other.fqName.asString())
            } else {
                0
            }
        }
    }

    inner class GroupPriority(private val priorities: List<Priority>) : ImportComparablePriority {
        private val groupPriority = priorities.max()

        override fun compareTo(other: ImportComparablePriority): Int {
            other as GroupPriority

            val c1 = groupPriority.compareTo(other.groupPriority)
            if (c1 != 0) return c1

            return other.priorities.size - priorities.size
        }
    }
}
