//package com.linqingying.cangjie.resolve
//
//import com.linqingying.cangjie.config.LanguageVersionSettings
//import com.linqingying.cangjie.descriptors.BindingTrace
//import com.linqingying.cangjie.descriptors.FunctionDescriptor
//import com.linqingying.cangjie.descriptors.PropertyDescriptor
//import com.linqingying.cangjie.psi.CjCallableDeclaration
//
//
//class AnalyzerExtensions(
//    trace:  BindingTrace,
//    reasonableInlineRules: Iterable<ReasonableInlineRule>,
//    languageVersionSettings:  LanguageVersionSettings
//) {
//    interface AnalyzerExtension {
//        fun process(
//            descriptor:  CallableMemberDescriptor,
//            functionOrProperty: CjCallableDeclaration,
//            trace:  BindingTrace
//        )
//    }
//
//    private val trace: BindingTrace = trace
//    private val reasonableInlineRules: Iterable<ReasonableInlineRule> = reasonableInlineRules
//    private val languageVersionSettings: LanguageVersionSettings = languageVersionSettings
//
//    fun process(bodiesResolveContext:  BodiesResolveContext) {
//        for ((function, functionDescriptor) in bodiesResolveContext.getFunctions().entries) {
//            for (extension in getFunctionExtensions(functionDescriptor)) {
//                extension.process(functionDescriptor, function, trace)
//            }
//        }
//
//        for ((function, propertyDescriptor) in bodiesResolveContext.getProperties().entries) {
//            for (extension in getPropertyExtensions(propertyDescriptor)) {
//                extension.process(propertyDescriptor, function, trace)
//            }
//        }
//    }
//
//    private fun getFunctionExtensions(functionDescriptor: FunctionDescriptor): List<InlineAnalyzerExtension> {
//        if ( inline.InlineUtil.isInline(functionDescriptor)) {
//            return listOf<InlineAnalyzerExtension>(
//                InlineAnalyzerExtension(
//                    reasonableInlineRules,
//                    languageVersionSettings
//                )
//            )
//        }
//        return emptyList<InlineAnalyzerExtension>()
//    }
//
//    private fun getPropertyExtensions(propertyDescriptor: PropertyDescriptor): List<InlineAnalyzerExtension> {
//        if ( inline.InlineUtil.hasInlineAccessors(propertyDescriptor)) {
//            return listOf<InlineAnalyzerExtension>(
//                InlineAnalyzerExtension(
//                    reasonableInlineRules,
//                    languageVersionSettings
//                )
//            )
//        }
//        return emptyList<InlineAnalyzerExtension>()
//    }
//}
