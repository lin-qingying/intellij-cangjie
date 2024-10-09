package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.ide.CangJieIndicesHelper
import com.huawei.cangjie.ide.codeinsight.ReferenceVariantsHelper
import com.huawei.cangjie.psi.CjSimpleNameExpression
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.ResolutionFacade
import com.huawei.cangjie.resolve.scopes.receivers.ExpressionReceiver
import com.huawei.cangjie.utils.CallTypeAndReceiver
import com.huawei.cangjie.utils.fqname.ImportableFqNameClassifier
import com.intellij.codeInsight.completion.PrefixMatcher

class ReferenceVariantsCollector(
    private val referenceVariantsHelper: ReferenceVariantsHelper,
    private val indicesHelper: CangJieIndicesHelper,
    private val prefixMatcher: PrefixMatcher,
    private val applicabilityFilter: (DeclarationDescriptor) -> Boolean,
    private val nameExpression: CjSimpleNameExpression,
    private val callTypeAndReceiver: CallTypeAndReceiver<*, *>,
    private val resolutionFacade: ResolutionFacade,
    private val bindingContext: BindingContext,
    private val importableFqNameClassifier: ImportableFqNameClassifier,
    private val configuration: CompletionSessionConfiguration,
    private val allowExpectedDeclarations: Boolean,
    private val runtimeReceiver: ExpressionReceiver? = null,
)
