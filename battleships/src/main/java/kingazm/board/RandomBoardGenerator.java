package kingazm.board;

import java.util.Random;

public class RandomBoardGenerator implements BoardGenerator {
    private final char MAST = BoardConfig.MAST;
    private final char WATER = BoardConfig.WATER;
    private final int NUM_OF_COLS;
    private final int NUM_OF_ROWS;
    char[][] board;
    private final Random random;
    int[] shipSizes;

    RandomBoardGenerator() {
        this(10, 10, new int[]{4, 3, 3, 2, 2, 2, 1, 1, 1, 1});
    }

    RandomBoardGenerator(int numOfCols, int numOfRows, int[] shipSizes) {
        NUM_OF_COLS = numOfCols;
        NUM_OF_ROWS = numOfRows;
        board = new char[numOfCols][numOfRows];
        random = new Random();
        this.shipSizes = shipSizes;
        restartBoard();
    }

    @Override
    public String generateMap() {
        for (int shipSize : shipSizes) {
            boolean notPlaced = true;
            int numOfAttempts = 0;

            while (notPlaced) {
                int[] directions = getRandomDirections();
                int dx = directions[0];
                int dy = directions[1];
                int x = getRandomXCoordinate();
                int y = getRandomYCoordinate();

                if (canPlaceShip(shipSize, x, y, dx, dy)) {
                    placeShip(shipSize, x, y, dx, dy);
                    notPlaced = false;
                }

                numOfAttempts += 1;

                if (numOfAttempts > 10000) {
                    restartBoard();
                    return generateMap();
                }
            }
        }
        return boardToString();
    }

    private void placeShip(int shipSize, int x, int y, int dx, int dy) {
        for (int i = 0; i < shipSize; i++) {
            int ix = x + i * dx;
            int iy = y + i * dy;
            board[ix][iy] = MAST;
        }
    }

    private boolean canPlaceShip(int shipSize, int x, int y, int dx, int dy) {
        for (int i = 0; i < shipSize; i++) {
            int ix = x + i * dx;
            int iy = y + i * dy;

            if (!isWithinBounds(ix, iy)) {
                return false;
            }

            if (board[ix][iy] == MAST) {
                return false;
            }

            if (!hasSpaceAround(ix, iy)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasSpaceAround(int x, int y) {
        int dx = -1;

        while (dx <= 1) {
            int dy = -1;
            while (dy <= 1) {
                int ix = x + dx;
                int iy = y + dy;

                if (isWithinBounds(ix, iy)) {
                    if (board[ix][iy] == MAST) {
                        return false;
                    }
                }
                dy += 1;
            }
            dx += 1;
        }
        return true;
    }

    private int[] getRandomDirections() {
        int[][] directions = {{1,0}, {0,1}};
        int axisX = directions[random.nextInt(0,1)][0];
        int axisY = directions[random.nextInt(0,1)][1];

        return new int[]{axisX, axisY};
    }

    private int getRandomXCoordinate() {
        return random.nextInt(NUM_OF_COLS);
    }

    private int getRandomYCoordinate() {
        return random.nextInt(NUM_OF_ROWS);
    }
    private String boardToString() {
        StringBuilder boardSB = new StringBuilder(NUM_OF_COLS * NUM_OF_ROWS);
        for (int i = 0; i < NUM_OF_COLS; i++) {
            for (int j = 0; j < NUM_OF_ROWS; j++) {
                boardSB.append(board[i][j]);
            }
        }
        return boardSB.toString();
    }

    private void restartBoard() {
        for (char[] col : board) {
            for (int i = 0; i < NUM_OF_ROWS; i++) {
                col[i] = WATER;
            }
        }
    }

    private boolean isWithinBounds(int x, int y) {
        return (x >= 0 && x < NUM_OF_COLS && y >= 0 && y < NUM_OF_ROWS);
    }

    public void printBoard() {
        for (int i = 0; i < NUM_OF_COLS; i++) {
            for (int j = 0; j < NUM_OF_ROWS; j++) {
                System.out.print(board[i][j]);
            }
            System.out.println();
        }
    }   
}
