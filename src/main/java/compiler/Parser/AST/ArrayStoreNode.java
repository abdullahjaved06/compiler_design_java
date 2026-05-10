package compiler.Parser.AST;

/**
 * Represents writing to an array element: a[i] = expr;
 */
public class ArrayStoreNode implements ASTNode {
    private final ASTNode array;
    private final ASTNode index;
    private final ASTNode value;

    public ArrayStoreNode(ASTNode array, ASTNode index, ASTNode value) {
        this.array = array;
        this.index = index;
        this.value = value;
    }

    public ASTNode getArray() { return array; }
    public ASTNode getIndex() { return index; }
    public ASTNode getValue() { return value; }

    @Override
    public String print(String indent) {
        return indent + "ArrayStore\n"
                + array.print(indent + "  ") + "\n"
                + indent + "  Index\n"
                + index.print(indent + "    ") + "\n"
                + indent + "  Value\n"
                + value.print(indent + "    ");
    }
}
