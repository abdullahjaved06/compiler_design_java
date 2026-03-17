package compiler.Parser.AST;

/**
 * Represents arithmetic operations like +, -, *, / in the AST.
 */
public class BinaryExpressionNode implements ASTNode {
    private final String operator;
    private final ASTNode left;
    private final ASTNode right;

    public BinaryExpressionNode(String operator, ASTNode left, ASTNode right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    @Override
    public String print(String indent) {
        StringBuilder sb = new StringBuilder();

        // Print the left operand
        sb.append(left.print(indent));

        // Print the operator line
        sb.append("\n").append(indent).append("ArithmeticOperator, ").append(operator).append("\n");

        // Print the right operand
        sb.append(right.print(indent));

        return sb.toString();
    }
}