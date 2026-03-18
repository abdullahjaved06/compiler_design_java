package compiler.Parser.AST;

public enum DataType {
    INT,
    FLOAT,
    STRING,
    TRUE,
    FALSE,
    VOID,
    ARRAY,      // Useful for the ARRAY keyword in the project statement
    COLLECTION  // Useful for the 'coll' keyword
}