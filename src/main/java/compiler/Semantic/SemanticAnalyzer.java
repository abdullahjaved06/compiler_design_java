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
    private String currentFunctionReturnType = null;

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
        functionRegistry.put("print",       new FunctionDef(null, List.of("ANY")));
        functionRegistry.put("println",     new FunctionDef(null, List.of("ANY")));
        functionRegistry.put("write",       new FunctionDef(null, List.of("ANY")));
        functionRegistry.put("read_INT",    new FunctionDef("INT", new ArrayList<>()));
        functionRegistry.put("read_FLOAT",  new FunctionDef("FLOAT", new ArrayList<>()));
        functionRegistry.put("read_STRING", new FunctionDef("STRING", new ArrayList<>()));
        functionRegistry.put("floor",       new FunctionDef("INT", List.of("FLOAT")));
        functionRegistry.put("ceil",        new FunctionDef("INT", List.of("FLOAT")));
        functionRegistry.put("str",         new FunctionDef("STRING", List.of("INT")));
        functionRegistry.put("length",      new FunctionDef("INT", List.of("ANY")));
        functionRegistry.put("print_INT",   new FunctionDef(null, List.of("INT")));
        functionRegistry.put("print_FLOAT", new FunctionDef(null, List.of("FLOAT")));
    }

    public void analyze(ASTNode root) {
        try {
            preRegister(root);
            if (root instanceof BlockNode) {
                for (ASTNode stmt : ((BlockNode) root).getStatements()) {
                    visit(stmt);
                }
            }
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
            System.exit(2);
        }
    }

    private void preRegister(ASTNode root) {
        if (!(root instanceof BlockNode)) return;
        for (ASTNode node : ((BlockNode) root).getStatements()) {
            if (node instanceof FunctionNode) {
                preRegisterFunction((FunctionNode) node);
            } else if (node instanceof CollectionNode) {
                preRegisterCollection((CollectionNode) node);
            } else if (node instanceof FinalNode) {
                ASTNode inner = ((FinalNode) node).getAssignment();
                if (inner instanceof CollectionNode) {
                    preRegisterCollection((CollectionNode) inner);
                }
            }
        }
    }

    private void preRegisterFunction(FunctionNode fn) {
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
        switch (node) {
            case BlockNode blockNode -> visitBlock(blockNode);
            case FinalNode finalNode -> visitFinal(finalNode);
            case AssignmentNode assignmentNode -> visitAssignment(assignmentNode);
            case CollectionNode collectionNode -> visitCollection(collectionNode);
            case FunctionNode functionNode -> visitFunction(functionNode);
            case IfNode ifNode -> visitIf(ifNode);
            case WhileNode whileNode -> visitWhile(whileNode);
            case ForNode forNode -> visitFor(forNode);
            case ReturnNode returnNode -> visitReturn(returnNode);
            case FunctionCallNode functionCallNode -> inferType(node);
            default -> {
                return;
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

    private void visitFinal(FinalNode node) {
        ASTNode inner = node.getAssignment();
        visit(inner);

        if (inner instanceof AssignmentNode) {
            symbolTable.markFinal(((AssignmentNode) inner).getIdentifier());
        }
    }

    private void visitAssignment(AssignmentNode node) {
        String id = node.getIdentifier();
        String declaredType = node.getType();

        if (declaredType != null) {
            if (!isPrimitive(declaredType) && !declaredType.endsWith("[]")) {
                String baseType = declaredType.replace("[]", "");
                if (!collectionRegistry.containsKey(baseType)) {
                    throw new RuntimeException(
                            "TypeError: Unknown type '" + declaredType +
                            "' for variable '" + id + "'.");
                }
            }

            symbolTable.declare(id, declaredType, false);
            if (node.getExpression() != null) {
                String rhsType = inferType(node.getExpression());
                if (!typesCompatible(declaredType, rhsType)) {
                    throw new RuntimeException(
                            "TypeError: Cannot assign '" + rhsType +
                            "' to variable '" + id + "' of type '" +
                            declaredType + "'.");
                }
            }
        } else {
            String existingType = symbolTable.lookupType(id);
            if (node.getExpression() != null) {
                String rhsType = inferType(node.getExpression());
                if (!typesCompatible(existingType, rhsType)) {
                    throw new RuntimeException(
                            "TypeError: Type mismatch in reassignment of '" +
                            id + "': expected '" + existingType +
                            "', found '" + rhsType + "'.");
                }
            }
        }
    }

    private void visitCollection(CollectionNode node) {
        for (ASTNode member : node.getBody().getStatements()) {
            if (member instanceof AssignmentNode) {
                AssignmentNode field = (AssignmentNode) member;
                String fieldType = field.getType();
                if (fieldType == null) {
                    continue;
                }
                String baseType = fieldType.replace("[]", "");
                if (!isPrimitive(baseType) && !collectionRegistry.containsKey(baseType)) {
                    throw new RuntimeException(
                            "CollectionError: Field '" + field.getIdentifier() +
                            "' in collection '" + node.getName() +
                            "' has unknown type '" + fieldType + "'.");
                }
            }
        }
    }

    private void visitFunction(FunctionNode node) {
        String previousReturnType = currentFunctionReturnType;
        currentFunctionReturnType = node.getReturnType();

        symbolTable.enterScope();

        // Register parameters as local variables
        for (ASTNode arg : node.getArgs()) {
            if (arg instanceof AssignmentNode) {
                AssignmentNode param = (AssignmentNode) arg;
                symbolTable.declare(param.getIdentifier(), param.getType(), false);
            }
        }

        for (ASTNode stmt : node.getBody().getStatements()) {
            visit(stmt);
        }

        symbolTable.exitScope();
        currentFunctionReturnType = previousReturnType;
    }

    private void visitIf(IfNode node) {
        String condType = inferType(node.getCondition());
        if (!"BOOL".equals(condType)) {
            throw new RuntimeException(
                    "MissingConditionError: 'if' condition must be BOOL, found '" +
                    condType + "'.");
        }
        visit(node.getThenBlock());
        if (node.getElseBlock() != null) {
            visit(node.getElseBlock());
        }
    }

    private void visitWhile(WhileNode node) {
        String condType = inferType(node.getCondition());
        if (!"BOOL".equals(condType)) {
            throw new RuntimeException(
                    "MissingConditionError: 'while' condition must be BOOL, found '" +
                    condType + "'.");
        }
        visit(node.getBody());
    }

    private void visitFor(ForNode node) {
        symbolTable.enterScope();
        visit(node.getInit());

        String startType = inferType(node.getRangeStart());
        String endType   = inferType(node.getRangeEnd());
        if (!"INT".equals(startType) || !"INT".equals(endType)) {
            throw new RuntimeException(
                    "MissingConditionError: For loop range bounds must be INT, found '" +
                    startType + "' and '" + endType + "'.");
        }
        for (ASTNode stmt : node.getBody().getStatements()) {
            visit(stmt);
        }
        symbolTable.exitScope();
    }

    private void visitReturn(ReturnNode node) {
        if (node.getExpression() == null) {
            if (currentFunctionReturnType != null) {
                throw new RuntimeException(
                        "ReturnError: Function with return type '" +
                        currentFunctionReturnType + "' must return a value.");
            }
            return;
        }
        String actualType = inferType(node.getExpression());
        if (currentFunctionReturnType == null) {
            throw new RuntimeException(
                    "ReturnError: Void function cannot return a value of type '" +
                    actualType + "'.");
        }

        if (!typesCompatible(currentFunctionReturnType, actualType)) {
            throw new RuntimeException(
                    "ReturnError: Function declares return type '" +
                    currentFunctionReturnType + "' but returns '" +
                    actualType + "'.");
        }
    }

    private String handleFunctionCall(FunctionCallNode node) {
        String name = node.getFunctionName();
        if (!functionRegistry.containsKey(name)) {
            throw new RuntimeException(
                    "ScopeError: Function '" + name + "' is not defined.");
        }

        FunctionDef def = functionRegistry.get(name);
        List<ASTNode> args = node.getArguments();

        boolean isInbuilt = def.paramTypes.size() == 1
                && "ANY".equals(def.paramTypes.getFirst());

        if (!isInbuilt && args.size() != def.paramTypes.size()) {
            throw new RuntimeException(
                    "ArgumentError: Function '" + name + "' expects " +
                            def.paramTypes.size() + " argument(s), but got " +
                            args.size() + ".");
        }

        if (!isInbuilt) {
            for (int i = 0; i < args.size(); i++) {
                String actual = inferType(args.get(i));
                String expected = def.paramTypes.get(i);
                if (!typesCompatible(expected, actual)) {
                    throw new RuntimeException(
                            "ArgumentError: Argument " + (i + 1) +
                            " of function '" + name + "' should be '" +
                            expected + "', but found '" + actual + "'.");
                }
            }
        }
        return def.returnType != null ? def.returnType : "VOID";
    }

    private String inferType(ASTNode node) {
        switch (node) {
            case null -> {
                return "VOID";
            }
            case LiteralNode literalNode -> {
                return literalNode.getType().name();
            }
            case IdentifierNode identifierNode -> {
                return symbolTable.lookupType(identifierNode.getName());
            }
            case UnaryNode unaryNode -> {
                String operandType = inferType(unaryNode.getOperand());
                if (!"INT".equals(operandType) && !"FLOAT".equals(operandType)) {
                    throw new RuntimeException(
                            "OperatorError: Unary operator '" +
                            unaryNode.getOperator() +
                            "' requires INT or FLOAT, found '" + operandType + "'.");
                }
                return operandType;
            }
            case BinaryExpressionNode ben -> {
                return inferBinaryType(ben);
            }
            case FunctionCallNode functionCallNode -> {
                return handleFunctionCall(functionCallNode);
            }
            case ConstructorCallNode constructorCallNode -> {
                return handleConstructorCall(constructorCallNode);
            }
            case ArrayInitNode arrayInitNode -> {
                String sizeType = inferType(arrayInitNode.getSize());
                if (!"INT".equals(sizeType)) {
                    throw new RuntimeException(
                            "TypeError: Array size must be INT, found '" +
                            sizeType + "'.");
                }
                return arrayInitNode.getType() + "[]";
            }
            case IndexAccessNode indexAccessNode -> {
                String arrayType = inferType(indexAccessNode.getArray());
                if (!arrayType.endsWith("[]")) {
                    throw new RuntimeException(
                            "TypeError: Index operator [] applied to non-array type '" +
                            arrayType + "'.");
                }
                String indexType = inferType(indexAccessNode.getIndex());
                if (!"INT".equals(indexType)) {
                    throw new RuntimeException(
                            "TypeError: Array index must be INT, found '" +
                            indexType + "'.");
                }
                return arrayType.substring(0, arrayType.length() - 2);
            }
            case MemberAccessNode memberAccessNode -> {
                return inferMemberAccessType(memberAccessNode);
            }
            default -> {
                throw new RuntimeException(
                        "TypeError: Cannot infer type of node: " +
                        node.getClass().getSimpleName());
            }
        }
    }

    private String inferBinaryType(BinaryExpressionNode node) {
        String op        = node.getOperator();
        String exprClass = node.getType();
        String leftType  = inferType(node.getLeft());
        String rightType = inferType(node.getRight());

        switch (exprClass) {
            case "Arithmetic": {
                if ("%".equals(op)) {
                    if (!"INT".equals(leftType) || !"INT".equals(rightType)) {
                        throw new RuntimeException(
                                "OperatorError: Operator '%' requires INT operands, found '" +
                                leftType + "' and '" + rightType + "'.");
                    }
                    return "INT";
                }

                if ("+".equals(op) && "STRING".equals(leftType) && "STRING".equals(rightType)) {
                    return "STRING";
                }

                boolean leftNumeric  = "INT".equals(leftType)  || "FLOAT".equals(leftType);
                boolean rightNumeric = "INT".equals(rightType) || "FLOAT".equals(rightType);

                if (!leftNumeric || !rightNumeric) {
                    throw new RuntimeException(
                            "OperatorError: Arithmetic operator '" + op +
                            "' requires numeric operands, found '" +
                            leftType + "' and '" + rightType + "'.");
                }

                if ("FLOAT".equals(leftType) || "FLOAT".equals(rightType)) {
                    return "FLOAT";
                }
                return "INT";
            }
            case "Relational": {
                if ("==".equals(op) || "=/=".equals(op)) {
                    if (!typesCompatible(leftType, rightType) && !typesCompatible(rightType, leftType)) {
                        throw new RuntimeException(
                                "OperatorError: Operator '" + op +
                                "' cannot compare '" + leftType +
                                "' with '" + rightType + "'.");
                    }
                    return "BOOL";
                }

                boolean leftNumeric  = "INT".equals(leftType)  || "FLOAT".equals(leftType);
                boolean rightNumeric = "INT".equals(rightType) || "FLOAT".equals(rightType);
                if (!leftNumeric || !rightNumeric) {
                    throw new RuntimeException(
                            "OperatorError: Relational operator '" + op +
                            "' requires numeric operands, found '" +
                            leftType + "' and '" + rightType + "'.");
                }
                return "BOOL";
            }
            case "Logical": {
                if (!"BOOL".equals(leftType) || !"BOOL".equals(rightType)) {
                    throw new RuntimeException(
                            "OperatorError: Logical operator '" + op +
                            "' requires BOOL operands, found '" +
                            leftType + "' and '" + rightType + "'.");
                }
                return "BOOL";
            }

            default:
                throw new RuntimeException(
                        "OperatorError: Unknown operator class '" +
                        exprClass + "'.");
        }
    }

    private String handleConstructorCall(ConstructorCallNode node) {
        String collName = node.getCollectionName();

        if (!collectionRegistry.containsKey(collName)) {
            throw new RuntimeException(
                    "CollectionError: Collection '" +
                    collName + "' is not defined.");
        }

        List<FieldDef> fields = collectionRegistry.get(collName);
        List<ASTNode> args = node.getArguments();

        if (args.size() != fields.size()) {
            throw new RuntimeException(
                    "ArgumentError: Constructor for '" + collName +
                    "' expects " + fields.size() + " argument(s), but got " +
                    args.size() + ".");
        }

        for (int i = 0; i < args.size(); i++) {
            String actual   = inferType(args.get(i));
            String expected = fields.get(i).type;
            if (!typesCompatible(expected, actual)) {
                throw new RuntimeException(
                        "ArgumentError: Field '" + fields.get(i).name +
                        "' of collection '" + collName + "' expects '" +
                        expected + "', but got '" + actual + "'.");
            }
        }
        return collName;
    }

    private String inferMemberAccessType(MemberAccessNode node) {
        String collType  = inferType(node.getCollection());
        String fieldName = node.getMember();
        String baseType = collType.replace("[]", "");

        if (!collectionRegistry.containsKey(baseType)) {
            throw new RuntimeException(
                    "TypeError: Member access '." + fieldName +
                    "' on non-collection type '" + collType + "'.");
        }

        for (FieldDef field : collectionRegistry.get(baseType)) {
            if (field.name.equals(fieldName)) {
                return field.type;
            }
        }

        throw new RuntimeException(
                "TypeError: Collection '" + baseType +
                "' has no field named '" + fieldName + "'.");
    }

    private boolean typesCompatible(String expected, String actual) {
        if (expected.equals(actual)) {
            return true;
        }
        if ("FLOAT".equals(expected) && "INT".equals(actual)) {
            return true;
        }
        return false;
    }

    private boolean isPrimitive(String type) {
        return "INT".equals(type) || "FLOAT".equals(type) ||
                "BOOL".equals(type) || "STRING".equals(type);
    }
}
