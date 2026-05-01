package compiler.CodeGen;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.AssignmentNode;
import compiler.Parser.AST.BinaryExpressionNode;
import compiler.Parser.AST.BlockNode;
import compiler.Parser.AST.FunctionCallNode;
import compiler.Parser.AST.FunctionNode;
import compiler.Parser.AST.IdentifierNode;
import compiler.Parser.AST.IfNode;
import compiler.Parser.AST.LiteralNode;
import compiler.Parser.AST.ReturnNode;
import compiler.Parser.AST.WhileNode;
import compiler.Parser.AST.ForNode;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class CodeGenerator {
    private final Map<String, Integer> localSlots = new HashMap<>();
    private final Map<String, String> localTypes = new HashMap<>();
    private final Map<String, String> functionTypes = new HashMap<>();
    private final Map<String, List<String>> functionParams = new HashMap<>();

    private int nextSlot = 1;
    private String currentClassName;
    private String currentReturnType = "VOID";

    public void generate(ASTNode root, String outputFile) throws IOException {
        String className = classNameFromFile(outputFile);
        this.currentClassName = className;

        if (!(root instanceof BlockNode block)) {
            throw new RuntimeException("CodeGenerationError: root must be BlockNode.");
        }

        ClassWriter writer = startClass(className);

        functionTypes.clear();
        functionParams.clear();

        for (ASTNode node : block.getStatements()) {
            if (node instanceof FunctionNode functionNode) {
                String returnType = functionNode.getReturnType();

                if (returnType == null) {
                    returnType = "VOID";
                }

                functionTypes.put(functionNode.getName(), returnType);
                functionParams.put(functionNode.getName(), getParameterTypes(functionNode));
            }
        }

        for (ASTNode node : block.getStatements()) {
            if (node instanceof FunctionNode functionNode) {
                if ("main".equals(functionNode.getName())) {
                    addMainFromBlock(writer, functionNode.getBody());
                } else {
                    addFunction(writer, functionNode);
                }
            }
        }

        writer.visitEnd();
        writeFile(outputFile, writer.toByteArray());
    }

    private List<String> getParameterTypes(FunctionNode function) {
        List<String> types = new ArrayList<>();

        for (ASTNode arg : function.getArgs()) {
            if (arg instanceof AssignmentNode assignment) {
                types.add(assignment.getType());
            }
        }

        return types;
    }

    private ClassWriter startClass(String className) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        writer.visit(
                V1_8,
                ACC_PUBLIC,
                className,
                null,
                "java/lang/Object",
                null
        );

        addConstructor(writer);

        return writer;
    }

    private void addConstructor(ClassWriter writer) {
        MethodVisitor method = writer.visitMethod(
                ACC_PUBLIC,
                "<init>",
                "()V",
                null,
                null
        );

        method.visitCode();

        method.visitVarInsn(ALOAD, 0);
        method.visitMethodInsn(
                INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V",
                false
        );

        method.visitInsn(RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private void addMainFromBlock(ClassWriter writer, BlockNode body) {
        MethodVisitor method = writer.visitMethod(
                ACC_PUBLIC | ACC_STATIC,
                "main",
                "([Ljava/lang/String;)V",
                null,
                null
        );

        method.visitCode();

        localSlots.clear();
        localTypes.clear();
        nextSlot = 1;

        String oldReturnType = currentReturnType;
        currentReturnType = "VOID";

        generateBlock(body, method);

        method.visitInsn(RETURN);

        currentReturnType = oldReturnType;

        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private void addFunction(ClassWriter writer, FunctionNode function) {
        String returnType = function.getReturnType();

        if (returnType == null) {
            returnType = "VOID";
        }

        List<String> paramTypes = getParameterTypes(function);

        MethodVisitor method = writer.visitMethod(
                ACC_PUBLIC | ACC_STATIC,
                function.getName(),
                methodDescriptor(paramTypes, returnType),
                null,
                null
        );

        method.visitCode();

        localSlots.clear();
        localTypes.clear();
        nextSlot = 0;

        for (ASTNode arg : function.getArgs()) {
            if (arg instanceof AssignmentNode assignment) {
                localSlots.put(assignment.getIdentifier(), nextSlot);
                localTypes.put(assignment.getIdentifier(), assignment.getType());
                nextSlot++;
            }
        }

        String oldReturnType = currentReturnType;
        currentReturnType = returnType;

        generateBlock(function.getBody(), method);

        if ("VOID".equals(returnType)) {
            method.visitInsn(RETURN);
        }

        currentReturnType = oldReturnType;

        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private void generateBlock(BlockNode block, MethodVisitor method) {
        for (ASTNode statement : block.getStatements()) {
            generateStatement(statement, method);
        }
    }

    private void generateStatement(ASTNode statement, MethodVisitor method) {
        if (statement instanceof FunctionCallNode call) {
            generateFunctionCall(call, method);
            return;
        }

        if (statement instanceof AssignmentNode assignment) {
            generateAssignment(assignment, method);
            return;
        }

        if (statement instanceof IfNode ifNode) {
            generateIf(ifNode, method);
            return;
        }

        if (statement instanceof WhileNode whileNode) {
            generateWhile(whileNode, method);
            return;
        }
        if (statement instanceof ForNode forNode) {
            generateFor(forNode, method);
            return;
        }

        if (statement instanceof ReturnNode returnNode) {
            generateReturn(returnNode, method);
            return;
        }

        throw new RuntimeException(
                "CodeGenerationError: unsupported statement: "
                        + statement.getClass().getSimpleName()
        );
    }

    private void generateAssignment(AssignmentNode assignment, MethodVisitor method) {
        String name = assignment.getIdentifier();
        String type = assignment.getType();

        if (assignment.getExpression() == null) {
            throw new RuntimeException("CodeGenerationError: assignment without value is not supported yet: " + name);
        }

        String valueType = generateExpression(assignment.getExpression(), method);

        if (type != null) {
            if (!type.equals(valueType)) {
                throw new RuntimeException("CodeGenerationError: variable type mismatch for " + name);
            }

            int slot = nextSlot;
            nextSlot++;

            localSlots.put(name, slot);
            localTypes.put(name, type);

            method.visitVarInsn(storeOpcode(type), slot);
            return;
        }

        if (!localSlots.containsKey(name)) {
            throw new RuntimeException("CodeGenerationError: unknown variable: " + name);
        }

        String oldType = localTypes.get(name);

        if (!oldType.equals(valueType)) {
            throw new RuntimeException("CodeGenerationError: variable type mismatch for " + name);
        }

        method.visitVarInsn(storeOpcode(oldType), localSlots.get(name));
    }

    private void generateIf(IfNode ifNode, MethodVisitor method) {
        String conditionType = generateExpression(ifNode.getCondition(), method);

        if (!"BOOL".equals(conditionType)) {
            throw new RuntimeException("CodeGenerationError: if condition must be BOOL.");
        }

        Label elseLabel = new Label();
        Label endLabel = new Label();

        method.visitJumpInsn(IFEQ, elseLabel);

        generateBlock(ifNode.getThenBlock(), method);

        method.visitJumpInsn(GOTO, endLabel);

        method.visitLabel(elseLabel);

        if (ifNode.getElseBlock() != null) {
            generateBlock(ifNode.getElseBlock(), method);
        }

        method.visitLabel(endLabel);
    }

    private void generateWhile(WhileNode whileNode, MethodVisitor method) {
        Label startLabel = new Label();
        Label endLabel = new Label();

        method.visitLabel(startLabel);

        String conditionType = generateExpression(whileNode.getCondition(), method);

        if (!"BOOL".equals(conditionType)) {
            throw new RuntimeException("CodeGenerationError: while condition must be BOOL.");
        }

        method.visitJumpInsn(IFEQ, endLabel);

        generateBlock(whileNode.getBody(), method);

        method.visitJumpInsn(GOTO, startLabel);
        method.visitLabel(endLabel);
    }
    private void generateFor(ForNode forNode, MethodVisitor method) {
        if (!(forNode.getInit() instanceof AssignmentNode init)) {
            throw new RuntimeException("CodeGenerationError: invalid for-loop initializer.");
        }

        String name = init.getIdentifier();
        String type = init.getType();

        if (!"INT".equals(type)) {
            throw new RuntimeException("CodeGenerationError: for-loop variable must be INT.");
        }

        int slot = nextSlot;
        nextSlot++;

        localSlots.put(name, slot);
        localTypes.put(name, "INT");

        String startType = generateExpression(forNode.getRangeStart(), method);

        if (!"INT".equals(startType)) {
            throw new RuntimeException("CodeGenerationError: for-loop start must be INT.");
        }

        method.visitVarInsn(ISTORE, slot);

        Label startLabel = new Label();
        Label endLabel = new Label();

        method.visitLabel(startLabel);

        method.visitVarInsn(ILOAD, slot);

        String endType = generateExpression(forNode.getRangeEnd(), method);

        if (!"INT".equals(endType)) {
            throw new RuntimeException("CodeGenerationError: for-loop end must be INT.");
        }

        method.visitJumpInsn(IF_ICMPGE, endLabel);

        generateBlock(forNode.getBody(), method);

        String updateType = generateExpression(forNode.getUpdate(), method);

        if (!"INT".equals(updateType)) {
            throw new RuntimeException("CodeGenerationError: for-loop update must be INT.");
        }

        method.visitVarInsn(ISTORE, slot);

        method.visitJumpInsn(GOTO, startLabel);
        method.visitLabel(endLabel);
    }

    private void generateReturn(ReturnNode returnNode, MethodVisitor method) {
        if ("VOID".equals(currentReturnType)) {
            if (returnNode.getExpression() != null) {
                throw new RuntimeException("CodeGenerationError: void function cannot return a value.");
            }

            method.visitInsn(RETURN);
            return;
        }

        if (returnNode.getExpression() == null) {
            throw new RuntimeException("CodeGenerationError: non-void function must return a value.");
        }

        String valueType = generateExpression(returnNode.getExpression(), method);

        if (!currentReturnType.equals(valueType)) {
            throw new RuntimeException("CodeGenerationError: wrong return type.");
        }

        method.visitInsn(returnOpcode(currentReturnType));
    }

    private void generateFunctionCall(FunctionCallNode call, MethodVisitor method) {
        String name = call.getFunctionName();

        if ("println".equals(name)) {
            generatePrintln(call, method);
            return;
        }

        if (!functionTypes.containsKey(name)) {
            throw new RuntimeException("CodeGenerationError: unknown function: " + name);
        }

        String returnType = functionTypes.get(name);
        List<String> paramTypes = functionParams.get(name);

        generateCallArguments(call, paramTypes, method);

        method.visitMethodInsn(
                INVOKESTATIC,
                currentClassName,
                name,
                methodDescriptor(paramTypes, returnType),
                false
        );

        if (!"VOID".equals(returnType)) {
            method.visitInsn(POP);
        }
    }

    private String generateFunctionCallExpression(FunctionCallNode call, MethodVisitor method) {
        String name = call.getFunctionName();

        if (!functionTypes.containsKey(name)) {
            throw new RuntimeException("CodeGenerationError: unknown function: " + name);
        }

        String returnType = functionTypes.get(name);

        if ("VOID".equals(returnType)) {
            throw new RuntimeException("CodeGenerationError: void function cannot be used as expression.");
        }

        List<String> paramTypes = functionParams.get(name);

        generateCallArguments(call, paramTypes, method);

        method.visitMethodInsn(
                INVOKESTATIC,
                currentClassName,
                name,
                methodDescriptor(paramTypes, returnType),
                false
        );

        return returnType;
    }

    private void generateCallArguments(FunctionCallNode call, List<String> paramTypes, MethodVisitor method) {
        if (call.getArguments().size() != paramTypes.size()) {
            throw new RuntimeException("CodeGenerationError: wrong number of arguments for " + call.getFunctionName());
        }

        for (int i = 0; i < call.getArguments().size(); i++) {
            String actualType = generateExpression(call.getArguments().get(i), method);
            String expectedType = paramTypes.get(i);

            if (!expectedType.equals(actualType)) {
                throw new RuntimeException("CodeGenerationError: wrong argument type for " + call.getFunctionName());
            }
        }
    }

    private void generatePrintln(FunctionCallNode call, MethodVisitor method) {
        if (call.getArguments().size() != 1) {
            throw new RuntimeException("CodeGenerationError: println expects 1 argument.");
        }

        method.visitFieldInsn(
                GETSTATIC,
                "java/lang/System",
                "out",
                "Ljava/io/PrintStream;"
        );

        String type = generateExpression(call.getArguments().get(0), method);

        method.visitMethodInsn(
                INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(" + descriptorFor(type) + ")V",
                false
        );
    }

    private String generateExpression(ASTNode expression, MethodVisitor method) {
        if (expression instanceof LiteralNode literal) {
            return generateLiteral(literal, method);
        }

        if (expression instanceof IdentifierNode identifier) {
            return generateIdentifier(identifier, method);
        }

        if (expression instanceof BinaryExpressionNode binary) {
            return generateBinaryExpression(binary, method);
        }

        if (expression instanceof FunctionCallNode call) {
            return generateFunctionCallExpression(call, method);
        }

        throw new RuntimeException(
                "CodeGenerationError: unsupported expression: "
                        + expression.getClass().getSimpleName()
        );
    }

    private String generateBinaryExpression(BinaryExpressionNode binary, MethodVisitor method) {
        String leftType = generateExpression(binary.getLeft(), method);
        String rightType = generateExpression(binary.getRight(), method);

        if (leftType.equals("INT") && rightType.equals("INT")) {
            return generateIntBinary(binary.getOperator(), method);
        }

        if (leftType.equals("FLOAT") && rightType.equals("FLOAT")) {
            return generateFloatBinary(binary.getOperator(), method);
        }

        if (leftType.equals("BOOL") && rightType.equals("BOOL")) {
            return generateBoolBinary(binary.getOperator(), method);
        }

        throw new RuntimeException(
                "CodeGenerationError: unsupported binary operation for "
                        + leftType + " and " + rightType
        );
    }

    private String generateIntBinary(String operator, MethodVisitor method) {
        switch (operator) {
            case "+":
                method.visitInsn(IADD);
                return "INT";

            case "-":
                method.visitInsn(ISUB);
                return "INT";

            case "*":
                method.visitInsn(IMUL);
                return "INT";

            case "/":
                method.visitInsn(IDIV);
                return "INT";

            case "%":
                method.visitInsn(IREM);
                return "INT";

            case "==":
                return generateIntComparison(IF_ICMPEQ, method);

            case "=/=":
            case "!=":
                return generateIntComparison(IF_ICMPNE, method);

            case "<":
                return generateIntComparison(IF_ICMPLT, method);

            case ">":
                return generateIntComparison(IF_ICMPGT, method);

            case "<=":
                return generateIntComparison(IF_ICMPLE, method);

            case ">=":
                return generateIntComparison(IF_ICMPGE, method);

            default:
                throw new RuntimeException("CodeGenerationError: unsupported INT operator: " + operator);
        }
    }

    private String generateFloatBinary(String operator, MethodVisitor method) {
        switch (operator) {
            case "+":
                method.visitInsn(FADD);
                return "FLOAT";

            case "-":
                method.visitInsn(FSUB);
                return "FLOAT";

            case "*":
                method.visitInsn(FMUL);
                return "FLOAT";

            case "/":
                method.visitInsn(FDIV);
                return "FLOAT";

            default:
                throw new RuntimeException("CodeGenerationError: unsupported FLOAT operator: " + operator);
        }
    }

    private String generateBoolBinary(String operator, MethodVisitor method) {
        switch (operator) {
            case "&&":
                method.visitInsn(IAND);
                return "BOOL";

            case "||":
                method.visitInsn(IOR);
                return "BOOL";

            case "==":
                return generateIntComparison(IF_ICMPEQ, method);

            case "=/=":
            case "!=":
                return generateIntComparison(IF_ICMPNE, method);

            default:
                throw new RuntimeException("CodeGenerationError: unsupported BOOL operator: " + operator);
        }
    }

    private String generateIntComparison(int jumpOpcode, MethodVisitor method) {
        Label trueLabel = new Label();
        Label endLabel = new Label();

        method.visitJumpInsn(jumpOpcode, trueLabel);

        method.visitInsn(ICONST_0);
        method.visitJumpInsn(GOTO, endLabel);

        method.visitLabel(trueLabel);
        method.visitInsn(ICONST_1);

        method.visitLabel(endLabel);

        return "BOOL";
    }

    private String generateIdentifier(IdentifierNode identifier, MethodVisitor method) {
        String name = identifier.getName();

        if (!localSlots.containsKey(name)) {
            throw new RuntimeException("CodeGenerationError: unknown variable: " + name);
        }

        String type = localTypes.get(name);
        int slot = localSlots.get(name);

        method.visitVarInsn(loadOpcode(type), slot);

        return type;
    }

    private String generateLiteral(LiteralNode literal, MethodVisitor method) {
        String value = literal.getValue();

        switch (literal.getType()) {
            case INT:
                method.visitLdcInsn(Integer.parseInt(value));
                return "INT";

            case FLOAT:
                method.visitLdcInsn(Float.parseFloat(value));
                return "FLOAT";

            case STRING:
                method.visitLdcInsn(value);
                return "STRING";

            case BOOL:
                method.visitInsn(Boolean.parseBoolean(value) ? ICONST_1 : ICONST_0);
                return "BOOL";

            default:
                throw new RuntimeException("CodeGenerationError: unsupported literal.");
        }
    }

    private String methodDescriptor(List<String> paramTypes, String returnType) {
        StringBuilder descriptor = new StringBuilder();

        descriptor.append("(");

        for (String paramType : paramTypes) {
            descriptor.append(descriptorFor(paramType));
        }

        descriptor.append(")");
        descriptor.append(descriptorFor(returnType));

        return descriptor.toString();
    }

    private String descriptorFor(String type) {
        switch (type) {
            case "INT":
                return "I";

            case "FLOAT":
                return "F";

            case "BOOL":
                return "Z";

            case "STRING":
                return "Ljava/lang/String;";

            case "VOID":
                return "V";

            default:
                throw new RuntimeException("CodeGenerationError: unsupported type: " + type);
        }
    }

    private int storeOpcode(String type) {
        switch (type) {
            case "INT":
            case "BOOL":
                return ISTORE;

            case "FLOAT":
                return FSTORE;

            case "STRING":
                return ASTORE;

            default:
                throw new RuntimeException("CodeGenerationError: unsupported variable type: " + type);
        }
    }

    private int loadOpcode(String type) {
        switch (type) {
            case "INT":
            case "BOOL":
                return ILOAD;

            case "FLOAT":
                return FLOAD;

            case "STRING":
                return ALOAD;

            default:
                throw new RuntimeException("CodeGenerationError: unsupported variable type: " + type);
        }
    }

    private int returnOpcode(String type) {
        switch (type) {
            case "INT":
            case "BOOL":
                return IRETURN;

            case "FLOAT":
                return FRETURN;

            case "STRING":
                return ARETURN;

            case "VOID":
                return RETURN;

            default:
                throw new RuntimeException("CodeGenerationError: unsupported return type: " + type);
        }
    }

    private String classNameFromFile(String outputFile) {
        String fileName = Path.of(outputFile).getFileName().toString();

        if (fileName.endsWith(".class")) {
            fileName = fileName.substring(0, fileName.length() - 6);
        }

        return fileName;
    }

    private void writeFile(String outputFile, byte[] bytes) throws IOException {
        Path path = Path.of(outputFile);

        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        Files.write(path, bytes);
    }
}