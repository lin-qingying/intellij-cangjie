package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.psi.CjSimpleNameExpression
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.codeInsight.completion.impl.RealPrefixMatchingWeigher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext


class CompletionSessionConfiguration(
    val useBetterPrefixMatcherForNonImportedClasses: Boolean,
    val nonAccessibleDeclarations: Boolean,
    val javaGettersAndSetters: Boolean,
    val javaClassesNotToBeUsed: Boolean,
    val staticMembers: Boolean,
    val dataClassComponentFunctions: Boolean
)
//abstract  class CompletionSession
