/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie


/**
 * Indicates sensitive frontend API, which should be used with caution to avoid invariant violation.
 * Use sites of this annotation include all methods for direct access to frontend components.
 * Please make sure that components don't receive resolution results (descriptors etc.) from different resolution facade for processing.
 * The simplest way to do so is to explicitly provide the same resolution facade to all related computations.
 * Not following this rule may lead to obscure memory leaks and other potential problems.
 */
@RequiresOptIn
annotation class FrontendInternals
