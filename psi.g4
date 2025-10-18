grammar psi;

// ==================== File Structure ====================

cangJieFile
    : preamble topLevelDeclaration* EOF
    ;

preamble
    : packageDirective importList
    ;

packageDirective
    : modifier* MACRO? PACKAGE qualifiedName SEMI?
    |
    ;

importList
    : importDirective*
    ;

importDirective
    : modifier* IMPORT importDirectiveItem SEMI?
    | modifier* IMPORT LBRACE importDirectiveItem (COMMA importDirectiveItem)* RBRACE SEMI?
    ;

importDirectiveItem
    : qualifiedName (DOT MUL)?
    | qualifiedName (AS IDENTIFIER)?
    ;

qualifiedName
    : IDENTIFIER (DOT IDENTIFIER)*
    ;

// ==================== Top Level Declarations ====================

topLevelDeclaration
    : classDeclaration
    | interfaceDeclaration
    | structDeclaration
    | enumDeclaration
    | extendDeclaration
    | functionDeclaration
    | variableDeclaration
    | typeAliasDeclaration
    | foreignDeclaration
    | macroDeclaration
    | SEMI
    ;

// ==================== Modifiers ====================

modifier
    : PUBLIC
    | PRIVATE
    | PROTECTED
    | ABSTRACT
    | MUT
    | OPERATOR
    | FOREIGN
    | CONST
    | UNSAFE
    | SEALED
    | REDEF
    | OPEN
    | STATIC
    ;

// ==================== Type Declarations ====================

classDeclaration
    : modifier* CLASS IDENTIFIER typeParameterList? (LTCOLON delegationSpecifierList)? typeConstraints? classBody?
    ;

interfaceDeclaration
    : modifier* INTERFACE IDENTIFIER typeParameterList? (LTCOLON delegationSpecifierList)? typeConstraints? classBody?
    ;

structDeclaration
    : modifier* STRUCT IDENTIFIER typeParameterList? (LTCOLON delegationSpecifierList)? typeConstraints? classBody?
    ;

enumDeclaration
    : modifier* ENUM IDENTIFIER enumBody
    ;

extendDeclaration
    : modifier* EXTEND typeParameterList? typeReference (LTCOLON delegationSpecifierList)? typeConstraints? classBody?
    ;

classBody
    : LBRACE memberDeclaration* RBRACE
    ;

enumBody
    : LBRACE OR enumEntry (OR enumEntry)* memberDeclaration* RBRACE
    ;

enumEntry
    : IDENTIFIER (LPAR typeList RPAR)?
    | ELLIPSIS
    ;

memberDeclaration
    : classDeclaration
    | interfaceDeclaration
    | structDeclaration
    | enumDeclaration
    | extendDeclaration
    | functionDeclaration
    | propertyDeclaration
    | variableDeclaration
    | primaryConstructor
    | secondaryConstructor
    | SEMI
    ;

primaryConstructor
    : IDENTIFIER LPAR valueParameterList? RPAR initBlock?
    ;

secondaryConstructor
    : INIT LPAR valueParameterList? RPAR initBlock?
    ;

initBlock
    : LBRACE constructorDelegationCall? statement* RBRACE
    ;

constructorDelegationCall
    : (THIS | SUPER) valueArgumentList
    ;

delegationSpecifierList
    : delegationSpecifier (AND delegationSpecifier)*
    ;

delegationSpecifier
    : typeReference
    ;

// ==================== Function Declaration ====================

functionDeclaration
    : modifier* FUNC (IDENTIFIER | operationName) typeParameterList? LPAR valueParameterList? RPAR (COLON typeReference)? typeConstraints? functionBody?
    ;

mainFunction
    : MAIN LPAR valueParameterList? RPAR (COLON typeReference)? functionBody
    ;

operationName
    : PLUS | MINUS | MUL | DIV | PERC | MULMUL
    | EQEQ | EXCLEQ | LT | GT | LTEQ | GTEQ
    | LBRACKET RBRACKET
    | LBRACKET RBRACKET EQ
    ;

functionBody
    : block
    ;

// ==================== Property and Variable ====================

propertyDeclaration
    : modifier* PROP IDENTIFIER COLON typeReference propertyBody?
    ;

propertyBody
    : LBRACE propertyAccessor* RBRACE
    ;

propertyAccessor
    : GET LPAR RPAR block
    | SET LPAR IDENTIFIER RPAR block
    ;

