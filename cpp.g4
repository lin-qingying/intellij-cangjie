// ANTLR4 Grammar for Cangjie Language (Based on C++ Compiler Parser)
// Generated from external/cangjie_compiler/src/Parse

grammar cpp;

// ==================== Top Level ====================

file
    : NL* preamble END* (topLevelObject (END+ topLevelObject?)*)? EOF
    ;

preamble
    : packageHeader importSpec*
    ;

packageHeader
    : packageModifier? macroPackage? PACKAGE (packageName DOT)* packageName END+
    |
    ;

macroPackage
    : MACRO PACKAGE
    ;

packageModifier
    : PUBLIC | PROTECTED | INTERNAL
    ;

packageName
    : IDENTIFIER
    ;

END
    : NL | SEMI
    ;

// ==================== Import Spec ====================

importSpec
    : modifier* IMPORT importContent SEMI?
    ;

importContent
    : importSingle
    | importMulti
    ;

importSingle
    : qualifiedName (DOT MUL)?
    | qualifiedName (AS IDENTIFIER)?
    ;

importMulti
    : qualifiedName DOT LCURL importSingle (COMMA importSingle)* RCURL
    ;

qualifiedName
    : IDENTIFIER (DOT IDENTIFIER)*
    ;

// ==================== Top Level Objects ====================

topLevelObject
    : annotation* modifier* topLevelDeclaration
    ;

topLevelDeclaration
    : classDeclaration
    | interfaceDeclaration
    | structDeclaration
    | enumDeclaration
    | extendDeclaration
    | functionDeclaration
    | mainDeclaration
    | macroDeclaration
    | variableDeclaration
    | typeAliasDeclaration
    | foreignDeclaration
    | macroCall
    ;

// ==================== Modifiers ====================

modifier
    : PUBLIC
    | PRIVATE
    | PROTECTED
    | INTERNAL
    | ABSTRACT
    | OPEN
    | SEALED
    | STATIC
    | MUT
    | OPERATOR
    | FOREIGN
    | CONST
    | UNSAFE
    | REDEF
    ;

// ==================== Annotations ====================

annotation
    : AT IDENTIFIER (LSQUARE annotationArguments RSQUARE)?
    ;

annotationArguments
    : annotationArgument (COMMA annotationArgument)*
    ;

annotationArgument
    : (IDENTIFIER COLON)? expression
    ;

// ==================== Class Declaration ====================

classDeclaration
    : CLASS IDENTIFIER generic? (UPPERBOUND inheritedTypes)? whereClause? classBody
    ;

classBody
    : LCURL (END* memberDeclaration)* END* RCURL
    ;

memberDeclaration
    : annotation* modifier* (
        functionDeclaration
        | propertyDeclaration
        | variableDeclaration
        | constructorDeclaration
        | primaryConstructorDeclaration
        | finalizerDeclaration
        | classDeclaration
        | interfaceDeclaration
        | structDeclaration
        | enumDeclaration
        | macroCall
    )
    ;

// ==================== Interface Declaration ====================

interfaceDeclaration
    : INTERFACE IDENTIFIER generic? (UPPERBOUND inheritedTypes)? whereClause? interfaceBody
    ;

interfaceBody
    : LCURL (SEMI* memberDeclaration)* SEMI* RCURL
    ;

// ==================== Struct Declaration ====================

structDeclaration
    : STRUCT IDENTIFIER generic? (UPPERBOUND inheritedTypes)? whereClause? structBody
    ;

structBody
    : LCURL (SEMI* memberDeclaration)* SEMI* RCURL
    ;

// ==================== Enum Declaration ====================

enumDeclaration
    : ENUM IDENTIFIER generic? (UPPERBOUND inheritedTypes)? whereClause? enumBody
    ;

enumBody
    : LCURL BITOR? enumConstructor (BITOR enumConstructor)* (SEMI* memberDeclaration)* RCURL
    ;

