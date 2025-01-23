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

package com.linqingying.cangjie.ide.search;


import com.linqingying.cangjie.psi.CjTypeStatement;
import com.intellij.lang.Language;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ExtensibleQueryFactory;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.Query;
import com.intellij.util.QueryExecutor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Search for <em>direct</em> inheritors of given class.
 * <p/>
 * For given hierarchy
 * <pre>
 *   class A {}
 *   class B extends A {}
 *   class C extends B {}
 * </pre>
 * searching for inheritors of {@code A} returns {@code B}.
 * <p/>
 * See {@link ClassInheritorsSearch} to search for all inheritors.
 *
 * @see com.intellij.psi.util.InheritanceUtil
 */
public final class DirectClassInheritorsSearch extends ExtensibleQueryFactory<CjTypeStatement, DirectClassInheritorsSearch.SearchParameters> {
    public static final ExtensionPointName<QueryExecutor<CjTypeStatement, SearchParameters>> EP_NAME = ExtensionPointName.create("com.linqingying.cangjie.ide.search.directClassInheritorsSearch");
    public static final DirectClassInheritorsSearch INSTANCE = new DirectClassInheritorsSearch();

    public static class SearchParameters {
        @NotNull
        private final CjTypeStatement myClass;
        @NotNull private final SearchScope myScope;
        private final boolean myIncludeAnonymous;
        private final boolean myCheckInheritance;

        public SearchParameters(@NotNull CjTypeStatement aClass, @NotNull SearchScope scope, boolean includeAnonymous, boolean checkInheritance) {
            myClass = aClass;
            myScope = scope;
            myIncludeAnonymous = includeAnonymous;
            myCheckInheritance = checkInheritance;
        }

        public SearchParameters(@NotNull CjTypeStatement aClass, @NotNull SearchScope scope, final boolean includeAnonymous) {
            this(aClass, scope, includeAnonymous, true);
        }

        public SearchParameters(@NotNull CjTypeStatement aClass, @NotNull SearchScope scope) {
            this(aClass, scope, true);
        }

        @NotNull
        public CjTypeStatement getClassToProcess() {
            return myClass;
        }

        @NotNull
        public SearchScope getScope() {
            return myScope;
        }

        public boolean isCheckInheritance() {
            return myCheckInheritance;
        }

        public boolean includeAnonymous() {
            return myIncludeAnonymous;
        }

        @Nullable
        public ClassInheritorsSearch.SearchParameters getOriginalParameters() {
            return null;
        }

        
        public boolean shouldSearchInLanguage(@NotNull Language language) {
            return true;
        }
    }

    private DirectClassInheritorsSearch() {
        super(EP_NAME);
    }

    @NotNull
    public static Query<CjTypeStatement> search(@NotNull CjTypeStatement aClass) {
        return search(aClass, GlobalSearchScope.allScope(PsiUtilCore.getProjectInReadAction(aClass)));
    }

    @NotNull
    public static Query<CjTypeStatement> search(@NotNull CjTypeStatement aClass, @NotNull SearchScope scope) {
        return search(aClass, scope, true);
    }

    @NotNull
    public static Query<CjTypeStatement> search(@NotNull CjTypeStatement aClass, @NotNull SearchScope scope, boolean includeAnonymous) {
        return search(new SearchParameters(aClass, scope, includeAnonymous, true));
    }

    @NotNull
    public static Query<CjTypeStatement> search(@NotNull SearchParameters parameters) {
        return INSTANCE.createUniqueResultsQuery(parameters);
    }
}
