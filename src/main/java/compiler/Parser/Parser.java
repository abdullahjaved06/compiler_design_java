package compiler.Parser;

import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;
import compiler.Lexer.TokenType;
import compiler.Parser.AST.*;

public class Parser {
    private final Lexer lexer;
    private Symbol currentSymbol;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.currentSymbol = lexer.getNextSymbol();
    }

    private void advance() {
        currentSymbol = lexer.getNextSymbol();
    }

    private void match(TokenType type) {
        if (currentSymbol.getType() == type) {
            advance();
        } else {
            throw new RuntimeException("Syntax Error at line " + currentSymbol.getLine() +
                    ": Expected " + type + " but found " + currentSymbol.getType());
        }
    }

    public ASTNode getAST() {
        return parseAssignment();
    }

    private ASTNode parseAssignment() {
        // 1. Get Type (INT)
        String type = currentSymbol.getValue();
        advance(); // Matches the type (INT/FLOAT/etc)

        // 2. Get Identifier (x)
        String id = currentSymbol.getValue();
        match(TokenType.IDENTIFIER);

        // 3. Match =
        match(TokenType.ASSIGN);

        // 4. Parse the WHOLE expression (this will handle 1 + 2)
        ASTNode rhs = parseExpression();

        // 5. match the semicolon
        match(TokenType.SEMICOLON);

        return new AssignmentNode(type, id, rhs);
    }

    public ASTNode parseExpression() {
        return parseAddition();
    }

    private ASTNode parseAddition() {
        ASTNode left = parsePrimary();

        // Loop as long as we see + or -
        while (currentSymbol.getType() == TokenType.PLUS || currentSymbol.getType() == TokenType.MINUS) {
            String op = currentSymbol.getValue();
            advance();
            ASTNode right = parsePrimary();
            left = new BinaryExpressionNode(op, left, right);
        }
        return left;
    }

    private ASTNode parsePrimary() {
        if (currentSymbol.getType() == TokenType.INTEGER_LITERAL) {
            ASTNode node = new IntegerNode(currentSymbol.getValue());
            advance();
            return node;
        } else if (currentSymbol.getType() == TokenType.IDENTIFIER) {
            ASTNode node = new IdentifierNode(currentSymbol.getValue());
            advance();
            return node;
        }
        throw new RuntimeException("Syntax Error: Unexpected symbol " + currentSymbol.getType() + " at line " + currentSymbol.getLine());
    }
}