
import compiler.Lexer.Lexer;
import compiler.Parser.Parser;
import compiler.Parser.AST.ASTNode;
import java.io.StringReader;

public class TestParser {
    public static void main(String[] args) {
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

        try {
            Lexer lexer = new Lexer(new StringReader(testInput));

            Parser parser = new Parser(lexer);

            System.out.println("Starting Parser");
            ASTNode root = parser.getAST();
            System.out.println("Generated AST:");
            System.out.println(root.print(""));

        } catch (Exception e) {
            System.err.println("Parser Test Error: " + e.getMessage());
        }
    }
}