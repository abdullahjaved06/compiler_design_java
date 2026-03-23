package compiler.Parser.AST;

public class IndexAccessNode implements ASTNode {
    private final ASTNode array;
    private final ASTNode index;

    public IndexAccessNode(ASTNode array, ASTNode index) {
        this.array = array;
        this.index = index;
    }

    @Override
    public String print(String indent) {
        StringBuilder sb = new StringBuilder();

        sb.append(indent).append("IndexAccess\n");

        sb.append(array.print(indent + "  ")).append("\n");
        sb.append(indent).append("  Index\n");
        sb.append(index.print(indent + "    "));

        return sb.toString();
    }
}