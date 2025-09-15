package proyecto.lenguaje.lexer;

public class Token {
    public enum Type {
        IDENTIFIER_VAR,    // Identificadores que comienzan con minúscula (variables y funciones)
        IDENTIFIER_TYPE,   // Identificadores que comienzan con mayúscula (tipos y constructores)
        INTEGER,           // Números enteros
        FLOAT,            // Números decimales
        CHAR,             // Caracteres
        STRING,           // Cadenas de texto
        BOOLEAN,          // Valores booleanos
        LIST_START,       // Inicio de lista [
        LIST_END,         // Fin de lista ]
        TUPLE_START,      // Inicio de tupla (
        TUPLE_END,        // Fin de tupla )
        OPERATOR,         // Operadores
        KEYWORD,          // Palabras reservadas
        SYMBOL,           // Símbolos especiales
        ERROR             // Errores léxicos
    }

    private final Type type;
    private final String value;
    private final int position;
    private final int line;

    public Token(Type type, String value, int position, int line) {
        this.type = type;
        this.value = value;
        this.position = position;
        this.line = line;
    }

    public Type getType() { return type; }
    public String getValue() { return value; }
    public int getPosition() { return position; }
    public int getLine() { return line; }

    @Override
    public String toString() {
        return String.format("'%s' (línea: %d, pos: %d) [%s]", value, line, position, type);
    }
}
