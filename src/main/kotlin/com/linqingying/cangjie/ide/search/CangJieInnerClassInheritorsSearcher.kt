package com.linqingying.cangjie.ide.search

import com.linqingying.cangjie.psi.CjTypeStatement
import com.linqingying.cangjie.utils.safeAs
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.ProgressManager.checkCanceled
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.PsiAnchor
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.AnyPsiChangeListener
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.ConcurrencyUtil
import com.intellij.util.Processor
import com.intellij.util.containers.CollectionFactory
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.HashingStrategy
import java.util.concurrent.ConcurrentMap
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate

@Service(Service.Level.PROJECT)
class HighlightingCaches(private val project: Project) {
    companion object {
        @JvmStatic
        fun getInstance(project: Project): HighlightingCaches {
            return project.getService(HighlightingCaches::class.java)
        }


    }
    @JvmField
    val allCaches = ContainerUtil.createConcurrentList<MutableMap<*, *>>()

    init {
        project.messageBus.connect().subscribe(PsiManagerImpl.ANY_PSI_CHANGE_TOPIC, object : AnyPsiChangeListener {
            override fun beforePsiChanged(isPhysical: Boolean) {
                if (isPhysical) {
                    allCaches.forEach { it.clear() }
                }
            }
        })
    }

    // baseClass -> list of direct subclasses
    val DIRECT_SUB_CLASSES: ConcurrentMap<CjTypeStatement, Array<CjTypeStatement>> = createWeakCache()
    // baseClass -> all sub classes transitively, including anonymous
    @JvmField
    val ALL_SUB_CLASSES: ConcurrentMap<CjTypeStatement, Iterable<CjTypeStatement>> = createWeakCache()
    // baseClass -> all sub classes transitively, excluding anonymous
    @JvmField

    val ALL_SUB_CLASSES_NO_ANONYMOUS: ConcurrentMap<CjTypeStatement, Iterable<CjTypeStatement>> = createWeakCache()
    // baseMethod -> all overriding methods
//    val OVERRIDING_METHODS: MutableMap<PsiMethod, Iterable<PsiMethod>> = createWeakCache()

    private fun <T, V> createWeakCache(): ConcurrentMap<T & Any, V & Any> {
        val map: ConcurrentMap<T & Any, V & Any> = CollectionFactory.createConcurrentWeakKeySoftValueMap(
            10, 0.7f, Runtime.getRuntime().availableProcessors(),
            HashingStrategy.canonical()
        )
        allCaches.add(map)
        return map
    }
}


class CangJieInnerClassInheritorsSearcher :
    QueryExecutorBase<CjTypeStatement, ClassInheritorsSearch.SearchParameters>() {
    override fun processQuery(
        queryParameters: ClassInheritorsSearch.SearchParameters,
        consumer: Processor<in CjTypeStatement>
    ) {
        val searchScope = queryParameters.scope.safeAs<LocalSearchScope>() ?: return
        val classToProcess = queryParameters.classToProcess
        val baseClass = classToProcess


        val progress = ProgressIndicatorProvider.getGlobalProgressIndicator()
        if (progress != null) {
            progress.pushState()
            progress.text =
                runReadAction { baseClass.name }?.let {
                    CangJieIndexingBundle.message(
                        "psi.search.inheritors.of.class.progress",
                        it
                    )
                }
                    ?: CangJieIndexingBundle.message("psi.search.inheritors.progress")
        }

        try {
            for (element in searchScope.scope) {
                checkCanceled()
                if (!runReadAction { processElementInScope(element, classToProcess, consumer) }) break
            }
        } finally {
            progress?.popState()
        }
    }

    private fun processElementInScope(
        element: PsiElement,
        classToProcess: CjTypeStatement,
        consumer: Processor<in CjTypeStatement>
    ): Boolean {
        val classesOrObjects = PsiTreeUtil.findChildrenOfType(element, CjTypeStatement::class.java)
        for (cjClassOrObject in classesOrObjects) {
            checkCanceled()
            if (cjClassOrObject.superTypeListEntries.isEmpty()) continue
//            cjClassOrObject.toLightClass()?.let {
//                if (it.isInheritor(classToProcess, true) && !consumer.process(it)) {
//                    return false
//                }
//            }
        }
        return true
    }
}
