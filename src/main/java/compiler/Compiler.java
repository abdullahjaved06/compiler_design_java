package compiler;

import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;
import compiler.Lexer.TokenType;
import compiler.Parser.Parser;
import compiler.Parser.AST.ASTNode;
import java.io.StringReader;
import java.io.Reader;
import java.io.FileReader;

public class Compiler {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("No arguments provided. Running default Parser test...");
            runTest();
            return;
        }
        String mode = args[0];
        String filepath = args[1];
        try {
            if (mode.equals("-lexer")) {
                runLexer(filepath);
            } else if (mode.equals("-parser")) {
                runParser(filepath);
            } else {
                System.out.println("Unknown mode: " + mode);
                System.exit(1);
            }
        } catch (Exception e) {
            // Requirement: Report syntax error and return non-zero for Inginious
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void runLexer(String filepath) throws Exception {
        System.out.println("Running Lexer on: " + filepath);
        try (Reader reader = new FileReader(filepath)) {
            Lexer lexer = new Lexer(reader);
            Symbol symbol;
            while ((symbol = lexer.getNextSymbol()).getType() != TokenType.EOF) {
                if (symbol.getType() == TokenType.ERROR) {
                    System.err.println("Lexical error at line " + symbol.getLine() + ": " + symbol.getValue());
                    System.exit(1);
                }
                System.out.println(symbol);
            }
        }
    }

    private static void runParser(String filepath) throws Exception {
        try (Reader reader = new FileReader(filepath)) {
            Lexer lexer = new Lexer(reader);
            Parser parser = new Parser(lexer);
            ASTNode root = parser.getAST();

            if (root != null) {
                System.out.println(root.print(""));
            }
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void runTest() {
        // A simple test case to verify the Parser logic
        String testInput = """
                INT ap;
                INT i = 3;\s
                FLOAT j = 3.2*5.0;\s
                INT k = i*3;\s
                STRING message = "Hello";\s
                BOOL isEmpty  = true;\s
                INT a = "Hello World"; # This is technically wrong\s
                BOOL c = (a / k) == k;
                for (FLOAT f; 0 -> 1.1; f + 0.1) {
                    while (i <= 10) {
                        i = i + 2;
                        if (i < 5) {
                            i = i - 1;
                        } else {
                            i = i + 1;
                        }
                        if (i == 6) {
                            i = 7;
                        }
                    }
                }
                        \s""";
        System.out.println("Input String: " + testInput);
        System.out.println("\nGenerated AST:");
        System.out.println("--------------");

        try {
            Lexer lexer = new Lexer(new StringReader(testInput));

            Parser parser = new Parser(lexer);
            ASTNode root = parser.getAST();

            if (root != null) {
                System.out.println(root.print(""));
            }
        } catch (Exception e) {
            System.err.println("Parser Test Error: " + e.getMessage());
        }
    }
}