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

package com.linqingying.cangjie.resolve.calls.results;


import com.linqingying.cangjie.descriptors.CallableDescriptor;
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface OverloadResolutionResults<D extends CallableDescriptor> {
    /* All candidates are collected only if ResolutionContext.collectAllCandidates is set to true */
    @Nullable
    Collection<ResolvedCall<D>> getAllCandidates();

    @NotNull
    Collection<? extends ResolvedCall<D>> getResultingCalls();

    @NotNull
    ResolvedCall<D> getResultingCall();

    @NotNull
    D getResultingDescriptor();

    @NotNull
    Code getResultCode();

    boolean isSuccess();

    boolean isSingleResult();

    boolean isNothing();

    boolean isAmbiguity();

    boolean isIncomplete();

    default OverloadResolutionResults<D> replaceCode(@NotNull Code newCode) {
        return this;
    }


    enum Code {
        SUCCESS(true), // 操作成功
        NAME_NOT_FOUND(false), // 未找到名称
        SINGLE_CANDIDATE_ARGUMENT_MISMATCH(false), // 单一候选者参数不匹配
        AMBIGUITY(false), // 存在歧义
        MANY_FAILED_CANDIDATES(false), // 多个候选者失败
        CANDIDATES_WITH_WRONG_RECEIVER(false), // 候选者接收者错误
        INCOMPLETE_TYPE_INFERENCE(false), // 类型推断不完整
        SUCCESS_NAME_NOT_FOUND(true) // 虽然是成功，但是没有找到合适的函数
        ;

        private final boolean success;

        Code(boolean success) {
            this.success = success;
        }

        boolean isSuccess() {
            return success;
        }
    }
}
