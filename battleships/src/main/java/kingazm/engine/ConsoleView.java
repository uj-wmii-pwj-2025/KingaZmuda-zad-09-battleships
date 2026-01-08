package kingazm.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple console view wrapper that renders a player's own full board
 * and the opponent's masked board side-by-side for a prettier console UI.
 * This is a non-invasive helper and does not change game logic.
 */
public class ConsoleView {
    private final GameLoop gameLoop;

    public ConsoleView(GameLoop gameLoop) {
        this.gameLoop = gameLoop;
    }

    /**
     * Render a combined view for the given player id.
     */
    public String renderFor(String playerId) {
        String myFull = gameLoop.getBoardFor(playerId);
        String opponentRevealed = gameLoop.getRevealedOpponentView(playerId);

        if (myFull == null) myFull = "(brak planszy)";
        if (opponentRevealed == null) opponentRevealed = "(brak planszy)";

        String[] leftLines = myFull.split("\n");
        String[] rightLines = opponentRevealed.split("\n");

        int leftWidth = 0;
        for (String s : leftLines) leftWidth = Math.max(leftWidth, s.length());

        List<String> out = getStrings(rightLines, leftLines, leftWidth);

        return String.join("\n", out) + "\n\n\n";
    }

    private static List<String> getStrings(String[] rightLines, String[] leftLines, int leftWidth) {
        int rightWidth = 0;
        for (String s : rightLines) rightWidth = Math.max(rightWidth, s.length());

        int maxLines = Math.max(leftLines.length, rightLines.length);

        List<String> out = new ArrayList<>();
        out.add("");
        out.add(String.format("%-" + leftWidth + "s    %s", "     Twoja plansza", "  Plansza przeciwnika"));
        out.add("\n");

        for (int i = 0; i < maxLines; i++) {
            String left = i < leftLines.length ? leftLines[i] : "";
            String right = i < rightLines.length ? rightLines[i] : "";

            String paddedLeft = String.format("%-" + leftWidth + "s", left);
            out.add(paddedLeft + "    " + right);
        }
        return out;
    }
}
