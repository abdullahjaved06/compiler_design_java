package CodeGen;

import compiler.CodeGen.CodeGenerator;
import org.junit.Test;

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
}