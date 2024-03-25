package com.huawei.cangjie.idea.highlighter

import com.huawei.cangjie.highlighter.visitor.AbstractHighlightingVisitor
import com.huawei.cangjie.resolve.BindingContext
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder


/**
 * 代码分析后的高亮逻辑
 */
abstract class AfterAnalysisHighlightingVisitor protected constructor(
    holder: HighlightInfoHolder,
    protected var bindingContext: BindingContext
): AbstractHighlightingVisitor(holder)