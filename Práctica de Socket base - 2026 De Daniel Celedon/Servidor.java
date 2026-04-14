import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * SERVIDOR - MathBot Server con Mensajería
 *
 * Servidor de sockets que actúa como calculadora remota y sistema de mensajería.
 * Escucha en el puerto 5000 y responde comandos especiales del cliente.
 *
 * Comandos soportados:
 *   RESOLVE "expresion"       -> Evalúa la expresión matemática
 *   MSG "texto"               -> Deja un mensaje en el servidor (mensajería)
 *   INBOX                     -> Muestra todos los mensajes recibidos
 *   HISTORY                   -> Devuelve el historial de operaciones matemáticas
 *   STATUS                    -> Devuelve información del servidor
 *   EXIT                      -> Cierra la conexión con el cliente
 */
public class Servidor {

    private static final int PUERTO = 5000;

    // Historial de operaciones matemáticas
    private static List<String> historial = new ArrayList<>();

    // Bandeja de mensajes: cada entrada = "[HH:mm:ss] Cliente #N: texto"
    private static List<String> bandejaMensajes = new ArrayList<>();

    private static int totalConexiones = 0;
    private static long tiempoInicio = System.currentTimeMillis();

    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║       MATHBOT SERVER v2.0            ║");
        System.out.println("║   Servidor de Cálculo + Mensajería   ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println("[SERVER] Iniciando en puerto " + PUERTO + "...");

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("[SERVER] Listo. Esperando conexiones...\n");

            while (true) {
                Socket socketCliente = serverSocket.accept();
                totalConexiones++;

                String ip = socketCliente.getInetAddress().getHostAddress();
                System.out.println("┌─────────────────────────────────────────────");
                System.out.println("│ [CONEXIÓN #" + totalConexiones + "] Cliente conectado desde: " + ip);
                System.out.println("└─────────────────────────────────────────────");

                atenderCliente(socketCliente, totalConexiones);
            }

        } catch (IOException e) {
            System.err.println("[ERROR] No se pudo iniciar el servidor: " + e.getMessage());
        }
    }

    /**
     * Maneja toda la comunicación con un cliente conectado.
     */
    private static void atenderCliente(Socket socket, int numConexion) {
        try (
            BufferedReader entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // Banner de bienvenida
            salida.println("╔════════════════════════════════════════════╗");
            salida.println("║    Bienvenido a MathBot Server v2.0        ║");
            salida.println("║  Comandos disponibles:                     ║");
            salida.println("║  RESOLVE \"expr\"  -> Calcula expresión       ║");
            salida.println("║  MSG \"texto\"     -> Dejar un mensaje        ║");
            salida.println("║  INBOX          -> Ver mensajes recibidos   ║");
            salida.println("║  HISTORY        -> Ver historial de cálculo ║");
            salida.println("║  STATUS         -> Info del servidor        ║");
            salida.println("║  EXIT           -> Desconectarse            ║");
            salida.println("╚════════════════════════════════════════════╝");
            salida.println("##END##");

            String mensajeRecibido;

            while ((mensajeRecibido = entrada.readLine()) != null) {

                System.out.println("[CLIENTE #" + numConexion + "] >> " + mensajeRecibido);

                String respuesta = procesarComando(mensajeRecibido.trim(), numConexion);
                salida.println(respuesta);
                // Marca de fin de mensaje: el cliente lee hasta esta línea
                salida.println("##END##");

                if (mensajeRecibido.trim().equalsIgnoreCase("EXIT")) {
                    System.out.println("[SERVER] Cliente #" + numConexion + " se desconectó.\n");
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("[ERROR] Fallo con cliente #" + numConexion + ": " + e.getMessage());
        }
    }

    /**
     * Interpreta el comando recibido y devuelve la respuesta correspondiente.
     */
    private static String procesarComando(String comando, int numConexion) {

        // ── RESOLVE "expresion" ──────────────────────────────────────────────
        if (comando.toUpperCase().startsWith("RESOLVE")) {
            int inicio = comando.indexOf('"');
            int fin    = comando.lastIndexOf('"');

            if (inicio == -1 || fin == -1 || inicio == fin) {
                return "[ERROR] Formato inválido. Usa: RESOLVE \"expresion\"";
            }

            String expresion = comando.substring(inicio + 1, fin);
            String resultado = evaluarExpresion(expresion);

            String entradaLog = "RESOLVE \"" + expresion + "\" = " + resultado;
            historial.add(entradaLog);
            System.out.println("[SERVER] Calculado: " + entradaLog);
            return "[RESULTADO] " + expresion + " = " + resultado;
        }

        // ── MSG "texto" ──────────────────────────────────────────────────────
        else if (comando.toUpperCase().startsWith("MSG")) {
            int inicio = comando.indexOf('"');
            int fin    = comando.lastIndexOf('"');

            if (inicio == -1 || fin == -1 || inicio == fin) {
                return "[ERROR] Formato inválido. Usa: MSG \"tu mensaje aquí\"";
            }

            String texto = comando.substring(inicio + 1, fin).trim();

            if (texto.isEmpty()) {
                return "[ERROR] El mensaje no puede estar vacío.";
            }

            String timestamp = SDF.format(new Date());
            String entrada   = "[" + timestamp + "] Cliente #" + numConexion + ": " + texto;
            bandejaMensajes.add(entrada);

            System.out.println("[MENSAJE NUEVO] " + entrada);
            return "[MSG] Mensaje guardado correctamente. Total en bandeja: " + bandejaMensajes.size();
        }

        // ── INBOX ────────────────────────────────────────────────────────────
        else if (comando.equalsIgnoreCase("INBOX")) {
            if (bandejaMensajes.isEmpty()) {
                return "[INBOX] No hay mensajes en la bandeja aún.";
            }
            StringBuilder sb = new StringBuilder("[INBOX] Mensajes recibidos (" + bandejaMensajes.size() + " total):\n");
            for (int i = 0; i < bandejaMensajes.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(bandejaMensajes.get(i)).append("\n");
            }
            return sb.toString().trim();
        }

        // ── HISTORY ──────────────────────────────────────────────────────────
        else if (comando.equalsIgnoreCase("HISTORY")) {
            if (historial.isEmpty()) {
                return "[HISTORY] No hay operaciones registradas aún.";
            }
            StringBuilder sb = new StringBuilder("[HISTORY] Últimas operaciones matemáticas:\n");
            int desde = Math.max(0, historial.size() - 5);
            for (int i = desde; i < historial.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(historial.get(i)).append("\n");
            }
            return sb.toString().trim();
        }

        // ── STATUS ───────────────────────────────────────────────────────────
        else if (comando.equalsIgnoreCase("STATUS")) {
            long uptime = (System.currentTimeMillis() - tiempoInicio) / 1000;
            return "[STATUS] Servidor activo | Uptime: " + uptime + "s | " +
                   "Conexiones: " + totalConexiones + " | " +
                   "Cálculos: " + historial.size() + " | " +
                   "Mensajes en bandeja: " + bandejaMensajes.size();
        }

        // ── EXIT ─────────────────────────────────────────────────────────────
        else if (comando.equalsIgnoreCase("EXIT")) {
            return "[SERVER] ¡Hasta luego! Conexión cerrada.";
        }

        // ── Comando desconocido ───────────────────────────────────────────────
        else {
            return "[ERROR] Comando desconocido: \"" + comando + "\". " +
                   "Válidos: RESOLVE \"expr\" | MSG \"texto\" | INBOX | HISTORY | STATUS | EXIT";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PARSER MATEMÁTICO PROPIO
    //  Soporta: + - * /  y paréntesis. Ej: "45*23/54+234", "(2+3)*4"
    // ─────────────────────────────────────────────────────────────────────────

    private static String evaluarExpresion(String expresion) {
        try {
            expresion = expresion.replaceAll("\\s+", "");
            double resultado = parsear(expresion, new int[]{0});

            if (resultado == Math.floor(resultado) && !Double.isInfinite(resultado)) {
                return String.valueOf((long) resultado);
            }
            return String.format("%.4f", resultado);

        } catch (Exception e) {
            return "ERROR: expresión inválida (" + e.getMessage() + ")";
        }
    }

    private static double parsear(String expr, int[] pos) {
        return parseAdicion(expr, pos);
    }

    private static double parseAdicion(String expr, int[] pos) {
        double resultado = parseMultiplicacion(expr, pos);
        while (pos[0] < expr.length() &&
               (expr.charAt(pos[0]) == '+' || expr.charAt(pos[0]) == '-')) {
            char op = expr.charAt(pos[0]++);
            double derecha = parseMultiplicacion(expr, pos);
            if (op == '+') resultado += derecha;
            else           resultado -= derecha;
        }
        return resultado;
    }

    private static double parseMultiplicacion(String expr, int[] pos) {
        double resultado = parseAtomo(expr, pos);
        while (pos[0] < expr.length() &&
               (expr.charAt(pos[0]) == '*' || expr.charAt(pos[0]) == '/')) {
            char op = expr.charAt(pos[0]++);
            double derecha = parseAtomo(expr, pos);
            if (op == '*') resultado *= derecha;
            else {
                if (derecha == 0) throw new ArithmeticException("División por cero");
                resultado /= derecha;
            }
        }
        return resultado;
    }

    private static double parseAtomo(String expr, int[] pos) {
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '(') {
            pos[0]++;
            double resultado = parseAdicion(expr, pos);
            if (pos[0] < expr.length() && expr.charAt(pos[0]) == ')') {
                pos[0]++;
            }
            return resultado;
        }

        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '-') {
            pos[0]++;
            return -parseAtomo(expr, pos);
        }

        int inicio = pos[0];
        while (pos[0] < expr.length() &&
               (Character.isDigit(expr.charAt(pos[0])) || expr.charAt(pos[0]) == '.')) {
            pos[0]++;
        }

        if (inicio == pos[0]) {
            throw new RuntimeException("Carácter inesperado en posición " + pos[0]);
        }

        return Double.parseDouble(expr.substring(inicio, pos[0]));
    }
}
