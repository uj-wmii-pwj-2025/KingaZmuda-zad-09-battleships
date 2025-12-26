package kingazm.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple TCP server that accepts connections and broadcasts text lines received
 * from one client to all other connected clients. Uses a fixed thread pool to
 * handle clients concurrently and a concurrent queue of PrintWriters to manage
 * connected client outputs. The server will listen on a specified port.
 */
public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private int port;

    /**
     * Concurrent collection of connected clients' output writers used for broadcasting the sent communication.
     */
    private static final ConcurrentLinkedQueue<PrintWriter> clients = new ConcurrentLinkedQueue<>();

    public Server(int port) {
        this.port = port;
    }

    /**
     * Start accepting clients. Each accepted connection is handled on a thread
     * so multiple clients may be connected concurrently.
     *
     * @throws IOException when ServerSocket cannot be opened or encounters an I/O error
     */
    public void start() throws IOException {
        logger.info("Server starting on port: " + port);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Waiting for clients...");
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                executor.submit(() -> {
                    try {
                        handleClient(socket);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Error handling client", e);
                    }
                });
            }
        } finally {
            executor.shutdown();
            logger.info("Server stopped, executor shutdown initiated");
        }
    }

    /**
     * Handle a client connection. Reads newline-delimited lines from the
     * socket and forwards each line to all other connected clients. Cleans up
     * client writer on disconnect.
     *
     * @param socket accepted client socket
     */
    private void handleClient(Socket socket) {
        logger.info("Client connected from: " + socket.getRemoteSocketAddress());
        PrintWriter out = null;
        try (Socket s = socket;
             InputStream in = s.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

            out = new PrintWriter(s.getOutputStream(), true);
            clients.add(out);

            String line;
            while ((line = reader.readLine()) != null) {
                for (PrintWriter pw : clients) {
                    if (pw != out) {
                        pw.println(line);
                    }
                }
            }

            logger.info("Client closed connection: " + socket.getRemoteSocketAddress());
        } catch (IOException e) {
            logger.log(Level.INFO, "Connection error", e);
        } finally {
            if (out != null) {
                clients.remove(out);
                out.close();
            }
        }
    }

    /**
     * Process command-line args regarding the server.
     * Unknown/invalid args are logged.
     *
     * @param args command-line arguments
     */
    private void handleArgs(String[] args) {

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-port":
                    if ((i + 1) < args.length) {
                        try {
                            port = Integer.parseInt(args[i + 1]);
                            i++;
                        } catch (NumberFormatException e) {
                            logger.warning("Invalid port number: " + args[i + 1]);
                        }
                    }
                    break;
                default:
                    logger.warning("Unknown argument: " + args[i]);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server(12345);
        server.handleArgs(args);
        server.start();
    }
}
