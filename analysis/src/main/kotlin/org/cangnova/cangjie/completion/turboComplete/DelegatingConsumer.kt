// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.cangnova.cangjie.completion.turboComplete


open class DelegatingConsumer(private val base: SuggestionGeneratorConsumer) : SuggestionGeneratorConsumer by base