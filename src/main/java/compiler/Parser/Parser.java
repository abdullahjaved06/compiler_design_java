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

        if (currentSymbol.getType() == TokenType.FINAL) {
            return parseFinalDeclaration();
        }

        if (type == TokenType.INT_TYPE || type == TokenType.FLOAT_TYPE ||
                type == TokenType.STRING_TYPE || type == TokenType.BOOL_TYPE ||
                type == TokenType.COLLECTION_NAME) {
            return parseAssignment();
        }

        if (type == TokenType.IF) {
            return parseIfStatement();
        }

        if (type == TokenType.WHILE) {
            return parseWhileLoop();
        }

        if (type == TokenType.FOR) {
            return parseForLoop();
        }

        if (type == TokenType.COLL) {
            return parseCollectionDeclaration();
        }

        if (type == TokenType.DEF) {
            return parseFunctionDeclaration();
        }

        if (type == TokenType.RETURN) {
            return parseReturn();
        }

        if (isInbuiltFunction(type)) {
            ASTNode node = parseInbuilt();
            match(TokenType.SEMICOLON);
            return node;
        }

        // Fallback for raw identifiers (e.g., re-assignment like x = 10;)
        if (type == TokenType.IDENTIFIER) {
            return parseAssignment();
        }

        throw new RuntimeException("Syntax Error: Unexpected token " + type + " at line " + currentSymbol.getLine());
    }

    private ASTNode parseInbuilt() {
        String name = currentSymbol.getValue();
        advance();

        match(TokenType.LPAREN);

        java.util.List<ASTNode> args = new java.util.ArrayList<>();

        if (currentSymbol.getType() != TokenType.RPAREN) {
            args.add(parseExpression());
            while (currentSymbol.getType() == TokenType.COMMA) {
                advance();
                args.add(parseExpression());
            }
        }

        match(TokenType.RPAREN);

        return new FunctionCallNode(name, args);
    }

    private ASTNode parseFinalDeclaration() {
        match(TokenType.FINAL);

        ASTNode node = parseAssignment();

        return new FinalNode(node);
    }

    private ASTNode parseReturn() {
        match(TokenType.RETURN);
        ASTNode expr = parseExpression();
        match(TokenType.SEMICOLON);
        return new ReturnNode(expr);
    }

    private ASTNode parseFunctionDeclaration() {
        match(TokenType.DEF);

        String returnType = null;
        if (currentSymbol.getType() != TokenType.MAIN) {
            returnType = currentSymbol.getValue();
            advance();
        }

        String name = null;
        if (currentSymbol.getType() == TokenType.IDENTIFIER ||
                currentSymbol.getType() == TokenType.MAIN) {
            name = currentSymbol.getValue();
            advance();
        }
        match(TokenType.LPAREN);

        java.util.List<ASTNode> args = new java.util.ArrayList<>();
        if (currentSymbol.getType() != TokenType.RPAREN) {
            args.add(parseArguments());
            while (currentSymbol.getType() == TokenType.COMMA) {
                advance();
                args.add(parseArguments());
            }
        }
        match(TokenType.RPAREN);

        BlockNode body = parseBlock();
        return new FunctionNode(returnType, name, args, body);
    }

    private ASTNode parseArguments() {
        String type = parseTypeString();
        if (currentSymbol.getType() == TokenType.LBRACKET) {
            match(TokenType.LBRACKET);
            match(TokenType.RBRACKET);
            type += "[]";
        }
        String id = currentSymbol.getValue();
        match(TokenType.IDENTIFIER);
        return new AssignmentNode(type, id, null);
    }

    private ASTNode parseCollectionDeclaration() {
        match(TokenType.COLL);
        String name = currentSymbol.getValue();
        match(TokenType.COLLECTION_NAME);
        BlockNode members = parseBlock();
        return new CollectionNode(name, members);
    }

    private ASTNode parseForLoop() {
        match(TokenType.FOR);
        match(TokenType.LPAREN);
        ASTNode init = parseAssignment();
        ASTNode rangeStart = parseExpression();
        match(TokenType.ARROW);
        ASTNode rangeEnd = parseExpression();
        match(TokenType.SEMICOLON);
        ASTNode update = parseExpression();
        match(TokenType.RPAREN);
        BlockNode body = parseBlock();

        return new ForNode(init, rangeStart, rangeEnd, update, body);
    }

    private ASTNode parseWhileLoop() {
        match(TokenType.WHILE);
        match(TokenType.LPAREN);
        ASTNode condition = parseExpression();
        match(TokenType.RPAREN);
        BlockNode body = parseBlock();

        return new WhileNode(condition, body);
    }

    private ASTNode parseIfStatement() {
        match(TokenType.IF);
        match(TokenType.LPAREN);
        ASTNode condition = parseExpression();
        match(TokenType.RPAREN);
        BlockNode body = parseBlock();
        if (currentSymbol.getType() == TokenType.ELSE) {
            advance();
            BlockNode elseBlock = parseBlock();
            return new IfNode(condition, body, elseBlock);
        }
        return new IfNode(condition, body);
    }

    private BlockNode parseBlock() {
        match(TokenType.LBRACE);
        BlockNode block = new BlockNode();

        while (currentSymbol.getType() != TokenType.RBRACE &&
                currentSymbol.getType() != TokenType.EOF) {
            block.addStatement(parseStatement());
        }

        match(TokenType.RBRACE);
        return block;
    }

    private String parseTypeString() {
        TokenType type = currentSymbol.getType();
        String typeName = currentSymbol.getValue();
        if (type == TokenType.INT_TYPE || type == TokenType.FLOAT_TYPE ||
                type == TokenType.STRING_TYPE || type == TokenType.BOOL_TYPE ||
                type == TokenType.COLLECTION_NAME) {

            advance();
            if (currentSymbol.getType() == TokenType.LBRACKET) {
                match(TokenType.LBRACKET);
                match(TokenType.RBRACKET);
                typeName += "[]";
            }
        }
        return typeName;
    }

    private ASTNode parseAssignment() {
        String typeStr;
        if (currentSymbol.getType() != TokenType.IDENTIFIER) {
            typeStr = parseTypeString();
        } else {
            typeStr = null;
        }

        String id = currentSymbol.getValue();
        match(TokenType.IDENTIFIER);

        if (currentSymbol.getType() == TokenType.SEMICOLON) {
            match(TokenType.SEMICOLON);
            return new AssignmentNode(typeStr, id);
        } else {
            match(TokenType.ASSIGN);
        }

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
        ASTNode node = parseAccess();

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

    private ASTNode parseAccess() {
        ASTNode node = parsePrimary();

        while (currentSymbol.getType() == TokenType.DOT || currentSymbol.getType() == TokenType.LBRACKET) {
            if (currentSymbol.getType() == TokenType.DOT) {
                advance();
                String member = currentSymbol.getValue();
                match(TokenType.IDENTIFIER);
                node = new MemberAccessNode(node, member);
            } else if (currentSymbol.getType() == TokenType.LBRACKET) {
                advance();
                ASTNode index = parseExpression();
                match(TokenType.RBRACKET);
                node = new IndexAccessNode(node, index);
            }
        }
        return node;
    }

    private ASTNode parsePrimary() {
        if (isInbuiltFunction(currentSymbol.getType())) {
            return parseInbuilt();
        }
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
        } else if (currentSymbol.getType() == TokenType.TRUE ||
            currentSymbol.getType() == TokenType.FALSE) {
            ASTNode node = new LiteralNode(currentSymbol.getValue(), DataType.BOOL);
            advance();
            return node;
        } else if (currentSymbol.getType() == TokenType.COLLECTION_NAME) {
            String collection = currentSymbol.getValue();
            advance();
            match(TokenType.LPAREN);

            java.util.List<ASTNode> args = new java.util.ArrayList<>();
            if (currentSymbol.getType() != TokenType.RPAREN) {
                args.add(parseExpression());
                while (currentSymbol.getType() == TokenType.COMMA) {
                    advance();
                    args.add(parseExpression());
                }
            }
            match(TokenType.RPAREN);
            return new ConstructorCallNode(collection, args);
        } else if (currentSymbol.getType() == TokenType.IDENTIFIER) {
            String name = currentSymbol.getValue();
            advance();

            ASTNode node;

            if (currentSymbol.getType() == TokenType.LPAREN) {
                advance();
                java.util.List<ASTNode> args = new java.util.ArrayList<>();
                if (currentSymbol.getType() != TokenType.RPAREN) {
                    args.add(parseExpression());
                    while (currentSymbol.getType() == TokenType.COMMA) {
                        advance();
                        args.add(parseExpression());
                    }
                }
                match(TokenType.RPAREN);
                node = new FunctionCallNode(name, args);
                return node;
            }

            node = new IdentifierNode(name);
            return node;
        } else if (currentSymbol.getType() == TokenType.INT_TYPE || currentSymbol.getType() == TokenType.FLOAT_TYPE) {
            String type = currentSymbol.getValue();
            advance();
            if (currentSymbol.getType() == TokenType.ARRAY_KEYWORD) {
                advance();
                match(TokenType.LBRACKET);
                ASTNode size = parseExpression();
                match(TokenType.RBRACKET);
                return new ArrayInitNode(type, size);
            }
        } else if (currentSymbol.getType() == TokenType.LPAREN) {
            match(TokenType.LPAREN);
            ASTNode node = parseExpression();
            match(TokenType.RPAREN);
            return node;
        }
        throw new RuntimeException("Syntax Error: Unexpected symbol " + currentSymbol.getType() + " at line " + currentSymbol.getLine());
    }

    private boolean isInbuiltFunction(TokenType type) {
        return type == TokenType.READ_INT || type == TokenType.READ_FLOAT ||
                type == TokenType.READ_STRING || type == TokenType.PRINT ||
                type == TokenType.PRINTLN || type == TokenType.FLOOR ||
                type == TokenType.CEIL || type == TokenType.STR ||
                type == TokenType.LENGTH || type == TokenType.WRITE;

    }
}
