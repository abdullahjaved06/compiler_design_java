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

    /**
     * Entry point for the parser.
     * Collects all statements,.
     */
    public ASTNode getAST() {
        BlockNode program = new BlockNode();

        while (currentSymbol.getType() != TokenType.EOF) {
            // We parse one statement at a time until the end of the file.
            program.addStatement(parseStatement());
        }

        return program;
    }

    /**
     * Decides what kind of statement to parse.
     */
    private ASTNode parseStatement() {
        // Check if the current token looks like a type declaration (INT, FLOAT, etc.)
        TokenType type = currentSymbol.getType();
        if (type == TokenType.INT_TYPE || type == TokenType.FLOAT_TYPE ||
                type == TokenType.STRING_TYPE || type == TokenType.BOOL_TYPE) {
            return parseAssignment();
        }

        // Fallback for raw identifiers (e.g., re-assignment like x = 10;)
        if (type == TokenType.IDENTIFIER) {
            return parseAssignment();
        }

        throw new RuntimeException("Syntax Error: Unexpected token " + type + " at line " + currentSymbol.getLine());
    }

    private ASTNode parseAssignment() {
        // Note: This logic assumes 'TYPE IDENTIFIER = EXPR;'
        String typeStr = currentSymbol.getValue();
        advance(); // Consume the type or identifier

        String id = currentSymbol.getValue();
        match(TokenType.IDENTIFIER);

        match(TokenType.ASSIGN);

        ASTNode rhs = parseExpression();

        match(TokenType.SEMICOLON);

        return new AssignmentNode(typeStr, id, rhs);
    }

    public ASTNode parseExpression() {
        return parseLogicalOR();
    }

    private ASTNode parseLogicalOR() {
        ASTNode node = parseLogicalAND();

        while (currentSymbol.getType() == TokenType.OR) {
            String op = currentSymbol.getValue();
            advance();
            ASTNode right = parseLogicalAND();
            node = new BinaryExpressionNode(op, node, right, "Logical");
        }
        return node;
    }

    private ASTNode parseLogicalAND() {
        ASTNode node = parseRelational();

        while (currentSymbol.getType() == TokenType.AND) {
            String op = currentSymbol.getValue();
            advance();
            ASTNode right = parseRelational();
            node = new BinaryExpressionNode(op, node, right, "Logical");
        }
        return node;
    }

    private ASTNode parseRelational() {
        ASTNode node = parseAddition();

        while (currentSymbol.getType() == TokenType.EQUAL ||
                currentSymbol.getType() == TokenType.NOT_EQUAL ||
                currentSymbol.getType() == TokenType.LESS ||
                currentSymbol.getType() == TokenType.LESS_EQUAL ||
                currentSymbol.getType() == TokenType.GREATER ||
                currentSymbol.getType() == TokenType.GREATER_EQUAL) {
            String op = currentSymbol.getValue();
            advance();
            ASTNode right = parseAddition();
            node = new BinaryExpressionNode(op, node, right, "Relational");
        }
        return node;
    }

    private ASTNode parseAddition() {
        if (currentSymbol.getType() == TokenType.MINUS) {
            advance();
            return new UnaryNode("-", parseMultiplication());
        }

        ASTNode node = parseMultiplication();

        while (currentSymbol.getType() == TokenType.PLUS ||
                currentSymbol.getType() == TokenType.MINUS) {
            String op = currentSymbol.getValue();
            advance();
            node = new BinaryExpressionNode(op, node, parseMultiplication(), "Arithmetic");
        }
        return node;
    }

    private ASTNode parseMultiplication() {
        ASTNode node = parsePrimary();

        while (currentSymbol.getType() == TokenType.STAR ||
                currentSymbol.getType() == TokenType.SLASH ||
                currentSymbol.getType() == TokenType.PERCENT) {
            String op = currentSymbol.getValue();
            advance();
            ASTNode right = parsePrimary();
            node = new BinaryExpressionNode(op, node, right, "Arithmetic");
        }
        return node;
    }

    private ASTNode parsePrimary() {
        if (currentSymbol.getType() == TokenType.INTEGER_LITERAL) {
            ASTNode node = new LiteralNode(currentSymbol.getValue(), DataType.INT);
            advance();
            return node;
        } else if (currentSymbol.getType() == TokenType.STRING_LITERAL) {
            ASTNode node = new LiteralNode(currentSymbol.getValue(), DataType.STRING);
            advance();
            return node;
        } else if (currentSymbol.getType() == TokenType.FLOAT_LITERAL) {
            ASTNode node = new LiteralNode(currentSymbol.getValue(), DataType.FLOAT);
            advance();
            return node;
        } else if (currentSymbol.getType() == TokenType.TRUE) {
            ASTNode node = new LiteralNode(currentSymbol.getValue(), DataType.TRUE);
            advance();
            return node;
        } else if (currentSymbol.getType() == TokenType.FALSE) {
            ASTNode node = new LiteralNode(currentSymbol.getValue(), DataType.FALSE);
            advance();
            return node;
        }else if (currentSymbol.getType() == TokenType.IDENTIFIER) {
            ASTNode node = new IdentifierNode(currentSymbol.getValue());
            advance();
            return node;
        } else if (currentSymbol.getType() == TokenType.LPAREN) {
            match(TokenType.LPAREN);
            ASTNode node = parseExpression();
            match(TokenType.RPAREN);
            return node;
        }
        throw new RuntimeException("Syntax Error: Unexpected symbol " + currentSymbol.getType() + " at line " + currentSymbol.getLine());
    }
}