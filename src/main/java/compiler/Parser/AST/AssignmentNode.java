package compiler.Parser.AST;

/**
 * Represents an assignment expression in the AST.
 * Structure: Type Identifier = Expression;
 */
public class AssignmentNode implements ASTNode {
    private final String type;
    private final String identifier;
    private final ASTNode expression;

    public AssignmentNode(String type, String identifier, ASTNode expression) {
        this.type = type;
        this.identifier = identifier;
        this.expression = expression;
    }

    public AssignmentNode(String type, String identifier) {
        this.type = type;
        this.identifier = identifier;
        this.expression = null;
    }

    @Override
    public String print(String indent) {
        StringBuilder sb = new StringBuilder();

        // Root of the assignment
        sb.append(indent).append("Expr\n");

        // The type (e.g., INT)
        if (type != null) {
            sb.append(indent).append("  Type, ").append(type).append("\n");
        }

        // The variable name (e.g., x)
        sb.append(indent).append("  Identifier, ").append(identifier).append("\n");

        if (expression != null) {
            // The assignment operator label
            sb.append(indent).append("  AssignmentOperator\n");

            // The right-hand side value/expression (e.g., 1 + 2)
            // We pass an increased indent to the child expression
            sb.append(expression.print(indent + "  "));
        }

        return sb.toString();
    }
}