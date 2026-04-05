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
            // The SymbolTable methods call System.exit(2) on errors.
            // This catch handles any other runtime exceptions thrown during visit.
            System.err.println(e.getMessage());
            System.exit(2);
        }
    }

    private void visit(ASTNode node) {
        if (node == null) return;

        if (node instanceof FunctionNode) {
            visitFunction((FunctionNode) node);
        } else if (node instanceof BlockNode) {
            visitBlock((BlockNode) node);
        } else if (node instanceof AssignmentNode) {
            visitAssignment((AssignmentNode) node);
        } else if (node instanceof IfNode) {
            visitIf((IfNode) node);
        } else if (node instanceof WhileNode) {
            visitWhile((WhileNode) node);
        } else if (node instanceof ForNode) {
            visitFor((ForNode) node);
        } else if (node instanceof IdentifierNode) {
            // Check if variable exists
            symbolTable.lookupType(((IdentifierNode) node).getName());
        }
    }

    private void visitFunction(FunctionNode node) {
        symbolTable.enterScope();
        // Visit the body of the function
        if (node.getBody() != null) {
            visit(node.getBody());
        }
        symbolTable.exitScope();
    }

    private void visitBlock(BlockNode node) {
        symbolTable.enterScope();
        for (ASTNode stmt : node.getStatements()) {
            visit(stmt);
        }
        symbolTable.exitScope();
    }

    private void visitAssignment(AssignmentNode node) {
        String id = node.getIdentifier();
        String type = node.getType();

        if (type != null) {
            // Declaration: INT x = 10;
            symbolTable.declare(id, type, false);
        } else {
            // Reassignment: x = 20; (checks if x was declared)
            symbolTable.lookupType(id);
        }

        // Check the RHS expression for undefined variables
        if (node.getExpression() != null) {
            visit(node.getExpression());
        }
    }

    private void visitIf(IfNode node) {
        // Visit blocks inside the if statement
        visit(node.getThenBlock());
        if (node.getElseBlock() != null) {
            visit(node.getElseBlock());
        }
    }

    private void visitWhile(WhileNode node) {
        visit(node.getBody());
    }

    private void visitFor(ForNode node) {
        symbolTable.enterScope();
        visit(node.getInit());
        visit(node.getBody());
        symbolTable.exitScope();
    }
}