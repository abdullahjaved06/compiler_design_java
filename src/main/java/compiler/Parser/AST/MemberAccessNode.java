package compiler.Parser.AST;

public class MemberAccessNode implements ASTNode {
    private final ASTNode collection;
    private final String member;

    public MemberAccessNode(ASTNode collection, String member) {
        this.collection = collection;
        this.member = member;
    }

    public ASTNode getCollection() {
        return collection;
    }

    public String getMember() {
        return member;
    }

    @Override
    public String print(String indent) {
        StringBuilder sb = new StringBuilder();

        sb.append(indent).append("MemberAccess\n");

        sb.append(collection.print(indent + "  ")).append("\n");
        sb.append(indent).append("  member ").append(member);

        return sb.toString();
    }
}
