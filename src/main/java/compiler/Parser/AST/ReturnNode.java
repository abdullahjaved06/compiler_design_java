package compiler.Parser.AST;

public class ReturnNode implements ASTNode {
    private final ASTNode expression;

    public ReturnNode(ASTNode expression) {
        this.expression = expression;
    }

    @Override
    public String print(String indent) {
        return indent + "ReturnStatement\n" + expression.print(indent + "  ");
    }
}