variableDeclaration
    : modifier* (LET | VAR | CONST) (pattern | IDENTIFIER) (COLON typeReference)? (EQ expression)?
    ;

// ==================== Type Alias ====================

typeAliasDeclaration
    : modifier* TYPE IDENTIFIER typeParameterList? EQ typeReference SEMI?
    ;

// ==================== Foreign Declaration ====================

foreignDeclaration
    : FOREIGN LBRACE foreignFunction* RBRACE
    ;

foreignFunction
    : modifier* FUNC IDENTIFIER typeParameterList? LPAR valueParameterList? RPAR (COLON typeReference)? typeConstraints?
    ;

// ==================== Macro Declaration ====================

macroDeclaration
    : MACRO IDENTIFIER typeParameterList? LPAR valueParameterList? RPAR (COLON typeReference)? typeConstraints? functionBody?
    ;

// ==================== Type System ====================

typeReference
    : userType
    | functionType
    | basicType
    | optionalType
    | tupleType
    | thisType
    | varrayType
    | parenthesizedType
    ;

userType
    : simpleUserType (DOT simpleUserType)*
    ;

simpleUserType
    : IDENTIFIER typeArgumentList?
    ;

functionType
    : LPAR valueParameterList? RPAR ARROW typeReference
    ;

basicType
    : BOOL | INT8 | INT16 | INT32 | INT64 | INTNATIVE
    | UINT8 | UINT16 | UINT32 | UINT64 | UINTNATIVE
    | FLOAT16 | FLOAT32 | FLOAT64
    | RUNE | UNIT | NOTHING
    ;

optionalType
    : QUEST typeReference
    ;

tupleType
    : LPAR typeReference (COMMA typeReference)+ RPAR
    ;

parenthesizedType
    : LPAR typeReference RPAR
    ;

thisType
    : THIS_UPPER
    ;

varrayType
    : VARRAY LT typeReference COMMA DOLLAR INTEGER_LITERAL GT
    ;

typeArgumentList
    : LT typeProjection (COMMA typeProjection)* GT
    ;

typeProjection
    : typeReference
    ;

typeParameterList
    : LT typeParameter (COMMA typeParameter)* GT
    ;

typeParameter
    : IDENTIFIER
    ;

typeConstraints
    : WHERE typeConstraint (COMMA typeConstraint)*
    ;

typeConstraint
    : IDENTIFIER LTCOLON typeReference (AND typeReference)*
    ;

typeList
    : typeReference (COMMA typeReference)*
    ;

// ==================== Parameters ====================

valueParameterList
    : valueParameter (COMMA valueParameter)*
    ;

valueParameter
    : modifier* (LET | VAR)? (IDENTIFIER | UNDERLINE) EXCL? (COLON typeReference)? (EQ expression)?
    ;

// ==================== Expressions ====================

expression
    : macroExpression
    | synchronizedExpression
    | assignmentExpression
    ;

assignmentExpression
    : disjunctionExpression (assignmentOperator disjunctionExpression)*
    ;

assignmentOperator
    : EQ | PLUSEQ | MINUSEQ | MULTEQ | DIVEQ | PERCEQ
    | ANDEQ | OREQ | XOREQ | ANDANDEQ | OROREQ
    | LTLTEQ | GTGTEQ | MULMULEQ
    ;

disjunctionExpression
    : conjunctionExpression (OROR conjunctionExpression)*
    ;

conjunctionExpression
    : bitwiseExpression (ANDAND bitwiseExpression)*
    ;

bitwiseExpression
    : equalityExpression ((AND | OR | XOR | LTLT | GTGT) equalityExpression)*
    ;

equalityExpression
    : comparisonExpression ((EQEQ | EXCLEQ) comparisonExpression)*
    ;

comparisonExpression
    : isExpression ((LT | GT | LTEQ | GTEQ) isExpression)*
    ;

isExpression
    : coalescingExpression (IS typeReference)?
    ;

coalescingExpression
    : rangeExpression (COALESCING rangeExpression)*
    ;

rangeExpression
    : additiveExpression ((RANGE | RANGEEQ) additiveExpression (COLON additiveExpression)?)*
    ;

additiveExpression
    : multiplicativeExpression ((PLUS | MINUS) multiplicativeExpression)*
    ;

multiplicativeExpression
    : asExpression ((MUL | DIV | PERC | MULMUL) asExpression)*
    ;

