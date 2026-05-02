package CodeGen;

import compiler.CodeGen.CodeGenerator;
import compiler.Lexer.Lexer;
import compiler.Parser.Parser;
import compiler.Parser.AST.ASTNode;
import compiler.Semantic.SemanticAnalyzer;

import org.junit.Test;

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static org.junit.Assert.assertEquals;

public class CodeGeneratorTest {

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
    public void compilesAndRunsLiteralPrints() throws Exception {
        String output = compileAndRun(
                "test/CodeGen/literals.lang",
                "literals"
        );

        assertEquals("hello\n123\n3.5\ntrue\nfalse\n", output);
    }

    @Test
    public void compilesAndRunsVariableDeclarations() throws Exception {
        String output = compileAndRun(
                "test/CodeGen/variables.lang",
                "variables"
        );

        assertEquals("10\ncompiler\n2.5\ntrue\n", output);
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
    public void compilesAndRunsSimpleFunctions() throws Exception {
        String output = compileAndRun(
                "test/CodeGen/functions.lang",
                "functions"
        );

        assertEquals("start\ninside helper\nend\n", output);
    }

    @Test
    public void compilesAndRunsReturnFunctions() throws Exception {
        String output = compileAndRun(
                "test/CodeGen/return_function.lang",
                "return_function"
        );

        assertEquals("42\ndone\ntrue\n", output);
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

    @Test
    public void compilesAndRunsVoidReturn() throws Exception {
        String output = compileAndRun(
                "test/CodeGen/void_return.lang",
                "void_return"
        );

        assertEquals("before return\n", output);
    }
    @Test
    public void compilesAndRunsFullProgram() throws Exception {
        String output = compileAndRun(
                "test/CodeGen/full_program.lang",
                "full_program"
        );

        assertEquals("7\n10\nok\n4.0\n", output);
    }
    @Test
    public void compilesAndRunsForLoop() throws Exception {
        String output = compileAndRun(
                "test/CodeGen/for_loop.lang",
                "for_loop"
        );

        assertEquals("0\n1\n2\n3\n4\n", output);
    }
    @Test
    public void failsWhenMainIsMissing() throws Exception {
        Path sourceFile = Path.of("test/CodeGen/missing_main.lang");
        Path outputFile = Path.of("build/test-codegen/missing_main.class");

        try (Reader reader = new FileReader(sourceFile.toFile())) {
            Lexer lexer = new Lexer(reader);
            Parser parser = new Parser(lexer);
            ASTNode root = parser.getAST();

            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            analyzer.analyze(root);

            CodeGenerator generator = new CodeGenerator();
            generator.generate(root, outputFile.toString());

            fail("Expected code generation to fail because main is missing.");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("main function not found"));
        }
    }
}