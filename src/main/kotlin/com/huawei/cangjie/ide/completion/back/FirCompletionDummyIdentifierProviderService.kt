package com.huawei.cangjie.ide.completion.back


//class FirCompletionDummyIdentifierProviderService : AbstractCompletionDummyIdentifierProviderService() {
//    override fun handleDefaultCase(context: CompletionInitializationContext): String? {
//        val elementAtOffset = context.file.findElementAt(context.startOffset) ?: return null
//        return when {
//            elementAtOffset.parentOfType<CjMatchEntry>() != null -> CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
//            else -> null
//        }
//    }
//
//    override fun allTargetsAreFunctionsOrClasses(nameReferenceExpression: CjNameReferenceExpression): Boolean {
//        return true
//
//    }
//}