asExpression
    : prefixExpression (AS typeReference)?
    ;

prefixExpression
    : prefixOperator* postfixExpression
    ;

prefixOperator
    : MINUS | EXCL
    ;

postfixExpression
    : atomicExpression postfixOperation*
    ;

postfixOperation
    : postfixOperator
    | arrayAccess
    | callSuffix
    | memberAccessOperation
    ;

postfixOperator
    : PLUSPLUS | MINUSMINUS
    ;

memberAccessOperation
    : (DOT | SAFE_ACCESS) atomicExpression callSuffix?
    ;

arrayAccess
    : LBRACKET expression (COMMA expression)* RBRACKET
    | LBRACKET RANGE expression? RBRACKET
    | SAFE_INDEXEX LBRACKET expression (COMMA expression)* RBRACKET
    ;

callSuffix
    : valueArgumentList
    | lambdaArgument+
    ;

lambdaArgument
    : lambdaExpression
    ;

// ==================== Atomic Expressions ====================

atomicExpression
    : quoteExpression
    | unsafeExpression
    | spawnExpression
    | parenthesizedExpression
    | collectionLiteralExpression
    | thisExpression
    | superExpression
    | throwExpression
    | returnExpression
    | continueExpression
    | breakExpression
    | ifExpression
    | matchExpression
    | tryExpression
    | forExpression
    | whileExpression
    | doWhileExpression
    | letExpression
    | simpleNameExpression
    | lambdaExpression
    | stringTemplate
    | literalConstant
    | basicReferenceExpression
    ;

// ==================== Literal Expressions ====================

literalConstant
    : booleanLiteral
    | integerLiteral
    | runeLiteral
    | characterByteLiteral
    | floatLiteral
    | unitLiteral
    ;

booleanLiteral
    : TRUE | FALSE
    ;

integerLiteral
    : INTEGER_LITERAL
    ;

runeLiteral
    : RUNE_LITERAL
    ;

characterByteLiteral
    : CHARACTER_BYTE_LITERAL
    ;

floatLiteral
    : FLOAT_LITERAL
    ;

unitLiteral
    : LPAR RPAR
    ;

// ==================== Collection and Tuple ====================

collectionLiteralExpression
    : LBRACKET (expression (COMMA expression)*)? RBRACKET
    | LBRACKET RANGE expression? RBRACKET
    ;

parenthesizedExpression
    : LPAR expression? (COMMA expression)* RPAR
    ;

// ==================== String Template ====================

stringTemplate
    : OPEN_QUOTE stringTemplateElement* CLOSING_QUOTE
    ;

stringTemplateElement
    : REGULAR_STRING_PART
    | ESCAPE_SEQUENCE
    | shortTemplateEntry
    | longTemplateEntry
    ;

shortTemplateEntry
    : SHORT_TEMPLATE_ENTRY_START (IDENTIFIER | THIS)
    ;

longTemplateEntry
    : LONG_TEMPLATE_ENTRY_START statement* LONG_TEMPLATE_ENTRY_END
    ;

// ==================== Control Flow ====================

ifExpression
    : IF condition thenBranch (SEMI? ELSE elseBranch)?
    ;

thenBranch
    : block
    ;

elseBranch
    : block
    | ifExpression
    ;

matchExpression
    : MATCH condition? LBRACE matchEntry* RBRACE
    ;

matchEntry
    : CASE casePattern (OR casePattern)* DOUBLE_ARROW caseBody
    ;

casePattern
    : pattern patternGuard?
    | expression patternGuard?
    ;

caseBody
    : statement*
    ;

tryExpression
    : TRY (LPAR tryResourceList RPAR)? block catchBlock* finallyBlock?
    ;

tryResourceList
    : tryResource (COMMA tryResource)*
    ;

tryResource
    : IDENTIFIER EQ expression
    ;

catchBlock
    : CATCH LPAR (UNDERLINE | IDENTIFIER) COLON typeReference (OR typeReference)* RPAR block
    ;

finallyBlock
    : FINALLY block
    ;

forExpression
    : FOR LPAR modifier* pattern IN expression patternGuard? RPAR loopBody
    ;

whileExpression
    : WHILE condition loopBody
    ;

doWhileExpression
    : DO loopBody WHILE condition
    ;

loopBody
    : block
    ;

condition
    : LPAR expression RPAR
    ;

// ==================== Jump Expressions ====================

throwExpression
    : THROW expression
    ;

