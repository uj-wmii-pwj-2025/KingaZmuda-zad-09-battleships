package kingazm.board;

public interface BoardGenerator {
    BoardConfig config = new BoardConfig();
    String generateMap();

    static BoardGenerator defaultInstance() {
        return new RandomBoardGenerator();
    }

}
