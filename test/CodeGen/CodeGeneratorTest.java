package CodeGen;

import compiler.CodeGen.CodeGenerator;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;
import compiler.Lexer.Lexer;
import compiler.Parser.Parser;
import compiler.Parser.AST.ASTNode;
import compiler.Semantic.SemanticAnalyzer;

import java.io.FileReader;
import java.io.Reader;

import static org.junit.Assert.assertEquals;

public class CodeGeneratorTest {
    @Test
    public void makesEmptyClassFile() throws Exception {
        CodeGenerator generator = new CodeGenerator();
        Path outputFile = Path.of("build/test-codegen/Test.class");

        Files.deleteIfExists(outputFile);
        generator.makeEmptyClass("Test",outputFile.toString());

        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile)>0);
    }
    @Test
    public void generatedFileHasClassHeader() throws Exception {
        CodeGenerator generator = new CodeGenerator();
        Path outputFile = Path.of("build/test-codegen/Test.class");
        Files.deleteIfExists(outputFile);
        generator.makeEmptyClass("Test", outputFile.toString());

        byte[] bytes = Files.readAllBytes(outputFile);
        assertTrue(bytes.length > 4);
        assertTrue((bytes[0] & 0xFF) == 0xCA);
        assertTrue((bytes[1] & 0xFF) == 0xFE);
        assertTrue((bytes[2] & 0xFF) == 0xBA);
        assertTrue((bytes[3] & 0xFF) == 0xBE);
    }
    @Test
    public void makesRunnableClassWithMain() throws Exception {
        CodeGenerator generator = new CodeGenerator();

        Path outputFile = Path.of("build/test-codegen/MainTest.class");

        Files.deleteIfExists(outputFile);

        generator.makeClassWithEmptyMain("MainTest", outputFile.toString());

        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0);
    }
    @Test
    public void makesClassThatPrintsHello() throws Exception {
        CodeGenerator generator = new CodeGenerator();

        Path outputFile = Path.of("build/test-codegen/HelloTest.class");

        Files.deleteIfExists(outputFile);

        generator.makeClassWithHelloMain("HelloTest", outputFile.toString());

        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0);
    }
    @Test
    public void usesOutputFileNameAsClassName() throws Exception {
        CodeGenerator generator = new CodeGenerator();

        Path outputFile = Path.of("build/test-codegen/test.class");

        Files.deleteIfExists(outputFile);

        generator.makeHelloClass(outputFile.toString());

        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0);
    }
    @Test
    public void generateCreatesRunnableClass() throws Exception {
        CodeGenerator generator = new CodeGenerator();

        Path outputFile = Path.of("build/test-codegen/Generated.class");

        Files.deleteIfExists(outputFile);

        generator.makeHelloClass(outputFile.toString());

        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0);
    }
    @Test
    public void compilesAndRunsVariableDeclarations() throws Exception {
        Path sourceFile = Path.of("test/CodeGen/variables.lang");
        Path outputFile = Path.of("build/test-codegen/variables.class");

        Files.deleteIfExists(outputFile);

        try (Reader reader = new FileReader(sourceFile.toFile())) {
            Lexer lexer = new Lexer(reader);
            Parser parser = new Parser(lexer);
            ASTNode root = parser.getAST();

            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            analyzer.analyze(root);

            CodeGenerator generator = new CodeGenerator();
            generator.generate(root, outputFile.toString());
        }

        Process process = new ProcessBuilder(
                "java",
                "-cp",
                "build/test-codegen",
                "variables"
        ).redirectErrorStream(true).start();

        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        output = output.replace("\r\n", "\n");

        assertEquals(0, exitCode);
        assertEquals("10\ncompiler\n2.5\ntrue\n", output);
    }
    @Test
    public void compilesAndRunsSimpleFunctions() throws Exception {
        Path sourceFile = Path.of("test/CodeGen/functions.lang");
        Path outputFile = Path.of("build/test-codegen/functions.class");

        Files.deleteIfExists(outputFile);

        try (Reader reader = new FileReader(sourceFile.toFile())) {
            Lexer lexer = new Lexer(reader);
            Parser parser = new Parser(lexer);
            ASTNode root = parser.getAST();

            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            analyzer.analyze(root);

            CodeGenerator generator = new CodeGenerator();
            generator.generate(root, outputFile.toString());
        }

        Process process = new ProcessBuilder(
                "java",
                "-cp",
                "build/test-codegen",
                "functions"
        ).redirectErrorStream(true).start();

        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        output = output.replace("\r\n", "\n");

        assertEquals(0, exitCode);
        assertEquals("start\ninside helper\nend\n", output);
    }
    @Test
    public void compilesAndRunsReturnFunctions() throws Exception {
        Path sourceFile = Path.of("test/CodeGen/return_function.lang");
        Path outputFile = Path.of("build/test-codegen/return_function.class");

        Files.deleteIfExists(outputFile);

        try (Reader reader = new FileReader(sourceFile.toFile())) {
            Lexer lexer = new Lexer(reader);
            Parser parser = new Parser(lexer);
            ASTNode root = parser.getAST();

            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            analyzer.analyze(root);

            CodeGenerator generator = new CodeGenerator();
            generator.generate(root, outputFile.toString());
        }

        Process process = new ProcessBuilder(
                "java",
                "-cp",
                "build/test-codegen",
                "return_function"
        ).redirectErrorStream(true).start();

        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        output = output.replace("\r\n", "\n");

        assertEquals(0, exitCode);
        assertEquals("42\ndone\ntrue\n", output);
    }
    private String compileAndRun(String sourcePath, String className) throws Exception {
        Path sourceFile = Path.of(sourcePath);
        Path outputFile = Path.of("build/test-codegen/" + className + ".class");

        Files.deleteIfExists(outputFile);

        try (Reader reader = new FileReader(sourceFile.toFile())) {
            Lexer lexer = new Lexer(reader);
            Parser parser = new Parser(lexer);
            ASTNode root = parser.getAST();

            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            analyzer.analyze(root);

            CodeGenerator generator = new CodeGenerator();
            generator.generate(root, outputFile.toString());
        }

        Process process = new ProcessBuilder(
                "java",
                "-cp",
                "build/test-codegen",
                className
        ).redirectErrorStream(true).start();

        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        output = output.replace("\r\n", "\n");

        assertEquals(0, exitCode);

        return output;
    }
    @Test
    public void compilesAndRunsIntArithmetic() throws Exception {
        String output = compileAndRun(
                "test/CodeGen/int_arithmetic.lang",
                "int_arithmetic"
        );

        assertEquals("15\n17\n24\n5\n2\n", output);
    }

    @Test
    public void compilesAndRunsIntComparisons() throws Exception {
        String output = compileAndRun(
                "test/CodeGen/int_comparison.lang",
                "int_comparison"
        );

        assertEquals("15\n17\nfalse\ntrue\ntrue\nfalse\n", output);
    }

    @Test
    public void compilesAndRunsReassignment() throws Exception {
        String output = compileAndRun(
                "test/CodeGen/reassignment.lang",
                "reassignment"
        );

        assertEquals("10\n20\n25\n", output);
    }

    @Test
    public void compilesAndRunsIfElse() throws Exception {
        String output = compileAndRun(
                "test/CodeGen/if_else.lang",
                "if_else"
        );

        assertEquals("small\nlarge\n", output);
    }

    @Test
    public void compilesAndRunsWhileLoop() throws Exception {
        String output = compileAndRun(
                "test/CodeGen/while_loop.lang",
                "while_loop"
        );

        assertEquals("0\n1\n2\n3\n4\n", output);
    }

    @Test
    public void compilesAndRunsFunctionParameters() throws Exception {
        String output = compileAndRun(
                "test/CodeGen/function_parameters.lang",
                "function_parameters"
        );

        assertEquals("7\n12\nok\n", output);
    }
    @Test
    public void compilesAndRunsFloatArithmetic() throws Exception {
        String output = compileAndRun(
                "test/CodeGen/float_arithmetic.lang",
                "float_arithmetic"
        );

        assertEquals("4.0\n3.5\n6.0\n3.0\n", output);
    }
}