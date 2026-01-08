package kingazm.net;

import kingazm.engine.GameLoop;
import kingazm.engine.Player;
import kingazm.engine.ConsoleView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private static final int MAX_COMMUNICATION_FAILURES = 3;
    private static final String STATUS_YOUR_TURN = "status;twoja tura";
    private static final String STATUS_WAIT = "status;czekaj";
    private static final String INFO_WAIT = "info;oczekiwanie na ruch przeciwnika";

    private int port;
    private final ConcurrentLinkedQueue<Player> waitingPlayers = new ConcurrentLinkedQueue<>();
    private final ConcurrentMap<String, GameLoop> gameSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> clientToSession = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> playerReady = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> sessionStarted = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> clientTerminated = new ConcurrentHashMap<>();

    public Server(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        logger.info("server starting on port: " + port);
        ExecutorService executor = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                executor.submit(() -> {
                    try {
                        handleClient(socket);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "error handling client", e);
                    }
                });
            }
        } finally {
            executor.shutdown();
            logger.info("server stopped");
        }
    }

    private void handleClient(Socket socket) {
        String clientId = UUID.randomUUID().toString();
        logger.info("client connected: " + socket.getRemoteSocketAddress() + " -> " + clientId);

        try (Socket s = socket;
             InputStream in = s.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {

            Player me = new Player(clientId, out);
            initializeSession(clientId, me);
            handleGameLoop(clientId, reader, out);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "I/O error with client " + clientId, e);
        } finally {
            cleanupClientSession();
        }
    }

    private void initializeSession(String clientId, Player me) {
        Player peer = waitingPlayers.poll();
        if (peer == null) {
            waitingPlayers.add(me);
            logger.info("client waiting: " + clientId);
            me.send("czekaj;" + clientId);
        } else {
            startGameSession(me, peer);
        }
    }

    private void startGameSession(Player me, Player peer) {
        String sessionId = UUID.randomUUID().toString();
        GameLoop game = new GameLoop(me, peer);

        gameSessions.put(sessionId, game);
        clientToSession.put(me.getId(), sessionId);
        clientToSession.put(peer.getId(), sessionId);
        playerReady.put(me.getId(), false);
        playerReady.put(peer.getId(), false);
        sessionStarted.put(sessionId, true);

        logger.info("paired " + me.getId() + " with " + peer.getId() + " in " + sessionId);

        me.send("start;" + sessionId + ";" + me.getId());
        peer.send("start;" + sessionId + ";" + peer.getId());
        sendInitialGameState(game, me, peer);
    }

    private void sendInitialGameState(GameLoop game, Player me, Player peer) {
        me.send("moja plansza;\n" + Objects.toString(game.getBoardFor(me.getId()), ""));
        me.send("plansza przeciwnika;\n" + Objects.toString(game.getMaskedOpponentView(me.getId()), ""));
        peer.send("moja plansza;\n" + Objects.toString(game.getBoardFor(peer.getId()), ""));
        peer.send("plansza przeciwnika;\n" + Objects.toString(game.getMaskedOpponentView(peer.getId()), ""));

        broadcastStartMessage(me, peer);
        renderAndSendUi(game, me, peer);
        sendTurnNotification(game, me, peer);
    }

    private void renderAndSendUi(GameLoop game, Player me, Player peer) {
        try {
            ConsoleView view = new ConsoleView(game);
            String viewA = view.renderFor(me.getId());
            String viewB = view.renderFor(peer.getId());

            for (String l : viewA.split("\\n")) {
                me.send("UI;" + l);
            }
            for (String l : viewB.split("\\n")) {
                peer.send("UI;" + l);
            }
        } catch (Exception ignored) {}
    }

    private void sendTurnNotification(GameLoop game, Player me, Player peer) {
        String currentTurn = game.getCurrentTurn();
        me.send("tura;" + currentTurn);
        peer.send("tura;" + currentTurn);

        if (currentTurn.equals(me.getId())) {
            me.send(STATUS_YOUR_TURN);
            peer.send(STATUS_WAIT);
            peer.send(INFO_WAIT);
        } else {
            me.send(STATUS_WAIT);
            me.send(INFO_WAIT);
            peer.send(STATUS_YOUR_TURN);
        }
    }

    private void handleGameLoop(String clientId, BufferedReader reader, PrintWriter out) throws IOException {
        int communicationFailures = 0;
        String line;

        while ((line = reader.readLine()) != null) {
            if (clientTerminated.getOrDefault(clientId, false)) {
                logger.info("terminating handler for client: " + clientId + " (session ended)");
                break;
            }

            String sessionId = clientToSession.get(clientId);
            if (sessionId == null) {
                continue;
            }

            GameLoop game = gameSessions.get(sessionId);
            if (game == null) {
                out.println("error: no active session");
                continue;
            }

            String normalized = normalizeInput(line);
            if (normalized == null) {
                continue;
            }

            String moveCoord = handleStartCommandIfPresent(clientId, normalized);
            if (moveCoord == null && normalized.toLowerCase().startsWith("start")) {
                continue;
            }

            String coord = moveCoord != null ? moveCoord : normalized;
            String[] outcome = game.applyMove(clientId, coord);
            String status = getStatus(outcome);

            if ("odrzucono".equals(status)) {
                communicationFailures = handleRejectedMove(out, clientId, communicationFailures, outcome);
                continue;
            }

            if (!"zaakceptowano".equals(status)) {
                communicationFailures = handleCommunicationError(out, clientId, communicationFailures, "Błąd: Nieprawdłowa odpowiedź");
                continue;
            }

            if (outcome.length < 4) {
                communicationFailures = handleCommunicationError(out, clientId, communicationFailures, "Błąd: Nieprawidłowa długość odpowiedzi");
                continue;
            }

            communicationFailures = 0;
            processMoveOutcome(game, outcome);
        }
    }

    private String normalizeInput(String line) {
        if (line == null) {
            return null;
        }

        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.toLowerCase().startsWith("ping")) {
            return null;
        }

        return trimmed;
    }

    private String handleStartCommandIfPresent(String clientId, String input) {
        if (!input.toLowerCase().startsWith("start")) {
            return input;
        }

        String coord = extractCoordinate(input);
        playerReady.put(clientId, true);
        notifyGameStartIfReady(clientId);

        return coord.isEmpty() ? null : coord;
    }

    private String extractCoordinate(String input) {
        int idx = input.indexOf(';');
        return (idx >= 0 && idx + 1 < input.length()) ? input.substring(idx + 1).trim() : "";
    }

    private void notifyGameStartIfReady(String clientId) {
        String sessionId = clientToSession.get(clientId);
        if (sessionId == null || sessionStarted.getOrDefault(sessionId, false)) {
            return;
        }

        GameLoop game = gameSessions.get(sessionId);
        if (game == null) {
            return;
        }

        sessionStarted.put(sessionId, true);
        Player playerA = game.getPlayerA();
        Player playerB = game.getPlayerB();

        if (playerA != null) playerA.send("info;gra rozpoczęta");
        if (playerB != null) playerB.send("info;gra rozpoczęta");

        String currentTurn = game.getCurrentTurn();
        if (playerA != null) playerA.send("tura;" + currentTurn);
        if (playerB != null) playerB.send("tura;" + currentTurn);

        if (playerA != null && playerB != null) {
            if (currentTurn.equals(playerA.getId())) {
                playerA.send(STATUS_YOUR_TURN);
                playerB.send(STATUS_WAIT);
            } else {
                playerA.send(STATUS_WAIT);
                playerB.send(STATUS_YOUR_TURN);
            }
        }
    }

    private String getStatus(String[] outcome) {
        return (outcome != null && outcome.length > 0 && outcome[0] != null)
                ? outcome[0].toLowerCase()
                : "";
    }

    private int handleRejectedMove(PrintWriter out, String clientId, int failures, String[] outcome) {
        String reason = outcome.length > 1 ? outcome[1] : "odrzucono";
        String lowerReason = reason == null ? "" : reason.toLowerCase();

        if (lowerReason.contains("nie") && lowerReason.contains("twoja")) {
            out.println("\nNie twoja tura. Poczekaj na ruch przeciwnika.");
        } else if (lowerReason.contains("pole")) {
            out.println("\nTo pole już zostało zestrzelone. Podaj inną współrzędną.");
        } else if (lowerReason.contains("nieprawidlowe")) {
            out.println("\nNieprawidłowe współrzędne. Spróbuj ponownie.");
        } else {
            out.println("\nBłąd: " + reason);
        }

        return handleCommunicationError(out, clientId, failures, null);
    }

    private int handleCommunicationError(PrintWriter out, String clientId, int failures, String message) {
        int nextFailures = failures + 1;
        if (message != null) {
            out.println(message);
        }

        if (nextFailures >= MAX_COMMUNICATION_FAILURES) {
            out.println("\nBłąd komunikacji");
            logger.info("comm failures >= " + MAX_COMMUNICATION_FAILURES + " for client: " + clientId + " - exiting server");
            System.exit(1);
        }

        return nextFailures;
    }

    private void processMoveOutcome(GameLoop game, String[] outcome) {
        String attacker = outcome[1];
        String coord = outcome[2];
        String nextTurn = outcome[3];
        String resultToken = outcome.length > 4 ? outcome[4] : "";

        Player playerA = game.getPlayerA();
        Player playerB = game.getPlayerB();

        if (playerA == null || playerB == null) {
            return;
        }

        Player attackingPlayer = playerA.getId().equals(attacker) ? playerA : playerB;
        Player defendingPlayer = attackingPlayer == playerA ? playerB : playerA;

        String rt = resultToken == null ? "" : resultToken;

        if ("ostatni_zatopiony".equals(rt)) {
            handleGameEnd(game, attackingPlayer, defendingPlayer);
            return;
        }

        String command = ("trafiony".equals(rt) ? "trafiony" : "pudło");
        attackingPlayer.send(command + ";" + coord);
        defendingPlayer.send(command + ";" + coord);

        updateBoardsAndTurn(game, playerA, playerB, nextTurn);
    }

    private void handleGameEnd(GameLoop game, Player attacker, Player defender) {
        attacker.send("ostatni zatopiony");
        defender.send("ostatni zatopiony");

        attacker.send("wynik;wygrana");
        defender.send("wynik;przegrana");

        try {
            ConsoleView view = new ConsoleView(game);
            String viewA = view.renderFor(attacker.getId());
            String viewB = view.renderFor(defender.getId());

            for (String l : viewA.split("\n")) {
                attacker.send("UI;" + l);
            }
            for (String l : viewB.split("\n")) {
                defender.send("UI;" + l);
            }
        } catch (Exception ignored) {
            attacker.send("plansza przeciwnika\n" + Objects.toString(game.getMaskedOpponentView(attacker.getId()), ""));
            defender.send("plansza przeciwnika\n" + Objects.toString(game.getMaskedOpponentView(defender.getId()), ""));
            attacker.send("moja plansza\n" + Objects.toString(game.getBoardFor(attacker.getId()), ""));
            defender.send("moja plansza\n" + Objects.toString(game.getBoardFor(defender.getId()), ""));
        }

        cleanupGameSession(game);
    }

    private void updateBoardsAndTurn(GameLoop game, Player playerA, Player playerB, String nextTurn) {
        try {
            ConsoleView view = new ConsoleView(game);
            String viewA = view.renderFor(playerA.getId());
            String viewB = view.renderFor(playerB.getId());

            for (String l : viewA.split("\\n")) {
                playerA.send("UI;" + l);
            }
            for (String l : viewB.split("\\n")) {
                playerB.send("UI;" + l);
            }
        } catch (Exception ignored) {}

        Player nextPlayer = playerA.getId().equals(nextTurn) ? playerA : playerB;
        Player otherPlayer = (nextPlayer == playerA) ? playerB : playerA;

        if (nextPlayer != null) {
            nextPlayer.send("tura;" + nextTurn);
            nextPlayer.send(STATUS_YOUR_TURN);
        }

        if (otherPlayer != null) {
            otherPlayer.send(STATUS_WAIT);
            otherPlayer.send(INFO_WAIT);
        }
    }

    private void cleanupGameSession(GameLoop game) {
        Player playerA = game.getPlayerA();
        Player playerB = game.getPlayerB();

        String sessionId = clientToSession.get(playerA.getId());
        if (sessionId != null) {
            gameSessions.remove(sessionId);
            clientToSession.remove(playerA.getId());
            clientToSession.remove(playerB.getId());
            sessionStarted.remove(sessionId);
            playerReady.remove(playerA.getId());
            playerReady.remove(playerB.getId());

            clientTerminated.put(playerA.getId(), true);
            clientTerminated.put(playerB.getId(), true);

            closeWriterQuietly(playerA);
            closeWriterQuietly(playerB);

            logger.info("Session " + sessionId + " finished. Closed session and notified clients: " + playerA.getId() + ", " + playerB.getId());
        }
    }

    private void closeWriterQuietly(Player player) {
        try {
            if (player.getWriter() != null) {
                player.getWriter().close();
            }
        } catch (Exception ignored) {}
    }

    private void cleanupClientSession() {
        waitingPlayers.removeIf(p -> {
            PrintWriter writer = p.getWriter();
            try {
                return writer == null || writer.checkError();
            } catch (Exception e) {
                return true;
            }
        });
    }

    private void handleArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("-port".equals(args[i]) && (i + 1) < args.length) {
                try {
                    port = Integer.parseInt(args[++i]);
                } catch (NumberFormatException e) {
                    logger.warning("invalid port: " + args[i]);
                }
            } else {
                logger.warning("unknown argument: " + args[i]);
            }
        }
    }

    private void broadcast(Player me, Player peer, String message) {
        me.send(message);
        peer.send(message);
    }

    private void broadcastStartMessage(Player me, Player peer) {
        broadcast(me, peer, """





                            ╔═══════════════════════════════════════════════╗
                            ║                   START GRY                   ║
                            ╚═══════════════════════════════════════════════╝
                            """);
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server(12345);
        server.handleArgs(args);
        server.start();
    }
}