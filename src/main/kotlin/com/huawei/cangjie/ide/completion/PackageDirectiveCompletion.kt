package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.ide.completion.handlers.InsertHandlerProvider
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.CjPackageDirective
import com.huawei.cangjie.psi.CjSimpleNameExpression
import com.huawei.cangjie.resolve.caches.getResolutionFacade
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.utils.CallType
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PlainPrefixMatcher
import com.intellij.patterns.PlatformPatterns

/**
 * Performs completion in package directive. Should suggest only packages and avoid showing fake package produced by
 * DUMMY_IDENTIFIER.
 */
object PackageDirectiveCompletion {
    val DUMMY_IDENTIFIER = "___package___"
    val ACTIVATION_PATTERN = PlatformPatterns.psiElement().inside(CjPackageDirective::class.java)

    fun perform(parameters: CompletionParameters, result: CompletionResultSet): Boolean {
        val position = parameters.position
        if (!ACTIVATION_PATTERN.accepts(position)) return false

        val file = position.containingFile as CjFile

        val expression = file.findElementAt(parameters.offset)?.parent as? CjSimpleNameExpression ?: return false

        val prefixLength = parameters.offset - expression.textOffset
        val prefix = expression.text!!
        val prefixMatcher = PlainPrefixMatcher(prefix.substring(0, prefixLength))
        val resultSet = result.withPrefixMatcher(prefixMatcher)

        val resolutionFacade = expression.getResolutionFacade()

        val packageMemberScope = resolutionFacade.moduleDescriptor.getPackage(file.packageFqName.parent()).memberScope

        val variants = packageMemberScope.getContributedDescriptors(DescriptorKindFilter.PACKAGES, prefixMatcher.asNameFilter())
        val lookupElementFactory = BasicLookupElementFactory(
            resolutionFacade.project,
            InsertHandlerProvider(callType = CallType.PACKAGE_DIRECTIVE, editor = parameters.editor, expectedInfosCalculator = { emptyList() })
        )
        for (variant in variants) {
            val lookupElement = lookupElementFactory.createLookupElement(variant)
            if (!lookupElement.lookupString.contains(DUMMY_IDENTIFIER)) {
                resultSet.addElement(lookupElement)
            }
        }

        return true
    }
}
