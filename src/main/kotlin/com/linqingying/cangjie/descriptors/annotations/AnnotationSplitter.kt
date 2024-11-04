package com.linqingying.cangjie.descriptors.annotations

import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.resolve.lazy.LazyEntity
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.storage.getValue

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

