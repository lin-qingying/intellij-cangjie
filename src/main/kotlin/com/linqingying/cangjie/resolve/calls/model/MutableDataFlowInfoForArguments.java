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

package com.linqingying.cangjie.resolve.calls.model;


import com.linqingying.cangjie.psi.ValueArgument;
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.annotations.NotNull;

public abstract class MutableDataFlowInfoForArguments implements DataFlowInfoForArguments {

    @NotNull
    protected final DataFlowInfo initialDataFlowInfo;

    public MutableDataFlowInfoForArguments(@NotNull DataFlowInfo initialDataFlowInfo) {
        this.initialDataFlowInfo = initialDataFlowInfo;
    }

    public abstract void updateInfo(@NotNull ValueArgument valueArgument, @NotNull DataFlowInfo dataFlowInfo);
    public abstract void updateResultInfo(@NotNull DataFlowInfo dataFlowInfo);

    @NotNull
    @Override
    public DataFlowInfo getResultInfo() {
        return initialDataFlowInfo;
    }

    public static class WithoutArgumentsCheck extends MutableDataFlowInfoForArguments {

        public WithoutArgumentsCheck(@NotNull DataFlowInfo dataFlowInfo) {
            super(dataFlowInfo);
        }

        @Override
        public void updateInfo(@NotNull ValueArgument valueArgument, @NotNull DataFlowInfo dataFlowInfo) {
            throw new IllegalStateException();
        }

        @Override
        public void updateResultInfo(@NotNull DataFlowInfo dataFlowInfo) {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public DataFlowInfo getInfo(@NotNull ValueArgument valueArgument) {
            throw new IllegalStateException();
        }
    }
}
