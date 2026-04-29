package compiler.CodeGen;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.BlockNode;
import compiler.Parser.AST.FunctionCallNode;
import compiler.Parser.AST.FunctionNode;
import compiler.Parser.AST.LiteralNode;
import compiler.Parser.AST.AssignmentNode;
import compiler.Parser.AST.IdentifierNode;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Label;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import compiler.Parser.AST.BinaryExpressionNode;
import static org.objectweb.asm.Opcodes.*;
import compiler.Parser.AST.IfNode;
import compiler.Parser.AST.WhileNode;

public class CodeGenerator {
    private final Map<String, Integer> localSlots = new HashMap<>();
    private final Map<String, String> localTypes = new HashMap<>();
    private int nextSlot = 1;
    public void makeEmptyClass(String className, String outputFile) throws IOException {
        ClassWriter writer = startClass(className);
        writer.visitEnd();
        writeFile(outputFile, writer.toByteArray());
    }

    public void makeClassWithEmptyMain(String className, String outputFile) throws IOException {
        ClassWriter writer = startClass(className);
        addEmptyMain(writer);
        writer.visitEnd();
        writeFile(outputFile, writer.toByteArray());
    }

    public void makeClassWithHelloMain(String className, String outputFile) throws IOException {
        ClassWriter writer = startClass(className);
        addHelloMain(writer);
        writer.visitEnd();
        writeFile(outputFile, writer.toByteArray());
    }

    public void makeHelloClass(String outputFile) throws IOException {
        String className = classNameFromFile(outputFile);
        makeClassWithHelloMain(className, outputFile);
    }

    public void generate(String outputFile) throws IOException {
        makeHelloClass(outputFile);
    }

    public void generate(ASTNode root, String outputFile) throws IOException {
        String className = classNameFromFile(outputFile);

        ClassWriter writer = startClass(className);

        FunctionNode mainFunction = findMain(root);
        addMainFromBlock(writer, mainFunction.getBody());

        writer.visitEnd();

        writeFile(outputFile, writer.toByteArray());
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

    private FunctionNode findMain(ASTNode root) {
        if (root instanceof BlockNode block) {
            for (ASTNode statement : block.getStatements()) {
                if (statement instanceof FunctionNode functionNode) {
                    if ("main".equals(functionNode.getName())) {
                        return functionNode;
                    }
                }
            }
        }

        throw new RuntimeException("CodeGenerationError: main function not found.");
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

    private void addEmptyMain(ClassWriter writer) {
        MethodVisitor method = writer.visitMethod(
                ACC_PUBLIC | ACC_STATIC,
                "main",
                "([Ljava/lang/String;)V",
                null,
                null
        );

        method.visitCode();
        method.visitInsn(RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private void addHelloMain(ClassWriter writer) {
        MethodVisitor method = writer.visitMethod(
                ACC_PUBLIC | ACC_STATIC,
                "main",
                "([Ljava/lang/String;)V",
                null,
                null
        );

        method.visitCode();

        method.visitFieldInsn(
                GETSTATIC,
                "java/lang/System",
                "out",
                "Ljava/io/PrintStream;"
        );

        method.visitLdcInsn("hello");

        method.visitMethodInsn(
                INVOKEVIRTUAL,
                "java/io/PrintStream",
                "println",
                "(Ljava/lang/String;)V",
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

        generateBlock(body, method);

        method.visitInsn(RETURN);
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
        if (statement instanceof WhileNode whileNode){
            generateWhile(whileNode, method);
            return;
        }
        throw new RuntimeException(
                "CodeGenerationError: unsupported statement: "
                        + statement.getClass().getSimpleName()
        );
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

    private void generateFunctionCall(FunctionCallNode call, MethodVisitor method) {
        if ("println".equals(call.getFunctionName())) {
            generatePrintln(call, method);
            return;
        }

        throw new RuntimeException(
                "CodeGenerationError: unsupported function call: "
                        + call.getFunctionName()
        );
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
    private String generateIntBinaryExpression(BinaryExpressionNode binary, MethodVisitor method) {
        switch (binary.getOperator()) {
            case "+":
                method.visitInsn(IADD);
                break;
            case "-":
                method.visitInsn(ISUB);
                break;
            case "*":
                method.visitInsn(IMUL);
                break;
            case "/":
                method.visitInsn(IDIV);
                break;
            case "%":
                method.visitInsn(IREM);
                break;
            case "==":
                method.visitJumpInsn(IF_ICMPEQ, new Label());
                break;
            case "!=":
                method.visitJumpInsn(IF_ICMPNE, new Label());
                break;
            case "<":
                method.visitJumpInsn(IF_ICMPLT, new Label());
                break;
            case ">":
                method.visitJumpInsn(IF_ICMPGT, new Label());
                break;
            case "<=":
                method.visitJumpInsn(IF_ICMPLE, new Label());
                break;
            case ">=":
                method.visitJumpInsn(IF_ICMPGE, new Label());
                break;
            default:
                throw new RuntimeException("Unsupported operator: " + binary.getOperator());
        }

        return "INT";
    }
    private String generateFloatBinaryExpression(BinaryExpressionNode binary, MethodVisitor method) {
        switch (binary.getOperator()) {
            case "+":
                method.visitInsn(FADD);
                break;
            case "-":
                method.visitInsn(FSUB);
                break;
            case "*":
                method.visitInsn(FMUL);
                break;
            case "/":
                method.visitInsn(FDIV);
                break;
            default:
                throw new RuntimeException("Unsupported operator: " + binary.getOperator());
        }

        return "FLOAT";
    }
    private String generateBoolBinaryExpression(BinaryExpressionNode binary, MethodVisitor method) {
        switch (binary.getOperator()) {
            case "&&":
                method.visitInsn(IAND);
                break;
            case "||":
                method.visitInsn(IOR);
                break;
            default:
                throw new RuntimeException("Unsupported operator: " + binary.getOperator());
        }

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