enumConstructor
    : annotation* IDENTIFIER (LPAREN typeList RPAREN)?
    | ELLIPSIS
    ;

// ==================== Extend Declaration ====================

extendDeclaration
    : EXTEND generic? typeReference (UPPERBOUND inheritedTypes)? whereClause? extendBody
    ;

extendBody
    : LCURL (SEMI* memberDeclaration)* SEMI* RCURL
    ;

// ==================== Function Declaration ====================

functionDeclaration
    : FUNC (IDENTIFIER | operatorName) generic? functionBody
    ;

mainDeclaration
    : MAIN functionBody
    ;

operatorName
    : ADD | SUB | MUL | DIV | MOD | EXP
    | EQUAL | NOTEQ | LT | GT | LE | GE
    | LSQUARE RSQUARE
    | LPAREN RPAREN
    | RSHIFT | LSHIFT
    ;

functionBody
    : parameterList+ (COLON typeReference)? whereClause? block?
    ;

parameterList
    : LPAREN (parameter (COMMA parameter)*)? RPAREN
    ;

parameter
    : annotation* modifier* (LET | VAR)? (IDENTIFIER | WILDCARD) NOT? COLON typeReference (ASSIGN expression)?
    | ELLIPSIS
    ;

// ==================== Constructor ====================

constructorDeclaration
    : INIT functionBody
    ;

primaryConstructorDeclaration
    : IDENTIFIER parameterList block?
    ;

finalizerDeclaration
    : BITNOT INIT functionBody
    ;

// ==================== Property Declaration ====================

propertyDeclaration
    : PROP IDENTIFIER COLON typeReference propertyBody?
    ;

propertyBody
    : LCURL propertyAccessor* RCURL
    ;

propertyAccessor
    : annotation* (GET | SET) functionBody
    ;

GET : 'get' ;
SET : 'set' ;

// ==================== Variable Declaration ====================

variableDeclaration
    : (LET | VAR | CONST) (pattern | IDENTIFIER) (COLON typeReference)? (ASSIGN expression)?
    ;

// ==================== Type Alias ====================

typeAliasDeclaration
    : TYPE IDENTIFIER generic? ASSIGN typeReference
    ;

// ==================== Foreign Declaration ====================

foreignDeclaration
    : FOREIGN LCURL functionDeclaration* RCURL
    | FOREIGN functionDeclaration
    ;

// ==================== Macro Declaration ====================

macroDeclaration
    : MACRO IDENTIFIER generic? parameterList (COLON typeReference)? whereClause? block?
    ;

macroCall
    : AT (NOT)? IDENTIFIER (LSQUARE macroAttr RSQUARE)? macroInput
    ;

macroAttr
    : quoteToken*
    ;

macroInput
    : LPAREN quoteToken* RPAREN
    | topLevelDeclaration
    ;

quoteToken
    : ~(LPAREN | RPAREN | LSQUARE | RSQUARE | LCURL | RCURL | AT | DOLLAR | EOF)+
    ;

// ==================== Generic ====================

generic
    : LT genericParameterDeclaration (COMMA genericParameterDeclaration)* GT
    ;

genericParameterDeclaration
    : IDENTIFIER
    ;

whereClause
    : WHERE genericConstraint (COMMA genericConstraint)*
    ;

genericConstraint
    : IDENTIFIER UPPERBOUND typeReference (BITAND typeReference)*
    ;

inheritedTypes
    : typeReference (BITAND typeReference)*
    ;

// ==================== Type System ====================

typeReference
    : refType
    | qualifiedType
    | functionType
    | tupleType
    | parenType
    | prefixType
    | varrayType
    | primitiveType
    | thisType
    | invalidType
    ;

refType
    : IDENTIFIER typeArguments?
    ;

qualifiedType
    : typeReference DOT IDENTIFIER typeArguments?
    ;

