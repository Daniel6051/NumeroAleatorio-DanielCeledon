import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * CLIENTE - MathBot Client v2.0
 *
 * Cliente de sockets que se conecta al MathBot Server.
 * Permite al usuario enviar comandos al servidor y ver las respuestas.
 *
 * Comandos que puede enviar:
 *   RESOLVE "expresion"  -> El servidor calcula la expresión matemática
 *   MSG "texto"          -> Deja un mensaje en la bandeja del servidor
 *   INBOX                -> Ver todos los mensajes dejados en el servidor
 *   HISTORY              -> Pide el historial de operaciones al servidor
 *   STATUS               -> Pide información del estado del servidor
 *   EXIT                 -> Cierra la conexión
 */
public class Cliente {

    private static final String HOST   = "localhost";
    private static final int    PUERTO = 5000;

    // Marca que usa el servidor para señalar el fin de cada respuesta
    private static final String FIN_MENSAJE = "##END##";

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║       MATHBOT CLIENT v2.0            ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println("[CLIENT] Conectando a " + HOST + ":" + PUERTO + "...");

        try (
            Socket socket = new Socket(HOST, PUERTO);

            BufferedReader entradaServidor = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

            PrintWriter salidaServidor = new PrintWriter(socket.getOutputStream(), true);

            Scanner teclado = new Scanner(System.in)
        ) {
            System.out.println("[CLIENT] ¡Conectado exitosamente!\n");

            // ── Recibir banner de bienvenida ──────────────────────────────────
            // Leemos hasta ##END##, no hasta un carácter especial del banner
            leerRespuesta(entradaServidor, true);

            System.out.println("\n[CLIENT] Escribe un comando (o EXIT para salir):");
            mostrarAyuda();

            // ── Bucle principal de interacción ────────────────────────────────
            while (true) {
                System.out.print("\n> ");

                String comandoUsuario = teclado.nextLine();

                if (comandoUsuario.trim().isEmpty()) {
                    System.out.println("[CLIENT] Escribe un comando válido.");
                    continue;
                }

                // Ayuda local (no se envía al servidor)
                if (comandoUsuario.trim().equalsIgnoreCase("HELP")) {
                    mostrarAyuda();
                    continue;
                }

                // Enviar al servidor
                salidaServidor.println(comandoUsuario);
                System.out.println("[CLIENT] Enviado → " + comandoUsuario);

                // ── Leer respuesta del servidor ───────────────────────────────
                // readLine() es BLOQUEANTE: espera línea por línea hasta ##END##
                // Esto elimina el bug de ready() que dejaba líneas en el buffer
                leerRespuesta(entradaServidor, false);

                // Salir si el usuario escribió EXIT
                if (comandoUsuario.trim().equalsIgnoreCase("EXIT")) {
                    System.out.println("\n[CLIENT] Desconectado. ¡Hasta luego!");
                    break;
                }
            }

        } catch (ConnectException e) {
            System.err.println("[ERROR] No se pudo conectar al servidor.");
            System.err.println("        Asegurate de que Servidor.java esté corriendo primero.");
        } catch (IOException e) {
            System.err.println("[ERROR] Error de comunicación: " + e.getMessage());
        }
    }

    /**
     * Lee líneas del servidor hasta encontrar la marca ##END##.
     *
     * Este método reemplaza el uso de ready(), que era NO bloqueante y causaba
     * que líneas de una respuesta quedaran en el buffer y se mezclaran con
     * la respuesta siguiente.
     *
     * readLine() es BLOQUEANTE: espera hasta recibir una línea completa,
     * garantizando que se lean TODAS las líneas de la respuesta actual
     * antes de volver al prompt.
     *
     * @param esBanner  true  = imprime cada línea con prefijo [SERVER] (banner inicial)
     *                  false = primera línea con [SERVER], el resto indentado
     */
    private static void leerRespuesta(BufferedReader entradaServidor, boolean esBanner)
            throws IOException {

        String linea;
        boolean primeraLinea = true;

        while ((linea = entradaServidor.readLine()) != null) {

            // ##END## es la señal de fin: no se imprime, solo se usa para cortar
            if (linea.equals(FIN_MENSAJE)) {
                break;
            }

            if (esBanner || primeraLinea) {
                System.out.println("[SERVER] " + linea);
                primeraLinea = false;
            } else {
                // Líneas adicionales de INBOX/HISTORY: indentadas sin prefijo extra
                System.out.println("         " + linea);
            }
        }
    }

    /**
     * Imprime una guía rápida de comandos disponibles (sin enviar al servidor).
     */
    private static void mostrarAyuda() {
        System.out.println("\n  ┌─── Comandos disponibles ───────────────────────────────┐");
        System.out.println("  │  RESOLVE \"expr\"   Calcula una expresión matemática      │");
        System.out.println("  │                   Ejemplo: RESOLVE \"(10+5)*3/2\"         │");
        System.out.println("  │                                                          │");
        System.out.println("  │  MSG \"texto\"      Deja un mensaje en el servidor        │");
        System.out.println("  │                   Ejemplo: MSG \"Hola desde el cliente\"  │");
        System.out.println("  │                                                          │");
        System.out.println("  │  INBOX            Ver todos los mensajes en el servidor  │");
        System.out.println("  │  HISTORY          Ver historial de cálculos              │");
        System.out.println("  │  STATUS           Ver estado del servidor                │");
        System.out.println("  │  HELP             Mostrar esta ayuda (local)             │");
        System.out.println("  │  EXIT             Desconectarse                          │");
        System.out.println("  └──────────────────────────────────────────────────────────┘");
    }
}
