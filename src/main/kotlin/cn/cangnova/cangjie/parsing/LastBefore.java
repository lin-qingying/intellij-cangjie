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

package cn.cangnova.cangjie.parsing;


public class LastBefore extends AbstractTokenStreamPattern {
    private final boolean dontStopRightAfterOccurrence;
    private final TokenStreamPredicate lookFor;
    private final TokenStreamPredicate stopAt;
    private boolean previousLookForResult;

    private LastBefore(TokenStreamPredicate lookFor, TokenStreamPredicate stopAt, boolean dontStopRightAfterOccurrence) {
        this.lookFor = lookFor;
        this.stopAt = stopAt;
        this.dontStopRightAfterOccurrence = dontStopRightAfterOccurrence;
    }

    public LastBefore(TokenStreamPredicate lookFor, TokenStreamPredicate stopAt) {
        this(lookFor, stopAt, false);
    }

    @Override
    public boolean processToken(int offset, boolean topLevel) {
        boolean lookForResult = lookFor.matching(topLevel);
        if (lookForResult) {
            lastOccurrence = offset;
        }
        if (stopAt.matching(topLevel)) {
            if (topLevel
                    && (!dontStopRightAfterOccurrence
                    || !previousLookForResult)) return true;
        }
        previousLookForResult = lookForResult;
        return false;
    }

    @Override
    public void reset() {
        super.reset();
        previousLookForResult = false;
    }
}