functionType
    : LPAREN (typeReference (COMMA typeReference)*)? RPAREN ARROW typeReference
    ;

tupleType
    : LPAREN typeReference (COMMA typeReference)+ RPAREN
    ;

parenType
    : LPAREN typeReference RPAREN
    ;

prefixType
    : QUEST typeReference
    ;

varrayType
    : VARRAY LT typeReference COMMA DOLLAR INTEGER_LITERAL GT
    ;

primitiveType
    : INT8 | INT16 | INT32 | INT64 | INTNATIVE
    | UINT8 | UINT16 | UINT32 | UINT64 | UINTNATIVE
    | FLOAT16 | FLOAT32 | FLOAT64
    | RUNE | BOOLEAN | UNIT | NOTHING
    ;

thisType
    : THISTYPE
    ;

invalidType
    : /* placeholder for error recovery */
    ;

typeArguments
    : LT typeReference (COMMA typeReference)* GT
    ;

typeList
    : typeReference (COMMA typeReference)*
    ;

// ==================== Pattern Matching ====================

pattern
    : wildcardPattern
    | tuplePattern
    | enumPattern
    | varPattern
    | constPattern
    | typePattern
    ;

wildcardPattern
    : WILDCARD
    ;

varPattern
    : IDENTIFIER
    ;

tuplePattern
    : LPAREN pattern (COMMA pattern)+ RPAREN
    ;

enumPattern
    : qualifiedName (LPAREN expression (COMMA expression)* RPAREN)?
    ;

constPattern
    : literal
    ;

typePattern
    : (IDENTIFIER | WILDCARD) COLON typeReference
    ;

// ==================== Expressions ====================

expression
    : assignmentExpression
    ;

assignmentExpression
    : conditionalExpression (assignmentOperator conditionalExpression)*
    ;

assignmentOperator
    : ASSIGN | PLUSEQ | MINUSEQ | MULEQ | DIVEQ | MODEQ | EXPEQ
    | BITANDEQ | BITOREQ | BITXOREQ
    | LSHIFTEQ | RSHIFTEQ
    | ANDANDEQ | OROREQ
    ;

conditionalExpression
    : logicalOrExpression
    ;

logicalOrExpression
    : logicalAndExpression (OROR logicalAndExpression)*
    ;

logicalAndExpression
    : bitwiseOrExpression (ANDAND bitwiseOrExpression)*
    ;

bitwiseOrExpression
    : bitwiseXorExpression (BITOR bitwiseXorExpression)*
    ;

bitwiseXorExpression
    : bitwiseAndExpression (BITXOR bitwiseAndExpression)*
    ;

bitwiseAndExpression
    : equalityExpression (BITAND equalityExpression)*
    ;

equalityExpression
    : relationalExpression ((EQUAL | NOTEQ) relationalExpression)*
    ;

relationalExpression
    : isExpression ((LT | GT | LE | GE) isExpression)*
    ;

isExpression
    : coalescingExpression (IS typeReference)?
    ;

coalescingExpression
    : rangeExpression (COALESCING rangeExpression)*
    ;

rangeExpression
    : shiftExpression ((RANGE | RANGEEQ) shiftExpression (COLON shiftExpression)?)*
    ;

shiftExpression
    : additiveExpression ((LSHIFT | RSHIFT) additiveExpression)*
    ;

additiveExpression
    : multiplicativeExpression ((ADD | SUB) multiplicativeExpression)*
    ;

multiplicativeExpression
    : asExpression ((MUL | DIV | MOD | EXP) asExpression)*
    ;

asExpression
    : prefixExpression (AS typeReference)?
    ;

prefixExpression
    : prefixOperator* postfixExpression
    ;

prefixOperator
    : SUB | NOT | ADD
    ;

postfixExpression
    : primaryExpression postfixOperator*
    ;

postfixOperator
    : INC
    | DEC
    | callSuffix
    | memberAccess
    | arrayAccess
    | optionalChaining
    ;

