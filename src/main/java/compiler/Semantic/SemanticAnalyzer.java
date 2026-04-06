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
            // The SymbolTable and this class throw RuntimeExceptions with
            // messages like "ScopeError: ..." or "TypeError: ..."
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

    private String inferType(ASTNode node) {
        if (node == null) return "UNKNOWN";

        // 1. Literals: Use getType() as defined in your LiteralNode.java
        if (node instanceof LiteralNode) {
            return ((LiteralNode) node).getType().toString();
        }

        // 2. Identifiers: Look up from Symbol Table
        if (node instanceof IdentifierNode) {
            return symbolTable.lookupType(((IdentifierNode) node).getName());
        }

        // 3. Binary Expressions: Use getType() as defined in your BinaryExpressionNode.java
        if (node instanceof BinaryExpressionNode) {
            BinaryExpressionNode bin = (BinaryExpressionNode) node;
            String leftType = inferType(bin.getLeft());
            String rightType = inferType(bin.getRight());

            // OperatorError: check if operand types match
            if (!leftType.equals(rightType)) {
                throw new RuntimeException("OperatorError: Type mismatch in " + bin.getType() +
                        " operation '" + bin.getOperator() + "': " + leftType + " and " + rightType);
            }

            // Relational/Logical operators result in BOOL
            if ("Relational".equals(bin.getType()) || "Logical".equals(bin.getType())) {
                return "BOOL";
            }

            return leftType; // Arithmetic results in same type (INT + INT = INT)
        }

        return "UNKNOWN";
    }

    private void visitAssignment(AssignmentNode node) {
        String id = node.getIdentifier();
        String declaredType = node.getType();

        if (declaredType != null) {
            // Declaration: [Type] ID = Expr;
            symbolTable.declare(id, declaredType, false);

            if (node.getExpression() != null) {
                String rhsType = inferType(node.getExpression());
                if (!declaredType.equals(rhsType)) {
                    throw new RuntimeException("TypeError: Cannot assign " + rhsType + " to variable '" + id + "' of type " + declaredType);
                }
            }
        } else {
            // Reassignment: ID = Expr;
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
        // Verify range types are INT
        if (!"INT".equals(inferType(node.getRangeStart())) || !"INT".equals(inferType(node.getRangeEnd()))) {
            throw new RuntimeException("TypeError: For loop range must be INT");
        }
        visit(node.getBody());
        symbolTable.exitScope();
    }

    private void visitFunction(FunctionNode node) {
        symbolTable.enterScope();
        // Future: Register parameters here
        visit(node.getBody());
        symbolTable.exitScope();
    }
}