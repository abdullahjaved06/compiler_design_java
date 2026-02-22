package compiler.Lexer;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

public class Lexer {
    private final Reader input;
    private int currentChar;
    private int line =1;
    private int column =0;
    private static final java.util.Map<String, TokenType> keywords = new java.util.HashMap<>();

    static {
        keywords.put("final", TokenType.FINAL);
        keywords.put("coll", TokenType.COLL);
        keywords.put("def", TokenType.DEF);
        keywords.put("for", TokenType.FOR);
        keywords.put("while", TokenType.WHILE);
        keywords.put("if", TokenType.IF);
        keywords.put("else", TokenType.ELSE);
        keywords.put("return", TokenType.RETURN);
        keywords.put("not", TokenType.NOT);
        keywords.put("ARRAY", TokenType.ARRAY_KEYWORD);
        keywords.put("INT", TokenType.INT_TYPE);
        keywords.put("FLOAT", TokenType.FLOAT_TYPE);
        keywords.put("BOOL", TokenType.BOOL_TYPE);
        keywords.put("STRING", TokenType.STRING_TYPE);
        keywords.put("true", TokenType.TRUE);
        keywords.put("false", TokenType.FALSE);
    }
    public Lexer(Reader input) {
        this.input = input;
        advance();
    }
    private void advance() {
        try {
            currentChar = input.read();
            if (currentChar == '\n') {
                line++;
                column = 0;
            } else if (currentChar != -1) {
                column++;
            }
        } catch (IOException e) {
            currentChar = -1;
        }
    }
    // return the current character as char
    private char peek() {
        return (char) currentChar;
    }

