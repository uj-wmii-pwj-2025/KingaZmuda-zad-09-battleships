package kingazm.app;

import kingazm.board.BoardGenerator;

public class Main {
    public static void main(String[] args){
        BoardGenerator board = BoardGenerator.defaultInstance();
        board.generateMap();
        System.out.println(board.generateMap()); //todo: print as a board - 2D
    }
}