returnExpression
    : RETURN expression?
    ;

continueExpression
    : CONTINUE
    ;

breakExpression
    : BREAK
    ;

// ==================== Special Expressions ====================

letExpression
    : LET pattern LEFT_ARROW expression
    ;

thisExpression
    : THIS
    ;

superExpression
    : SUPER (LT typeReference GT)? labelReference?
    ;

simpleNameExpression
    : IDENTIFIER typeArgumentList?
    ;

basicReferenceExpression
    : basicType
    ;

lambdaExpression
    : LBRACE (valueParameterList? DOUBLE_ARROW)? statement* RBRACE
    ;

unsafeExpression
    : UNSAFE block
    ;

spawnExpression
    : SPAWN lambdaArgument+
    ;

synchronizedExpression
    : SYNCHRONIZED LPAR expression RPAR block
    ;

// ==================== Quote Expression ====================

quoteExpression
    : QUOTE LPAR quoteParameters RPAR quoteBody
    ;

quoteParameters
    : (quoteToken | quoteInterpolate | macroExpression)+
    ;

quoteToken
    : ANY_TOKEN
    ;

quoteInterpolate
    : DOLLAR LPAR expression RPAR
    ;

quoteBody
    : LBRACE statement* RBRACE
    ;

// ==================== Macro Expression ====================

macroExpression
    : AT IDENTIFIER macroAttr? macroInput
    ;

macroAttr
    : LBRACKET quoteToken* RBRACKET
    ;

macroInput
    : LPAR quoteToken* RPAR
    | declaration
    ;

// ==================== Pattern Matching ====================

pattern
    : constantPattern
    | wildcardPattern
    | bindingPattern
    | tuplePattern
    | typePattern
    | enumPattern
    ;

constantPattern
    : literalConstant
    | stringTemplate
    ;

wildcardPattern
    : UNDERLINE
    ;

bindingPattern
    : IDENTIFIER
    ;

tuplePattern
    : LPAR pattern (COMMA pattern)+ RPAR
    ;

typePattern
    : (IDENTIFIER | UNDERLINE) COLON typeReference
    ;

enumPattern
    : qualifiedName (LPAR expression (COMMA expression)* RPAR)?
    ;

patternGuard
    : WHERE expression
    ;

// ==================== Value Arguments ====================

valueArgumentList
    : LPAR (valueArgument (COMMA valueArgument)*)? RPAR
    ;

valueArgument
    : (IDENTIFIER COLON)? expression
    ;

// ==================== Statements ====================

statement
    : declaration
    | expression
    | SEMI
    ;

declaration
    : classDeclaration
    | interfaceDeclaration
    | structDeclaration
    | enumDeclaration
    | extendDeclaration
    | functionDeclaration
    | variableDeclaration
    | propertyDeclaration
    | macroExpression
    ;

block
    : LBRACE statement* RBRACE
    ;

// ==================== Label ====================

labelReference
    : AT IDENTIFIER
    ;

// ==================== Lexer Rules ====================

// Keywords
PACKAGE: 'package';
IMPORT: 'import';
AS: 'as';
TYPE: 'type';
CLASS: 'class';
INTERFACE: 'interface';
STRUCT: 'struct';
ENUM: 'enum';
EXTEND: 'extend';
FUNC: 'func';
MAIN: 'main';
PROP: 'prop';
LET: 'let';
VAR: 'var';
CONST: 'const';
INIT: 'init';
GET: 'get';
SET: 'set';
IF: 'if';
ELSE: 'else';
MATCH: 'match';
CASE: 'case';
WHERE: 'where';
FOR: 'for';
IN: 'in';
WHILE: 'while';
DO: 'do';
TRY: 'try';
CATCH: 'catch';
FINALLY: 'finally';
THROW: 'throw';
RETURN: 'return';
BREAK: 'break';
CONTINUE: 'continue';
IS: 'is';
AS_KEYWORD: 'as';
THIS: 'this';
SUPER: 'super';
TRUE: 'true';
FALSE: 'false';
UNSAFE: 'unsafe';
SPAWN: 'spawn';
SYNCHRONIZED: 'synchronized';
FOREIGN: 'foreign';
MACRO: 'macro';
QUOTE: 'quote';

// Modifiers
PUBLIC: 'public';
PRIVATE: 'private';
PROTECTED: 'protected';
INTERNAL: 'internal';
ABSTRACT: 'abstract';
OPEN: 'open';
SEALED: 'sealed';
STATIC: 'static';
MUT: 'mut';
OPERATOR: 'operator';
REDEF: 'redef';

