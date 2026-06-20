package CC;

import java.util.*;
import java.io.*;
import java.nio.file.*;

// ============================================================
//  GREEK-KEYWORD COMPILER  (Ellinikos / Ελληνικός)
//  CCP – Compiler Construction | Iqra University
//  Components: Tokenizer → Lexical Analyser → Syntax Analyser
//              → Semantic Analyser → Translator & Executor
// ============================================================

public class GreekCompiler {

    // =========================================================
    // 1. TOKEN TYPES (Generalized like Hindi Compiler)
    // =========================================================
    enum TokenType {
        KEYWORD, IDENTIFIER, NUMBER, FLOAT, STRING, CHAR,
        OPERATOR, RELATIONAL, ASSIGN,
        LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET,
        SEMICOLON, COMMA, COLON, DOT, EOF, UNKNOWN
    }

    // =========================================================
    // 2. KEYWORD LIST
    // =========================================================
    static final List<String> KEYWORD_LIST = new ArrayList<>(Arrays.asList(
        "τάξη", "taxh", "με", "me", "δημόσιο", "demosia", "ιδιωτικό", "idiotiko",
        "προστατευμένο", "prostateymeno", "στατικό", "statiko", "τελικό", "teliko",
        "αφηρημένο", "aferemeno", "διεπαφή", "diepafe", "υλοποιεί", "ylopoiei",
        "πακέτο", "paketo", "εισαγωγή", "eisagoge", "νέο", "neo", "επιστρέφει", "epistrefei",
        "άκυρο", "akyro", "ακέραιος", "akeraios", "πραγματικός", "pragmatikos",
        "χαρακτήρας", "xarakteras", "λογικός", "logikos", "μακρύς", "makrys",
        "μικρός", "mikros", "βάρος", "varos", "μπάιτ", "bait", "αν", "an",
        "αλλιώς", "allios", "όσο", "oso", "για", "gia", "κάνε", "kane",
        "διακοπή", "diakope", "συνέχεια", "synexeia", "επιλογή", "epiloge",
        "περίπτωση", "periptose", "προεπιλογή", "proepiloge", "εκτύπωσε", "ektypose",
        "διάβασε", "diavase", "δοκίμασε", "dokimase", "πιάσει", "piasei",
        "τελικά", "telika", "ρίπτει", "riptei", "ρίπτειΠολλά", "ripteiPolla",
        "συγχρονίζεται", "syngronizetai", "πτητικό", "ptitiko", "μεταβατό", "metavato",
        "αυστηρό", "afstiro", "απαρίθμηση", "aparithmese", "ισχυρίζομαι", "isxyrizomai",
        "πάρω", "paro", "αυτό", "ayto", "εξωτερικό", "exoteriko",
        "παράδειγμαΤου", "paradeigmaTou", "αλήθεια", "aletheia", "ψεύδος", "psevdos", "μηδέν", "meden"
    ));

    // =========================================================
    // 3. TOKEN CLASS
    // =========================================================
    static class Token {
        final TokenType type;
        final String    value;
        final int       line;
        final int       col;

        Token(TokenType type, String value, int line, int col) {
            this.type  = type;
            this.value = value;
            this.line  = line;
            this.col   = col;
        }
        @Override public String toString() {
            return String.format("[%-12s | %-25s | L%-3d C%d]", type.name(), "\"" + value + "\"", line, col);
        }
    }

    // =========================================================
    // 4. LEXICAL ANALYSER (State Machine Architecture)
    // =========================================================
    enum State {
        START, IDENTIFIER, NUMBER, STRING, CHAR, 
        OPERATOR, COMMENT_SINGLE, COMMENT_MULTI, DONE
    }

    static class Lexer {
        private final String src;
        private int pos = 0;
        private int line = 1;
        private int col = 1;
        final List<Token> tokens = new ArrayList<>();
        final List<String> errors = new ArrayList<>();

        private static final List<String> KEYWORDS_SORTED;
        static {
            KEYWORDS_SORTED = new ArrayList<>(KEYWORD_LIST);
            KEYWORDS_SORTED.sort((a, b) -> b.length() - a.length());
        }

        Lexer(String src) { this.src = src; }

        private char peek()     { return pos < src.length() ? src.charAt(pos) : '\0'; }
        private char peekNext() { return pos + 1 < src.length() ? src.charAt(pos + 1) : '\0'; }
        
        private char advance() { 
            char c = src.charAt(pos++);
            if (c == '\n') { line++; col = 1; } else { col++; } 
            return c;
        }

        private boolean isAlpha(char c) { return Character.isLetter(c) || c == '_'; }
        private boolean isAlNum(char c) { return isAlpha(c) || Character.isDigit(c); }

        void tokenize() {
            State state = State.START;
            StringBuilder sb = new StringBuilder();
            int startLine = 1, startCol = 1;

            while (pos < src.length()) {
                char ch = peek();

                switch (state) {
                    case START:
                        sb.setLength(0);
                        startLine = line;
                        startCol = col;

                        if (Character.isWhitespace(ch)) { advance(); continue; }

                        if (ch == '/' && peekNext() == '/') {
                            state = State.COMMENT_SINGLE; advance(); advance(); continue;
                        }

                        if (ch == '/' && peekNext() == '*') {
                            state = State.COMMENT_MULTI; advance(); advance(); continue;
                        }

                        if (Character.isDigit(ch)) {
                            state = State.NUMBER;
                        } else if (ch == '"') {
                            state = State.STRING; advance(); 
                        } else if (ch == '\'') {
                            state = State.CHAR; advance(); 
                        } else if (isAlpha(ch)) {
                            String matchedKw = tryMatchKeyword();
                            if (matchedKw != null) {
                                tokens.add(new Token(TokenType.KEYWORD, matchedKw, startLine, startCol));
                                for (int i = 0; i < matchedKw.length(); i++) advance();
                            } else {
                                state = State.IDENTIFIER;
                            }
                        } else {
                            state = State.OPERATOR;
                        }
                        break;

                    case IDENTIFIER:
                        if (isAlNum(ch)) { sb.append(advance()); } 
                        else {
                            tokens.add(new Token(TokenType.IDENTIFIER, sb.toString(), startLine, startCol));
                            state = State.START;
                        }
                        break;

                    case NUMBER:
                        if (Character.isDigit(ch) || ch == '.') { sb.append(advance()); } 
                        else {
                            String numStr = sb.toString();
                            TokenType type = numStr.contains(".") ? TokenType.FLOAT : TokenType.NUMBER;
                            tokens.add(new Token(type, numStr, startLine, startCol));
                            state = State.START;
                        }
                        break;

                    case STRING:
                        if (ch == '"') {
                            advance(); 
                            tokens.add(new Token(TokenType.STRING, sb.toString(), startLine, startCol));
                            state = State.START;
                        } else if (ch == '\n') {
                            lexError("Unterminated string literal", startLine, startCol);
                            state = State.START;
                        } else if (ch == '\\') {
                            advance(); sb.append(escape(advance()));
                        } else {
                            sb.append(advance());
                        }
                        break;

                    case CHAR:
                        if (ch == '\'') {
                            advance(); 
                            tokens.add(new Token(TokenType.CHAR, sb.toString(), startLine, startCol));
                            state = State.START;
                        } else if (ch == '\n') {
                            lexError("Unterminated char literal", startLine, startCol);
                            state = State.START;
                        } else if (ch == '\\') {
                            advance(); sb.append(escape(advance()));
                        } else {
                            sb.append(advance());
                        }
                        break;

                    case COMMENT_SINGLE:
                        if (ch == '\n') state = State.START;
                        advance();
                        break;

                    case COMMENT_MULTI:
                        if (ch == '*' && peekNext() == '/') {
                            advance(); advance(); state = State.START;
                        } else advance();
                        break;

                    case OPERATOR:
                        handleOperator(startLine, startCol);
                        state = State.START;
                        break;

                    case DONE:
                        break;
                }
            }
            tokens.add(new Token(TokenType.EOF, "EOF", line, col));
        }

