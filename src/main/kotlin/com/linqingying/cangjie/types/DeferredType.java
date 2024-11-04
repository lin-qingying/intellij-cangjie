package com.linqingying.cangjie.types;


import com.linqingying.cangjie.descriptors.BindingTrace;
import com.linqingying.cangjie.storage.NotNullLazyValue;
import com.linqingying.cangjie.storage.ReenteringLazyValueComputationException;
import com.linqingying.cangjie.storage.StorageManager;
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner;
import com.linqingying.cangjie.types.error.ErrorTypeKind;
import com.linqingying.cangjie.utils.Box;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;

import static com.linqingying.cangjie.resolve.BindingContext.DEFERRED_TYPE;

public class DeferredType extends WrappedType {
    private static final Function1<Boolean, CangJieType> RECURSION_PREVENTER = firstTime -> {
        if (firstTime) throw new ReenteringLazyValueComputationException();
        return ErrorUtils.createErrorType(ErrorTypeKind.RECURSIVE_TYPE);
    };

    @NotNull
    /*package private*/ static DeferredType create(
            @NotNull StorageManager storageManager,
            @NotNull BindingTrace trace,
            @NotNull Function0<CangJieType> compute
    ) {
        DeferredType deferredType = new DeferredType(storageManager.createLazyValue(compute));
        trace.record(DEFERRED_TYPE, new Box<>(deferredType));
        return deferredType;
    }

    @NotNull
    /*package private*/ static DeferredType createRecursionIntolerant(
            @NotNull StorageManager storageManager,
            @NotNull BindingTrace trace,
            @NotNull Function0<CangJieType> compute
    ) {
        DeferredType deferredType = new DeferredType(storageManager.createLazyValue(compute, RECURSION_PREVENTER));
        trace.record(DEFERRED_TYPE, new Box<>(deferredType));
        return deferredType;
    }

    private final NotNullLazyValue<CangJieType> lazyValue;

    private DeferredType(@NotNull NotNullLazyValue<CangJieType> lazyValue) {
        super();
        this.lazyValue = lazyValue;
    }

    @NotNull
    @Override
    public CangJieType refine(@NotNull CangJieTypeRefiner cangjieTypeRefiner) {
        return new DeferredType(new NotNullLazyValue<CangJieType>() {
            @NotNull
            @Override
            public String renderDebugInformation() {
                return lazyValue.renderDebugInformation();
            }

            @Override
            public boolean isComputed() {
                return lazyValue.isComputed();
            }

            @Override
            public boolean isComputing() {
                return lazyValue.isComputing();
            }

            @Override
            @TypeRefinement
            public CangJieType invoke() {
                return cangjieTypeRefiner.refineType(lazyValue.invoke());
            }
        });
    }

    public boolean isComputing() {
        return lazyValue.isComputing();
    }

    @Override
    public boolean isComputed() {
        return lazyValue.isComputed();
    }

    @NotNull
    @Override
    public CangJieType getDelegate() {
        return lazyValue.invoke();
    }

    @NotNull
    @Override
    public String toString() {
        try {
            if (lazyValue.isComputed()) {
                return getDelegate().toString();
            }
            else {
                return "<Not computed yet>";
            }
        }
        catch (ReenteringLazyValueComputationException e) {
            return "<Failed to compute this type>";
        }
    }
}
