package compiler.Parser.AST;

/**
 * Represents a variable identifier (like 'x' or 'myVar') in the AST.
 */
public class IdentifierNode implements ASTNode {
    private final String name;

    public IdentifierNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String print(String indent) {
        // Matches the format: "Identifier, x"
        return indent + "Identifier, " + name;
    }
}