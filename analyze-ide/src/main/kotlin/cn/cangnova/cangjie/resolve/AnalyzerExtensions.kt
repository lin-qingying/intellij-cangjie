package cn.cangnova.cangjie.resolve/*
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

//package cn.cangnova.cangjie.resolve
//
//import cn.cangnova.cangjie.config.LanguageVersionSettings
//import cn.cangnova.cangjie.descriptors.BindingTrace
//import cn.cangnova.cangjie.descriptors.FunctionDescriptor
//import cn.cangnova.cangjie.descriptors.PropertyDescriptor
//import cn.cangnova.cangjie.psi.CjCallableDeclaration
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
