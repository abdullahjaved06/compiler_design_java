package compiler.Parser.AST;

public class FinalNode implements ASTNode {
    private final ASTNode assignment; // Can be any node

    public FinalNode(ASTNode assignment) {
        this.assignment = assignment;
    }

    public ASTNode getAssignment() {
        return assignment;
    }

    @Override
    public String print(String indent) {
        return indent + "FinalModifier\n" + assignment.print(indent + "  ");
    }
}
