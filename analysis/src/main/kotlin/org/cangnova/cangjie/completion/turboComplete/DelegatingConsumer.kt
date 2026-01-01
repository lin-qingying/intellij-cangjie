package org.cangnova.cangjie.completion.turboComplete


open class DelegatingConsumer(private val base: SuggestionGeneratorConsumer) : SuggestionGeneratorConsumer by base