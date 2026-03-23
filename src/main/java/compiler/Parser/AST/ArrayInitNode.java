package compiler.Parser.AST;

public class ArrayInitNode implements ASTNode {
    private final String type;
    private final ASTNode size;

    public ArrayInitNode(String type, ASTNode size) {
        this.type = type;
        this.size = size;
    }

    @Override
    public String print(String indent) {
        StringBuilder sb = new StringBuilder();

        sb.append(indent).append("ArrayInit\n");
        sb.append(indent).append("  Type, ").append(type).append("\n");
        sb.append(indent).append("  Size\n");
        sb.append(size.print(indent + "    "));

        return sb.toString();
    }
}