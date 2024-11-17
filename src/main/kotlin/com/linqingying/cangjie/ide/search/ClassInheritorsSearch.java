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
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;

import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ExtensibleQueryFactory;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Search for inheritors of given class.
 * <p/>
 * For given hierarchy
 * <pre>
 *   class A {}
 *   class B extends A {}
 *   class C extends B {}
 * </pre>
 * searching for inheritors of {@code A} with default {@code checkDeep=true} returns {@code B} and {@code C}.
 * <p/>
 * Use {@code checkDeep=false} or {@link DirectClassInheritorsSearch} to search for direct inheritors only.
 *
 * @see com.intellij.psi.util.InheritanceUtil
 */
public final class ClassInheritorsSearch extends ExtensibleQueryFactory<CjTypeStatement, ClassInheritorsSearch.SearchParameters> {
    public static final ExtensionPointName<QueryExecutor<CjTypeStatement, ClassInheritorsSearch.SearchParameters>> EP_NAME = ExtensionPointName.create("com.linqingying.cangjie.ide.search.classInheritorsSearch");
    public static final ClassInheritorsSearch INSTANCE = new ClassInheritorsSearch();

    public static class SearchParameters implements QueryParameters {
        @NotNull
        private final CjTypeStatement myClass;
        @NotNull
        private final SearchScope myScope;
        private final boolean myCheckDeep;
        private final boolean myCheckInheritance;
        private final boolean myIncludeAnonymous;
        @NotNull
        private final Condition<? super String> myNameCondition;
        @NotNull
        private final Project myProject;

        public SearchParameters(@NotNull CjTypeStatement aClass, @NotNull SearchScope scope, boolean checkDeep, boolean checkInheritance, boolean includeAnonymous) {
            this(aClass, scope, checkDeep, checkInheritance, includeAnonymous, Conditions.alwaysTrue());
        }

        public SearchParameters(@NotNull CjTypeStatement aClass, @NotNull SearchScope scope, boolean checkDeep, boolean checkInheritance,
                                boolean includeAnonymous, @NotNull Condition<? super String> nameCondition) {
            myClass = aClass;
            myScope = scope;
            myCheckDeep = checkDeep;
            myCheckInheritance = checkInheritance;
            assert checkInheritance;
            myIncludeAnonymous = includeAnonymous;
            myNameCondition = nameCondition;
            myProject = PsiUtilCore.getProjectInReadAction(myClass);
        }

        @NotNull
        public CjTypeStatement getClassToProcess() {
            return myClass;
        }

        @NotNull
        @Override
        public Project getProject() {
            return myProject;
        }

        @Override
        public boolean isQueryValid() {
            return myClass.isValid();
        }

        @NotNull
        public Condition<? super String> getNameCondition() {
            return myNameCondition;
        }

        public boolean isCheckDeep() {
            return myCheckDeep;
        }

        @NotNull
        public SearchScope getScope() {
            return myScope;
        }

        public boolean isCheckInheritance() {
            return myCheckInheritance;
        }

        public boolean isIncludeAnonymous() {
            return myIncludeAnonymous;
        }

        @ApiStatus.Experimental
        public boolean shouldSearchInLanguage(@NotNull Language language) {
            return true;
        }

        @Override
        public String toString() {
            return "'"+myClass.getFqName()+
                    "' scope="+myScope+
                    (myCheckDeep ? " (deep)" : "") +
                    (myCheckInheritance ? " (check inheritance)":"") +
                    (myIncludeAnonymous ? " (anonymous)":"")+
                    (myNameCondition == Conditions.<String>alwaysTrue() ? "" : " condition: "+myNameCondition);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SearchParameters that = (SearchParameters)o;

            if (myCheckDeep != that.myCheckDeep) return false;
            if (myCheckInheritance != that.myCheckInheritance) return false;
            if (myIncludeAnonymous != that.myIncludeAnonymous) return false;
            if (!myClass.equals(that.myClass)) return false;
            if (!myScope.equals(that.myScope)) return false;
            return myNameCondition.equals(that.myNameCondition);
        }

        @Override
        public int hashCode() {
            int result = myClass.hashCode();
            result = 31 * result + myScope.hashCode();
            result = 31 * result + (myCheckDeep ? 1 : 0);
            result = 31 * result + (myCheckInheritance ? 1 : 0);
            result = 31 * result + (myIncludeAnonymous ? 1 : 0);
            result = 31 * result + myNameCondition.hashCode();
            return result;
        }
    }

    private ClassInheritorsSearch() {
        super(EP_NAME);
    }

    @NotNull
    public static Query<CjTypeStatement> search(@NotNull CjTypeStatement aClass,
                                         @NotNull SearchScope scope,
                                         boolean checkDeep,
                                         boolean checkInheritance,
                                         boolean includeAnonymous) {
        return search(new SearchParameters(aClass, scope, checkDeep, checkInheritance, includeAnonymous));
    }

    @NotNull
    public static Query<CjTypeStatement> search(@NotNull SearchParameters parameters) {
        if (!parameters.isCheckDeep()) {
            Query<CjTypeStatement> directQuery = DirectClassInheritorsSearch
                    .search(new DirectClassInheritorsSearch.SearchParameters(parameters.getClassToProcess(), parameters.getScope(),
                            parameters.isIncludeAnonymous(), true) {
                        @Override
                        public boolean shouldSearchInLanguage(@NotNull Language language) {
                            return parameters.shouldSearchInLanguage(language);
                        }

                        @Override
                        public ClassInheritorsSearch.SearchParameters getOriginalParameters() {
                            return parameters;
                        }
                    });
            if (parameters.getNameCondition() != Conditions.<String>alwaysTrue()) {
                directQuery = new FilteredQuery<>(directQuery, CjTypeStatement -> parameters.getNameCondition()
                        .value(ReadAction.compute(CjTypeStatement::getName)));
            }
            return AbstractQuery.wrapInReadAction(directQuery);
        }
        return INSTANCE.createUniqueResultsQuery(parameters, CjTypeStatement ->
                ReadAction.compute(() -> SmartPointerManager.getInstance(CjTypeStatement.getProject()).createSmartPsiElementPointer(CjTypeStatement)));
    }

    @NotNull
    public static Query<CjTypeStatement> search(@NotNull CjTypeStatement aClass, @NotNull SearchScope scope, boolean checkDeep) {
        return search(aClass, scope, checkDeep, true, true);
    }

    @NotNull
    public static Query<CjTypeStatement> search(@NotNull CjTypeStatement aClass, boolean checkDeep) {
        return search(aClass, ReadAction.compute(() -> {
            if (!aClass.isValid()) {
                throw new ProcessCanceledException();
            }
            PsiFile file = aClass.getContainingFile();
            return PsiSearchHelper.getInstance(aClass.getProject()).getUseScope(file != null ? file : aClass);
        }), checkDeep);
    }

    @NotNull
    public static Query<CjTypeStatement> search(@NotNull CjTypeStatement aClass) {
        return search(aClass, true);
    }
}