callSuffix
    : valueArgumentList
    | trailingClosure
    ;

memberAccess
    : (DOT | SAFE_ACCESS) IDENTIFIER typeArguments?
    ;

arrayAccess
    : LSQUARE expression (COMMA expression)* RSQUARE
    | LSQUARE RANGE expression? RSQUARE
    | SAFE_INDEXEX LSQUARE expression RSQUARE
    ;

optionalChaining
    : QUEST (callSuffix | memberAccess | arrayAccess)
    ;

valueArgumentList
    : LPAREN (valueArgument (COMMA valueArgument)*)? RPAREN
    ;

valueArgument
    : (IDENTIFIER COLON)? expression
    ;

trailingClosure
    : lambdaExpression
    ;

// ==================== Primary Expressions ====================

primaryExpression
    : literal
    | IDENTIFIER typeArguments?
    | parenExpression
    | arrayLiteralExpression
    | tupleLiteralExpression
    | thisExpression
    | superExpression
    | lambdaExpression
    | ifExpression
    | matchExpression
    | forExpression
    | whileExpression
    | doWhileExpression
    | tryExpression
    | quoteExpression
    | throwExpression
    | returnExpression
    | breakExpression
    | continueExpression
    | performExpression
    | resumeExpression
    | spawnExpression
    | synchronizedExpression
    | unsafeExpression
    | varrayExpression
    | macroCall
    | ifAvailableExpression
    ;

literal
    : INTEGER_LITERAL
    | FLOAT_LITERAL
    | RUNE_LITERAL
    | RUNE_BYTE_LITERAL
    | BOOL_LITERAL
    | STRING_LITERAL
    | JSTRING_LITERAL
    | MULTILINE_STRING
    | MULTILINE_RAW_STRING
    | UNIT_LITERAL
    ;

parenExpression
    : LPAREN expression? RPAREN
    ;

arrayLiteralExpression
    : LSQUARE (expression (COMMA expression)*)? RSQUARE
    | LSQUARE RANGE expression? RSQUARE
    ;

tupleLiteralExpression
    : LPAREN expression (COMMA expression)+ RPAREN
    ;

thisExpression
    : THIS
    ;

superExpression
    : SUPER (LT typeReference GT)?
    ;

lambdaExpression
    : LCURL (lambdaParameterList DOUBLE_ARROW)? (expression | block) RCURL
    ;

lambdaParameterList
    : lambdaParameter (COMMA lambdaParameter)*
    ;

lambdaParameter
    : IDENTIFIER (COLON typeReference)?
    ;

// ==================== Control Flow ====================

ifExpression
    : IF condition thenBranch (SEMI? ELSE elseBranch)?
    ;

condition
    : LPAREN expression RPAREN
    ;

thenBranch
    : block
    ;

elseBranch
    : block
    | ifExpression
    ;

matchExpression
    : MATCH condition? LCURL matchCase* RCURL
    ;

matchCase
    : CASE casePattern (BITOR casePattern)* DOUBLE_ARROW caseBody
    ;

casePattern
    : pattern patternGuard?
    | expression patternGuard?
    ;

patternGuard
    : WHERE expression
    ;

caseBody
    : (expressionOrDeclaration END*)+
    ;

expressionOrDeclaration
    : variableDeclaration
    | expression
    | macroCall
    ;

forExpression
    : FOR LPAREN modifier* pattern IN expression patternGuard? RPAREN block
    ;

whileExpression
    : WHILE condition block
    ;

doWhileExpression
    : DO block WHILE condition
    ;

tryExpression
    : TRY (LPAREN tryResourceList RPAREN)? block catchBlock* (HANDLE handleBlock)? (FINALLY block)?
    ;

tryResourceList
    : tryResource (COMMA tryResource)*
    ;

tryResource
    : IDENTIFIER ASSIGN expression
    ;

