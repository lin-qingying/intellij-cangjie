package com.linqingying.cangjie.highlighter;


import com.linqingying.cangjie.diagnostics.Errors;
import com.linqingying.cangjie.diagnostics.TypeMismatchDueToTypeProjectionsData;
import com.linqingying.cangjie.diagnostics.UnboundDiagnostic;
import static com.linqingying.cangjie.diagnostics.rendering.CommonRenderers.STRING;
import com.linqingying.cangjie.diagnostics.rendering.DefaultErrorMessages;
import com.linqingying.cangjie.diagnostics.rendering.DiagnosticFactoryToRendererMap;
import static com.linqingying.cangjie.diagnostics.rendering.Renderers.RENDER_TYPE_STATMENT;

import com.linqingying.cangjie.diagnostics.rendering.DiagnosticRenderer;
import com.linqingying.cangjie.diagnostics.rendering.RenderingContext;
import static com.linqingying.cangjie.highlighter.IdeRenderers.HTML;
import static com.linqingying.cangjie.highlighter.IdeRenderers.HTML_AMBIGUOUS_CALLS;
import static com.linqingying.cangjie.highlighter.IdeRenderers.HTML_AMBIGUOUS_REFERENCES;
import static com.linqingying.cangjie.highlighter.IdeRenderers.HTML_COMPATIBILITY_CANDIDATE;
import static com.linqingying.cangjie.highlighter.IdeRenderers.HTML_NONE_APPLICABLE_CALLS;
import static com.linqingying.cangjie.highlighter.IdeRenderers.HTML_RENDER_RETURN_TYPE;
import static com.linqingying.cangjie.highlighter.IdeRenderers.HTML_RENDER_TYPE;
import static com.linqingying.cangjie.highlighter.IdeRenderers.HTML_THROWABLE;
import static com.linqingying.cangjie.highlighter.IdeRenderers.HTML_WITH_ANNOTATIONS_WHITELIST;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.TestOnly;
/**
 * @see DefaultErrorMessages
 */
public class IdeErrorMessages {
    private static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap("IDE");

    // TODO: i18n
    public static String render(UnboundDiagnostic diagnostic) {
        DiagnosticRenderer renderer = MAP.get(diagnostic.getFactory());
        if (renderer != null) {
            return renderer.render(diagnostic);
        }
        return DefaultErrorMessages.render(diagnostic);
    }

    @TestOnly
    public static boolean hasIdeSpecificMessage(UnboundDiagnostic diagnostic) {
        return MAP.get(diagnostic.getFactory()) != null;
    }

