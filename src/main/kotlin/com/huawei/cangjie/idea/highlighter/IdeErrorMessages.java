package com.huawei.cangjie.idea.highlighter;


import com.huawei.cangjie.diagnostics.UnboundDiagnostic;
import com.huawei.cangjie.diagnostics.rendering.DefaultErrorMessages;
import com.huawei.cangjie.diagnostics.rendering.DiagnosticFactoryToRendererMap;
import com.huawei.cangjie.diagnostics.rendering.DiagnosticRenderer;
import com.huawei.cangjie.idea.completion.back.CangJieHighlightingBundle;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

public final class IdeErrorMessages {
    private static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap("IDE");

    // TODO: i18n
    @NlsSafe
    @NotNull
    public static String render(@NotNull UnboundDiagnostic diagnostic) {
        DiagnosticRenderer renderer = MAP.get(diagnostic.getFactory());

        if (renderer != null) {
            //noinspection unchecked
            return renderer.render(diagnostic);
        }

        return DefaultErrorMessages.render(diagnostic);
    }

    @TestOnly
    public static boolean hasIdeSpecificMessage(@NotNull UnboundDiagnostic diagnostic) {
        return MAP.get(diagnostic.getFactory()) != null;
    }

//    static {
//        MAP.put(TYPE_MISMATCH, CangJieHighlightingBundle.htmlMessage("html.type.mismatch.table.tr.td.required.td.td.0.td.tr.tr.td.found.td.td.1.td.tr.table.html"), HTML_RENDER_TYPE, HTML_RENDER_TYPE);
//        MAP.put(NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS, CangJieHighlightingBundle.htmlMessage("html.type.mismatch.table.tr.td.required.td.td.0.td.tr.tr.td.found.td.td.1.td.tr.table.html"), HTML_RENDER_TYPE, HTML_RENDER_TYPE);
//
//        MAP.put(
//                TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS,
//                CangJieHighlightingBundle.htmlMessage("html.type.mismatch.table.tr.td.required.td.td.0.td.tr.tr.td.found.td.td.1.td.tr.table.br.projected.type.2.restricts.use.of.br.3.html"),
//                object -> {
//                    RenderingContext context = RenderingContext
//                            .of(object.getExpectedType(), object.getExpressionType(), object.getReceiverType(),
//                                    object.getCallableDescriptor());
//
//                    return new String[] {
//                            HTML_RENDER_TYPE.render(object.getExpectedType(), context),
//                            HTML_RENDER_TYPE.render(object.getExpressionType(), context),
//                            HTML_RENDER_TYPE.render(object.getReceiverType(), context),
//                            HTML.render(object.getCallableDescriptor(), context)
//                    };
//                }
//        );
//
//        MAP.put(ASSIGN_OPERATOR_AMBIGUITY, CangJieHighlightingBundle.htmlMessage("html.assignment.operators.ambiguity.all.these.functions.match.ul.0.ul.table.html"), HTML_AMBIGUOUS_CALLS);
//        MAP.put(TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS, CangJieHighlightingBundle.htmlMessage("html.type.inference.failed.0.html"), HTML_TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS_RENDERER);
//        MAP.put(TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER, CangJieHighlightingBundle.htmlMessage("html.type.inference.failed.0.html"), HTML_TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER_RENDERER);
//        MAP.put(TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR, CangJieHighlightingBundle.htmlMessage("html.type.inference.failed.0.html"), HTML_TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR_RENDERER);
//
//        MAP.put(TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH, tableForTypes(
//                CangJieHighlightingBundle.message("type.inference.failed.expected.type.mismatch"),
//                CangJieHighlightingBundle.message("required.space"), TextElementType.STRONG,
//                CangJieHighlightingBundle.message("found.space"), TextElementType.ERROR), HTML_RENDER_TYPE, HTML_RENDER_TYPE
//        );
//
//        MAP.put(TYPE_INFERENCE_UPPER_BOUND_VIOLATED, "<html>{0}</html>", HTML_TYPE_INFERENCE_UPPER_BOUND_VIOLATED_RENDERER);
//        MAP.put(WRONG_SETTER_PARAMETER_TYPE, CangJieHighlightingBundle.htmlMessage("html.setter.parameter.type.must.be.equal.to.the.type.of.the.property.table.tr.td.expected.td.td.0.td.tr.tr.td.found.td.td.1.td.tr.table.html"), HTML_RENDER_TYPE, HTML_RENDER_TYPE);
//        MAP.put(WRONG_GETTER_RETURN_TYPE, CangJieHighlightingBundle.htmlMessage("html.getter.return.type.must.be.equal.to.the.type.of.the.property.table.tr.td.expected.td.td.0.td.tr.tr.td.found.td.td.1.td.tr.table.html"), HTML_RENDER_TYPE, HTML_RENDER_TYPE);
//        MAP.put(ITERATOR_AMBIGUITY, CangJieHighlightingBundle.htmlMessage("html.method.iterator.is.ambiguous.for.this.expression.ul.0.ul.html"), HTML_AMBIGUOUS_CALLS);
//        MAP.put(UPPER_BOUND_VIOLATED, CangJieHighlightingBundle.htmlMessage("html.type.argument.is.not.within.its.bounds.table.tr.td.expected.td.td.0.td.tr.tr.td.found.td.td.1.td.tr.table.html"), HTML_RENDER_TYPE, HTML_RENDER_TYPE);
//        MAP.put(TYPE_MISMATCH_IN_FOR_LOOP, CangJieHighlightingBundle.htmlMessage("html.loop.parameter.type.mismatch.table.tr.td.iterated.values.td.td.0.td.tr.tr.td.parameter.td.td.1.td.tr.table.html"), HTML_RENDER_TYPE, HTML_RENDER_TYPE);
//        MAP.put(RETURN_TYPE_MISMATCH_ON_OVERRIDE, CangJieHighlightingBundle.htmlMessage("html.return.type.is.0.which.is.not.a.subtype.of.overridden.br.1.html"), HTML_RENDER_RETURN_TYPE, HTML_WITH_ANNOTATIONS_WHITELIST);
//        MAP.put(RETURN_TYPE_MISMATCH_ON_INHERITANCE, CangJieHighlightingBundle.htmlMessage("html.return.types.of.inherited.members.are.incompatible.br.0.br.1.html"), HTML, HTML);
//        MAP.put(PROPERTY_TYPE_MISMATCH_ON_OVERRIDE, CangJieHighlightingBundle.htmlMessage("html.property.type.is.0.which.is.not.a.subtype.type.of.overridden.br.1.html"), HTML_RENDER_RETURN_TYPE, HTML);
//        MAP.put(VAR_TYPE_MISMATCH_ON_OVERRIDE, CangJieHighlightingBundle.htmlMessage("html.var.property.type.is.0.which.is.not.a.type.of.overridden.br.1.html"), HTML_RENDER_RETURN_TYPE, HTML);
//        MAP.put(PROPERTY_TYPE_MISMATCH_ON_INHERITANCE, CangJieHighlightingBundle.htmlMessage("html.types.of.inherited.properties.are.incompatible.br.0.br.1.html"), HTML, HTML);
//        MAP.put(VAR_TYPE_MISMATCH_ON_INHERITANCE, CangJieHighlightingBundle.htmlMessage("html.types.of.inherited.var.properties.do.not.match.br.0.br.1.html"), HTML, HTML);
//        MAP.put(VAR_OVERRIDDEN_BY_VAL, CangJieHighlightingBundle.htmlMessage("html.val.property.cannot.override.var.property.br.1.html"), HTML, HTML);
//        MAP.put(VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION, CangJieHighlightingBundle.htmlMessage("html.val.property.cannot.override.var.property.br.1.html"), HTML, HTML);
//        MAP.put(ABSTRACT_MEMBER_NOT_IMPLEMENTED, CangJieHighlightingBundle.htmlMessage("html.0.is.not.abstract.and.does.not.implement.abstract.member.br.1.html"), RENDER_CLASS_OR_OBJECT, HTML);
//        MAP.put(ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED, CangJieHighlightingBundle.htmlMessage("html.0.is.not.abstract.and.does.not.implement.abstract.base.class.member.br.1.html"), RENDER_CLASS_OR_OBJECT, HTML);
//        MAP.put(MANY_IMPL_MEMBER_NOT_IMPLEMENTED, CangJieHighlightingBundle.htmlMessage("html.0.must.override.1.br.because.it.inherits.many.implementations.of.it.html"), RENDER_CLASS_OR_OBJECT, HTML);
//        MAP.put(RESULT_TYPE_MISMATCH, CangJieHighlightingBundle.htmlMessage("html.function.return.type.mismatch.table.tr.td.expected.td.td.1.td.tr.tr.td.found.td.td.2.td.tr.table.html"), STRING, HTML_RENDER_TYPE, HTML_RENDER_TYPE);
//        MAP.put(OVERLOAD_RESOLUTION_AMBIGUITY, CangJieHighlightingBundle.htmlMessage("html.overload.resolution.ambiguity.all.these.functions.match.ul.0.ul.html"), HTML_AMBIGUOUS_CALLS);
//        MAP.put(CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY, CangJieHighlightingBundle.htmlMessage("html.overload.resolution.ambiguity.all.these.functions.match.ul.0.ul.html"), HTML_AMBIGUOUS_REFERENCES);
//        MAP.put(NONE_APPLICABLE, CangJieHighlightingBundle.htmlMessage("html.none.of.the.following.functions.can.be.called.with.the.arguments.supplied.ul.0.ul.html"), HTML_NONE_APPLICABLE_CALLS);
//        MAP.put(CANNOT_COMPLETE_RESOLVE, CangJieHighlightingBundle.htmlMessage("html.cannot.choose.among.the.following.candidates.without.completing.type.inference.ul.0.ul.html"), HTML_AMBIGUOUS_CALLS);
//        MAP.put(UNRESOLVED_REFERENCE_WRONG_RECEIVER, CangJieHighlightingBundle.htmlMessage("html.unresolved.reference.br.none.of.the.following.candidates.is.applicable.because.of.receiver.type.mismatch.ul.0.ul.html"), HTML_AMBIGUOUS_CALLS);
//        MAP.put(DELEGATE_SPECIAL_FUNCTION_AMBIGUITY, CangJieHighlightingBundle.htmlMessage("html.overload.resolution.ambiguity.on.method.0.all.these.functions.match.ul.1.ul.html"), STRING, HTML_AMBIGUOUS_CALLS);
//        MAP.put(DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, CangJieHighlightingBundle.htmlMessage("html.property.delegate.must.have.a.0.method.none.of.the.following.functions.are.suitable.ul.1.ul.html"), STRING, HTML_NONE_APPLICABLE_CALLS);
//        MAP.put(DELEGATE_PD_METHOD_NONE_APPLICABLE, CangJieHighlightingBundle.htmlMessage("html.0.method.may.be.missing.none.of.the.following.functions.will.be.called.ul.1.ul.html"), STRING, HTML_NONE_APPLICABLE_CALLS);
//        MAP.put(COMPATIBILITY_WARNING, CangJieHighlightingBundle.htmlMessage("html.candidate.resolution.will.be.changed.soon.please.use.fully.qualified.name.to.invoke.the.following.closer.candidate.explicitly.ul.0.ul.html"), HTML_COMPATIBILITY_CANDIDATE);
//        MAP.put(CONFLICTING_JVM_DECLARATIONS, CangJieHighlightingBundle.htmlMessage("html.platform.declaration.clash.0.html"), HTML_CONFLICTING_JVM_DECLARATIONS_DATA);
//        MAP.put(ACCIDENTAL_OVERRIDE, CangJieHighlightingBundle.htmlMessage("html.accidental.override.0.html"), HTML_CONFLICTING_JVM_DECLARATIONS_DATA);
//        MAP.put(EXCEPTION_FROM_ANALYZER, CangJieHighlightingBundle.htmlMessage("html.internal.error.occurred.while.analyzing.this.expression.br.0.html"), HTML_THROWABLE);
//        MAP.put(ErrorsJs.JSCODE_ERROR, CangJieHighlightingBundle.htmlMessage("html.javascript.0.html"), JsCallDataHtmlRenderer.INSTANCE);
//        MAP.put(ErrorsJs.JSCODE_WARNING, CangJieHighlightingBundle.htmlMessage("html.javascript.0.html"), JsCallDataHtmlRenderer.INSTANCE);
//        MAP.put(UNSUPPORTED_FEATURE, "<html>{0}</html>", new LanguageFeatureMessageRenderer(LanguageFeatureMessageRenderer.Type.UNSUPPORTED, true));
//        MAP.put(EXPERIMENTAL_FEATURE_WARNING, "<html>{0}</html>", new LanguageFeatureMessageRenderer(LanguageFeatureMessageRenderer.Type.WARNING, true));
//        MAP.put(NO_ACTUAL_FOR_EXPECT, CangJieHighlightingBundle.htmlMessage("html.expected.0.has.no.actual.declaration.in.module.1.2.html"), DECLARATION_NAME_WITH_KIND, MODULE_WITH_PLATFORM, adaptGenerics1(new PlatformIncompatibilityDiagnosticRenderer(IdeMultiplatformDiagnosticRenderingMode.INSTANCE)));
//        MAP.put(ACTUAL_WITHOUT_EXPECT, CangJieHighlightingBundle.htmlMessage("html.0.has.no.corresponding.expected.declaration.1.html"), CAPITALIZED_DECLARATION_NAME_WITH_KIND_AND_PLATFORM, adaptGenerics1(new PlatformIncompatibilityDiagnosticRenderer(IdeMultiplatformDiagnosticRenderingMode.INSTANCE)));
//        MAP.put(NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS, CangJieHighlightingBundle.htmlMessage("html.actual.class.0.has.no.corresponding.members.for.expected.class.members.1.html"), NAME, adaptGenerics2(new IncompatibleExpectedActualClassScopesRenderer(IdeMultiplatformDiagnosticRenderingMode.INSTANCE)));
//        MAP.put(ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING, CangJieHighlightingBundle.htmlMessage("html.non.final.expect.class.and.its.actual.class.must.declare.exactly.the.same.non.private.members.html"), CAPITALIZED_DECLARATION_NAME_WITH_KIND_AND_PLATFORM, new ExpectActualScopeDiffsRenderer(IdeMultiplatformDiagnosticRenderingMode.INSTANCE), NAME);
//        MAP.put(CONCURRENT_HASH_MAP_CONTAINS_OPERATOR.getErrorFactory(), CangJieHighlightingBundle.htmlMessage("html.method.contains.from.concurrenthashmap.may.have.unexpected.semantics.it.calls.containsvalue.instead.of.containskey.br.use.explicit.form.of.the.call.to.containskey.containsvalue.contains.or.cast.the.value.to.kotlin.collections.map.instead.br.see.https.youtrack.jetbrains.com.issue.kt.18053.for.more.details.html"));
//        MAP.put(CONCURRENT_HASH_MAP_CONTAINS_OPERATOR.getWarningFactory(), CangJieHighlightingBundle.htmlMessage("html.method.contains.from.concurrenthashmap.may.have.unexpected.semantics.it.calls.containsvalue.instead.of.containskey.br.use.explicit.form.of.the.call.to.containskey.containsvalue.contains.or.cast.the.value.to.kotlin.collections.map.instead.br.see.https.youtrack.jetbrains.com.issue.kt.18053.for.more.details.html"));
//
//        MAP.setImmutable();
//    }

    private IdeErrorMessages() {}
}
