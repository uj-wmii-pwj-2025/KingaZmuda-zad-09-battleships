package kingazm.board;

public interface BoardGenerator {
    String generateMap();

    static BoardGenerator defaultInstance() {
        return new RandomBoardGenerator();
    }

}
