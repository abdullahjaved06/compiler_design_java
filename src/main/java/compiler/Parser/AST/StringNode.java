package compiler.Parser.AST;

public class StringNode implements ASTNode{
    private final String value;

    public StringNode(String value) {
        this.value = value;
    }

    @Override
    public String print(String indent) {
        // Must follow the format: "String, abcd"
        return indent + "String, " + value;
    }
}
