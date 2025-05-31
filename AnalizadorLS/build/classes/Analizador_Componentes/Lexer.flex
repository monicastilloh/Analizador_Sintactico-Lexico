package analizador;

import java_cup.runtime.Symbol;

// Añade esta clase al principio del archivo para manejar errores léxicos
%{
    public static class LexicalError {
        public final int line;
        public final int column;
        public final String text;
        
        public LexicalError(int line, int column, String text) {
            this.line = line;
            this.column = column;
            this.text = text;
        }
        
        @Override
        public String toString() {
            return "Error léxico en línea " + line + ", columna " + column + ": '" + text + "'";
        }
    }
%}

%%

%class Lexer
%cup
%unicode
%line
%column
%public

%{
    private Symbol symbol(int type) {
        return new Symbol(type, yyline+1, yycolumn+1);
    }
    
    private Symbol symbol(int type, Object value) {
        return new Symbol(type, yyline+1, yycolumn+1, value);
    }
%}

LineTerminator = \r|\n|\r\n
WhiteSpace = {LineTerminator}|[ \t\f]

Identificador = [a-zA-Z][a-zA-Z0-9_]*
Numero = [0-9]+

%%

<YYINITIAL> {
    // Palabras reservadas
    "Robot"         { return symbol(sym.ROBOT, yytext()); }
    "iniciar"      { return symbol(sym.INICIAR, yytext()); }
    "detener"      { return symbol(sym.DETENER, yytext()); }
    "cerrarGarra"  { return symbol(sym.CERRAR_GARRA, yytext()); }
    "abrirGarra"   { return symbol(sym.ABRIR_GARRA, yytext()); }
    "repetir"      { return symbol(sym.REPETIR, yytext()); }
    
    // Componentes del robot
    "base"         { return symbol(sym.BASE, yytext()); }
    "cuerpo"       { return symbol(sym.CUERPO, yytext()); }
    "garra"        { return symbol(sym.GARRA, yytext()); }
    "velocidad"    { return symbol(sym.VELOCIDAD, yytext()); }
    
    // Operadores y símbolos
    "="            { return symbol(sym.IGUAL, yytext()); }
    "."            { return symbol(sym.PUNTO, yytext()); }
    "("            { return symbol(sym.PARENTESIS_ABRE, yytext()); }
    ")"            { return symbol(sym.PARENTESIS_CIERRA, yytext()); }
    ";"            { return symbol(sym.PUNTO_COMA, yytext()); }
    // Literales
    {Numero}       { return symbol(sym.NUMERO, new Integer(yytext())); }
    {Identificador} { return symbol(sym.IDENTIFICADOR, yytext()); }
    
    // Espacios en blanco (ignorar)
    {WhiteSpace}   { /* ignorar */ }
}

/* Cualquier otro carácter no reconocido */
// En Lexer.flex, modifica la sección de manejo de errores:
[^] { 
    String errorMsg = "Error léxico en línea " + (yyline+1) + ", columna " + (yycolumn+1) + 
                     ": Carácter no válido '" + yytext() + "'";
    System.err.println(errorMsg);
    // Devuelve un símbolo de error con la posición correcta
    return symbol(sym.error, new LexicalError(yyline+1, yycolumn+1, yytext()));
}

