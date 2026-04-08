package compiler;

import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;
import compiler.Lexer.TokenType;
import compiler.Parser.Parser;
import compiler.Parser.AST.ASTNode;
import compiler.Semantic.SemanticAnalyzer;

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
            } else if (mode.equals("-semantic")) {
                runSemantic(filepath);
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

    private static void runSemantic(String filepath) throws Exception {
        try (Reader reader = new FileReader(filepath)) {
            Lexer lexer = new Lexer(reader);
            Parser parser = new Parser(lexer);
            ASTNode root = parser.getAST();
            if (root != null) {
                SemanticAnalyzer analyzer = new SemanticAnalyzer();
                analyzer.analyze(root);
            }
        }
    }

    private static void runTest() {
        // A simple test case to verify the Parser logic
        String testInput = """
                INT i = 3 ; 
                final FLOAT j = 3.2 * 5.0 ; 
                final INT k = i * 3 ; 
                final STRING message = "Hello" ; 
                final BOOL isEmpty = true ; 
                coll Point { 
                    INT x ;
                    INT y ;
                } 
                coll Person { #10
                    STRING name ;
                    Point location ; 
                    INT [ ] history ; 
                } 
                INT a = 3 ; 
                INT [ ] c = INT ARRAY [ 5 ] ; 
                Person d = Person ( "me" , Point ( 3 , 7 ) , INT ARRAY [ i * 2 ] ) ; 
                def INT square ( INT v ) { 
                    return v * v ; 
                } #20
                def Point copyPoints ( Point [ ] p ) { 
                    return Point ( p [ 0 ] . x + p [ 1 ] . x , p [ 0 ] . y + p [ 1 ] . y ) ; 
                } 
                def main ( ) { 
                    INT value = read_INT ( ) ;
                    println( square ( value ) ) ; 
                    INT i ; 
                    for ( i ; 1 -> 100 ; i + 1 ) { 
                        while ( value =/= 3 ) { 
                            if ( i > 10 ) { #30
                                value = value - 1 ; 
                            } else { 
                                write( message ) ; 
                            } 
                        } 
                    } 
                    i = ( i + 2 ) * 2 ;
                 }\s""";
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
