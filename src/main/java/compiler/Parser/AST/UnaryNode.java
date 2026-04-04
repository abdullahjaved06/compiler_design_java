package compiler.Parser.AST;

public class UnaryNode implements ASTNode {
    private final String operator;
    private final ASTNode operand;

    public UnaryNode(String operator, ASTNode operand) {
        this.operator = operator;
        this.operand = operand;
    }
    public String getOperator() {
        return operator;
    }

    public ASTNode getOperand() {
        return operand;
    }
    @Override
    public String print(String indent) {
        return indent + "UnaryOperator, " + operator + "\n" +
                operand.print(indent + "  ");
    }
}