    static {
        MAP.put(
                Errors.TYPE_MISMATCH,
                CangJieHighlightingBundle.htmlMessage("html.type.mismatch.table.tr.td.required.td.td.0.td.tr.tr.td.found.td.td.1.td.tr.table.html"),
                HTML_RENDER_TYPE,
                HTML_RENDER_TYPE
        );

        MAP.put(
                Errors.TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS,
                CangJieHighlightingBundle.htmlMessage("html.type.mismatch.table.tr.td.required.td.td.0.td.tr.tr.td.found.td.td.1.td.tr.table.br.projected.type.2.restricts.use.of.br.3.html"),
                object -> {
                    RenderingContext context = RenderingContext
                            .of(object.getExpectedType(), object.getExpressionType(), object.getReceiverType(),
                                    object.getCallableDescriptor());

                    return new String[] {
                            HTML_RENDER_TYPE.render(object.getExpectedType(), context),
                            HTML_RENDER_TYPE.render(object.getExpressionType(), context),
                            HTML_RENDER_TYPE.render(object.getReceiverType(), context),
                            HTML.render(object.getCallableDescriptor(), context)
                    };
                }
        );

        MAP.put(
                Errors.ASSIGN_OPERATOR_AMBIGUITY,
                CangJieHighlightingBundle.htmlMessage("html.assignment.operators.ambiguity.all.these.functions.match.ul.0.ul.table.html"),
                HTML_AMBIGUOUS_CALLS
        );

        MAP.put(
                Errors.WRONG_SETTER_PARAMETER_TYPE,
                CangJieHighlightingBundle.htmlMessage("html.setter.parameter.type.must.be.equal.to.the.type.of.the.property.table.tr.td.expected.td.td.0.td.tr.tr.td.found.td.td.1.td.tr.table.html"),
                HTML_RENDER_TYPE,
                HTML_RENDER_TYPE
        );

        MAP.put(
                Errors.WRONG_GETTER_RETURN_TYPE,
                CangJieHighlightingBundle.htmlMessage("html.getter.return.type.must.be.equal.to.the.type.of.the.property.table.tr.td.expected.td.td.0.td.tr.tr.td.found.td.td.1.td.tr.table.html"),
                HTML_RENDER_TYPE,
                HTML_RENDER_TYPE
        );

        MAP.put(
                Errors.ITERATOR_AMBIGUITY,
                CangJieHighlightingBundle.htmlMessage("html.method.iterator.is.ambiguous.for.this.expression.ul.0.ul.html"),
                HTML_AMBIGUOUS_CALLS
        );

        MAP.put(
                Errors.UPPER_BOUND_VIOLATED,
                CangJieHighlightingBundle.htmlMessage("html.type.argument.is.not.within.its.bounds.table.tr.td.expected.td.td.0.td.tr.tr.td.found.td.td.1.td.tr.table.html"),
                HTML_RENDER_TYPE,
                HTML_RENDER_TYPE
        );

        MAP.put(
                Errors.TYPE_MISMATCH_IN_FOR_LOOP,
                CangJieHighlightingBundle.htmlMessage("html.loop.parameter.type.mismatch.table.tr.td.iterated.values.td.td.0.td.tr.tr.td.parameter.td.td.1.td.tr.table.html"),
                HTML_RENDER_TYPE,
                HTML_RENDER_TYPE
        );

        MAP.put(
                Errors.RETURN_TYPE_MISMATCH_ON_OVERRIDE,
                CangJieHighlightingBundle.htmlMessage("html.return.type.is.0.which.is.not.a.subtype.of.overridden.br.1.html"),
                HTML_RENDER_RETURN_TYPE,
                HTML_WITH_ANNOTATIONS_WHITELIST
        );

        MAP.put(
                Errors.RETURN_TYPE_MISMATCH_ON_INHERITANCE,
                CangJieHighlightingBundle.htmlMessage("html.return.types.of.inherited.members.are.incompatible.br.0.br.1.html"),
                HTML,
                HTML
        );

        MAP.put(
                Errors.PROPERTY_TYPE_MISMATCH_ON_OVERRIDE,
                CangJieHighlightingBundle.htmlMessage("html.property.type.is.0.which.is.not.a.subtype.type.of.overridden.br.1.html"),
                HTML_RENDER_RETURN_TYPE,
                HTML
        );

        MAP.put(
                Errors.VAR_TYPE_MISMATCH_ON_OVERRIDE,
                CangJieHighlightingBundle.htmlMessage("html.var.property.type.is.0.which.is.not.a.type.of.overridden.br.1.html"),
                HTML_RENDER_RETURN_TYPE,
                HTML
        );

        MAP.put(
                Errors.PROPERTY_TYPE_MISMATCH_ON_INHERITANCE,
                CangJieHighlightingBundle.htmlMessage("html.types.of.inherited.properties.are.incompatible.br.0.br.1.html"),
                HTML,
                HTML
        );

        MAP.put(
                Errors.VAR_TYPE_MISMATCH_ON_INHERITANCE,
                CangJieHighlightingBundle.htmlMessage("html.types.of.inherited.var.properties.do.not.match.br.0.br.1.html"),
                HTML,
                HTML
        );

        MAP.put(
                Errors.VAR_OVERRIDDEN_BY_LET,
                CangJieHighlightingBundle.htmlMessage("html.val.property.cannot.override.var.property.br.1.html"),
                HTML,
                HTML
        );

        MAP.put(
                Errors.VAR_OVERRIDDEN_BY_LET_BY_DELEGATION,
                CangJieHighlightingBundle.htmlMessage("html.val.property.cannot.override.var.property.br.1.html"),
                HTML,
                HTML
        );

        MAP.put(
                Errors.ABSTRACT_MEMBER_NOT_IMPLEMENTED,
                CangJieHighlightingBundle.htmlMessage("html.0.is.not.abstract.and.does.not.implement.abstract.member.br.1.html"),
                RENDER_TYPE_STATMENT,
                HTML
        );

        MAP.put(
                Errors.ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED,
                CangJieHighlightingBundle.htmlMessage("html.0.is.not.abstract.and.does.not.implement.abstract.base.class.member.br.1.html"),
                RENDER_TYPE_STATMENT,
                HTML
        );

        MAP.put(
                Errors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED,
                CangJieHighlightingBundle.htmlMessage("html.0.must.override.1.br.because.it.inherits.many.implementations.of.it.html"),
                RENDER_TYPE_STATMENT,
                HTML
        );

        MAP.put(
                Errors.RESULT_TYPE_MISMATCH,
                CangJieHighlightingBundle.htmlMessage("html.function.return.type.mismatch.table.tr.td.expected.td.td.1.td.tr.tr.td.found.td.td.2.td.tr.table.html"),
                STRING,
                HTML_RENDER_TYPE,
                HTML_RENDER_TYPE
        );

        MAP.put(
                Errors.OVERLOAD_RESOLUTION_AMBIGUITY,
                CangJieHighlightingBundle.htmlMessage("html.overload.resolution.ambiguity.all.these.functions.match.ul.0.ul.html"),
                HTML_AMBIGUOUS_CALLS
        );

        MAP.put(
                Errors.CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY,
                CangJieHighlightingBundle.htmlMessage("html.overload.resolution.ambiguity.all.these.functions.match.ul.0.ul.html"),
                HTML_AMBIGUOUS_REFERENCES
        );

        MAP.put(
                Errors.NONE_APPLICABLE,
                CangJieHighlightingBundle.htmlMessage("html.none.of.the.following.functions.can.be.called.with.the.arguments.supplied.ul.0.ul.html"),
                HTML_NONE_APPLICABLE_CALLS
        );

        MAP.put(
                Errors.CANNOT_COMPLETE_RESOLVE,
                CangJieHighlightingBundle.htmlMessage("html.cannot.choose.among.the.following.candidates.without.completing.type.inference.ul.0.ul.html"),
                HTML_AMBIGUOUS_CALLS
        );

//        MAP.put(
//                Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER,
//                CangJieHighlightingBundle.htmlMessage("html.unresolved.reference.br.none.of.the.following.candidates.is.applicable.because.of.receiver.type.mismatch.ul.0.ul.html"),
//                HTML_AMBIGUOUS_CALLS
//        );

        MAP.put(
                Errors.DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE,
                CangJieHighlightingBundle.htmlMessage("html.property.delegate.must.have.a.0.method.none.of.the.following.functions.are.suitable.ul.1.ul.html"),
                STRING,
                HTML_NONE_APPLICABLE_CALLS
        );

        MAP.put(
                Errors.COMPATIBILITY_WARNING,
                CangJieHighlightingBundle.htmlMessage("html.candidate.resolution.will.be.changed.soon.please.use.fully.qualified.name.to.invoke.the.following.closer.candidate.explicitly.ul.0.ul.html"),
                HTML_COMPATIBILITY_CANDIDATE
        );

        MAP.put(
                Errors.EXCEPTION_FROM_ANALYZER,
                CangJieHighlightingBundle.htmlMessage("html.internal.error.occurred.while.analyzing.this.expression.br.0.html"),
                HTML_THROWABLE
        );

        MAP.setImmutable();
    }


}
