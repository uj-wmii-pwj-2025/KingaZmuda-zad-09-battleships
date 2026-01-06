package kingazm.net;

import kingazm.board.BoardConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Simple interactive TCP client used for testing the server.
 * It connects to a server, starts a background reader thread to print incoming lines,
 * and reads lines from stdin to send to the server.
 */
public class Client {

    static {
        Logger root = Logger.getLogger("");
        Handler[] handlers = root.getHandlers();
        for (Handler h : handlers) {
            h.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return record.getLevel() + ": " + record.getMessage() + System.lineSeparator();
                }
            });
        }
    }

    private final String clientName;
    private final String host;
    private final int port;
    private static final Logger logger = Logger.getLogger(Client.class.getName());
    private static final Object consoleLock = new Object();
    private volatile boolean myTurn = false;
    private volatile boolean gameOver = false;
    private String myClientId = null;
    private final Set<String> shotCoordinates = new HashSet<>();
    private static final int COLS = BoardConfig.COLS;

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

            Thread readerThread = startServerListener(socketReader);
            readerThread.start();

            handleUserInput(stdin, out);
            logger.info("Input closed, client exiting");
        }
    }

    private Thread startServerListener(BufferedReader socketReader) {
        Runnable readerTask = () -> {
            try {
                String line;
                while ((line = socketReader.readLine()) != null) {
                    synchronized (consoleLock) {
                        handleServerMessage(line);
                    }
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error reading from server", e);
            }
        };

        Thread readerThread = new Thread(readerTask, "socket-reader");
        readerThread.setDaemon(true);
        return readerThread;
    }

    private void handleServerMessage(String line) {
        if (line == null) {
            return;
        }

        String up = line.toUpperCase();
        String command = up.contains(";")
                ? up.substring(0, up.indexOf(';') + 1)
                : up;

        switch (command) {

            case "UI;" -> {
                System.out.println(line.substring(3));
            }

            case "MAP;" -> {
                showBoard(line.substring(4), null);
            }

            case "OPP_MAP;" -> {
                showBoard(line.substring(8), "Plansza przeciwnika:");
            }

            case "START;" -> {
                String[] parts = line.split(";", 3);
                if (parts.length >= 3) {
                    myClientId = parts[2];
                }
            }

            case "TURA;", "TURN;" -> {
                handleTurnMessage(line);
            }

            case "INFO;" -> {
                String infoMsg = line.substring(5).trim();
                if (!"oczekiwanie na ruch przeciwnika".equalsIgnoreCase(infoMsg)) {
                    System.out.println(infoMsg);
                }
            }

            case "STATUS;" -> {
                String statusMsg = line.substring(7).trim();
                if (!"twoja tura".equalsIgnoreCase(statusMsg)
                    && !"czekaj".equalsIgnoreCase(statusMsg)) {
                    System.out.println(statusMsg);
                }
            }

            case "TRAFIONY;" -> {
                printHitMessage(extractCoordinate(line));
            }

            case "PUDŁO;" -> {
                printMissMessage(extractCoordinate(line));
            }

            case "WYNIK;" -> {
                String result = line.substring(line.indexOf(';') + 1).trim();
                printEndMessage(result);
                gameOver = true;
            }

            case "MOVE;" -> {
                printMove(line);
            }

            default -> {
                System.out.println(line);
            }
        }
    }

    private void handleTurnMessage(String line) {
        String whose = line.substring(line.indexOf(';') + 1);
        boolean nowMine = myClientId != null && myClientId.equals(whose);
        myTurn = nowMine;
        if (myTurn) {
            System.out.println("\nTwoja tura. Podaj współrzędne (np. A1):");
        } else {
            System.out.println("\nCzekaj na ruch przeciwnika...");
        }
    }

    private String extractCoordinate(String line) {
        String[] parts = line.split(";");
        return parts.length >= 2 ? parts[1].trim().toUpperCase() : "";
    }

    private void showBoard(String map, String header) {
        if (header != null && !header.isEmpty()) {
            System.out.println(header);
        }

        for (int idx = 0; idx + COLS <= map.length(); idx += COLS) {
            System.out.println(map.substring(idx, idx + COLS));
        }
    }

    private void handleUserInput(BufferedReader stdin, PrintWriter out) throws IOException {
        String input;

        while ((input = stdin.readLine()) != null) {

            if (gameOver) {
                return;
            }

            String trimmed = input.trim();

            if (trimmed.isEmpty()) {
                continue;
            }

            if (shouldExit(trimmed)) {
                return;
            }

            if (!myTurn) {
                synchronized (consoleLock) {
                    System.out.println("Nie twoja tura. Poczekaj na ruch przeciwnika.");
                }
                continue;
            }

            String coord = trimmed.toUpperCase();

            if (shotCoordinates.contains(coord)) {
                synchronized (consoleLock) {
                    System.out.println("To pole było już ostrzelane. Podaj inną współrzędną:");
                }
                continue;
            }

            out.println(coord);
            shotCoordinates.add(coord);
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

    private void printEndMessage(String result) {
        System.out.printf("""
                    
                    =====================================
                                  KONIEC GRY!
                                    WYNIK:
                                     %s
                    =====================================
                    """, result);
    }

    private void printMissMessage(String coord) {
        System.out.printf("""
                    
                    =================================================
                                          PUDŁO!
                                            %s
                    =================================================
                    """, coord);
    }

    private void printHitMessage(String coord) {
        System.out.printf("""
                    
                    =================================================
                                        TRAFIONY!
                                           %s
                    =================================================
                    """, coord);
    }

    private void printMove(String moveLine) {
        System.out.println(moveLine.replace(';', ' '));
    }

    private boolean shouldExit(String trimmed) {
        return "q".equalsIgnoreCase(trimmed);
    }
}