// Basic Types
BOOL: 'Bool';
INT8: 'Int8';
INT16: 'Int16';
INT32: 'Int32';
INT64: 'Int64';
INTNATIVE: 'IntNative';
UINT8: 'UInt8';
UINT16: 'UInt16';
UINT32: 'UInt32';
UINT64: 'UInt64';
UINTNATIVE: 'UIntNative';
FLOAT16: 'Float16';
FLOAT32: 'Float32';
FLOAT64: 'Float64';
RUNE: 'Rune';
UNIT: 'Unit';
NOTHING: 'Nothing';
VARRAY: 'VArray';
THIS_UPPER: 'This';

// Operators
PLUS: '+';
MINUS: '-';
MUL: '*';
DIV: '/';
PERC: '%';
MULMUL: '**';
PLUSPLUS: '++';
MINUSMINUS: '--';
AND: '&';
OR: '|';
XOR: '^';
LTLT: '<<';
GTGT: '>>';
ANDAND: '&&';
OROR: '||';
EXCL: '!';
QUEST: '?';
COLON: ':';
SEMICOLON: ';';
EQ: '=';
PLUSEQ: '+=';
MINUSEQ: '-=';
MULTEQ: '*=';
DIVEQ: '/=';
PERCEQ: '%=';
MULMULEQ: '**=';
ANDEQ: '&=';
OREQ: '|=';
XOREQ: '^=';
ANDANDEQ: '&&=';
OROREQ: '||=';
LTLTEQ: '<<=';
GTGTEQ: '>>=';
EQEQ: '==';
EXCLEQ: '!=';
LT: '<';
GT: '>';
LTEQ: '<=';
GTEQ: '>=';
ARROW: '->';
LEFT_ARROW: '<-';
DOUBLE_ARROW: '=>';
DOT: '.';
COMMA: ',';
RANGE: '..';
RANGEEQ: '..=';
ELLIPSIS: '...';
COALESCING: '??';
SAFE_ACCESS: '?.';
SAFE_INDEXEX: '?[';
LTCOLON: '<:';
PIPELINE: '|>';
COMPOSITION: '>>';
AT: '@';
HASH: '#';
DOLLAR: '$';
UNDERLINE: '_';
TILDE: '~';
BACKSLASH: '\\';

// Delimiters
LPAR: '(';
RPAR: ')';
LBRACE: '{';
RBRACE: '}';
LBRACKET: '[';
RBRACKET: ']';

// Literals
INTEGER_LITERAL
    : [0-9] ([0-9_]* [0-9])?
    | '0' [xX] [0-9a-fA-F] ([0-9a-fA-F_]* [0-9a-fA-F])?
    | '0' [oO] [0-7] ([0-7_]* [0-7])?
    | '0' [bB] [01] ([01_]* [01])?
    ;

FLOAT_LITERAL
    : [0-9] ([0-9_]* [0-9])? '.' [0-9] ([0-9_]* [0-9])? ([eE] [+-]? [0-9] ([0-9_]* [0-9])?)?
    | [0-9] ([0-9_]* [0-9])? [eE] [+-]? [0-9] ([0-9_]* [0-9])?
    ;

RUNE_LITERAL
    : '\'' (~['\\] | ESCAPE_SEQUENCE) '\''
    ;

CHARACTER_BYTE_LITERAL
    : 'b\'' (~['\\] | ESCAPE_SEQUENCE) '\''
    ;

// String
OPEN_QUOTE: '"';
CLOSING_QUOTE: '"';
REGULAR_STRING_PART: ~["$\\]+;
ESCAPE_SEQUENCE: '\\' [nrtbf"'\\$];
SHORT_TEMPLATE_ENTRY_START: '$';
LONG_TEMPLATE_ENTRY_START: '${';
LONG_TEMPLATE_ENTRY_END: '}';

// Identifier
IDENTIFIER
    : [a-zA-Z_] [a-zA-Z0-9_]*
    ;

// Whitespace and Comments
WS: [ \t\r\n]+ -> skip;
LINE_COMMENT: '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;
DOC_COMMENT: '///' ~[\r\n]* -> skip;

// End of Line or Semicolon
EOL_OR_SEMICOLON: ('\r'? '\n' | ';')+;

// Any token for quote expressions
ANY_TOKEN: .;