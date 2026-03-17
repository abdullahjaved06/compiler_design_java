package compiler.Parser.AST;

import java.util.ArrayList;
import java.util.List;

public class BlockNode implements ASTNode {
    private final List<ASTNode> statements = new ArrayList<>();

    public void addStatement(ASTNode statement) {
        statements.add(statement);
    }

    @Override
    public String print(String indent) {
        StringBuilder sb = new StringBuilder();
        for (ASTNode statement : statements) {
            sb.append(statement.print(indent));
            if (statements.indexOf(statement) < statements.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}