        private String tryMatchKeyword() {
            for (String kw : KEYWORDS_SORTED) {
                if (src.startsWith(kw, pos)) {
                    int end = pos + kw.length();
                    if (end >= src.length() || !isAlNum(src.charAt(end))) return kw;
                }
            }
            return null;
        }

        private void handleOperator(int startLine, int startCol) {
            char c = advance();
            switch (c) {
                case '+': 
                    if (peek() == '+') { advance(); tokens.add(tok(TokenType.OPERATOR, "++", startLine, startCol)); } 
                    else if (peek() == '=') { advance(); tokens.add(tok(TokenType.ASSIGN, "+=", startLine, startCol)); } 
                    else tokens.add(tok(TokenType.OPERATOR, "+", startLine, startCol)); break;
                case '-': 
                    if (peek() == '-') { advance(); tokens.add(tok(TokenType.OPERATOR, "--", startLine, startCol)); } 
                    else if (peek() == '=') { advance(); tokens.add(tok(TokenType.ASSIGN, "-=", startLine, startCol)); } 
                    else tokens.add(tok(TokenType.OPERATOR, "-", startLine, startCol)); break;
                case '*': tokens.add(tok(TokenType.OPERATOR, "*", startLine, startCol)); break;
                case '/': tokens.add(tok(TokenType.OPERATOR, "/", startLine, startCol)); break;
                case '%': tokens.add(tok(TokenType.OPERATOR, "%", startLine, startCol)); break;
                case '=': 
                    if (peek() == '=') { advance(); tokens.add(tok(TokenType.RELATIONAL, "==", startLine, startCol)); } 
                    else tokens.add(tok(TokenType.ASSIGN, "=", startLine, startCol)); break;
                case '!': 
                    if (peek() == '=') { advance(); tokens.add(tok(TokenType.RELATIONAL, "!=", startLine, startCol)); } 
                    else tokens.add(tok(TokenType.OPERATOR, "!", startLine, startCol)); break;
                case '<': 
                    if (peek() == '=') { advance(); tokens.add(tok(TokenType.RELATIONAL, "<=", startLine, startCol)); } 
                    else tokens.add(tok(TokenType.RELATIONAL, "<", startLine, startCol)); break;
                case '>': 
                    if (peek() == '=') { advance(); tokens.add(tok(TokenType.RELATIONAL, ">=", startLine, startCol)); } 
                    else tokens.add(tok(TokenType.RELATIONAL, ">", startLine, startCol)); break;
                case '&': 
                    if (peek() == '&') { advance(); tokens.add(tok(TokenType.OPERATOR, "&&", startLine, startCol)); } 
                    else lexError("Unknown character '&'", startLine, startCol); break;
                case '|': 
                    if (peek() == '|') { advance(); tokens.add(tok(TokenType.OPERATOR, "||", startLine, startCol)); } 
                    else lexError("Unknown character '|'", startLine, startCol); break;
                case '.': tokens.add(tok(TokenType.DOT, ".", startLine, startCol)); break;
                case '(': tokens.add(tok(TokenType.LPAREN, "(", startLine, startCol)); break;
                case ')': tokens.add(tok(TokenType.RPAREN, ")", startLine, startCol)); break;
                case '{': tokens.add(tok(TokenType.LBRACE, "{", startLine, startCol)); break;
                case '}': tokens.add(tok(TokenType.RBRACE, "}", startLine, startCol)); break;
                case '[': tokens.add(tok(TokenType.LBRACKET, "[", startLine, startCol)); break;
                case ']': tokens.add(tok(TokenType.RBRACKET, "]", startLine, startCol)); break;
                case ';': tokens.add(tok(TokenType.SEMICOLON, ";", startLine, startCol)); break;
                case ':': tokens.add(tok(TokenType.COLON, ":", startLine, startCol)); break;
                case ',': tokens.add(tok(TokenType.COMMA, ",", startLine, startCol)); break;
                default: 
                    lexError("Unrecognized character '" + c + "'", startLine, startCol); 
                    tokens.add(tok(TokenType.UNKNOWN, String.valueOf(c), startLine, startCol));
            }
        }

        private Token tok(TokenType t, String v, int l, int c) { return new Token(t, v, l, c); }
        private char escape(char e) { return switch(e){ case 'n'->'\n'; case 't'->'\t'; case 'r'->'\r'; case '"'->'"'; case '\''->'\''; case '\\'->'\\'; default->e; }; }
        private void lexError(String msg, int l, int c) { errors.add(String.format("  [LEXICAL ERROR] Line %d, Col %d: %s", l, c, msg)); }
    }

    // =========================================================
    // 5. SYNTAX ANALYSER
    // =========================================================
    static class Parser {
        private static class ParseException extends RuntimeException {}
        
        private final List<Token> tokens;
        private int pos = 0;
        final List<String> errors = new ArrayList<>();
        final List<String> parseLog = new ArrayList<>();

        Parser(List<Token> tokens) { this.tokens = tokens; }

        private Token peek() { return tokens.get(pos); }
        private Token consume() { Token t = tokens.get(pos); if (t.type != TokenType.EOF) pos++; return t; }
        
        private boolean check(TokenType t) { return peek().type == t; }
        private boolean check(String val) { return peek().value.equals(val); }
        private boolean checkKw(String gr, String lat) { return check(gr) || check(lat); }

        private void expect(TokenType t, String msg) { 
            if (check(t)) { consume(); return; }
            syntaxError(msg + " (got '" + peek().value + "')"); 
        }
        private void expectKw(String gr, String lat, String msg) {
            if (checkKw(gr, lat)) { consume(); return; }
            syntaxError(msg + " (got '" + peek().value + "')");
        }
        
        private void syntaxError(String msg) { 
            Token t = peek();
            String errStr = String.format("  [SYNTAX ERROR] Line %d, Col %d: %s", t.line, t.col, msg);
            errors.add(errStr); 
            parseLog.add(errStr);
            throw new ParseException();
        }

