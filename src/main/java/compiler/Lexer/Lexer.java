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

    public Symbol getNextSymbol() {
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
