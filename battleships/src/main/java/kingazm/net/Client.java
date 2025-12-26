package kingazm.net;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple interactive TCP client used for testing the server.
 * It connects to a server, starts a background reader thread to print incoming lines,
 * and reads lines from stdin to send to the server.
 *
 * The client uses consoleLock to synchronize printing between the
 * background reader thread and the stdin prompt so output doesn't interleave and
 * the prompt can be cleared/reprinted cleanly when incoming messages arrive.
 */
public class Client {
    private String clientName;
    private String host;
    private int port;

    private static final Logger logger = Logger.getLogger(Client.class.getName());

    private static final Object consoleLock = new Object();
    private volatile boolean promptShown = false;

    /**
     * Create a client with a random auto-generated name to keep track
     * of the different clients communicating with each other, as the printed prompts
     * indicate which client sent which text.
     */
    public Client(String host, int port) {
        this.host = host;
        this.port = port;
        this.clientName = "Client-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void printPrompt() {
        synchronized (consoleLock) {
            // keep prompt printing as System.out to allow printing without newline
            System.out.print("You: ");
            System.out.flush();
            promptShown = true;
        }
    }

    /**
     * Clear a previously printed prompt line for clean output sequence from each
     * client's perspective.
     */
    private void clearPromptLine() {
        synchronized (consoleLock) {
            System.out.print("\r");
            System.out.print("\r");
            System.out.flush();
            promptShown = false;
        }
    }

    /**
     * Connect to the server and run interactive I/O until stdin or the socket closes.
     * @throws IOException when socket I/O fails
     */
    public void connect() throws IOException {
        logger.info("Starting a client... " + host + ":" + port);
        try (Socket socket = new Socket(host, port);
             BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in))) {

            logger.info("Connected to " + host + ":" + port);

            Thread readerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = socketReader.readLine()) != null) {
                        if (promptShown) {
                            clearPromptLine();
                        }
                        synchronized (consoleLock) {
                            logger.info(clientName + " : " + line);
                        }
                        printPrompt();
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error reading from server", e);
                }
            }, "socket-reader");
            readerThread.setDaemon(true);
            readerThread.start();

            String input;
            printPrompt();
            while ((input = stdin.readLine()) != null) {
                promptShown = false;
                out.println(input);
                printPrompt();
            }

            logger.info("Input closed, client exiting");
        }
    }

    /**
     * Try to connect up to {@code retries} times, waiting {@code delayMs} between attempts.
     */
    public boolean connectWithRetries(int retries, long delayMs) {
        for (int attempt = 1; attempt <= retries; attempt++) {
            try {
                connect();
                return true;
            } catch (IOException e) {
                logger.log(Level.WARNING, "Attempt " + attempt + " failed to connect to " + host + ":" + port + " - " + e.getMessage(), e);
                if (attempt < retries) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Process command-line args and return a connection configuration map.
     * In case of no args, default values are used.
     */
    public static Map<String, String> processArgs(String[] args) {
        Map<String, String> config = new HashMap<>();
        config.put("host", "localhost");
        config.put("port", "12345");
        config.put("retries", "5");
        config.put("delay", "1000");

        for (int arg = 0; arg < args.length; arg++) {
            switch (args[arg]) {
                case "-host":
                    if ((arg + 1) < args.length) {
                        config.put("host", args[++arg]);
                    }
                    break;
                case "-port":
                    if ((arg + 1) < args.length) {
                        config.put("port", args[++arg]);
                    }
                    break;
                case "-retries":
                    if ((arg + 1) < args.length) {
                        config.put("retries", args[++arg]);
                    }
                    break;
                case "-delay":
                    if ((arg + 1) < args.length) {
                        config.put("delay", args[++arg]);
                    }
                    break;
            }
        }

        return config;
    }

    public static int parseIntOrDefault(String s, int defaulf) {
        if (s == null) {
            return defaulf;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaulf;
        }
    }

    public static long parseLongOrDefault(String s, long defaulf) {
        if (s == null) {
            return defaulf;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return defaulf;
        }
    }


    public static void main(String[] args) {
        Map<String, String> config = processArgs(args);

        String host = config.get("host");
        int port = parseIntOrDefault(config.get("port"), 12345);
        int retries = parseIntOrDefault(config.get("retries"), 5);
        long delayMs = parseLongOrDefault(config.get("delay"), 1000L);

        Client client = new Client(host, port);
        boolean isConnected = client.connectWithRetries(retries, delayMs);

        if (isConnected) {
            logger.info("Connected to " + host + ":" + port);
        } else {
            logger.severe("Unable to connect after " + retries + " attempts.");
            System.exit(1);
        }
    }
}
