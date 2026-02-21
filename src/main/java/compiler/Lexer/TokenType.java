package compiler.Lexer;

public enum TokenType {
    // === LITERALS ===
    INTEGER_LITERAL,    // 123
    FLOAT_LITERAL,      // 3.14
    STRING_LITERAL,     // "hello"
    TRUE,               // true
    FALSE,              // false

    // === IDENTIFIERS ===
    IDENTIFIER,         // variable names: x, myVar, _count
    COLLECTION_NAME,    // collection names: Point, Person

    // === KEYWORDS ===
    FINAL,              // final
    COLL,               // coll
    DEF,                // def
    FOR,                // for
    WHILE,              // while
    IF,                 // if
    ELSE,               // else
    RETURN,             // return
    NOT,                // not
    ARRAY_KEYWORD,      // ARRAY

    // === TYPE KEYWORDS ===
    INT_TYPE,           // INT
    FLOAT_TYPE,         // FLOAT
    BOOL_TYPE,          // BOOL
    STRING_TYPE,        // STRING

    // === ARITHMETIC OPERATORS ===
    PLUS,               // +
    MINUS,              // -
    STAR,               // *
    SLASH,              // /
    PERCENT,            // %

    // === COMPARISON OPERATORS ===
    EQUAL,              // ==
    NOT_EQUAL,          // =/=
    LESS,               //
    GREATER,            // >
    LESS_EQUAL,         // <=
    GREATER_EQUAL,      // >=

    // === LOGICAL OPERATORS ===
    AND,                // &&
    OR,                 // ||

    // === ASSIGNMENT AND PUNCTUATION ===
    ASSIGN,             // =
    LPAREN,             // (
    RPAREN,             // )
    LBRACE,             // {
    RBRACE,             // }
    LBRACKET,           // [
    RBRACKET,           // ]
    DOT,                // .
    SEMICOLON,          // ;
    COMMA,              // ,
    ARROW,              // ->

    // === SPECIAL ===
    EOF,                // End of file
    ERROR               // Lexical error
}