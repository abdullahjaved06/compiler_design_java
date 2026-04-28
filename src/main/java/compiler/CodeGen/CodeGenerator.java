package compiler.CodeGen;

import compiler.Parser.AST.ASTNode;
import compiler.Parser.AST.BlockNode;
import compiler.Parser.AST.FunctionCallNode;
import compiler.Parser.AST.FunctionNode;
import compiler.Parser.AST.LiteralNode;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.objectweb.asm.Opcodes.*;

public class CodeGenerator {

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

        for (ASTNode statement : body.getStatements()) {
            generateStatement(statement, method);
        }

        method.visitInsn(RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private void generateStatement(ASTNode statement, MethodVisitor method) {
        if (statement instanceof FunctionCallNode call) {
            generateFunctionCall(call, method);
            return;
        }

        throw new RuntimeException(
                "CodeGenerationError: unsupported statement: "
                        + statement.getClass().getSimpleName()
        );
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

        throw new RuntimeException(
                "CodeGenerationError: unsupported expression: "
                        + expression.getClass().getSimpleName()
        );
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