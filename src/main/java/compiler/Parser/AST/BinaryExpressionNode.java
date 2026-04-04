package compiler.Parser.AST;

/**
 * Represents arithmetic operations like +, -, *, / in the AST.
 */
public class BinaryExpressionNode implements ASTNode {
    private final String operator;
    private final ASTNode left;
    private final ASTNode right;
    private final String type;

    public BinaryExpressionNode(String operator, ASTNode left, ASTNode right, String type) {
        this.operator = operator;
        this.left = left;
        this.right = right;
        this.type = type;
    }
    public String getOperator(){
        return operator;
    }
    public ASTNode getLeft(){
        return left;
    }
    public ASTNode getRight(){
        return right;
    }
    public String getType(){
        return type;
    }

    @Override
    public String print(String indent) {
        StringBuilder sb = new StringBuilder();

        // Print the left operand
        sb.append(left.print(indent)).append("\n");

        // Print the operator line
        switch (type){
            case "Logical":
                sb.append(indent).append("LogicalOperator, ").append(operator).append("\n");
                break;
            case "Relational":
                sb.append(indent).append("RelationalOperator, ").append(operator).append("\n");
                break;
            default:
                sb.append(indent).append("ArithmeticOperator, ").append(operator).append("\n");
                break;
        }

        // Print the right operand
        sb.append(right.print(indent));

        return sb.toString();
    }
}