package compiler.Parser.AST;

import java.util.List;

public class ConstructorCallNode implements ASTNode {
    private final String collection;
    private final List<ASTNode> args;

    public ConstructorCallNode(String collection, List<ASTNode> args) {
        this.collection = collection;
        this.args = args;
    }

    public String getCollectionName() {
        return collection;
    }

    public List<ASTNode> getArguments() {
        return args;
    }

    @Override
    public String print(String indent) {
        StringBuilder sb = new StringBuilder();

        sb.append(indent).append("ConstructorCall\n");
        sb.append(indent).append("  Collection ").append(collection).append("\n");
        sb.append(indent).append("  Arguments");

        if (args.isEmpty()) {
            sb.append("  None");
        } else {
            for (ASTNode arg : args) {
                sb.append("\n").append(arg.print(indent + "    "));
            }
        }
        return sb.toString();
    }
}
