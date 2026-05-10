package compiler.CodeGen;

import compiler.Parser.AST.*;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class CodeGenerator {
    private final Map<String, Integer> localSlots = new HashMap<>();
    private final Map<String, String> localTypes = new HashMap<>();
    private final Map<String, String> functionTypes = new HashMap<>();
    private final Map<String, List<String>> functionParams = new HashMap<>();
    private final Map<String, List<String[]>> collectionFields = new LinkedHashMap<>();
    private final Map<String, String> globalFieldDescriptors = new LinkedHashMap<>();
    private final Map<String, String> globalFieldTypes = new LinkedHashMap<>();


    private int nextSlot = 0;
    private String currentClassName;
    private String currentReturnType = "VOID";

    public void generate(ASTNode root, String outputFile) throws IOException {
        if (!(root instanceof BlockNode block)) {
            throw new RuntimeException("CodeGenerationError: root must be BlockNode.");
        }

        String className = classNameFromFile(outputFile);
        this.currentClassName = className;
        String outputDir = outputDirectoryFromFile(outputFile);

        functionTypes.clear();
        functionParams.clear();

        for (ASTNode node : block.getStatements()) {
            if (node instanceof CollectionNode coll) {
                registerCollection(coll);
            } else if (node instanceof FunctionNode fn) {
                registerFunction(fn);
            } else if (node instanceof FinalNode fin && fin.getAssignment() instanceof AssignmentNode a) {
                registerGlobal(a);
            } else if (node instanceof AssignmentNode a) {
                registerGlobal(a);
            }
        }

        boolean hasMain = functionTypes.containsKey("main");
        if (!hasMain) {
            throw new RuntimeException("CodeGenerationError: main function not found.");
        }

        for (Map.Entry<String, List<String[]>> entry : collectionFields.entrySet()) {
            byte[] collBytes = generateCollectionClass(entry.getKey(), entry.getValue());
            String collPath  = outputDir + entry.getKey() + ".class";
            writeFile(collPath, collBytes);
        }

        ClassWriter writer = startClass(className);

        emitStaticFields(writer);

        emitStaticInitializer(writer, block);

        for (ASTNode node : block.getStatements()) {
            if (node instanceof FunctionNode fn && !"main".equals(fn.getName())) {
                addFunction(writer, fn);
            }
        }

        for (ASTNode node : block.getStatements()) {
            if (node instanceof FunctionNode fn && "main".equals(fn.getName())) {
                addMainFromBlock(writer, fn.getBody());
                }
        }

        writer.visitEnd();
        writeFile(outputFile, writer.toByteArray());
    }
    private void registerFunction(FunctionNode fn) {
        String returnType = fn.getReturnType() == null ? "VOID" : fn.getReturnType();
        functionTypes.put(fn.getName(), returnType);

        List<String> params = new ArrayList<>();
        for (ASTNode arg : fn.getArgs()) {
            if (arg instanceof AssignmentNode a) {
                params.add(a.getType());
            }
        }

        functionParams.put(fn.getName(), params);
    }
            }
        }

        return types;
    }

    private ClassWriter startClass(String className) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(V1_8, ACC_PUBLIC, className, null, "java/lang/Object", null);
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

        for (Map.Entry<String, String> e : globalFieldTypes.entrySet()) {
            localSlots.put(e.getKey(), -1);
            localTypes.put(e.getKey(), e.getValue());
        }

        String oldReturnType = currentReturnType;
        currentReturnType = "VOID";
        generateBlock(body, method);
        method.visitInsn(RETURN);
        currentReturnType = oldReturnType;
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private void addFunction(ClassWriter writer, FunctionNode function) {
        String returnType = function.getReturnType() == null ? "VOID" : function.getReturnType();
        List<String> paramTypes = functionParams.get(function.getName());
        MethodVisitor method = writer.visitMethod(
                ACC_PUBLIC | ACC_STATIC,
                function.getName(),
                methodDescriptor(paramTypes, returnType),
                null,
                null);
        method.visitCode();

        localSlots.clear();
        localTypes.clear();
        nextSlot = 0;

        for (ASTNode arg : function.getArgs()) {
            if (arg instanceof AssignmentNode assignment) {
                localSlots.put(assignment.getIdentifier(), nextSlot);
                localTypes.put(assignment.getIdentifier(), assignment.getType());
                nextSlot += slotSize(assignment.getType());
            }
        }

        for (Map.Entry<String, String> e : globalFieldTypes.entrySet()) {
            if (!localSlots.containsKey(e.getKey())) {
                localSlots.put(e.getKey(), -1);
                localTypes.put(e.getKey(), e.getValue());
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
        switch (statement) {
            case FunctionCallNode call -> generateFunctionCallStatement(call, method);
            case AssignmentNode assignment -> generateAssignment(assignment, method);
            case IfNode ifNode -> generateIf(ifNode, method);
            case WhileNode whileNode -> generateWhile(whileNode, method);
            case ForNode forNode -> generateFor(forNode, method);
            case ReturnNode returnNode -> generateReturn(returnNode, method);
            default -> throw new RuntimeException(
                    "CodeGenerationError: unsupported statement: "
                            + statement.getClass().getSimpleName());
        }
    }

    private void generateAssignment(AssignmentNode assignment, MethodVisitor method) {
        String name = assignment.getIdentifier();
        String type = assignment.getType();

        if (type != null) {
            if (assignment.getExpression() == null) {
                pushDefault(type, method);
                int slot = allocateSlot(name, type);
                storeToSlot(type, slot, name, method);
            return;
        }

            String valueType = generateExpression(assignment.getExpression(), method);

            if ("FLOAT".equals(type) && "INT".equals(valueType)) {
                method.visitInsn(I2F);
                valueType = "FLOAT";
        }

            if (!type.equals(valueType)) {
                throw new RuntimeException(
                        "CodeGenerationError: variable type mismatch for " + name
                                + ": expected " + type + " got " + valueType);
        }

            int slot = allocateSlot(name, type);
            storeToSlot(type, slot, name, method);
            return;
        }

        if (!localSlots.containsKey(name)) {
            throw new RuntimeException("CodeGenerationError: unknown variable: " + name);
        }

        String oldType    = localTypes.get(name);
        String valueType  = generateExpression(assignment.getExpression(), method);

        if ("FLOAT".equals(oldType) && "INT".equals(valueType)) {
            method.visitInsn(I2F);
            valueType = "FLOAT";
        }

        if (!oldType.equals(valueType)) {
            throw new RuntimeException(
                    "CodeGenerationError: variable type mismatch for " + name);
            }

        int slot = localSlots.get(name);
        storeToSlot(oldType, slot, name, method);
    }

    private int allocateSlot(String name, String type) {
            int slot = nextSlot;
        nextSlot += slotSize(type);
            localSlots.put(name, slot);
            localTypes.put(name, type);
        return slot;
    }

    private void storeToSlot(String type, int slot, String name, MethodVisitor method) {
        if (slot == -1) {
            method.visitFieldInsn(PUTSTATIC, currentClassName, name, descriptorFor(type));
        } else {
            method.visitVarInsn(storeOpcode(type), slot);
        }
    }
    private void pushDefault(String type, MethodVisitor method) {
        switch (type) {
            case "INT", "BOOL" -> method.visitInsn(ICONST_0);
            case "FLOAT" -> method.visitInsn(FCONST_0);
            case "STRING" -> method.visitInsn(ACONST_NULL);
            default -> {
                method.visitInsn(ACONST_NULL);
            }
        }
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
        ASTNode initNode = forNode.getInit();

        String varName;
        String varType = "INT";

        switch (initNode) {
            case AssignmentNode init when init.getType() != null -> {
                varName = init.getIdentifier();
                varType = init.getType();

                if (!"INT".equals(varType)) {
                    throw new RuntimeException("CodeGenerationError: for-loop variable must be INT.");
        }

                allocateSlot(varName, varType);
            }
            case IdentifierNode ident -> {
                varName = ident.getName();

                if (!localSlots.containsKey(varName)) {
                    throw new RuntimeException(
                            "CodeGenerationError: for-loop variable '" + varName + "' is not declared.");
                }

                varType = localTypes.get(varName);

                if (!"INT".equals(varType)) {
            throw new RuntimeException("CodeGenerationError: for-loop variable must be INT.");
        }
            }
            case AssignmentNode init when init.getType() == null -> {
                varName = init.getIdentifier();

                if (!localSlots.containsKey(varName)) {
                    throw new RuntimeException(
                            "CodeGenerationError: for-loop variable '" + varName + "' is not declared.");
                }

                varType = localTypes.get(varName);

                if (!"INT".equals(varType)) {
                    throw new RuntimeException("CodeGenerationError: for-loop variable must be INT.");
                }
            }
            case null, default -> throw new RuntimeException("CodeGenerationError: invalid for-loop initializer.");
        }

        int slot = localSlots.get(varName);
        String startType = generateExpression(forNode.getRangeStart(), method);

        if (!"INT".equals(startType)) {
            throw new RuntimeException("CodeGenerationError: for-loop start must be INT.");
        }

        storeToSlot(varType, slot, varName, method);

        Label startLabel = new Label();
        Label endLabel   = new Label();

        method.visitLabel(startLabel);
        loadFromSlot(varType, slot, varName, method);
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

        storeToSlot(varType, slot, varName, method);
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

        if ("FLOAT".equals(currentReturnType) && "INT".equals(valueType)) {
            method.visitInsn(I2F);
            valueType = "FLOAT";
        }

        if (!currentReturnType.equals(valueType)) {
            throw new RuntimeException("CodeGenerationError: wrong return type. Expected " + currentReturnType + " got " + valueType);
        }

        method.visitInsn(returnOpcode(currentReturnType));
    }

    private void generateFunctionCallStatement(FunctionCallNode call, MethodVisitor method) {
        String name = call.getFunctionName();

        switch (name) {
            case "println"     -> { generatePrintln(call, method); return; }
            case "print", "write" -> { generatePrint(call, method, false); return; }
            case "print_INT"   -> { generatePrintTyped(call, "INT",   method); return; }
            case "print_FLOAT" -> { generatePrintTyped(call, "FLOAT", method); return; }
        }

        String returnType = generateFunctionCallExpression(call, method);

        if (!"VOID".equals(returnType)) {
            method.visitInsn(POP);
        }
    }

    private String generateFunctionCallExpression(FunctionCallNode call, MethodVisitor method) {
        String name = call.getFunctionName();

        switch (name) {
            case "read_INT" -> {
                emitReadScanner("nextInt", "I", method);
                return "INT";
            }
            case "read_FLOAT" -> {
                emitReadScanner("nextFloat", "F", method);
                return "FLOAT";
            }
            case "read_STRING" -> {
                emitReadScanner("next", "Ljava/lang/String;", method);
                return "STRING";
            }
            case "floor" -> {
                generateExpression(call.getArguments().getFirst(), method);
                method.visitInsn(F2D);
                method.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "floor", "(D)D", false);
                method.visitInsn(D2I);
                return "INT";
            }
            case "ceil" -> {
                generateExpression(call.getArguments().getFirst(), method);
                method.visitInsn(F2D);
                method.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "ceil", "(D)D", false);
                method.visitInsn(D2I);
                return "INT";
            }
            case "str" -> {
                generateExpression(call.getArguments().getFirst(), method);
                method.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf",
                        "(I)Ljava/lang/String;", false);
                return "STRING";
            }
            case "length" -> {
                ASTNode arg = call.getArguments().getFirst();
                String argType = generateExpression(arg, method);
                if ("STRING".equals(argType)) {
                    method.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
                } else {
                    // Array type
                    method.visitInsn(ARRAYLENGTH);
                }
                return "INT";
            }
            case "println" -> {
                generatePrintln(call, method);
                return "VOID";
            }
            case "print", "write" -> {
                generatePrint(call, method, false);
                return "VOID";
            }
            case "print_INT" -> {
                generatePrintTyped(call, "INT", method);
                return "VOID";
            }
            case "print_FLOAT" -> {
                generatePrintTyped(call, "FLOAT", method);
                return "VOID";
            }
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
                false);

        return returnType;
    }

    private void generateCallArguments(FunctionCallNode call, List<String> paramTypes, MethodVisitor method) {
        if (call.getArguments().size() != paramTypes.size()) {
            throw new RuntimeException("CodeGenerationError: wrong number of arguments for " + call.getFunctionName());
        }

        for (int i = 0; i < call.getArguments().size(); i++) {
            String actualType = generateExpression(call.getArguments().get(i), method);
            String expectedType = paramTypes.get(i);

            if ("FLOAT".equals(expectedType) && "INT".equals(actualType)) {
                method.visitInsn(I2F);
            } else if (!expectedType.equals(actualType)) {
                throw new RuntimeException("CodeGenerationError: wrong argument type for "
                        + call.getFunctionName() + ": expected " + expectedType + " got " + actualType);
            }
        }
    }

    private void emitReadScanner(String scannerMethod, String returnDesc, MethodVisitor method) {
        method.visitTypeInsn(NEW, "java/util/Scanner");
        method.visitInsn(DUP);
        method.visitFieldInsn(GETSTATIC, "java/lang/System", "in", "Ljava/io/InputStream;");
        method.visitMethodInsn(INVOKESPECIAL, "java/util/Scanner", "<init>",
                "(Ljava/io/InputStream;)V", false);
        method.visitMethodInsn(INVOKEVIRTUAL, "java/util/Scanner", scannerMethod,
                "()" + returnDesc, false);
    }

    private void generatePrintln(FunctionCallNode call, MethodVisitor method) {
        method.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");

        if (call.getArguments().isEmpty()) {
            method.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "()V", false);
            return;
        }

        String type = generateExpression(call.getArguments().get(0), method);
        String desc = printDescriptorFor(type);
        method.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(" + desc + ")V", false);
    }

    private void generatePrint(FunctionCallNode call, MethodVisitor method, boolean withNewline) {
        if (call.getArguments().isEmpty()) return;

        method.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        String type = generateExpression(call.getArguments().get(0), method);
        String desc = printDescriptorFor(type);
        String printMethod = withNewline ? "println" : "print";
        method.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", printMethod,
                "(" + desc + ")V", false);
        }

    private void generatePrintTyped(FunctionCallNode call, String expectedType, MethodVisitor method) {
        method.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        String type = generateExpression(call.getArguments().get(0), method);

        if ("FLOAT".equals(expectedType) && "INT".equals(type)) {
            method.visitInsn(I2F);
            type = "FLOAT";
        }

        String desc = printDescriptorFor(type);
        method.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(" + desc + ")V", false);
    }

    private String printDescriptorFor(String type) {
        return switch (type) {
            case "INT"    -> "I";
            case "FLOAT"  -> "F";
            case "BOOL"   -> "Z";
            case "STRING" -> "Ljava/lang/String;";
            default       -> "Ljava/lang/Object;";
        };
    }

    private String generateExpression(ASTNode expression, MethodVisitor method) {
        return switch (expression) {
            case LiteralNode literal -> generateLiteral(literal, method);
            case IdentifierNode identifier -> generateIdentifier(identifier, method);
            case BinaryExpressionNode binary -> generateBinaryExpression(binary, method);
            case FunctionCallNode call -> generateFunctionCallExpression(call, method);
            case null, default -> {
                assert expression != null;
                throw new RuntimeException("CodeGenerationError: unsupported expression: "
                        + expression.getClass().getSimpleName());
            }
        };
    }
            case "-":
                method.visitInsn(ISUB);
                return "INT";

            case "*":
                method.visitInsn(IMUL);
                return "INT";

            case "/":
                method.visitInsn(IDIV);
                return "INT";
    private String generateBinaryExpression(BinaryExpressionNode binary, MethodVisitor method) {
        String op       = binary.getOperator();
        String exprKind = binary.getType();

        String leftType  = generateExpression(binary.getLeft(),  method);
        String rightType = generateExpression(binary.getRight(), method);

        if ("FLOAT".equals(leftType) && "INT".equals(rightType)) {
            method.visitInsn(I2F);
            rightType = "FLOAT";
        } else if ("INT".equals(leftType) && "FLOAT".equals(rightType)) {
            method.visitInsn(SWAP);
            method.visitInsn(I2F);
            method.visitInsn(SWAP);
            leftType = "FLOAT";
        }

        if ("INT".equals(leftType) && "INT".equals(rightType)) {
            return generateIntBinary(op, exprKind, method);
        }

        if ("FLOAT".equals(leftType) && "FLOAT".equals(rightType)) {
            return generateFloatBinary(op, exprKind, method);
        }

        if ("BOOL".equals(leftType) && "BOOL".equals(rightType)) {
            return generateBoolBinary(op, method);
        }

        if ("STRING".equals(leftType) && "STRING".equals(rightType)) {
            return generateStringBinary(op, method);
        }

        throw new RuntimeException("CodeGenerationError: unsupported binary operation for "
                + leftType + " and " + rightType);
    }

    private String generateIntBinary(String op, String exprKind, MethodVisitor method) {
        return switch (op) {
            case "+" -> { method.visitInsn(IADD); yield "INT"; }
            case "-" -> { method.visitInsn(ISUB); yield "INT"; }
            case "*" -> { method.visitInsn(IMUL); yield "INT"; }
            case "/" -> { method.visitInsn(IDIV); yield "INT"; }
            case "%" -> { method.visitInsn(IREM); yield "INT"; }
            case "==" -> generateIntComparison(IF_ICMPEQ, method);
            case "=/=", "!=" -> generateIntComparison(IF_ICMPNE, method);
            case "<"  -> generateIntComparison(IF_ICMPLT, method);
            case ">"  -> generateIntComparison(IF_ICMPGT, method);
            case "<=" -> generateIntComparison(IF_ICMPLE, method);
            case ">=" -> generateIntComparison(IF_ICMPGE, method);
            default -> throw new RuntimeException(
                    "CodeGenerationError: unsupported INT operator: " + op);
        };
    }

    private String generateFloatBinary(String op, String exprKind, MethodVisitor method) {
        return switch (op) {
            case "+" -> { method.visitInsn(FADD); yield "FLOAT"; }
            case "-" -> { method.visitInsn(FSUB); yield "FLOAT"; }
            case "*" -> { method.visitInsn(FMUL); yield "FLOAT"; }
            case "/" -> { method.visitInsn(FDIV); yield "FLOAT"; }
            case "==" -> {
                method.visitInsn(FCMPL);
                yield generateZeroComparison(IFEQ, method);
            }
            case "=/=", "!=" -> {
                method.visitInsn(FCMPL);
                yield generateZeroComparison(IFNE, method);
            }
            case "<" -> {
                method.visitInsn(FCMPL);
                yield generateZeroComparison(IFLT, method);
            }
            case ">" -> {
                method.visitInsn(FCMPG);
                yield generateZeroComparison(IFGT, method);
            }
            case "<=" -> {
                method.visitInsn(FCMPL);
                yield generateZeroComparison(IFLE, method);
            }
            case ">=" -> {
                method.visitInsn(FCMPG);
                yield generateZeroComparison(IFGE, method);
            }
            default -> throw new RuntimeException(
                    "CodeGenerationError: unsupported FLOAT operator: " + op);
        };
    }

    private String generateBoolBinary(String op, MethodVisitor method) {
        return switch (op) {
            case "&&" -> generateShortCircuitAnd(method);
            case "||" -> generateShortCircuitOr(method);
            case "==" -> generateIntComparison(IF_ICMPEQ, method);
            case "=/=", "!=" -> generateIntComparison(IF_ICMPNE, method);
            default -> throw new RuntimeException(
                    "CodeGenerationError: unsupported BOOL operator: " + op);
        };
    }

    private String generateShortCircuitAnd(MethodVisitor method) {
                method.visitInsn(IAND);
                return "BOOL";
    }

    private String generateShortCircuitOr(MethodVisitor method) {
                method.visitInsn(IOR);
                return "BOOL";
    }

    private String generateZeroComparison(int jumpOpcode, MethodVisitor method) {
        Label trueLabel = new Label();
        Label endLabel  = new Label();
        method.visitJumpInsn(jumpOpcode, trueLabel);
        method.visitInsn(ICONST_0);
        method.visitJumpInsn(GOTO, endLabel);
        method.visitLabel(trueLabel);
        method.visitInsn(ICONST_1);
        method.visitLabel(endLabel);
        return "BOOL";
    }

    private String generateStringBinary(String op, MethodVisitor method) {
        if ("+".equals(op)) {
            method.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "concat",
                    "(Ljava/lang/String;)Ljava/lang/String;", false);
            return "STRING";
        }
        if ("==".equals(op)) {
            method.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals",
                    "(Ljava/lang/Object;)Z", false);
            return "BOOL";
        }
        if ("=/=".equals(op) || "!=".equals(op)) {
            method.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals",
                    "(Ljava/lang/Object;)Z", false);
            method.visitInsn(ICONST_1);
            method.visitInsn(IXOR);
            return "BOOL";
        }
        throw new RuntimeException("CodeGenerationError: unsupported STRING operator: " + op);
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