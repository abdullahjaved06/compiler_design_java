package compiler.Parser.AST;

public class LiteralNode implements ASTNode {
    private final String value;
    private final DataType type;

    public LiteralNode(String value, DataType type) {
        this.value = value;
        this.type = type;
    }

    @Override
    public String print(String indent) {
        // Mapping the Enum back to the specific string format required for the project tree
        String label = switch (type) {
            case INT -> "Integer";
            case FLOAT -> "Float";
            case STRING -> "String";
            case BOOL -> "Bool";
            default -> type.name();
        };
        return indent + label + ", " + value;
    }
}