package compiler.Lexer;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

public class Lexer {
    private final Reader input;
    private int currentChar;
    private int line =1;
    private int column =0;
    private static final java.util.Map<String, TokenType> keywords = new java.util.HashMap<>();

    static {
        keywords.put("final", TokenType.FINAL);
        keywords.put("coll", TokenType.COLL);
        keywords.put("def", TokenType.DEF);
        keywords.put("for", TokenType.FOR);
        keywords.put("while", TokenType.WHILE);
        keywords.put("if", TokenType.IF);
        keywords.put("else", TokenType.ELSE);
        keywords.put("return", TokenType.RETURN);
        keywords.put("not", TokenType.NOT);
        keywords.put("ARRAY", TokenType.ARRAY_KEYWORD);
        keywords.put("INT", TokenType.INT_TYPE);
        keywords.put("FLOAT", TokenType.FLOAT_TYPE);
        keywords.put("BOOL", TokenType.BOOL_TYPE);
        keywords.put("STRING", TokenType.STRING_TYPE);
        keywords.put("true", TokenType.TRUE);
        keywords.put("false", TokenType.FALSE);
    }
    public Lexer(Reader input) {
        this.input = input;
    }
    private void advance() {
        try {
            currentChar = input.read();
            column++;
            if(currentChar == '\n'){
                line++;
                column=0;
            }
        } catch (IOException e){
            currentChar =-1;
        }
    }
    // return the current character as char
    private char peek() {
        return (char) currentChar;
    }

    // if we reach the end of file.
    private boolean isAtEnd(){
        return currentChar ==-1;
    }
    // Skip whitespace and comments
    private void skipWhitespaceAndComments() {
        while (!isAtEnd()) {
            char c = peek();

            // Skip whitespace (space, tab, newline, carriage return)
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                advance();
            }
            // Skip comments: # until end of line
            else if (c == '#') {
                while (!isAtEnd() && peek() != '\n') {
                    advance();
                }
            }
            // Not whitespace or comment - stop skipping
            else {
                break;
            }
        }
    }

    // Read an identifier or keyword
    private Symbol readIdentifierOrKeyword(int startLine, int startColumn) {
        StringBuilder sb = new StringBuilder();

        // Keep reading while we see letters, digits, or underscore
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            sb.append(peek());
            advance();
        }

        String text = sb.toString();

        // Check if it's a keyword
        TokenType type = keywords.get(text);
        if (type != null) {
            return new Symbol(type, text, startLine, startColumn);
        }

        // Check if it starts with uppercase (collection name)
        if (Character.isUpperCase(text.charAt(0))) {
            return new Symbol(TokenType.COLLECTION_NAME, text, startLine, startColumn);
        }

        // Regular identifier
        return new Symbol(TokenType.IDENTIFIER, text, startLine, startColumn);
    }

    // Main method - returns the next token
    public Symbol getNextSymbol() {
        skipWhitespaceAndComments();

        if (isAtEnd()) {
            return new Symbol(TokenType.EOF, "", line, column);
        }

        int startLine = line;
        int startColumn = column;
        char c = peek();

        // Identifiers and keywords start with letter or underscore
        if (Character.isLetter(c) || c == '_') {
            return readIdentifierOrKeyword(startLine, startColumn);
        }

        // For now, return unknown characters as errors
        advance();
        return new Symbol(TokenType.ERROR, String.valueOf(c), startLine, startColumn);
    }
}
