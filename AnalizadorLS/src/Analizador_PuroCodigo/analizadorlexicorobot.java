package Analizador_PuroCodigo;

import java.util.*;
import java.util.regex.*;

public class analizadorlexicorobot {
    public enum TipoToken {
        PALABRA_CLAVE, ACCION_MOVIMIENTO, COMPONENTE,
        NUMERO, OPERADOR, DELIMITADOR, COMENTARIO, 
        IDENTIFICADOR, DESCONOCIDO, PARENTESIS, PUNTO, IGUAL,
        METODO
    }

    public static class Token {
        public TipoToken tipo;
        public String lexema;
        public int linea;

        public Token(TipoToken tipo, String lexema, int linea) {
            this.tipo = tipo;
            this.lexema = lexema;
            this.linea = linea;
        }
    }

    private static final Set<String> palabrasClave = Set.of(
        "Robot", "iniciar", "detener", "repetir"
    );

    private static final Set<String> acciones = Set.of(
        "subir", "bajar", "girar", "abrir", "cerrar"
    );

    private static final Set<String> componentes = Set.of(
        "base", "cuerpo", "garra"
    );

    private static final Map<TipoToken, String> patrones = new LinkedHashMap<>();
    static {
        patrones.put(TipoToken.COMENTARIO, "//.*");
        patrones.put(TipoToken.NUMERO, "\\b\\d+\\b");
        patrones.put(TipoToken.IDENTIFICADOR, "\\b[a-zA-Z_][a-zA-Z0-9_]*\\b");
        patrones.put(TipoToken.OPERADOR, "[+\\-*/]");
        patrones.put(TipoToken.PARENTESIS, "[()]");
        patrones.put(TipoToken.PUNTO, "\\.");
        patrones.put(TipoToken.IGUAL, "=");
        patrones.put(TipoToken.DELIMITADOR, "[,;]");
    }

    public static List<Token> analizarLexico(String codigo) {
        List<Token> tokens = new ArrayList<>();
        String[] lineas = codigo.split("\n");
        
        for (int numLinea = 0; numLinea < lineas.length; numLinea++) {
            String linea = lineas[numLinea].trim();
            if (linea.isEmpty()) continue;

            int pos = 0;
            while (pos < linea.length()) {
                if (Character.isWhitespace(linea.charAt(pos))) {
                    pos++;
                    continue;
                }

                boolean match = false;
                for (Map.Entry<TipoToken, String> entry : patrones.entrySet()) {
                    Pattern pattern = Pattern.compile(entry.getValue());
                    Matcher matcher = pattern.matcher(linea.substring(pos));
                    
                    if (matcher.find() && matcher.start() == 0) {
                        String lexema = matcher.group();
                        TipoToken tipo = entry.getKey();
                        
                        if (tipo == TipoToken.IDENTIFICADOR) {
                            tipo = clasificarIdentificador(lexema);
                        }
                        
                        tokens.add(new Token(tipo, lexema, numLinea + 1));
                        pos += lexema.length();
                        match = true;
                        break;
                    }
                }

                if (!match) {
                    int end = pos + 1;
                    while (end < linea.length() && !Character.isWhitespace(linea.charAt(end))) {
                        end++;
                    }
                    String lexema = linea.substring(pos, end);
                    tokens.add(new Token(TipoToken.DESCONOCIDO, lexema, numLinea + 1));
                    pos = end;
                }
            }
        }
        return tokens;
    }

    private static TipoToken clasificarIdentificador(String lexema) {
        if (palabrasClave.contains(lexema)) return TipoToken.PALABRA_CLAVE;
        if (acciones.contains(lexema)) return TipoToken.ACCION_MOVIMIENTO;
        if (componentes.contains(lexema)) return TipoToken.COMPONENTE;
        return TipoToken.IDENTIFICADOR;
    }

    public static List<String> analizarSintactico(List<Token> tokens) {
        List<String> errores = new ArrayList<>();
        Set<String> robotsDeclarados = new HashSet<>();
        int i = 0;

        while (i < tokens.size()) {
            Token token = tokens.get(i);

            if (token.tipo == TipoToken.COMENTARIO) {
                i++;
                continue;
            }

            if (token.lexema.equals("Robot")) {
                i++;
                if (i < tokens.size() && tokens.get(i).tipo == TipoToken.IDENTIFICADOR) {
                    robotsDeclarados.add(tokens.get(i).lexema);
                    i++;
                    if (i >= tokens.size() || !tokens.get(i).lexema.equals(";")) {
                        errores.add("Línea " + token.linea + ": Falta ';' después de declaración Robot");
                    } else {
                        i++;
                    }
                } else {
                    errores.add("Línea " + token.linea + ": Se esperaba nombre de Robot");
                }
            }
            else if (robotsDeclarados.contains(token.lexema)) {
                i++;
                if (i < tokens.size() && tokens.get(i).lexema.equals(".")) {
                    i++;
                    if (i < tokens.size()) {
                        Token siguiente = tokens.get(i);
                        
                        if (siguiente.tipo == TipoToken.COMPONENTE) {
                            i++;
                            if (i < tokens.size() && tokens.get(i).lexema.equals("=")) {
                                i++;
                                if (i < tokens.size() && tokens.get(i).tipo == TipoToken.NUMERO) {
                                    i++;
                                    if (i >= tokens.size() || !tokens.get(i).lexema.equals(";")) {
                                        errores.add("Línea " + token.linea + ": Falta ';' después de asignación");
                                    } else {
                                        i++;
                                    }
                                } else {
                                    errores.add("Línea " + token.linea + ": Se esperaba número");
                                }
                            } else {
                                errores.add("Línea " + token.linea + ": Se esperaba '='");
                            }
                        }
                        else if (siguiente.tipo == TipoToken.PALABRA_CLAVE && 
                                Set.of("iniciar", "detener", "repetir").contains(siguiente.lexema)) {
                            i++;
                            if (i < tokens.size() && tokens.get(i).lexema.equals("(")) {
                                i++;
                                if (siguiente.lexema.equals("repetir")) {
                                    if (i < tokens.size() && tokens.get(i).tipo == TipoToken.NUMERO) {
                                        i++;
                                    } else {
                                        errores.add("Línea " + token.linea + ": Se esperaba número de repeticiones");
                                    }
                                }
                                if (i < tokens.size() && tokens.get(i).lexema.equals(")")) {
                                    i++;
                                    if (i >= tokens.size() || !tokens.get(i).lexema.equals(";")) {
                                        errores.add("Línea " + token.linea + ": Falta ';' después de método");
                                    } else {
                                        i++;
                                    }
                                } else {
                                    errores.add("Línea " + token.linea + ": Se esperaba ')'");
                                }
                            } else {
                                errores.add("Línea " + token.linea + ": Se esperaba '('");
                            }
                        } else {
                            errores.add("Línea " + token.linea + ": Se esperaba componente o método");
                        }
                    }
                } else {
                    errores.add("Línea " + token.linea + ": Se esperaba '.'");
                }
            }
            else if (token.tipo != TipoToken.DESCONOCIDO) {
                errores.add("Línea " + token.linea + ": Sentencia no reconocida");
                i++;
            } else {
                errores.add("Línea " + token.linea + ": Símbolo no reconocido");
                i++;
            }
        }

        return errores;
    }
}