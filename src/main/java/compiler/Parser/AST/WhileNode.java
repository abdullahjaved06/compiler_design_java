package compiler.Parser.AST;

public class WhileNode implements ASTNode {
    private final ASTNode condition;
    private final BlockNode body;

    public WhileNode(ASTNode condition, BlockNode body) {
        this.condition = condition;
        this.body = body;
    }
    public ASTNode getCondition() {
        return condition;
    }
    public BlockNode getBody() {
        return body;
    }

    @Override
    public String print(String indent) {
        StringBuilder sb = new StringBuilder();

        sb.append(indent).append("WhileLoop\n");

        sb.append(indent).append("  Condition\n");
        sb.append(condition.print(indent + "    ")).append("\n");

        sb.append(indent).append("  LoopBody\n");
        sb.append(body.print(indent + "    "));


        return sb.toString();
    }
}