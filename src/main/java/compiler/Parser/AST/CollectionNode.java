package compiler.Parser.AST;

public class CollectionNode implements ASTNode {
    private final String name;
    private final BlockNode body;

    public CollectionNode(String name, BlockNode body) {
        this.name = name;
        this.body = body;
    }

    @Override
    public String print(String indent) {
        StringBuilder sb = new StringBuilder();

        sb.append(indent).append("CollectionDefinition\n");

        sb.append(indent).append("  Name,  ").append(name).append("\n");

        sb.append(indent).append("  CollectionMembers\n");
        sb.append(body.print(indent + "    "));

        return sb.toString();
    }
}