catchBlock
    : CATCH LPAREN (WILDCARD | IDENTIFIER) COLON typeReference (BITOR typeReference)* RPAREN block
    ;

handleBlock
    : LPAREN pattern RPAREN block
    ;

// ==================== Quote Expression ====================

quoteExpression
    : QUOTE LPAREN quoteParameters RPAREN quoteBody
    ;

quoteParameters
    : (quoteToken | quoteInterpolate | macroCall)*
    ;

quoteInterpolate
    : DOLLAR LPAREN expression RPAREN
    ;

quoteBody
    : LCURL (expressionOrDeclaration END*)* RCURL
    ;

// ==================== Other Expressions ====================

throwExpression
    : THROW expression
    ;

returnExpression
    : RETURN expression?
    ;

breakExpression
    : BREAK
    ;

continueExpression
    : CONTINUE
    ;

performExpression
    : PERFORM expression
    ;

resumeExpression
    : RESUME expression
    ;

spawnExpression
    : SPAWN trailingClosure+
    ;

synchronizedExpression
    : SYNCHRONIZED LPAREN expression RPAREN block
    ;

unsafeExpression
    : UNSAFE block
    ;

varrayExpression
    : VARRAY LT typeReference COMMA DOLLAR INTEGER_LITERAL GT valueArgumentList
    ;

ifAvailableExpression
    : AT IF_AVAILABLE valueArgumentList
    ;

// ==================== Block ====================

block
    : LCURL (expressionOrDeclaration END*)* RCURL
    ;

// ==================== Keywords ====================

PACKAGE : 'package' ;
IMPORT : 'import' ;
AS : 'as' ;
CLASS : 'class' ;
INTERFACE : 'interface' ;
STRUCT : 'struct' ;
ENUM : 'enum' ;
EXTEND : 'extend' ;
TYPE : 'type' ;
FUNC : 'func' ;
MAIN : 'main' ;
MACRO : 'macro' ;
PROP : 'prop' ;
LET : 'let' ;
VAR : 'var' ;
CONST : 'const' ;
INIT : 'init' ;
IF : 'if' ;
ELSE : 'else' ;
MATCH : 'match' ;
CASE : 'case' ;
WHERE : 'where' ;
FOR : 'for' ;
IN : 'in' ;
WHILE : 'while' ;
DO : 'do' ;
TRY : 'try' ;
CATCH : 'catch' ;
HANDLE : 'handle' ;
FINALLY : 'finally' ;
THROW : 'throw' ;
RETURN : 'return' ;
BREAK : 'break' ;
CONTINUE : 'continue' ;
PERFORM : 'perform' ;
RESUME : 'resume' ;
IS : 'is' ;
THIS : 'this' ;
SUPER : 'super' ;
QUOTE : 'quote' ;
SPAWN : 'spawn' ;
SYNCHRONIZED : 'synchronized' ;
UNSAFE : 'unsafe' ;
FOREIGN : 'foreign' ;

// Modifiers
PUBLIC : 'public' ;
PRIVATE : 'private' ;
PROTECTED : 'protected' ;
INTERNAL : 'internal' ;
ABSTRACT : 'abstract' ;
OPEN : 'open' ;
SEALED : 'sealed' ;
STATIC : 'static' ;
MUT : 'mut' ;
OPERATOR : 'operator' ;
REDEF : 'redef' ;

// Primitive Types
INT8 : 'Int8' ;
INT16 : 'Int16' ;
INT32 : 'Int32' ;
INT64 : 'Int64' ;
INTNATIVE : 'IntNative' ;
UINT8 : 'UInt8' ;
UINT16 : 'UInt16' ;
UINT32 : 'UInt32' ;
UINT64 : 'UInt64' ;
UINTNATIVE : 'UIntNative' ;
FLOAT16 : 'Float16' ;
FLOAT32 : 'Float32' ;
FLOAT64 : 'Float64' ;
RUNE : 'Rune' ;
BOOLEAN : 'Bool' ;
UNIT : 'Unit' ;
NOTHING : 'Nothing' ;
VARRAY : 'VArray' ;
THISTYPE : 'This' ;

