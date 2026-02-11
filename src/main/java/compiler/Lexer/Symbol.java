package compiler.Lexer;

public class Symbol {
    private TokenType type;
    private String value;
    private int line;
    private int column;

    //constructor - creates a new symbol
    public Symbol(TokenType type, String value, int line, int column) {
        this.type = type;
        this.value = value;
        this.line  = line;
        this.column = column;
    }
    // Getter functions. it will allow other classses to read thedata.
    public TokenType getType() {
        return type;
    }
    public String getValue(){
        return value;
    }
    public int getLine(){
        return line;
    }
    public int getColumn() {
        return column;
    }
    @override
    public String toString() {
        return "<" + type + "," + value + ">";
    }
}
