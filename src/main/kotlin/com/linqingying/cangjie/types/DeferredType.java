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
