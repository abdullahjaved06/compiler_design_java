package compiler.Parser.AST;

import java.util.List;

public class FunctionNode implements ASTNode {
    private final String returnType;
    private final String name;
    private final List<ASTNode> args;
    private final BlockNode body;

    public FunctionNode(String returnType, String name, List<ASTNode> args, BlockNode body) {
        this.returnType = returnType;
        this.name = name;
        this.args = args;
        this.body = body;
    }

    @Override
    public String print(String indent) {
        StringBuilder sb = new StringBuilder();

        sb.append(indent).append("FunctionDefinition ").append(name).append("\n");
        sb.append(indent).append("  ReturnType ").append(returnType).append("\n");
        sb.append(indent).append("  Arguments");

        if (args.isEmpty()) {
             sb.append(", None");
        } else {
            for (ASTNode arg : args) {
                sb.append("\n").append(arg.print(indent + "    "));
            }
        }
        sb.append("\n").append(indent).append("  Body\n");
        sb.append(body.print(indent + "    "));

        return sb.toString();
    }
}