// Operators and Delimiters
LPAREN : '(' ;
RPAREN : ')' ;
LCURL : '{' ;
RCURL : '}' ;
LSQUARE : '[' ;
RSQUARE : ']' ;
DOT : '.' ;
COMMA : ',' ;
SEMI : ';' ;
COLON : ':' ;
DOUBLE_COLON : '::' ;
QUEST : '?' ;
AT : '@' ;
HASH : '#' ;
DOLLAR : '$' ;
WILDCARD : '_' ;
BITNOT : '~' ;
NOT : '!' ;

// Assignment
ASSIGN : '=' ;
PLUSEQ : '+=' ;
MINUSEQ : '-=' ;
MULEQ : '*=' ;
DIVEQ : '/=' ;
MODEQ : '%=' ;
EXPEQ : '**=' ;
BITANDEQ : '&=' ;
BITOREQ : '|=' ;
BITXOREQ : '^=' ;
LSHIFTEQ : '<<=' ;
RSHIFTEQ : '>>=' ;
ANDANDEQ : '&&=' ;
OROREQ : '||=' ;

// Comparison
EQUAL : '==' ;
NOTEQ : '!=' ;
LT : '<' ;
GT : '>' ;
LE : '<=' ;
GE : '>=' ;

// Arithmetic
ADD : '+' ;
SUB : '-' ;
MUL : '*' ;
DIV : '/' ;
MOD : '%' ;
EXP : '**' ;
INC : '++' ;
DEC : '--' ;

// Logical
ANDAND : '&&' ;
OROR : '||' ;

// Bitwise
BITAND : '&' ;
BITOR : '|' ;
BITXOR : '^' ;
LSHIFT : '<<' ;
RSHIFT : '>>' ;

// Other
ARROW : '->' ;
DOUBLE_ARROW : '=>' ;
UPPERBOUND : '<:' ;
RANGE : '..' ;
RANGEEQ : '..=' ;
ELLIPSIS : '...' ;
COALESCING : '??' ;
SAFE_ACCESS : '?.' ;
SAFE_INDEXEX : '?[' ;

// Special Annotation
IF_AVAILABLE : 'IfAvailable' ;

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

RUNE_BYTE_LITERAL
    : 'b\'' (~['\\] | ESCAPE_SEQUENCE) '\''
    ;

BOOL_LITERAL
    : 'true'
    | 'false'
    ;

STRING_LITERAL
    : '"' (~["\\$] | ESCAPE_SEQUENCE | DOLLAR_ESCAPE)* '"'
    ;

JSTRING_LITERAL
    : 'j"' (~["\\] | ESCAPE_SEQUENCE)* '"'
    ;

MULTILINE_STRING
    : '"""' .*? '"""'
    ;

MULTILINE_RAW_STRING
    : 'r"""' .*? '"""'
    ;

UNIT_LITERAL
    : '()'
    ;

fragment ESCAPE_SEQUENCE
    : '\\' [nrtbf"'\\$]
    | '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;

fragment DOLLAR_ESCAPE
    : '\\' '$'
    ;

fragment HEX_DIGIT
    : [0-9a-fA-F]
    ;

// Identifier
IDENTIFIER
    : [a-zA-Z_] [a-zA-Z0-9_]*
    | '`' (~[`])+ '`'
    ;

// Whitespace and Comments
WS : [ \t\r]+ -> skip ;
NL : '\n' ;

LINE_COMMENT
    : '//' ~[\r\n]* -> skip
    ;

BLOCK_COMMENT
    : '/*' .*? '*/' -> skip
    ;

DOC_COMMENT
    : '///' ~[\r\n]* -> skip
    ;