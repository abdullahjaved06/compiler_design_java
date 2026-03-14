package compiler.Parser;

import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;
import compiler.Lexer.TokenType;

public class Parser {
    private final Lexer lexer;
    private Symbol currentSymbol;

    public Parser(Lexer lexer){
        this.lexer = lexer;
        this.currentSymbol = lexer.getNextSymbol();
    }
    //helper methods:
    //to advance to next symbol.
    private void advance(){
        currentSymbol = lexer.getNextSymbol();
    }

    private void match(TokenType type) {
        if (currentSymbol.getType() == type) {
            advance();
        } else {
            throw new RuntimeException("Syntax Error at line " + currentSymbol.getLine() +
                                        ": Expected " + type + "but found " + currentSymbol.getType());

        }
    }

}