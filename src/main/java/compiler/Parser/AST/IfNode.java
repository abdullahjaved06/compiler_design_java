package compiler.Parser.AST;

public class IfNode implements ASTNode {
    private final ASTNode condition;
    private final BlockNode body;
    private final BlockNode elseBody;

    public IfNode(ASTNode condition, BlockNode body, BlockNode elseBody) {
        this.condition = condition;
        this.body = body;
        this.elseBody = elseBody;
    }

    public IfNode(ASTNode condition, BlockNode body) {
        this(condition, body, null);
    }
    public ASTNode getCondition(){
        return condition;
    }
    public BlockNode getBody(){
        return body;
    }
    public BlockNode getElseBody(){
        return elseBody;
    }

    @Override
    public String print(String indent) {
        StringBuilder sb = new StringBuilder();

        sb.append(indent).append("IfStatement\n");

        sb.append(indent).append("  Condition\n");
        sb.append(condition.print(indent + "    ")).append("\n");

        sb.append(indent).append("  Then\n");
        sb.append(body.print(indent + "    "));

        // Print the else block only if it exists
        if (elseBody != null) {
            sb.append("\n").append(indent).append("  Else\n");
            sb.append(elseBody.print(indent + "    "));
        }

        return sb.toString();
    }
}