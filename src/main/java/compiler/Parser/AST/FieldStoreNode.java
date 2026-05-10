package compiler.Parser.AST;

/**
 * Represents writing to a collection field: a.x = expr; or a[i].x = expr;
 */
public class FieldStoreNode implements ASTNode {
    private final ASTNode target;
    private final String  field;
    private final ASTNode value;

    public FieldStoreNode(ASTNode target, String field, ASTNode value) {
        this.target = target;
        this.field  = field;
        this.value  = value;
    }

    public ASTNode getTarget() { return target; }
    public String  getField()  { return field; }
    public ASTNode getValue()  { return value; }

    @Override
    public String print(String indent) {
        return indent + "FieldStore\n"
                + target.print(indent + "  ") + "\n"
                + indent + "  Field, " + field + "\n"
                + indent + "  Value\n"
                + value.print(indent + "    ");
    }
}
