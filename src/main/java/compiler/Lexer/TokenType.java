package compiler.Lexer;

public enum TokenType {
    INTEGER_LITERAL, //123
    FLOAT_LITERAL,   // 3.14
    STRING_LITERAL,  // "HELLO"
    TRUE,
    FALSE,

    IDENTIFIER,   // variable names: x, myVar, _count
    EOF
}
