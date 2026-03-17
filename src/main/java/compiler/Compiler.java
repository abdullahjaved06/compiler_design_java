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
        String testInput = "INT x = 5; INT y = x + 10;";
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