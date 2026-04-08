package compiler.Semantic;

import org.checkerframework.common.returnsreceiver.qual.This;

import java.util.*;

/**
 * Manages variabnles scopes and types.
 */
public class SymbolTable {
    private static class Symbol {
        String type;
        boolean isFinal;

        Symbol(String type, boolean isFinal) {
            this.type = type;
            this.isFinal = isFinal;
        }
    }
    // usign stack to handle scopes local, global...
    //we are using stack becasue scopes are someitme nested, like a gloabl scope inside a local one.
    private final Stack<Map<String, Symbol>> scopes = new Stack<>();

    public SymbolTable() {
        // Push the initial global scope
        scopes.push(new HashMap<>());
    }
    // use when it will enter the scope, like " if { "
    public void enterScope() {
        scopes.push(new HashMap<>());
    }
    // delete, pop the table/notebook when we go out of scooe.
    public void exitScope() {
        if (scopes.size() > 1) {
            scopes.pop();
        } else {
            throw new RuntimeException("Internal Error: Cannot exit global scope.");
        }
    }
    // adds a new variable.
    public void declare(String name, String type, boolean isFinal) {
        if (scopes.peek().containsKey(name)) {
            System.err.print("ScopeError: Variable '" + name +
                    "' is already defined in this scope.");
            System.exit(2);
        }
        scopes.peek().put(name, new Symbol(type, isFinal));
    }
    public String lookupType(String name) {
        for (int i = scopes.size() -1; i>=0;i--) {
            if (scopes.get(i).containsKey(name)) {
                return scopes.get(i).get(name).type;
            }
        }
        System.err.println("ScopeError: Variable '" + name +
                "' is not defined in any accessible scope.");
        System.exit(2);
        return null;
    }

    public void markFinal(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                scopes.get(i).get(name).isFinal = true;
                return;
            }
        }
        throw new RuntimeException(
                "Internal Error: markFinal called on undeclared variable '" +
                name + "'.");
    }

    public boolean isFinal(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                return scopes.get(i).get(name).isFinal;
            }
        }
        return false;
    }
}
