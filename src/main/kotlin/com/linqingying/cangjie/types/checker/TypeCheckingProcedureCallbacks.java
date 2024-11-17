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

package com.linqingying.cangjie.types.checker;

import com.linqingying.cangjie.types.CangJieType;
import com.linqingying.cangjie.types.TypeConstructor;
import com.linqingying.cangjie.types.TypeProjection;
import org.jetbrains.annotations.NotNull;


/**
 * Methods of this class return true to continue type checking and false to fail
 */
public interface TypeCheckingProcedureCallbacks {
    boolean assertEqualTypes(@NotNull CangJieType a, @NotNull CangJieType b, @NotNull TypeCheckingProcedure typeCheckingProcedure);

    boolean assertEqualTypeConstructors(@NotNull TypeConstructor a, @NotNull TypeConstructor b);

    boolean assertSubtype(@NotNull CangJieType subtype, @NotNull CangJieType supertype, @NotNull TypeCheckingProcedure typeCheckingProcedure);

    boolean capture(@NotNull CangJieType type, @NotNull TypeProjection typeProjection);

    boolean noCorrespondingSupertype(@NotNull CangJieType subtype, @NotNull CangJieType supertype);
}
