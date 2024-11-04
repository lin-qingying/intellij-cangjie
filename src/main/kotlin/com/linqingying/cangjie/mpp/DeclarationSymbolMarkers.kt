package com.linqingying.cangjie.mpp
/*
 * Those markers are needed for implementation of common algorithm of expect/actual
 *   compatibility checking, implemented in
 *   com.linqingying.cangjie.resolve.calls.mpp.AbstractExpectActualCompatibilityChecker
 */
interface DeclarationSymbolMarker
interface CallableSymbolMarker : DeclarationSymbolMarker
interface FunctionSymbolMarker : CallableSymbolMarker
interface ConstructorSymbolMarker : FunctionSymbolMarker
interface SimpleFunctionSymbolMarker : FunctionSymbolMarker
interface PropertySymbolMarker : CallableSymbolMarker
interface VariableSymbolMarker : CallableSymbolMarker

interface ValueParameterSymbolMarker : CallableSymbolMarker
interface FieldSymbolMarker : CallableSymbolMarker
interface EnumEntrySymbolMarker : CallableSymbolMarker

interface ClassifierSymbolMarker : DeclarationSymbolMarker
interface TypeParameterSymbolMarker : ClassifierSymbolMarker
interface ClassLikeSymbolMarker : ClassifierSymbolMarker
interface RegularClassSymbolMarker : ClassLikeSymbolMarker
interface TypeAliasSymbolMarker : ClassLikeSymbolMarker
interface  SyntheticClassifierSymbolMarker : ClassifierSymbolMarker
