package compiler.Parser.AST;

import java.util.ArrayList;
import java.util.List;

public class BlockNode implements ASTNode {
    private final List<ASTNode> statements = new ArrayList<>();

    public void addStatement(ASTNode statement) {
        statements.add(statement);
    }
    public List<ASTNode> getStatements() {
        return statements;
    }

    @Override
    public String print(String indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < statements.size(); i++) {
            sb.append(statements.get(i).print(indent));
            if (i < statements.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
