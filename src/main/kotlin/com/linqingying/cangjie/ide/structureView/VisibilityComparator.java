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

package com.linqingying.cangjie.ide.structureView;

import com.intellij.ide.structureView.customRegions.CustomRegionTreeElement;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public class VisibilityComparator implements Comparator {
    private static final Logger LOG = Logger.getInstance(VisibilityComparator.class);
    private static final int GROUP_ACCESS_SUBLEVEL = 1;
    public static final Comparator INSTANCE = new VisibilityComparator(null);

    private final Comparator myNextComparator;
    private static final int UNKNOWN_ACCESS_LEVEL = -1;

    public VisibilityComparator(Comparator comparator) {
        myNextComparator = comparator;
    }

    @Override
    public int compare(@NotNull Object descriptor1, @NotNull Object descriptor2) {
        int accessLevel1 = getAccessLevel(descriptor1);
        int accessLevel2 = getAccessLevel(descriptor2);
        if (accessLevel1 == accessLevel2 && myNextComparator != null) {
            return myNextComparator.compare(descriptor1, descriptor2);
        }
        return accessLevel2 - accessLevel1;
    }

    private static int getAccessLevel(@NotNull Object element) {
        if (element instanceof AccessLevelProvider) {
            return ((AccessLevelProvider)element).getAccessLevel() * (GROUP_ACCESS_SUBLEVEL + 1) + ((AccessLevelProvider)element).getSubLevel();
        }
        else {
            if (!(element instanceof CustomRegionTreeElement)) {
                LOG.error(element.getClass().getName());
            }
            return UNKNOWN_ACCESS_LEVEL;
        }
    }
}
