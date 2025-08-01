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

package cn.cangnova.cangjie.descriptors.annotations

import cn.cangnova.cangjie.resolve.lazy.LazyEntity
import cn.cangnova.cangjie.storage.getValue

class AnnotationSplitter(
    private val storageManager: StorageManager,
    allAnnotations: Annotations,
    applicableTargets: Set<AnnotationUseSiteTarget>
) {
    companion object {
        private val TARGET_PRIORITIES = setOf(
            AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER,
            AnnotationUseSiteTarget.PROPERTY, AnnotationUseSiteTarget.FIELD
        )
    }
    fun getOtherAnnotations(): Annotations = LazySplitAnnotations(storageManager, null)

    private val splitAnnotations = storageManager.createLazyValue {
        val map = hashMapOf<AnnotationUseSiteTarget, MutableList<AnnotationDescriptor>>()
        val other = arrayListOf<AnnotationDescriptor>()
        val applicableTargetsWithoutUseSiteTarget = applicableTargets.intersect(TARGET_PRIORITIES)

        outer@ for (annotation in allAnnotations) {
//            for (target in TARGET_PRIORITIES) {
//                if (target !in applicableTargetsWithoutUseSiteTarget) continue

//                val declarationSiteTargetForCurrentTarget = CangJieTarget.USE_SITE_MAPPING[target] ?: continue
//                val applicableTargetsForAnnotation = AnnotationChecker.applicableTargetSet(annotation)
//
//                if (declarationSiteTargetForCurrentTarget in applicableTargetsForAnnotation) {
//                    map.getOrPut(target) { arrayListOf() }.add(annotation)
//                    continue@outer
//                }
//            }

            other.add(annotation)
        }

        for ((annotation, target) in @Suppress("DEPRECATION") allAnnotations.getUseSiteTargetedAnnotations()) {
            if (target in applicableTargets) {
                map.getOrPut(target) { arrayListOf() }.add(annotation)
            }
        }

        map to Annotations.create(other)
    }

    private inner class LazySplitAnnotations(
        storageManager: StorageManager,
        val target: AnnotationUseSiteTarget?
    ) : Annotations, LazyEntity {
        private val annotations by storageManager.createLazyValue {
            val (targeted, other) = this@AnnotationSplitter.splitAnnotations()

            if (target != null) {
                targeted[target]?.let(Annotations.Companion::create) ?: Annotations.EMPTY
            } else {
                other
            }

        }

        override fun forceResolveAllContents() {
            for (annotation in this) {
                // TODO: probably we should do ForceResolveUtil.forceResolveAllContents(annotation) here
            }
        }

        override fun isEmpty() = annotations.isEmpty()
        override fun hasAnnotation(fqName: FqName) = annotations.hasAnnotation(fqName)
        override fun findAnnotation(fqName: FqName) = annotations.findAnnotation(fqName)
        override fun iterator() = annotations.iterator()
        override fun toString() = annotations.toString()

    }

    fun getAnnotationsForTarget(target: AnnotationUseSiteTarget): Annotations =
        LazySplitAnnotations(storageManager, target)

}

