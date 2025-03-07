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

package cn.cangnova.cangjie.ide.inspections.suppress

import com.intellij.codeInsight.intention.FileModifier
import com.intellij.openapi.util.NlsSafe
import org.jetbrains.annotations.Nls


@FileModifier.SafeTypeForPreview
class AnnotationHostKind(
    /** Human-readable `CjElement` kind on which the annotation is placed. E.g., 'file', 'class' or 'statement'. */
    @Nls val kind: String,

    /** Name of the annotation owner. Might be null if the owner is not a named declaration (for instance, if it is a statement). */
    @NlsSafe val name: String?,

    /** True if the annotation needs to be added to a separate line. */
    val newLineNeeded: Boolean
)
