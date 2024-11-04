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
