package kingazm.engine;

import kingazm.board.BoardConfig;
import kingazm.board.BoardGenerator;

import java.util.UUID;

/**
 * Game session holder: two players, their boards and writers, and whose turn it is 
 * (as in task description, sequential turns are enforced).
 */
public class GameLoop {
    private final String sessionId = UUID.randomUUID().toString();
    private final Player playerA;
    private final Player playerB;
    private String currentTurn;

    private record Players(Player attacker, Player defender) {}
    private record Position(int row, int col) {}

    public GameLoop(Player playerA, Player playerB) {
        this.playerA = playerA;
        this.playerB = playerB;

        String mapA = BoardGenerator.defaultInstance().generateMap();
        String mapB = BoardGenerator.defaultInstance().generateMap();
        this.playerA.setBoard(new BoardState(mapA));
        this.playerB.setBoard(new BoardState(mapB));

        this.currentTurn = playerA.getId();
    }

    public String getSessionId() {
        return sessionId;
    }

    public Player getPlayerA() {
        return playerA;
    }

    public Player getPlayerB() {
        return playerB;
    }

    public String getBoardFor(String playerId) {
        Player p = playerA.getId().equals(playerId) ? playerA : playerB;
        BoardState bs = p.getBoard();
        return bs == null ? null : bs.fullView();
    }

    public String getMaskedOpponentView(String playerId) {
        Player opponent = playerA.getId().equals(playerId) ? playerB : playerA;
        BoardState bs = opponent.getBoard();
        return bs == null ? null : bs.maskedView();
    }

    public String getCurrentTurn() {
        return currentTurn;
    }


    public synchronized String[] applyMove(String clientId, String rawCoord) {

        String coord = normalizeCoord(rawCoord);
        if (coord == null) {
            return reject("nieprawidlowe wspolrzedne");
        }

        if (!isPlayersTurn(clientId)) {
            return reject("nie twoja tura");
        }

        Position pos = parsePosition(coord);
        if (pos == null) {
            return reject("nieprawidlowe wspolrzedne");
        }

        Players players = resolvePlayers(clientId);
        if (players == null) {
            return reject("nieprawidlowa sesja");
        }

        String boardResult = fire(players.defender(), pos);
        if (boardResult == null) {
            return reject("nieprawidlowe wspolrzedne");
        }

        String nextTurnId = players.defender().getId();
        this.currentTurn = nextTurnId;

        return accept(players.attacker(), coord, boardResult, nextTurnId);
    }


    private String normalizeCoord(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isPlayersTurn(String clientId) {
        String curTurn = getCurrentTurn();
        return curTurn != null && curTurn.equals(clientId);
    }

    private Position parsePosition(String coord) {
        try {
            char letter = coord.charAt(0);
            int col = Character.toUpperCase(letter) - 'A';
            int row = Integer.parseInt(coord.substring(1).trim()) - 1;

            if (col < 0 || col >= BoardConfig.COLS
                || row < 0 || row >= BoardConfig.ROWS) {
                return null;
            }

            return new Position(row, col);
        } catch (Exception e) {
            return null;
        }
    }

    private Players resolvePlayers(String clientId) {
        Player a = getPlayerA();
        Player b = getPlayerB();

        if (a == null || b == null) return null;

        Player attacker = a.getId().equals(clientId) ? a : b;
        Player defender = attacker == a ? b : a;

        if (defender.getBoard() == null) return null;

        return new Players(attacker, defender);
    }

    private String fire(Player defender, Position pos) {
        try {
            return defender.getBoard().fireAt(pos.row(), pos.col());
        } catch (Exception e) {
            return null;
        }
    }

    private void advanceTurn(Players players) {
        Player attacker = players.attacker();
        Player defender = players.defender();
        this.currentTurn = defender.getId();
    }

    private String mapResult(String boardResult) {
        String token = boardResult == null ? "" : boardResult.toLowerCase();

        return switch (token) {
            case "ostatni_zatopiony", "last_sunk" -> "ostatni_zatopiony";
            case "trafiony", "hit"                -> "trafiony";
            default                               -> "pudlo";
        };
    }

    private String[] reject(String reason) {
        return new String[] { "odrzucono", reason };
    }

    private String[] accept(
            Player attacker,
            String coord,
            String boardResult,
            String nextTurnId
    ) {
        return new String[] {
                "zaakceptowano",
                attacker.getId(),
                coord.toUpperCase(),
                nextTurnId,
                mapResult(boardResult)
        };
    }
}