package compiler.Semantic;

import compiler.Parser.AST.*;
import java.util.*;

public class SemanticAnalyzer {

    private final SymbolTable symbolTable = new SymbolTable();

    public void analyze(ASTNode root) {
        try {
            visit(root);
            System.out.println("Semantic Analysis Successful!");
        } catch (RuntimeException e) {
            // Catches ScopeError, TypeError, and OperatorError from symbolTable or this class
            System.err.println(e.getMessage());
            System.exit(2);
        }
    }

    private void visit(ASTNode node) {
        if (node == null) return;

        if (node instanceof BlockNode) {
            visitBlock((BlockNode) node);
        } else if (node instanceof AssignmentNode) {
            visitAssignment((AssignmentNode) node);
        } else if (node instanceof IfNode) {
            visitIf((IfNode) node);
        } else if (node instanceof WhileNode) {
            visitWhile((WhileNode) node);
        } else if (node instanceof ForNode) {
            visitFor((ForNode) node);
        } else if (node instanceof FunctionNode) {
            visitFunction((FunctionNode) node);
        }
    }

    /**
     * Logic for inferring the resulting type of an expression.
     */
    private String inferType(ASTNode node) {
        if (node == null) return "UNKNOWN";

        //  Literal types (INT, FLOAT, STRING, BOOL)
        if (node instanceof LiteralNode) {
            return ((LiteralNode) node).getType().toString();
        }

        //  Variable types (Look up from Symbol Table)
        if (node instanceof IdentifierNode) {
            return symbolTable.lookupType(((IdentifierNode) node).getName());
        }

        //  Binary Expressions (+, -, *, /, ==, <, &&, etc.)
        if (node instanceof BinaryExpressionNode) {
            BinaryExpressionNode bin = (BinaryExpressionNode) node;
            String leftType = inferType(bin.getLeft());
            String rightType = inferType(bin.getRight());

            // RULE: Types must match for all binary operations
            if (!leftType.equals(rightType)) {
                throw new RuntimeException("OperatorError: Type mismatch in " + bin.getOperator() +
                        " operation between " + leftType + " and " + rightType);
            }

            // Determine result based on category
            String category = bin.getType(); // e.g., "Relational", "Arithmetic", "Logical"
            if ("Relational".equals(category) || "Logical".equals(category)) {
                return "BOOL"; // (5 < 10) is a BOOL, (true && false) is a BOOL
            }

            return leftType; // (5 + 5) is an INT, (5.0 + 5.0) is a FLOAT
        }

        return "UNKNOWN";
    }

    private void visitAssignment(AssignmentNode node) {
        String id = node.getIdentifier();
        String declaredType = node.getType();

        if (declaredType != null) {
            // DECLARATION (e.g., INT x = 5;)
            symbolTable.declare(id, declaredType, false);

            if (node.getExpression() != null) {
                String rhsType = inferType(node.getExpression());
                if (!declaredType.equals(rhsType)) {
                    throw new RuntimeException("TypeError: Cannot assign " + rhsType + " to variable '" + id + "' of type " + declaredType);
                }
            }
        } else {
            // RE-ASSIGNMENT (e.g., x = 10;)
            String existingType = symbolTable.lookupType(id);
            String rhsType = inferType(node.getExpression());
            if (!existingType.equals(rhsType)) {
                throw new RuntimeException("TypeError: Type mismatch in reassignment of '" + id + "' (" + existingType + " vs " + rhsType + ")");
            }
        }
    }

    private void visitBlock(BlockNode node) {
        symbolTable.enterScope();
        for (ASTNode stmt : node.getStatements()) {
            visit(stmt);
        }
        symbolTable.exitScope();
    }

    private void visitIf(IfNode node) {
        String condType = inferType(node.getCondition());
        if (!"BOOL".equals(condType)) {
            throw new RuntimeException("Missing ConditionError: 'if' condition must be BOOL, found " + condType);
        }
        visit(node.getThenBlock());
        if (node.getElseBlock() != null) visit(node.getElseBlock());
    }

    private void visitWhile(WhileNode node) {
        String condType = inferType(node.getCondition());
        if (!"BOOL".equals(condType)) {
            throw new RuntimeException("Missing ConditionError: 'while' condition must be BOOL, found " + condType);
        }
        visit(node.getBody());
    }

    private void visitFor(ForNode node) {
        symbolTable.enterScope();
        visit(node.getInit());
        // For project requirements: check if rangeStart and rangeEnd are INTs
        if (!"INT".equals(inferType(node.getRangeStart())) || !"INT".equals(inferType(node.getRangeEnd()))) {
            throw new RuntimeException("TypeError: For loop range must be INT");
        }
        visit(node.getBody());
        symbolTable.exitScope();
    }

    private void visitFunction(FunctionNode node) {
        symbolTable.enterScope();
        visit(node.getBody());
        symbolTable.exitScope();
    }
}