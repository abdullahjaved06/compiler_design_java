import static org.junit.Assert.assertNotNull;

import compiler.Lexer.Symbol;
import compiler.Lexer.TokenType;
import org.junit.Test;

import java.io.StringReader;
import compiler.Lexer.Lexer;

public class TestLexer {
    
    @Test
    public void test() {
        String input = "FLOAT f; ceil(f);";
        StringReader reader = new StringReader(input);
        Lexer lexer = new Lexer(reader);
        assertNotNull(lexer.getNextSymbol());
        Symbol symbol;
        while ((symbol = lexer.getNextSymbol()).getType() != TokenType.EOF) {
            System.out.println(symbol);
        }
    }

}
