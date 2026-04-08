package compiler.Semantic;

import compiler.Parser.AST.*;
import java.util.*;

/**
 * Phase 3: Semantic Analysis
 */
public class SemanticAnalyzer {

    private final SymbolTable symbolTable = new SymbolTable();
    private final Map<String, FunctionDef> functionRegistry = new HashMap<>();
    private final Map<String, List<FieldDef>> collectionRegistry = new HashMap<>();

    private static class FunctionDef {
        String returnType;
        List<String> paramTypes;

        FunctionDef(String returnType, List<String> paramTypes) {
            this.returnType = returnType;
            this.paramTypes = paramTypes;
        }
    }

    private static class FieldDef {
        final String name;
        final String type;

        FieldDef(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    public SemanticAnalyzer() {
        registerInbuiltFunctions();
    }

    private void registerInbuiltFunctions() {
        functionRegistry.put("print", new FunctionDef(null, List.of("STRING")));
        functionRegistry.put("println", new FunctionDef(null, List.of("STRING")));
        functionRegistry.put("read_INT", new FunctionDef("INT", new ArrayList<>()));
    }

    public void analyze(ASTNode root) {
        try {
            preRegisterFunctions(root);
            visit(root);
            System.out.println("Semantic Analysis Successful!");
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(2);
        }
    }

    private void preRegisterFunctions(ASTNode root) {
        if (root instanceof BlockNode) {
            for (ASTNode node : ((BlockNode) root).getStatements()) {
                if (node instanceof FunctionNode) {
                    FunctionNode fn = (FunctionNode) node;
                    List<String> paramTypes = new ArrayList<>();

                    // Extracting types from FunctionNode's args (ASTNodes)
                    for (ASTNode arg : fn.getArgs()) {
                        if (arg instanceof AssignmentNode) {
                            paramTypes.add(((AssignmentNode) arg).getType());
                        }
                    }
                    functionRegistry.put(fn.getName(), new FunctionDef(fn.getReturnType(), paramTypes));
                }
            
    private void preRegisterCollection(CollectionNode cn) {
        String name = cn.getName();

        if (!Character.isUpperCase(name.charAt(0))) {
            throw new RuntimeException(
                    "CollectionError: Collection name '" + name +
                    "' must start with an uppercase letter.");
        }

        Set<String> primitives = Set.of("INT", "FLOAT", "BOOL", "STRING");
        if (primitives.contains(name)) {
            throw new RuntimeException(
                    "CollectionError: '" + name + "' shadows a primitive type.");
        }

        if (collectionRegistry.containsKey(name)) {
            throw new RuntimeException(
                    "CollectionError: Collection '" + name + "' is already defined.");
        }

        List<FieldDef> fields = new ArrayList<>();
        for (ASTNode member : cn.getBody().getStatements()) {
            if (member instanceof AssignmentNode) {
                AssignmentNode field = (AssignmentNode) member;
                fields.add(new FieldDef(field.getIdentifier(), field.getType()));
            }
        }
        collectionRegistry.put(name, fields);
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
        } else if (node instanceof ReturnNode) {
            visitReturn((ReturnNode) node);
        }
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
        String declaredType = node.getType();

        if (declaredType != null) {
            symbolTable.declare(id, declaredType, false);
            if (node.getExpression() != null) {
                String rhsType = inferType(node.getExpression());
                if (!declaredType.equals(rhsType)) {
                    throw new RuntimeException("TypeError: Cannot assign " + rhsType + " to " + declaredType);
                }
            }
        } else {
            String existingType = symbolTable.lookupType(id);
            String rhsType = inferType(node.getExpression());
            if (!existingType.equals(rhsType)) {
                throw new RuntimeException("TypeError: Type mismatch in reassignment of '" + id + "'");
            }
        }
    }

    private String inferType(ASTNode node) {
        if (node instanceof LiteralNode) {
            return ((LiteralNode) node).getType().toString();
        }
        if (node instanceof IdentifierNode) {
            return symbolTable.lookupType(((IdentifierNode) node).getName());
        }
        if (node instanceof BinaryExpressionNode) {
            BinaryExpressionNode ben = (BinaryExpressionNode) node;
            String left = inferType(ben.getLeft());
            String right = inferType(ben.getRight());

            if (!left.equals(right)) {
                throw new RuntimeException("OperatorError: Type mismatch in " + ben.getType() + " operation: " + left + " and " + right);
            }
            return ben.getType().equals("Relational") ? "BOOL" : left;
        }
        if (node instanceof FunctionCallNode) {
            return handleFunctionCall((FunctionCallNode) node);
        }
        return "UNKNOWN";
    }

    private String handleFunctionCall(FunctionCallNode node) {
        String name = node.getFunctionName();
        if (!functionRegistry.containsKey(name)) {
            throw new RuntimeException("ScopeError: Function '" + name + "' is not defined.");
        }

        FunctionDef def = functionRegistry.get(name);
        List<ASTNode> args = node.getArguments();

        if (args.size() != def.paramTypes.size()) {
            throw new RuntimeException("TypeError: " + name + " expects " + def.paramTypes.size() + " args, got " + args.size());
        }

        for (int i = 0; i < args.size(); i++) {
            String actual = inferType(args.get(i));
            String expected = def.paramTypes.get(i);
            if (!actual.equals(expected)) {
                throw new RuntimeException("TypeError: Arg " + (i + 1) + " of " + name + " should be " + expected + ", found " + actual);
            }
        }
        return def.returnType != null ? def.returnType : "VOID";
    }

    private void visitIf(IfNode node) {
        if (!"BOOL".equals(inferType(node.getCondition()))) {
            throw new RuntimeException("Missing ConditionError: 'if' condition must be BOOL");
        }
        visit(node.getThenBlock());
        if (node.getElseBlock() != null) visit(node.getElseBlock());
    }

    private void visitWhile(WhileNode node) {
        if (!"BOOL".equals(inferType(node.getCondition()))) {
            throw new RuntimeException("Missing ConditionError: 'while' condition must be BOOL");
        }
        visit(node.getBody());
    }

    private void visitFor(ForNode node) {
        symbolTable.enterScope();
        visit(node.getInit());
        if (!"INT".equals(inferType(node.getRangeStart())) || !"INT".equals(inferType(node.getRangeEnd()))) {
            throw new RuntimeException("TypeError: For loop range must be INT");
        }
        visit(node.getBody());
        symbolTable.exitScope();
    }

    private void visitFunction(FunctionNode node) {
        symbolTable.enterScope();

        // Register parameters as local variables
        for (ASTNode arg : node.getArgs()) {
            if (arg instanceof AssignmentNode) {
                AssignmentNode param = (AssignmentNode) arg;
                symbolTable.declare(param.getIdentifier(), param.getType(), false);
            }
        }

        visit(node.getBody());
        symbolTable.exitScope();
    }

    private void visitReturn(ReturnNode node) {
        if (node.getExpression() != null) {
            inferType(node.getExpression());
        }
    }
}