
grammar Demo;





variableDeclaration :
 variableModifier* NL* (LET | VAR | CONST) NL* (patternsMaybeIrrefutable | (identifier (NL* COLON NL* type)?))  (
  (NL* ASSIGN NL* expression)
|  variableModifier* NL* (LET | VAR | CONST) NL*   identifier (NL* COLON NL*
  type)
);
