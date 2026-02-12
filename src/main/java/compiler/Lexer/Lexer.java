package compiler.Lexer;
import java.io.IOException;
import java.io.Reader;

public class Lexer {
    private final Reader input;
    private int currentChar;
    private int line =1;
    private int column =0;

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
    public Symbol getNextSymbol() {
        skipWhitespaceAndComments();
        //now, only testing one character ata time.
        if (isAtEnd()){
            return new Symbol(TokenType.EOF, "",line, column);
        }
        char c = peek();
        int startLine = line;
        int startColumn = column;
        advance();  // move to next character.
        // for now, return each character as an identifier.
        return new Symbol(TokenType.IDENTIFIER, String.valueOf(c),startLine,startColumn);
    }
}
