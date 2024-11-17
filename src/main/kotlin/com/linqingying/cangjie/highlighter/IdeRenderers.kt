/*
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

package com.linqingying.cangjie.highlighter

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.CallableMemberDescriptor
import com.linqingying.cangjie.diagnostics.rendering.*
import com.linqingying.cangjie.highlighter.renderersUtil.renderResolvedCall
import com.linqingying.cangjie.renderer.DescriptorRenderer
import com.linqingying.cangjie.renderer.DescriptorRendererModifier
import com.linqingying.cangjie.resolve.MemberComparator
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall


object IdeRenderers {

    @JvmField
    val HTML_AMBIGUOUS_CALLS = renderer { calls: Collection<ResolvedCall<*>> ->
        renderAmbiguousDescriptors(calls.map { it.resultingDescriptor })
    }

    @JvmField
    val HTML_COMPATIBILITY_CANDIDATE = renderer { call: CallableDescriptor ->
        renderAmbiguousDescriptors(listOf(call))
    }

    @JvmField
    val HTML_AMBIGUOUS_REFERENCES = renderer { descriptors: Collection<CallableDescriptor> ->
        renderAmbiguousDescriptors(descriptors)
    }

    private fun renderAmbiguousDescriptors(descriptors: Collection<CallableDescriptor>): String {
        val sortedDescriptors = descriptors.sortedWith(MemberComparator)
        val context = RenderingContext.Impl(sortedDescriptors)
        return sortedDescriptors.joinToString("") { "<li>${HTML.render(it, context)}</li>" }
    }

    @JvmField
    val HTML_RENDER_TYPE = SmartTypeRenderer(DescriptorRenderer.HTML.withOptions {
        parameterNamesInFunctionalTypes = false
        modifiers = DescriptorRendererModifier.ALL_EXCEPT_ANNOTATIONS
    })

    @JvmField
    val HTML_NONE_APPLICABLE_CALLS = renderer { calls: Collection<ResolvedCall<*>> ->
        val context = RenderingContext.Impl(calls.map { it.resultingDescriptor })
        val comparator = compareBy(MemberComparator) { c: ResolvedCall<*> -> c.resultingDescriptor }
        calls
            .sortedWith(comparator)
            .joinToString("") { "<li>${renderResolvedCall(it, context)}</li>" }
    }




    @JvmField
    val HTML_RENDER_RETURN_TYPE = ContextDependentRenderer<CallableMemberDescriptor> { member, context ->
        HTML_RENDER_TYPE.render(member.returnType!!, context)
    }



    @JvmField
    val HTML_THROWABLE = renderer<Throwable> { throwable ->
        if (throwable.message != null) {
            "${throwable::class.java.simpleName}: ${throwable.message}"
        } else {
            "<pre>${CommonRenderers.THROWABLE.render(throwable)}</pre>"
        }.replace("\n", "<br/>")
    }

    @JvmField
    val HTML = DescriptorRenderer.HTML.withOptions {
        modifiers = DescriptorRendererModifier.ALL_EXCEPT_ANNOTATIONS
    }.asRenderer()

    @JvmField
    val HTML_WITH_ANNOTATIONS = DescriptorRenderer.HTML.withOptions {
        modifiers = DescriptorRendererModifier.ALL
    }.asRenderer()

    @JvmField
    val HTML_WITH_ANNOTATIONS_WHITELIST = DescriptorRenderer.HTML.withAnnotationsWhitelist()
}