    // if we reach the end of file.
    private boolean isAtEnd(){
        return currentChar ==-1;
    }
    // Skip whitespace and comments
    private void skipWhitespaceAndComments() {
        while (!isAtEnd()) {
            char c = peek();

            // Skip whitespace (space, tab, newline, carriage return)
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                advance();
            }
            // Skip comments: # until end of line
            else if (c == '#') {
                while (!isAtEnd() && peek() != '\n') {
                    advance();
                }
            }
            // Not whitespace or comment - stop skipping
            else {
                break;
            }
        }
    }

    // Read an identifier or keyword
    private Symbol readIdentifierOrKeyword(int startLine, int startColumn) {
        StringBuilder sb = new StringBuilder();

        // Keep reading while we see letters, digits, or underscore
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            sb.append(peek());
            advance();
        }

        String text = sb.toString();

        // Check if it's a keyword
        TokenType type = keywords.get(text);
        if (type != null) {
            return new Symbol(type, text, startLine, startColumn);
        }

        // Check if it starts with uppercase (collection name)
        if (Character.isUpperCase(text.charAt(0))) {
            return new Symbol(TokenType.COLLECTION_NAME, text, startLine, startColumn);
        }

        // Regular identifier
        return new Symbol(TokenType.IDENTIFIER, text, startLine, startColumn);
    }
    //REad a numbre (integer or float)
     private Symbol readNumber(int startLine, int startColumn) {
        StringBuilder sb = new StringBuilder();
        
        // Handle leading zeros: 00342 -> 342
        while (!isAtEnd() && peek() == '0') {
            advance();
            // Check what comes next
            if (isAtEnd() || (!Character.isDigit(peek()) && peek() != '.')) {
                // It's just "0" or "0" followed by non-digit
                return new Symbol(TokenType.INTEGER_LITERAL, "0", startLine, startColumn);
            }
            if (peek() == '.') {
                sb.append('0');
                break;
            }
            // Otherwise skip the leading zero and continue
        }
        
        // Read integer part
        while (!isAtEnd() && Character.isDigit(peek())) {
            sb.append(peek());
            advance();
        }
        
        // Check for decimal point (float)
        if (!isAtEnd() && peek() == '.') {
            sb.append('.');
            advance();
            
            // Read fractional part
            while (!isAtEnd() && Character.isDigit(peek())) {
                sb.append(peek());
                advance();
            }
            
            return new Symbol(TokenType.FLOAT_LITERAL, sb.toString(), startLine, startColumn);
        }
        
        // It's an integer
        String value = sb.toString();
        if (value.isEmpty()) {
            value = "0";
        }
        return new Symbol(TokenType.INTEGER_LITERAL, value, startLine, startColumn);
    }
    // Check if next character is a digit (for .234 style floats)
    private boolean isDigitAhead() {
        try {
            input.mark(1);
            int next = input.read();
            input.reset();
            return next != -1 && Character.isDigit((char) next);
        } catch (IOException e) {
            return false;
        }
    }

    // Read a float that starts with dot: .234 -> 0.234
    private Symbol readDotFloat(int startLine, int startColumn) {
        StringBuilder sb = new StringBuilder("0");
        sb.append(peek());  // Add the dot
        advance();

        // Read the fractional part
        while (!isAtEnd() && Character.isDigit(peek())) {
            sb.append(peek());
            advance();
        }

        return new Symbol(TokenType.FLOAT_LITERAL, sb.toString(), startLine, startColumn);
    }

    // Read operatorrs and puncteuation
    private Symbol readOperator(int startLine, int startColumn) {
        char c = peek();
        advance();

        switch (c) {
            // Single character operators
            case '+': return new Symbol(TokenType.PLUS, "+", startLine, startColumn);
            case '*': return new Symbol(TokenType.STAR, "*", startLine, startColumn);
            case '/': return new Symbol(TokenType.SLASH, "/", startLine, startColumn);
            case '%': return new Symbol(TokenType.PERCENT, "%", startLine, startColumn);
            case '(': return new Symbol(TokenType.LPAREN, "(", startLine, startColumn);
            case ')': return new Symbol(TokenType.RPAREN, ")", startLine, startColumn);
            case '{': return new Symbol(TokenType.LBRACE, "{", startLine, startColumn);
            case '}': return new Symbol(TokenType.RBRACE, "}", startLine, startColumn);
            case '[': return new Symbol(TokenType.LBRACKET, "[", startLine, startColumn);
            case ']': return new Symbol(TokenType.RBRACKET, "]", startLine, startColumn);
            case '.': return new Symbol(TokenType.DOT, ".", startLine, startColumn);
            case ';': return new Symbol(TokenType.SEMICOLON, ";", startLine, startColumn);
            case ',': return new Symbol(TokenType.COMMA, ",", startLine, startColumn);

            // Minus or Arrow (-)
            case '-':
                if (!isAtEnd() && peek() == '>') {
                    advance();
                    return new Symbol(TokenType.ARROW, "->", startLine, startColumn);
                }
                return new Symbol(TokenType.MINUS, "-", startLine, startColumn);

            // Equals, Equal-Equal, or Not-Equal (=, ==, =/=)
            case '=':
                if (!isAtEnd() && peek() == '=') {
                    advance();
                    return new Symbol(TokenType.EQUAL, "==", startLine, startColumn);
                }
                if (!isAtEnd() && peek() == '/') {
                    advance();
                    if (!isAtEnd() && peek() == '=') {
                        advance();
                        return new Symbol(TokenType.NOT_EQUAL, "=/=", startLine, startColumn);
                    }
                    return new Symbol(TokenType.ERROR, "=/", startLine, startColumn);
                }
                return new Symbol(TokenType.ASSIGN, "=", startLine, startColumn);

            // Less or Less-Equal (<, <=)
            case '<':
                if (!isAtEnd() && peek() == '=') {
                    advance();
                    return new Symbol(TokenType.LESS_EQUAL, "<=", startLine, startColumn);
                }
                return new Symbol(TokenType.LESS, "<", startLine, startColumn);

            // Greater or Greater-Equal (>, >=)
            case '>':
                if (!isAtEnd() && peek() == '=') {
                    advance();
                    return new Symbol(TokenType.GREATER_EQUAL, ">=", startLine, startColumn);
                }
                return new Symbol(TokenType.GREATER, ">", startLine, startColumn);

            // And (&&)
            case '&':
                if (!isAtEnd() && peek() == '&') {
                    advance();
                    return new Symbol(TokenType.AND, "&&", startLine, startColumn);
                }
                return new Symbol(TokenType.ERROR, "&", startLine, startColumn);

            // Or (||)
            case '|':
                if (!isAtEnd() && peek() == '|') {
                    advance();
                    return new Symbol(TokenType.OR, "||", startLine, startColumn);
                }
                return new Symbol(TokenType.ERROR, "|", startLine, startColumn);

            default:
                return new Symbol(TokenType.ERROR, String.valueOf(c), startLine, startColumn);
        }
    }
    // Read a string liteiral
    private Symbol readString(int startLine, int startColumn) {
        StringBuilder sb = new StringBuilder();
        advance();  // Skip opening "

        while (!isAtEnd() && peek() != '"') {
            if (peek() == '\\') {
                // Handle escape sequences
                advance();  // Skip backslaesh
                if (isAtEnd()) {
                    return new Symbol(TokenType.ERROR, "Unterminated string", startLine, startColumn);
                }
                switch (peek()) {
                    case 'n':  sb.append('\n'); break;
                    case '\\': sb.append('\\'); break;
                    case '"':  sb.append('"');  break;
                    default:
                        return new Symbol(TokenType.ERROR, "Invalid escape: \\" + peek(), startLine, startColumn);
                }
            } else {
                sb.append(peek());
            }
            advance();
        }

        if (isAtEnd()) {
            return new Symbol(TokenType.ERROR, "Unterminated string", startLine, startColumn);
        }

        advance();  // Skip closing "
        return new Symbol(TokenType.STRING_LITERAL, sb.toString(), startLine, startColumn);
    }
    // Main method - returns the next token
    public Symbol getNextSymbol() {
        skipWhitespaceAndComments();

        if (isAtEnd()) {
            return new Symbol(TokenType.EOF, "", line, column);
        }

        int startLine = line;
        int startColumn = column;
        char c = peek();

        // Identifiers and keywords start with letter or underscore
        if (Character.isLetter(c) || c == '_') {
            return readIdentifierOrKeyword(startLine, startColumn);
        }

        // Numbers start with digit
        if (Character.isDigit(c)) {
            return readNumber(startLine, startColumn);
        }

        // Handle .234 style floats (dot followed by digit)
        if (c == '.' && isDigitAhead()) {
            return readDotFloat(startLine, startColumn);
        }

        // Strings start with "
        if (c == '"') {
            return readString(startLine, startColumn);
        }

        // Everything else is an operator or punctuation
        return readOperator(startLine, startColumn);
    }
}
