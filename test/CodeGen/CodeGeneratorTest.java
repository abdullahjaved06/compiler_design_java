package CodeGen;

import compiler.CodeGen.CodeGenerator;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;


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
}