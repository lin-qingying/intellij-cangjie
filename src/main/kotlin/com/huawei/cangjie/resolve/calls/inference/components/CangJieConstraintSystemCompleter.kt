package com.huawei.cangjie.resolve.calls.inference.components

import com.huawei.cangjie.resolve.calls.model.PostponedResolvedAtom
import com.huawei.cangjie.resolve.calls.model.ResolvedAtom
import com.intellij.util.containers.addIfNotNull

class CangJieConstraintSystemCompleter(
    private val resultTypeResolver: ResultTypeResolver,
    val variableFixationFinder: VariableFixationFinder,
    private val postponedArgumentsInputTypesResolver: PostponedArgumentInputTypesResolver,
//    private val languageVersionSettings: LanguageVersionSettings
)
{
    companion object{
        fun getOrderedNotAnalyzedPostponedArguments(topLevelAtoms: List<ResolvedAtom>): List<PostponedResolvedAtom> {
            fun ResolvedAtom.process(to: MutableList<PostponedResolvedAtom>) {
                to.addIfNotNull((this as? PostponedResolvedAtom)?.takeUnless { it.analyzed })

                if (analyzed) {
                    subResolvedAtoms?.forEach { it.process(to) }
                }
            }

            val notAnalyzedArguments = arrayListOf<PostponedResolvedAtom>()
            for (primitive in topLevelAtoms) {
                primitive.process(notAnalyzedArguments)
            }

            return notAnalyzedArguments
        }
    }
}
