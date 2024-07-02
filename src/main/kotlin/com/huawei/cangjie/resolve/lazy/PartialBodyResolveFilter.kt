package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.resolve.StatementFilter

class PartialBodyResolveFilter(
    elementsToResolve: Collection<CjElement>,
    private val declaration: CjDeclaration,
    forCompletion: Boolean
) : StatementFilter()