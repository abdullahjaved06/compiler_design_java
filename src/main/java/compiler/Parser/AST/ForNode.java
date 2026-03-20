package compiler.Parser.AST;

public class ForNode implements ASTNode {
    private final ASTNode init;
    private final ASTNode rangeStart;
    private final ASTNode rangeEnd;
    private final ASTNode update;
    private final BlockNode body;

    public ForNode(ASTNode init, ASTNode rangeStart, ASTNode rangeEnd, ASTNode update, BlockNode body) {
        this.init = init;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.update = update;
        this.body = body;
    }

    @Override
    public String print(String indent) {
        StringBuilder sb = new StringBuilder();

        sb.append(indent).append("ForLoop\n");
        sb.append(indent).append("  Iterator\n").append(init.print(indent + "    "));
        sb.append(indent).append("  RangeStart\n").append(rangeStart.print(indent + "    ")).append("\n");
        sb.append(indent).append("  RangeEnd\n").append(rangeEnd.print(indent + "    ")).append("\n");
        sb.append(indent).append("  UpdatedValue\n");
        sb.append(update.print(indent + "    ")).append("\n");
        sb.append(indent).append("  LoopBody\n");
        sb.append(body.print(indent + "    "));

        return sb.toString();
    }
}