        private void synchronize() {
            log("parse: [RECOVERY] Panic-Mode triggered. Searching for synchronization token...");
            while (!check(TokenType.EOF)) {
                if (pos > 0 && tokens.get(pos - 1).type == TokenType.SEMICOLON) return;
                String v = peek().value;
                if (v.equals("τάξη") || v.equals("taxh") || v.equals("δημόσιο") || v.equals("demosia") || 
                    v.equals("αν") || v.equals("an") || v.equals("όσο") || v.equals("oso") || 
                    v.equals("για") || v.equals("gia") || v.equals("επιστρέφει") || v.equals("epistrefei") || 
                    v.equals("δοκίμασε") || v.equals("dokimase") || v.equals("εκτύπωσε") || v.equals("ektypose")) {
                    log("parse: [RECOVERY] Synchronization successful. Resuming parse.");
                    return;
                }
                consume();
            }
        }

        private boolean isAccessMod() { return checkKw("δημόσιο", "demosia") || checkKw("ιδιωτικό", "idiotiko") || checkKw("προστατευμένο", "prostateymeno"); }
        private boolean isType() { 
            return checkKw("ακέραιος", "akeraios") || checkKw("πραγματικός", "pragmatikos") || 
                   checkKw("λογικός", "logikos") || checkKw("χαρακτήρας", "xarakteras") || 
                   checkKw("μακρύς", "makrys") || checkKw("μικρός", "mikros") || 
                   checkKw("βάρος", "varos") || checkKw("μπάιτ", "bait") || 
                   checkKw("άκυρο", "akyro") || check(TokenType.IDENTIFIER);
        }

        void parseProgram() { 
            log("parse: Program");
            while (!check(TokenType.EOF)) { 
                try {
                    if (!parseDeclaration()) { Token bad = consume(); syntaxError("Unexpected token '" + bad.value + "'"); } 
                } catch (ParseException e) { synchronize(); }
            } 
        }
        
        private boolean parseDeclaration() { 
            if (checkKw("πακέτο", "paketo") || checkKw("εισαγωγή", "eisagoge")) { 
                consume(); while (!check(TokenType.SEMICOLON) && !check(TokenType.EOF)) consume(); expect(TokenType.SEMICOLON, "Expected ';' after package/import"); return true;
            } 
            if (checkKw("τάξη", "taxh") || checkKw("διεπαφή", "diepafe") || isAccessMod() || checkKw("αφηρημένο", "aferemeno") || checkKw("στατικό", "statiko") || checkKw("τελικό", "teliko")) { 
                parseClassDecl(); return true; 
            } return false;
        }

        private void parseClassDecl() { 
            log("parse: ClassDeclaration");
            while (isAccessMod() || checkKw("αφηρημένο", "aferemeno") || checkKw("στατικό", "statiko") || checkKw("τελικό", "teliko")) consume(); 
            boolean isInterface = checkKw("διεπαφή", "diepafe"); 
            if (isInterface) consume(); else expectKw("τάξη", "taxh", "Expected 'τάξη/taxh' (class)"); 
            expect(TokenType.IDENTIFIER, "Expected class/interface name"); 
            if (checkKw("με", "me")) { consume(); expect(TokenType.IDENTIFIER, "Expected superclass name"); } 
            if (checkKw("υλοποιεί", "ylopoiei")) { consume(); expect(TokenType.IDENTIFIER, "Expected interface name"); while (check(TokenType.COMMA)) { consume(); expect(TokenType.IDENTIFIER, "Expected interface name"); } } 
            expect(TokenType.LBRACE, "Expected '{' to begin class body"); parseClassBody(); expect(TokenType.RBRACE, "Expected '}' to end class body");
        }
        
        private void parseClassBody() { 
            log("parse: ClassBody");
            while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
                try { parseMember(); } catch (ParseException e) { synchronize(); }
            } 
        }
        
        private void parseMember() { 
            while (isAccessMod() || checkKw("στατικό", "statiko") || checkKw("τελικό", "teliko") || checkKw("αφηρημένο", "aferemeno")) consume();
            if (!isType()) { Token bad = consume(); syntaxError("Expected type or member declaration, got '" + bad.value + "'"); return; } 
            consume(); 
            if (check(TokenType.LPAREN)) { 
                log("parse: ConstructorDeclaration"); parseParams(); if (check(TokenType.LBRACE)) parseBlock(); else expect(TokenType.SEMICOLON, "Expected ';' after abstract constructor"); return;
            } 
            expect(TokenType.IDENTIFIER, "Expected member name"); 
            if (check(TokenType.LPAREN)) { 
                log("parse: MethodDeclaration"); parseParams(); if (check(TokenType.LBRACE)) parseBlock(); else expect(TokenType.SEMICOLON, "Expected ';' after abstract method"); 
            } else { 
                log("parse: FieldDeclaration"); if (check(TokenType.ASSIGN)) { consume(); parseExpr(); } 
                while (check(TokenType.COMMA)) { consume(); expect(TokenType.IDENTIFIER, "Expected variable name"); if (check(TokenType.ASSIGN)) { consume(); parseExpr(); } } 
                expect(TokenType.SEMICOLON, "Expected ';' after field declaration"); 
            } 
        }

        private void parseParams() { expect(TokenType.LPAREN, "Expected '('"); if (!check(TokenType.RPAREN)) { parseParam(); while (check(TokenType.COMMA)) { consume(); parseParam(); } } expect(TokenType.RPAREN, "Expected ')'"); }
        private void parseParam() { if (!isType()) syntaxError("Expected parameter type"); else consume(); expect(TokenType.IDENTIFIER, "Expected parameter name"); }
        
