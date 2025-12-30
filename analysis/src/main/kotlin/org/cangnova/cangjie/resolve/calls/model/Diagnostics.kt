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

package org.cangnova.cangjie.resolve.calls.model

import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import io.github.classgraph.TypeArgument


abstract class CangJieCallDiagnostic(
    val candidateApplicability: CandidateApplicability
) {
    abstract fun report(reporter: DiagnosticReporter)
}

interface DiagnosticReporter {
    fun onExplicitReceiver(diagnostic: CangJieCallDiagnostic)

    fun onCall(diagnostic: CangJieCallDiagnostic)

    fun onTypeArguments(diagnostic: CangJieCallDiagnostic)

    fun onCallName(diagnostic: CangJieCallDiagnostic)

    fun onTypeArgument(typeArgument: TypeArgument, diagnostic: CangJieCallDiagnostic)

    //
    fun onCallReceiver(callReceiver: SimpleCangJieCallArgument, diagnostic: CangJieCallDiagnostic)


    fun onCallArgument(callArgument: CangJieCallArgument, diagnostic: CangJieCallDiagnostic)
    fun onCallArgumentName(callArgument: CangJieCallArgument, diagnostic: CangJieCallDiagnostic)
    fun onCallArgumentSpread(callArgument: CangJieCallArgument, diagnostic: CangJieCallDiagnostic)

    fun constraintError(error: ConstraintSystemError)
}
