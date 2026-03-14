package compiler.Parser.AST;

public class IntegerNode implements ASTNode {
    private final String value;

    public IntegerNode(String value) {
        this.value = value;
    }

    @Override
    public String print(String indent) {
        // Must follow the format: "Integer, 1"
        return indent + "Integer, " + value;
    }
}