        private void parseBlock() { 
            log("parse: Block");
            expect(TokenType.LBRACE, "Expected '{'"); 
            while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
                try { parseStatement(); } catch (ParseException e) { synchronize(); }
            } 
            if (!check(TokenType.EOF)) expect(TokenType.RBRACE, "Expected '}'");
        }
        
        private void parseStatement() { 
            Token t = peek();
            if (checkKw("αν", "an")) parseIf(); 
            else if (checkKw("όσο", "oso")) parseWhile(); 
            else if (checkKw("για", "gia")) parseFor(); 
            else if (checkKw("κάνε", "kane")) parseDoWhile();
            else if (checkKw("επιλογή", "epiloge")) parseSwitch(); 
            else if (checkKw("επιστρέφει", "epistrefei")) { consume(); if (!check(TokenType.SEMICOLON)) parseExpr(); expect(TokenType.SEMICOLON, "Expected ';'"); } 
            else if (checkKw("διακοπή", "diakope") || checkKw("συνέχεια", "synexeia")) { consume(); expect(TokenType.SEMICOLON, "Expected ';'"); } 
            else if (checkKw("εκτύπωσε", "ektypose")) parsePrint(); 
            else if (checkKw("διάβασε", "diavase")) parseRead();
            else if (checkKw("δοκίμασε", "dokimase")) parseTryCatch(); 
            else if (checkKw("ρίπτει", "riptei")) { consume(); parseExpr(); expect(TokenType.SEMICOLON, "Expected ';'"); } 
            else if (t.type == TokenType.LBRACE) parseBlock();
            else { 
                if (isType() && pos + 1 < tokens.size() && tokens.get(pos+1).type == TokenType.IDENTIFIER) { 
                    log("parse: LocalVarDecl"); consume(); expect(TokenType.IDENTIFIER, "Expected variable name"); 
                    if (check(TokenType.ASSIGN)) { consume(); parseExpr(); } 
                    while (check(TokenType.COMMA)) { consume(); expect(TokenType.IDENTIFIER, "Expected variable name"); if (check(TokenType.ASSIGN)) { consume(); parseExpr(); } } 
                    expect(TokenType.SEMICOLON, "Expected ';'"); 
                } else { 
                    parseExpr(); expect(TokenType.SEMICOLON, "Expected ';' after expression");
                } 
            } 
        }

        private void parseIf() { log("parse: IfStatement");
            expectKw("αν", "an", "Expected 'αν/an' (if)"); expect(TokenType.LPAREN, "Expected '('"); parseExpr(); expect(TokenType.RPAREN, "Expected ')'"); parseBlock(); 
            if (checkKw("αλλιώς", "allios")) { consume(); if (checkKw("αν", "an")) parseIf(); else parseBlock(); } 
        }
        private void parseWhile() { log("parse: WhileStatement");
            expectKw("όσο", "oso", "Expected 'όσο/oso' (while)"); expect(TokenType.LPAREN, "Expected '('"); parseExpr(); expect(TokenType.RPAREN, "Expected ')'"); parseBlock();
        }
        private void parseFor() { log("parse: ForStatement"); expectKw("για", "gia", "Expected 'για/gia' (for)");
            expect(TokenType.LPAREN, "Expected '('"); if (!check(TokenType.SEMICOLON)) { if (isType() && pos+1 < tokens.size() && tokens.get(pos+1).type == TokenType.IDENTIFIER) { consume(); expect(TokenType.IDENTIFIER, "Expected var"); if (check(TokenType.ASSIGN)) { consume(); parseExpr(); } } else parseExpr(); } expect(TokenType.SEMICOLON, "Expected ';'"); if (!check(TokenType.SEMICOLON)) parseExpr(); expect(TokenType.SEMICOLON, "Expected ';'"); if (!check(TokenType.RPAREN)) parseExpr(); expect(TokenType.RPAREN, "Expected ')'");
            parseBlock(); 
        }
        private void parseDoWhile() { log("parse: DoWhileStatement"); expectKw("κάνε", "kane", "Expected 'κάνε/kane' (do)"); parseBlock();
            expectKw("όσο", "oso", "Expected 'όσο/oso' (while) after do"); expect(TokenType.LPAREN, "Expected '('"); parseExpr(); expect(TokenType.RPAREN, "Expected ')'"); expect(TokenType.SEMICOLON, "Expected ';'");
        }
        private void parseSwitch() { log("parse: SwitchStatement"); expectKw("επιλογή", "epiloge", "Expected 'επιλογή/epiloge' (switch)");
            expect(TokenType.LPAREN, "Expected '('"); parseExpr(); expect(TokenType.RPAREN, "Expected ')'"); expect(TokenType.LBRACE, "Expected '{'"); while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) { if (checkKw("περίπτωση", "periptose")) { consume(); parseExpr(); expect(TokenType.COLON, "Expected ':'"); } else if (checkKw("προεπιλογή", "proepiloge")) { consume(); expect(TokenType.COLON, "Expected ':'"); } else { parseStatement(); } } expect(TokenType.RBRACE, "Expected '}'"); 
        }
        private void parsePrint() { log("parse: PrintStatement");
            expectKw("εκτύπωσε", "ektypose", "Expected 'εκτύπωσε/ektypose'"); expect(TokenType.LPAREN, "Expected '('"); parseExpr(); expect(TokenType.RPAREN, "Expected ')'"); expect(TokenType.SEMICOLON, "Expected ';'");
        }
        private void parseRead() { log("parse: ReadStatement"); expectKw("διάβασε", "diavase", "Expected 'διάβασε/diavase'");
            expect(TokenType.LPAREN, "Expected '('"); expect(TokenType.IDENTIFIER, "Expected variable name"); expect(TokenType.RPAREN, "Expected ')'"); expect(TokenType.SEMICOLON, "Expected ';'");
        }
        private void parseTryCatch() { log("parse: TryCatchStatement"); expectKw("δοκίμασε", "dokimase", "Expected 'δοκίμασε/dokimase' (try)"); parseBlock();
            if (checkKw("πιάσει", "piasei")) { consume(); expect(TokenType.LPAREN, "Expected '('"); if (isType()) consume(); expect(TokenType.IDENTIFIER, "Expected exception variable"); expect(TokenType.RPAREN, "Expected ')'"); parseBlock(); } 
            if (checkKw("τελικά", "telika")) { consume(); parseBlock(); } 
        }

        private void parseExpr()   { parseAssign(); }
        private void parseAssign() { parseOr(); if (check(TokenType.ASSIGN)) { consume(); parseAssign(); } }
        private void parseOr()     { parseAnd(); while (check("||"))  { consume(); parseAnd(); } }
        private void parseAnd()    { parseEq(); while (check("&&")) { consume(); parseEq();  } }
        private void parseEq()     { parseRel(); while (check("==") || check("!=")) { consume(); parseRel(); } }
        private void parseRel()    { parseAdd(); while (check("<") || check(">") || check("<=")|| check(">=")) { consume(); parseAdd(); } }
        private void parseAdd()    { parseMul(); while (check("+") || check("-")) { consume(); parseMul(); } }
        private void parseMul()    { parseUnary(); while (check("*") || check("/") || check("%")) { consume(); parseUnary(); } }
        private void parseUnary()  { if (check("!") || check("-") || check("++") || check("--")) { consume(); parseUnary(); } else parsePostfix(); }
        
        private void parsePostfix() { 
            parsePrimary();
            while (true) { 
                if (check(TokenType.DOT)) { consume(); expect(TokenType.IDENTIFIER, "Expected member name"); } 
                else if (check(TokenType.LPAREN)) { parseArgList(); } 
                else if (check(TokenType.LBRACKET)) { consume(); parseExpr(); expect(TokenType.RBRACKET, "Expected ']'"); } 
                else if (check("++") || check("--")) { consume(); } 
                else break; 
            } 
        }
        
        private void parsePrimary() { Token t = peek();
            if (t.type == TokenType.NUMBER || t.type == TokenType.FLOAT || t.type == TokenType.STRING || t.type == TokenType.CHAR || t.type == TokenType.IDENTIFIER || checkKw("αλήθεια", "aletheia") || checkKw("ψεύδος", "psevdos") || checkKw("μηδέν", "meden")) { consume(); }
            else if (checkKw("νέο", "neo")) { consume(); expect(TokenType.IDENTIFIER, "Expected class name after 'νέο/neo'"); parseArgList(); } 
            else if (checkKw("αυτό", "ayto") || checkKw("πάρω", "paro")) { consume(); }
            else if (check(TokenType.LPAREN)) { consume(); parseExpr(); expect(TokenType.RPAREN, "Expected ')'"); } 
            else syntaxError("Unexpected token in expression: '" + t.value + "'");
        }
        
        private void parseArgList() { expect(TokenType.LPAREN, "Expected '('"); if (!check(TokenType.RPAREN)) { parseExpr(); while (check(TokenType.COMMA)) { consume(); parseExpr(); } } expect(TokenType.RPAREN, "Expected ')'"); }
        private void log(String msg) { parseLog.add("  " + msg); }
    }

    // =========================================================
    // 6. SYMBOL TABLE + SEMANTIC ANALYSER
    // =========================================================
    static class SymbolTable {
        private final Deque<Map<String,String>> scopes = new ArrayDeque<>();
        final List<String> errors = new ArrayList<>();
        void enterScope() { scopes.push(new LinkedHashMap<>()); }
        void exitScope()  { if (!scopes.isEmpty()) scopes.pop(); }
        void declare(String name, String type, int line) { if (scopes.isEmpty()) return;
            if (scopes.peek().containsKey(name)) errors.add(String.format("  [SEMANTIC ERROR] Line %d: Variable '%s' already declared in this scope.", line, name)); else scopes.peek().put(name, type);
        }
        String lookup(String name) { for (Map<String,String> scope : scopes) if (scope.containsKey(name)) return scope.get(name); return null; }
        boolean isDeclared(String name) { return lookup(name) != null; }
    }

    static class SemanticAnalyser {
        private final List<Token> tokens;
        private int pos = 0;
        final SymbolTable symTable = new SymbolTable();
        final List<String> errors = new ArrayList<>();
        final List<String> semLog = new ArrayList<>();
        private final Set<String> classes = new HashSet<>();

        SemanticAnalyser(List<Token> tokens) { this.tokens = tokens; }
        private Token peek() { return tokens.get(pos); }
        private Token consume() { Token t = tokens.get(pos); if (t.type != TokenType.EOF) pos++; return t; }
        private boolean check(TokenType t) { return peek().type == t; }
        private boolean check(String val) { return peek().value.equals(val); }
        private boolean checkKw(String gr, String lat) { return check(gr) || check(lat); }

        private boolean isAccessMod() { return checkKw("δημόσιο", "demosia") || checkKw("ιδιωτικό", "idiotiko") || checkKw("προστατευμένο", "prostateymeno"); }
        private boolean isType() { 
            return checkKw("ακέραιος", "akeraios") || checkKw("πραγματικός", "pragmatikos") || 
                   checkKw("λογικός", "logikos") || checkKw("χαρακτήρας", "xarakteras") || 
                   checkKw("μακρύς", "makrys") || checkKw("μικρός", "mikros") || 
                   checkKw("βάρος", "varos") || checkKw("μπάιτ", "bait") || 
                   checkKw("άκυρο", "akyro") || check(TokenType.IDENTIFIER);
        }

        void analyse() { while (!check(TokenType.EOF)) { skipToClassOrEOF(); if (checkKw("τάξη", "taxh") || checkKw("διεπαφή", "diepafe")) analyseClass(); else if (!check(TokenType.EOF)) consume(); } }
        private void skipToClassOrEOF() { while (!check(TokenType.EOF) && !checkKw("τάξη", "taxh") && !checkKw("διεπαφή", "diepafe") && !isAccessMod() && !checkKw("αφηρημένο", "aferemeno")) { consume(); } while (isAccessMod()||checkKw("αφηρημένο", "aferemeno")||checkKw("στατικό", "statiko")||checkKw("τελικό", "teliko")) consume(); }
        
        private void analyseClass() { 
            consume(); if (!check(TokenType.IDENTIFIER)) return; String className = consume().value; classes.add(className); semLog.add("  [SEM] Analysing class: " + className);
            if (checkKw("με", "me")) { consume(); if (check(TokenType.IDENTIFIER)) { String parent = consume().value; if (!classes.contains(parent)) errors.add("  [SEMANTIC WARNING] Class '" + className + "' extends '" + parent + "' which is not yet defined."); } } 
            if (checkKw("υλοποιεί", "ylopoiei")) { consume(); while (check(TokenType.IDENTIFIER) || check(TokenType.COMMA)) consume(); } 
            while (!check(TokenType.LBRACE) && !check(TokenType.EOF)) consume(); if (!check(TokenType.LBRACE)) return;
            consume(); symTable.enterScope(); analyseClassBody(); symTable.exitScope(); if (check(TokenType.RBRACE)) consume(); 
        }

        private void analyseClassBody() { 
            while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) { 
                while (isAccessMod()||checkKw("στατικό", "statiko")||checkKw("τελικό", "teliko")||checkKw("αφηρημένο", "aferemeno")) consume();
                if (!isType()) { consume(); continue; } String typeName = consume().value; if (!check(TokenType.IDENTIFIER) && !check(TokenType.LPAREN)) continue; String memberName = peek().value;
                if (!check(TokenType.LPAREN)) memberName = consume().value; 
                if (check(TokenType.LPAREN)) { 
                    semLog.add("  [SEM] Method/Constructor: " + typeName + " " + memberName + "()");
                    symTable.enterScope(); analyseParams(); if (check(TokenType.LBRACE)) analyseBlock(typeName); else if (check(TokenType.SEMICOLON)) consume(); symTable.exitScope();
                } else { 
                    semLog.add("  [SEM] Field: " + typeName + " " + memberName); symTable.declare(memberName, typeName, peek().line);
                    if (check(TokenType.ASSIGN)) { consume(); analyseExprSem(); } 
                    while (check(TokenType.COMMA)) { consume(); if (check(TokenType.IDENTIFIER)) { String extraVar = consume().value; symTable.declare(extraVar, typeName, peek().line); if (check(TokenType.ASSIGN)) { consume(); analyseExprSem(); } } } 
                    if (check(TokenType.SEMICOLON)) consume();
                } 
            } 
        }

        private void analyseParams() { if (!check(TokenType.LPAREN)) return; consume(); while (!check(TokenType.RPAREN) && !check(TokenType.EOF)) { if (isType()) { String pt = consume().value; if (check(TokenType.IDENTIFIER)) { symTable.declare(consume().value, pt, peek().line); } } if (check(TokenType.COMMA)) consume(); } if (check(TokenType.RPAREN)) consume(); }
        private void analyseBlock(String returnType) { if (!check(TokenType.LBRACE)) return; consume(); symTable.enterScope(); while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) analyseStatementSem(returnType); symTable.exitScope(); if (check(TokenType.RBRACE)) consume(); }
        
        private void analyseStatementSem(String returnType) { 
            Token t = peek();
            if (isType() && pos+1 < tokens.size() && tokens.get(pos+1).type == TokenType.IDENTIFIER) { 
                String varType = consume().value; String varName = consume().value; symTable.declare(varName, varType, t.line); semLog.add("  [SEM] Declare var: " + varType + " " + varName); 
                if (check(TokenType.ASSIGN)) { consume(); analyseExprSem(); } 
                while (check(TokenType.COMMA)) { consume(); if (check(TokenType.IDENTIFIER)) { String extraVar = consume().value; symTable.declare(extraVar, varType, t.line); if (check(TokenType.ASSIGN)) { consume(); analyseExprSem(); } } } 
                if (check(TokenType.SEMICOLON)) consume(); return; 
            } 
            
            if (checkKw("επιστρέφει", "epistrefei")) { consume(); if (returnType.equals("akyro") || returnType.equals("άκυρο")) { if (!check(TokenType.SEMICOLON)) errors.add("  [SEMANTIC ERROR] Line "+t.line+": void method cannot return a value."); } if (!check(TokenType.SEMICOLON)) analyseExprSem(); if (check(TokenType.SEMICOLON)) consume(); } 
            else if (checkKw("αν", "an")) { consume(); if (check(TokenType.LPAREN)) consume(); analyseExprSem(); if (check(TokenType.RPAREN)) consume(); analyseBlock(returnType); if (checkKw("αλλιώς", "allios")) { consume(); if (checkKw("αν", "an")) analyseStatementSem(returnType); else analyseBlock(returnType); } } 
            else if (checkKw("όσο", "oso")) { consume(); if (check(TokenType.LPAREN)) consume(); analyseExprSem(); if (check(TokenType.RPAREN)) consume(); analyseBlock(returnType); } 
            else if (checkKw("για", "gia")) { consume(); symTable.enterScope(); if (check(TokenType.LPAREN)) consume(); if (isType() && pos+1 < tokens.size() && tokens.get(pos+1).type == TokenType.IDENTIFIER) { String vt = consume().value; String vn = consume().value; symTable.declare(vn, vt, t.line); if (check(TokenType.ASSIGN)) { consume(); analyseExprSem(); } } else { analyseExprSem(); } if (check(TokenType.SEMICOLON)) consume(); analyseExprSem(); if (check(TokenType.SEMICOLON)) consume(); analyseExprSem(); if (check(TokenType.RPAREN)) consume(); analyseBlock(returnType); symTable.exitScope(); } 
            else if (checkKw("κάνε", "kane")) { consume(); analyseBlock(returnType); if (checkKw("όσο", "oso")) { consume(); if (check(TokenType.LPAREN)) consume(); analyseExprSem(); if (check(TokenType.RPAREN)) consume(); if (check(TokenType.SEMICOLON)) consume(); } } 
            else if (checkKw("επιλογή", "epiloge")) { consume(); if (check(TokenType.LPAREN)) consume(); analyseExprSem(); if (check(TokenType.RPAREN)) consume(); if (check(TokenType.LBRACE)) consume(); while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) { if (checkKw("περίπτωση", "periptose")) { consume(); analyseExprSem(); if (check(TokenType.COLON)) consume(); } else if (checkKw("προεπιλογή", "proepiloge")) { consume(); if (check(TokenType.COLON)) consume(); } else { analyseStatementSem(returnType); } } if (check(TokenType.RBRACE)) consume(); } 
            else if (checkKw("δοκίμασε", "dokimase")) { consume(); analyseBlock(returnType); if (checkKw("πιάσει", "piasei")) { consume(); if (check(TokenType.LPAREN)) consume(); if (isType()) consume(); if (check(TokenType.IDENTIFIER)) symTable.declare(consume().value, "Exception", t.line); if (check(TokenType.RPAREN)) consume(); analyseBlock(returnType); } if (checkKw("τελικά", "telika")) { consume(); analyseBlock(returnType); } } 
            else if (checkKw("εκτύπωσε", "ektypose")) { consume(); if (check(TokenType.LPAREN)) consume(); analyseExprSem(); if (check(TokenType.RPAREN)) consume(); if (check(TokenType.SEMICOLON)) consume(); } 
            else if (checkKw("διάβασε", "diavase")) { consume(); if (check(TokenType.LPAREN)) consume(); if (check(TokenType.IDENTIFIER)) { String vn = consume().value; if (!symTable.isDeclared(vn)) errors.add("  [SEMANTIC ERROR] Line "+t.line+": Variable '"+vn+"' used before declaration."); } if (check(TokenType.RPAREN)) consume(); if (check(TokenType.SEMICOLON)) consume(); } 
            else if (t.type == TokenType.LBRACE) analyseBlock(returnType); 
            else if (checkKw("διακοπή", "diakope") || checkKw("συνέχεια", "synexeia")) { consume(); if (check(TokenType.SEMICOLON)) consume(); } 
            else if (checkKw("ρίπτει", "riptei")) { consume(); analyseExprSem(); if (check(TokenType.SEMICOLON)) consume(); } 
            else { int startPos = pos; analyseExprSem(); if (check(TokenType.SEMICOLON)) consume(); else if (startPos == pos && !check(TokenType.EOF)) consume(); } 
        }

        private void analyseExprSem() { 
            String prevValue = null; int parenDepth = 0; int braceDepth = 0; 
            while (!check(TokenType.EOF)) { 
                Token t = peek();
                if (parenDepth == 0 && braceDepth == 0) { if (t.type == TokenType.SEMICOLON || t.type == TokenType.RPAREN || t.type == TokenType.RBRACE || t.type == TokenType.COMMA || t.type == TokenType.COLON) break; } 
                if (t.type == TokenType.LPAREN) parenDepth++; else if (t.type == TokenType.RPAREN) parenDepth--; else if (t.type == TokenType.LBRACE) braceDepth++; else if (t.type == TokenType.RBRACE) braceDepth--; 
                if (t.type == TokenType.IDENTIFIER) { 
                    boolean isMethodCall = (pos + 1 < tokens.size() && tokens.get(pos+1).type == TokenType.LPAREN); boolean isMemberAccess = (".".equals(prevValue)); 
                    if (!isMethodCall && !isMemberAccess && !symTable.isDeclared(t.value) && !classes.contains(t.value) && Character.isLowerCase(t.value.charAt(0))) { 
                        errors.add("  [SEMANTIC ERROR] Line "+t.line+": Variable '"+t.value+"' might be undeclared."); 
                    } 
                } 
                prevValue = t.value; consume(); 
            } 
        }
    }

    // =========================================================
    // 7. TRANSLATOR (GREEK TO JAVA)
    // =========================================================
    static class Translator {
        private final List<Token> tokens;
        public String mainClass = null;

        Translator(List<Token> tokens) { this.tokens = tokens; }

        String translate() {
            StringBuilder sb = new StringBuilder();
            String currentClass = "Main";
            
            for (int i = 0; i < tokens.size(); i++) {
                Token t = tokens.get(i);
                if (t.type == TokenType.EOF) break;

                if ((t.value.equals("τάξη") || t.value.equals("taxh")) && i + 1 < tokens.size() && tokens.get(i+1).type == TokenType.IDENTIFIER) {
                    currentClass = tokens.get(i+1).value;
                }
                
                if ((t.value.equals("δημόσιο") || t.value.equals("demosia")) && i + 1 < tokens.size() && (tokens.get(i+1).value.equals("τάξη") || tokens.get(i+1).value.equals("taxh"))) {
                    continue;
                }

                if (t.type == TokenType.IDENTIFIER && t.value.equals("arxi")) {
                    mainClass = currentClass;
                    if (i + 2 < tokens.size() && tokens.get(i+1).type == TokenType.LPAREN && tokens.get(i+2).type == TokenType.RPAREN) {
                        sb.append("main(String[] args) ");
                        i += 2;
                        continue;
                    }
                }

                if (t.value.equals("διάβασε") || t.value.equals("diavase")) {
                    if (i + 3 < tokens.size() && tokens.get(i+1).type == TokenType.LPAREN && tokens.get(i+2).type == TokenType.IDENTIFIER && tokens.get(i+3).type == TokenType.RPAREN) {
                        String varName = tokens.get(i+2).value;
                        sb.append(varName).append(" = new java.util.Scanner(System.in, \"UTF-8\").nextInt() ");
                        i += 3; 
                        continue;
                    }
                }

                sb.append(toJava(t)).append(" ");
            }
            return sb.toString();
        }

        private String toJava(Token t) {
            if (t.type == TokenType.KEYWORD) {
                switch(t.value) {
                    case "τάξη": case "taxh": return "class";
                    case "με": case "me": return "extends";
                    case "δημόσιο": case "demosia": return "public";
                    case "ιδιωτικό": case "idiotiko": return "private";
                    case "προστατευμένο": case "prostateymeno": return "protected";
                    case "στατικό": case "statiko": return "static";
                    case "τελικό": case "teliko": return "final";
                    case "αφηρημένο": case "aferemeno": return "abstract";
                    case "διεπαφή": case "diepafe": return "interface";
                    case "υλοποιεί": case "ylopoiei": return "implements";
                    case "πακέτο": case "paketo": return "package";
                    case "εισαγωγή": case "eisagoge": return "import";
                    case "νέο": case "neo": return "new";
                    case "επιστρέφει": case "epistrefei": return "return";
                    case "άκυρο": case "akyro": return "void";
                    case "ακέραιος": case "akeraios": return "int";
                    case "πραγματικός": case "pragmatikos": return "double";
                    case "χαρακτήρας": case "xarakteras": return "char";
                    case "λογικός": case "logikos": return "boolean";
                    case "μακρύς": case "makrys": return "long";
                    case "μικρός": case "mikros": return "short";
                    case "βάρος": case "varos": return "float";
                    case "μπάιτ": case "bait": return "byte";
                    case "αν": case "an": return "if";
                    case "αλλιώς": case "allios": return "else";
                    case "όσο": case "oso": return "while";
                    case "για": case "gia": return "for";
                    case "κάνε": case "kane": return "do";
                    case "διακοπή": case "diakope": return "break";
                    case "συνέχεια": case "synexeia": return "continue";
                    case "επιλογή": case "epiloge": return "switch";
                    case "περίπτωση": case "periptose": return "case";
                    case "προεπιλογή": case "proepiloge": return "default";
                    case "εκτύπωσε": case "ektypose": return "new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8).println";
                    case "δοκίμασε": case "dokimase": return "try";
                    case "πιάσει": case "piasei": return "catch";
                    case "τελικά": case "telika": return "finally";
                    case "ρίπτει": case "riptei": return "throw";
                    case "ρίπτειΠολλά": case "ripteiPolla": return "throws";
                    case "συγχρονίζεται": case "syngronizetai": return "synchronized";
                    case "πτητικό": case "ptitiko": return "volatile";
                    case "μεταβατό": case "metavato": return "transient";
                    case "αυστηρό": case "afstiro": return "strictfp";
                    case "απαρίθμηση": case "aparithmese": return "enum";
                    case "ισχυρίζομαι": case "isxyrizomai": return "assert";
                    case "πάρω": case "paro": return "super";
                    case "αυτό": case "ayto": return "this";
                    case "εξωτερικό": case "exoteriko": return "native";
                    case "παράδειγμαΤου": case "paradeigmaTou": return "instanceof";
                    case "αλήθεια": case "aletheia": return "true";
                    case "ψεύδος": case "psevdos": return "false";
                    case "μηδέν": case "meden": return "null";
                    default: return t.value;
                }
            } else if (t.type == TokenType.STRING) {
                return "\"" + t.value.replace("\n", "\\n").replace("\"", "\\\"") + "\""; 
            } else if (t.type == TokenType.CHAR) {
                return "'" + t.value + "'";
            }
            return t.value;
        }
    }

    // =========================================================
    // 8. REPORT GENERATOR
    // =========================================================
    static void printReport(String src, Lexer lexer, Parser parser, SemanticAnalyser sem) {
        String sep  = "═".repeat(70);
        String sep2 = "─".repeat(70);

        System.out.println("\n" + sep);
        System.out.println("  ΕΛΛΗΝΙΚΟΣ ΜΕΤΑΓΛΩΤΤΙΣΤΗΣ  –  Ellinikos Compiler v1.1");
        System.out.println("  Compiler Construction CCP | Iqra University");
        System.out.println(sep);

        System.out.println("\n[ SOURCE CODE ]");
        System.out.println(sep2);
        String[] lines = src.split("\n");
        for (int i = 0; i < lines.length; i++) System.out.printf("  %3d | %s%n", i+1, lines[i]);
        System.out.println("\n[ TOKEN STREAM ]  (" + (lexer.tokens.size()-1) + " tokens)");
        System.out.println(sep2);
        for (Token t : lexer.tokens) if (t.type != TokenType.EOF) System.out.println("  " + t);

        System.out.println("\n[ LEXICAL ANALYSIS ]");
        System.out.println(sep2);
        if (lexer.errors.isEmpty()) System.out.println("  ✓ No lexical errors found."); else lexer.errors.forEach(System.out::println);

        System.out.println("\n[ SYNTAX ANALYSIS – Parse Trace ]");
        System.out.println(sep2);
        parser.parseLog.forEach(System.out::println);
        if (parser.errors.isEmpty()) System.out.println("  ✓ No syntax errors found."); else parser.errors.forEach(System.out::println);

        System.out.println("\n[ SEMANTIC ANALYSIS ]");
        System.out.println(sep2);
        sem.semLog.forEach(System.out::println);
        if (sem.errors.isEmpty() && sem.symTable.errors.isEmpty()) System.out.println("  ✓ No semantic errors found.");
        else { sem.errors.forEach(System.out::println); sem.symTable.errors.forEach(System.out::println); }

        int totalErrors = lexer.errors.size() + parser.errors.size() + sem.errors.size() + sem.symTable.errors.size();
        System.out.println("\n[ SUMMARY ]");
        System.out.println(sep2);
        System.out.printf("  Tokens          : %d%n", lexer.tokens.size()-1);
        System.out.printf("  Lexical Errors  : %d%n", lexer.errors.size());
        System.out.printf("  Syntax Errors   : %d%n", parser.errors.size());
        System.out.printf("  Semantic Errors : %d%n", sem.errors.size() + sem.symTable.errors.size());
        System.out.println(sep2);
        if (totalErrors == 0) System.out.println("  ✓ COMPILATION SUCCESSFUL");
        else System.out.println("  ✗ COMPILATION FAILED with " + totalErrors + " error(s).");
        System.out.println(sep + "\n");
    }

    // =========================================================
    // 9. BUILT-IN SAMPLE PROGRAMS 
    // =========================================================
    static final String[] SAMPLES = {
        "// Program 1: Hello World Execution\n" +
        "taxh Kyria {\n" +
        "    demosia statiko akyro arxi() {\n" +
        "        ektypose(\"Γεια σου Κόσμε! Hello from Ellinikos Compiler!\");\n" +
        "    }\n" +
        "}",
        "// Program 2: Input reading and Output Printing\n" +
        "taxh ScannerTest {\n" +
        "    demosia statiko akyro arxi() {\n" +
        "        akeraios ilikia;\n" +
        "        ektypose(\"Πόσο χρονών είστε; (Enter your age):\");\n" +
        "        diavase(ilikia);\n" +
        "        ektypose(\"Η ηλικία σας είναι (You entered):\");\n" +
        "        ektypose(ilikia);\n" +
        "    }\n" +
        "}",
        "// Program 3: Control flow with execution\n" +
        "taxh Elegxos {\n" +
        "    demosia statiko akyro arxi() {\n" +
        "        akeraios x = 1;\n" +
        "        oso (x < 3) {\n" +
        "            ektypose(\"Εκτέλεση βρόχου... (Looping...)\");\n" +
        "            x = x + 1;\n" +
        "        }\n" +
        "        ektypose(\"Ολοκληρώθηκε! (Done!)\");\n" +
        "    }\n" +
        "}",
        "// Program 4: Switch statement logic\n" +
        "taxh SwitchTest {\n" +
        "    demosia statiko akyro arxi() {\n" +
        "        akeraios x = 3;\n" +
        "        epiloge (x) {\n" +
        "            periptose 3:\n" +
        "                ektypose(\"Ο αριθμός είναι το Τρία! (Number is Three!)\");\n" +
        "                diakope;\n" +
        "            proepiloge:\n" +
        "                ektypose(\"Άλλος αριθμός (Other Number)\");\n" +
        "        }\n" +
        "    }\n" +
        "}",
        "// Program 5: Exception handling\n" +
        "taxh ExceptionTest {\n" +
        "    demosia statiko akyro arxi() {\n" +
        "        dokimase {\n" +
        "            akeraios a = 10;\n" +
        "            akeraios b = 0;\n" +
        "            ektypose(\"Πρόκληση εξαίρεσης... (About to throw exception...)\");\n" +
        "            akeraios c = a / b;\n" +
        "        } piasei (Exception e) {\n" +
        "            ektypose(\"Σφάλμα μαθηματικών εντοπίστηκε! (Math Error caught!)\");\n" +
        "        } telika {\n" +
        "            ektypose(\"Το μπλοκ 'finally' εκτελέστηκε. (Finally block executed.)\");\n" +
        "        }\n" +
        "    }\n" +
        "}"
    };

    // =========================================================
    // 10. ENTRY POINT & EXECUTION LOGIC
    // =========================================================
    public static void main(String[] args) throws Exception {
        
        System.setOut(new java.io.PrintStream(new java.io.FileOutputStream(FileDescriptor.out), true, "UTF-8"));

        try (Scanner sc = new Scanner(System.in, "UTF-8")) {
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║    ΕΛΛΗΝΙΚΟΣ ΜΕΤΑΓΛΩΤΤΙΣΤΗΣ  –  Greek-Keyword Compiler v1.1    ║");
            System.out.println("║      Compiler Construction CCP | Iqra University                 ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");

            while (true) {
                System.out.println("\n┌─────────────────────────────────────────┐");
                System.out.println("│  Main Menu                              │");
                System.out.println("│  1. Run built-in sample programs (1-5)  │");
                System.out.println("│  2. Type/paste source code interactively│");
                System.out.println("│  3. Exit                                │");
                System.out.println("└─────────────────────────────────────────┘");
                System.out.print("  Choice: ");
                String choice = sc.nextLine().trim();

                switch (choice) {
                    case "1" -> {
                        System.out.print("  Enter sample number (1-5): ");
                        String sn = sc.nextLine().trim();
                        try {
                            int idx = Integer.parseInt(sn) - 1;
                            if (idx >= 0 && idx < SAMPLES.length) compileAndRun(SAMPLES[idx]);
                            else System.out.println("  Invalid sample number.");
                        } catch (NumberFormatException e) {
                            System.out.println("  Invalid input. Please enter a number.");
                        }
                    }
                    case "2" -> {
                        System.out.println("  Paste/type your Greek source code.");
                        System.out.println("  Type 'END' on a new line when done:");
                        StringBuilder sb = new StringBuilder();
                        while (sc.hasNextLine()) {
                            String ln = sc.nextLine();
                            if (ln.equals("END")) break;
                            sb.append(ln).append("\n");
                        }
                        compileAndRun(sb.toString());
                    }
                    case "3" -> { System.out.println("  Exiting. Goodbye / Αντίο!"); return; }
                    default  -> System.out.println("  Invalid choice.");
                }
            }
        }
    }

    static void compileAndRun(String src) {
        Lexer lexer = new Lexer(src);
        lexer.tokenize();
        Parser parser = new Parser(lexer.tokens); 
        parser.parseProgram();
        
        SemanticAnalyser sem = new SemanticAnalyser(lexer.tokens);
        if (parser.errors.isEmpty()) {
            sem.analyse();
        } else {
            sem.semLog.add("  [SEM] Skipped because Syntax errors were found.");
        }
        
        printReport(src, lexer, parser, sem);
        int totalErrors = lexer.errors.size() + parser.errors.size() + sem.errors.size() + sem.symTable.errors.size();
        
        // --- EXECUTION PHASE ---
        if (totalErrors == 0) {
            System.out.println("\n[ TRANSLATION & EXECUTION ]");
            System.out.println("─".repeat(70));
            Translator translator = new Translator(lexer.tokens);
            String javaCode = translator.translate();
            if (translator.mainClass == null) {
                System.out.println("  ✓ Translation successful, but no 'arxi()' (main) entry point found to execute.");
                System.out.println("═".repeat(70) + "\n");
                return;
            }

            try {
                Path tempDir = Files.createTempDirectory("ellinikos_run");
                Path javaFile = tempDir.resolve(translator.mainClass + ".java");
                Files.writeString(javaFile, javaCode, java.nio.charset.StandardCharsets.UTF_8);
                System.out.println("  ⚙ Compiling to native Java bytecode...");
                
                ProcessBuilder javacPb = new ProcessBuilder("javac", "-encoding", "UTF-8", javaFile.toString());
                javacPb.inheritIO();
                Process javac = javacPb.start();
                
                if (javac.waitFor() == 0) {
                    System.out.println("  🚀 Running Application Output:\n");
                    ProcessBuilder javaPb = new ProcessBuilder("java", "-Dfile.encoding=UTF-8", "-cp", tempDir.toString(), translator.mainClass);
                    javaPb.inheritIO();
                    Process java = javaPb.start();
                    java.waitFor();
                    
                    System.out.println("\n  ✓ Execution Process Complete.");
                } else {
                    System.out.println("  ✗ Failed to compile the translated Java code. Check for mismatched native keywords.");
                }
            } catch (Exception e) {
                System.out.println("  ✗ Execution runtime error: " + e.getMessage());
            }
            System.out.println("═".repeat(70) + "\n");
        }
    }
}