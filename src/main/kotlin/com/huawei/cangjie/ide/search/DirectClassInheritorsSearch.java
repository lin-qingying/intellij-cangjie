package com.huawei.cangjie.ide.search;


import com.huawei.cangjie.psi.CjTypeStatement;
import com.intellij.lang.Language;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ExtensibleQueryFactory;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.Query;
import com.intellij.util.QueryExecutor;
import org.jetbrains.annotations.ApiStatus;
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
    public static final ExtensionPointName<QueryExecutor<CjTypeStatement, SearchParameters>> EP_NAME = ExtensionPointName.create("com.huawei.cangjie.ide.search.directClassInheritorsSearch");
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

        @ApiStatus.Experimental
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
