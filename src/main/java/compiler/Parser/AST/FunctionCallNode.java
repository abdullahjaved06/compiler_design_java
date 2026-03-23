package compiler.Parser.AST;

import java.util.List;

public class FunctionCallNode implements ASTNode {
    private final String name;
    private final List<ASTNode> args;

    public FunctionCallNode(String name, List<ASTNode> args) {
        this.name = name;
        this.args = args;
    }

    @Override
    public String print(String indent) {
        StringBuilder sb = new StringBuilder();

        sb.append(indent).append("FunctionCall ").append(name).append("\n");
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