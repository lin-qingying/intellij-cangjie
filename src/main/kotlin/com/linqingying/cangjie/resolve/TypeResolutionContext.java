package com.linqingying.cangjie.resolve;

import com.linqingying.cangjie.descriptors.BindingTrace;
import com.linqingying.cangjie.resolve.scopes.LexicalScope;
import org.jetbrains.annotations.NotNull;

public class TypeResolutionContext {

    public final LexicalScope scope;
    public final BindingTrace trace;
    public final boolean checkBounds;
    public final boolean allowBareTypes;
    public final boolean allowIntersectionTypes;
    public final boolean isDebuggerContext;
    public final boolean abbreviated;

    public TypeResolutionContext(@NotNull LexicalScope scope, @NotNull BindingTrace trace, boolean checkBounds, boolean allowBareTypes, boolean isDebuggerContext) {
        this(scope, trace, checkBounds, allowBareTypes, isDebuggerContext, false, false);
    }

    public TypeResolutionContext(
            @NotNull LexicalScope scope,
            @NotNull BindingTrace trace,
            boolean checkBounds,
            boolean allowBareTypes,
            boolean isDebuggerContext,
            boolean abbreviated
    ) {
        this(scope, trace, checkBounds, allowBareTypes, isDebuggerContext, abbreviated, false);
    }

    public TypeResolutionContext(
            @NotNull LexicalScope scope,
            @NotNull BindingTrace trace,
            boolean checkBounds,
            boolean allowBareTypes,
            boolean isDebuggerContext,
            boolean abbreviated,
            boolean allowIntersectionTypes
    ) {
        this.scope = scope;
        this.trace = trace;
        this.checkBounds = checkBounds;
        this.allowBareTypes = allowBareTypes;
        this.isDebuggerContext = isDebuggerContext;
        this.abbreviated = abbreviated;
        this.allowIntersectionTypes = allowIntersectionTypes;
    }

    @NotNull
    public TypeResolutionContext noBareTypes() {
        return new TypeResolutionContext(scope, trace, checkBounds, false, isDebuggerContext, abbreviated, allowIntersectionTypes);
    }

}
