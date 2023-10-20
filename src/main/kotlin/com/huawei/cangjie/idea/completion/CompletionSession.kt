package com.huawei.cangjie.idea.completion


class CompletionSessionConfiguration(
    val useBetterPrefixMatcherForNonImportedClasses: Boolean,
    val nonAccessibleDeclarations: Boolean,
    val javaGettersAndSetters: Boolean,
    val javaClassesNotToBeUsed: Boolean,
    val staticMembers: Boolean,
    val dataClassComponentFunctions: Boolean
)
//abstract  class CompletionSession
