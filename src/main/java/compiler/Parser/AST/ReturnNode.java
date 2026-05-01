package compiler.Parser.AST;

public class ReturnNode implements ASTNode {
    private final ASTNode expression;

    public ReturnNode(ASTNode expression) {
        this.expression = expression;
    }

    public ASTNode getExpression() {
        return expression;
    }
    @Override
    public String print(String indent) {
        if (expression == null) {
            return indent + "ReturnStatement";
        }

        return indent + "ReturnStatement\n" + expression.print(indent + "  ");
    }

}