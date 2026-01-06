package kingazm.engine;

import java.io.PrintWriter;
import java.util.Objects;

public class Player {
    private final String id;
    private final PrintWriter writer;
    private BoardState board;

    public Player(String id, PrintWriter writer) {
        this.id = Objects.requireNonNull(id, "id");
        this.writer = writer;
    }

    public String getId() {
        return id;
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public synchronized BoardState getBoard() {
        return board;
    }

    public synchronized void setBoard(BoardState board) {
        this.board = board;
    }

    public synchronized void send(String msg) {
        if (writer != null) {
            writer.println(msg);
        }
    }

    @Override
    public String toString() {
        return "Player{" + id + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Player)) {
            return false;
        }

        return id.equals(((Player) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}