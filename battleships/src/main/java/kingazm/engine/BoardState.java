package kingazm.engine;

import kingazm.board.BoardConfig;

public class BoardState {
    private static final char MAST = BoardConfig.MAST;
    private static final char WATER = BoardConfig.WATER;
    private static final char HIT = BoardConfig.HIT;
    private static final char MISS = BoardConfig.MISS;
    private static final char UNKNOWN = BoardConfig.UNKNOWN;
    private static final String TOKEN_HIT = GameConfig.TOKEN_HIT;
    private static final String TOKEN_MISS = GameConfig.TOKEN_MISS;
    private static final String TOKEN_LAST_SUNK = GameConfig.TOKEN_LAST_SUNK;
    private final int rows = BoardConfig.ROWS;
    private final int cols = BoardConfig.COLS;
    private final char[][] currentBoard;

    public BoardState(String map) {
        if (map == null || map.length() != rows * cols) {
            throw new IllegalArgumentException("map must be " + (rows * cols) + " chars");
        }

        currentBoard = new char[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                currentBoard[r][c] = map.charAt(r * cols + c);
            }
        }
    }

    public synchronized String fireAt(int row, int col) {
        char cur = currentBoard[row][col];

        if (cur == MAST) {
            currentBoard[row][col] = HIT;

            if (allSunk()) {
                return TOKEN_LAST_SUNK;
            }
            return TOKEN_HIT;

        } else if (cur == WATER) {
            currentBoard[row][col] = MISS;
            return TOKEN_MISS;
        } else {
            if (cur == HIT) {
                return TOKEN_HIT;
            } else {
                return TOKEN_MISS;
            }
        }
    }

    public synchronized boolean allSunk() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (currentBoard[r][c] == MAST) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public synchronized String maskedView() {
        StringBuilder sb = new StringBuilder();
        sb.append("   ");
        for (int c = 0; c < cols; c++) {
            sb.append((char)('A' + c));
            if (c < cols - 1) {
                sb.append(' ');
            }
        }
        sb.append('\n');

        for (int r = 0; r < rows; r++) {
            String rowLabel = Integer.toString(r + 1);
            if (rowLabel.length() == 1) {
                sb.append(' ');
            }
            sb.append(rowLabel).append(' ');

            for (int c = 0; c < cols; c++) {
                char ch = currentBoard[r][c];
                char out = switch (ch) {
                    case HIT -> HIT;
                    case MISS -> MISS;
                    default -> UNKNOWN;
                };
                sb.append(out);
                if (c < cols - 1) {
                    sb.append(' ');
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public synchronized String revealedView() {
        StringBuilder sb = new StringBuilder();
        sb.append("   ");
        for (int c = 0; c < cols; c++) {
            sb.append((char)('A' + c));
            if (c < cols - 1) {
                sb.append(' ');
            }
        }
        sb.append('\n');

        for (int r = 0; r < rows; r++) {
            String rowLabel = Integer.toString(r + 1);
            if (rowLabel.length() == 1) {
                sb.append(' ');
            }
            sb.append(rowLabel).append(' ');

            for (int c = 0; c < cols; c++) {
                char ch = currentBoard[r][c];
                char out = switch (ch) {
                    case HIT -> MAST;      // Show the ship that was hit
                    case MISS -> WATER;    // Show the water that was hit
                    default -> UNKNOWN;    // Show unknown for places not yet shot
                };
                sb.append(out);
                if (c < cols - 1) {
                    sb.append(' ');
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public synchronized String fullView() {
        StringBuilder sb = new StringBuilder();
        sb.append("   ");
        for (int c = 0; c < cols; c++) {
            sb.append((char)('A' + c));

            if (c < cols - 1) {
                sb.append(' ');
            }
        }
        sb.append('\n');

        for (int r = 0; r < rows; r++) {
            String rowLabel = Integer.toString(r + 1);

            if (rowLabel.length() == 1) {
                sb.append(' ');
            }

            sb.append(rowLabel).append(' ');

            for (int c = 0; c < cols; c++) {
                sb.append(currentBoard[r][c]);
                if (c < cols - 1) {
                    sb.append(' ');
                }
            }

            sb.append('\n');
        }
        return sb.toString();
    }
}