package kingazm.board;

import org.junit.jupiter.api.RepeatedTest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

import kingazm.board.BoardGenerator;

public class BattleshipGeneratorTest {

    private static final char MAST = '#';
    private static final char WATER = '.';

    @RepeatedTest(value = 100, name = "Battleship map correctness, test {currentRepetition} of {totalRepetitions}")
    public void battleShipTest() {
        String map = BoardGenerator.defaultInstance().generateMap();
        assertThat(map).withFailMessage("Expected 100 map size, found %d for map:\n%s", map.length(), map).hasSize(100);

        String[] mapLines = cutBy10(map);
        String prettyMap = prettyMap(mapLines);

        var mastCount = map.chars().filter(c -> c == MAST).count();
        assertThat(mastCount).withFailMessage("Expected 20 elements of ships, found %d for map:\n%s", mastCount, prettyMap).isEqualTo(20);
        var waterCount = map.chars().filter(c -> c == WATER).count();
        assertThat(waterCount).withFailMessage("Expected 80 elements of water, found %d for map:\n%s", waterCount, prettyMap).isEqualTo(80);

        Set<List<Point>> ships = prepareShips(mapLines);
        assertThat(ships).withFailMessage("Expected 10 ships, found %d for map:\n%s", ships.size(), prettyMap).hasSize(10);

        var s1 = ships.stream().filter(l -> l.size() == 1).count();
        var s2 = ships.stream().filter(l -> l.size() == 2).count();
        var s3 = ships.stream().filter(l -> l.size() == 3).count();
        var s4 = ships.stream().filter(l -> l.size() == 4).count();
        assertThat(s1).withFailMessage("Expected 4 ships of size 1, found %d for map:\n%s", s1, prettyMap).isEqualTo(4);
        assertThat(s2).withFailMessage("Expected 3 ships of size 2, found %d for map:\n%s", s2, prettyMap).isEqualTo(3);
        assertThat(s3).withFailMessage("Expected 2 ships of size 3, found %d for map:\n%s", s3, prettyMap).isEqualTo(2);
        assertThat(s4).withFailMessage("Expected 1 ships of size 4, found %d for map:\n%s", s4, prettyMap).isEqualTo(1);

        for(var ship: ships) {
            assertThat(onlyWaterAroundShip(ship, mapLines)).withFailMessage("Found corner-touching ships for map:\n%s", prettyMap).isTrue();
        }
    }

    @RepeatedTest(value = 2, name = "Battleship map randomness, test {currentRepetition} of {totalRepetitions}")
    public void checkNotSame() {
        String m1 = BoardGenerator.defaultInstance().generateMap();
        String m2 = BoardGenerator.defaultInstance().generateMap();
        assertThat(m1).isNotEqualTo(m2);
    }

    private String[] cutBy10(String origin) {
        String[] result = new String[10];
        for (int i = 0; i < 10; i++) {
            result[i] = origin.substring(i * 10, i * 10 + 10);
        }
        return result;
    }

    private String prettyMap(String[] map) {
        StringBuilder result = new StringBuilder();
        for (String s : map) {
            result.append(s).append('\n');
        }
        return result.toString();
    }

    private Set<List<Point>> prepareShips(String[] map) {
        Set<List<Point>> ships = new HashSet<>();
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length(); j++) {
                if (map[i].charAt(j) == MAST) {
                    Point p = Point.of(i,j);
                    if (ships.stream().noneMatch(s -> s.contains(p)))
                        ships.add(shipDetected(p, map));
                }
            }
        }
        return ships;
    }

    private List<Point> shipDetected(Point p, String[] map) {
        List<Point> ship = new ArrayList<>();
        ship.add(p);
        checkShipNeighbours(map, ship, 0);
        return ship;
    }

    private void addPoints(List<Point> ship, Point... points) {
        for (var p: points) {
            if (p != null && !ship.contains(p)) ship.add(p);
        }
    }

    private void checkShipNeighbours(String[] map, List<Point> ship, int currentIndex) {
        Point p = ship.get(currentIndex);
        addPoints(ship, checkUp(p, map), checkDown(p, map), checkLeft(p, map), checkRight(p, map));
        if (ship.size() - 1 > currentIndex) {
            checkShipNeighbours(map, ship, currentIndex + 1);
        }
    }

    private Point checkUp(Point p, String[] map) {
        if (p.y() == 0) return null;
        if (map[p.x()].charAt(p.y() - 1) == MAST) return Point.of(p.x(), p.y() - 1);
        else return null;
    }

    private Point checkDown(Point p, String[] map) {
        if (p.y() == 9) return null;
        if (map[p.x()].charAt(p.y() + 1) == MAST) return Point.of(p.x(), p.y() + 1);
        else return null;
    }

    private Point checkLeft(Point p, String[] map) {
        if (p.x() == 0) return null;
        if (map[p.x() - 1].charAt(p.y()) == MAST) return Point.of(p.x() - 1, p.y());
        else return null;
    }

    private Point checkRight(Point p, String[] map) {
        if (p.x() == 9) return null;
        if (map[p.x() + 1].charAt(p.y()) == MAST) return Point.of(p.x() + 1, p.y());
        else return null;
    }

    private boolean onlyWaterAroundShip(List<Point> ship, String[] map) {
        boolean result = true;
        for(var p: ship) {
            List<Point> violated = violatedWaterPoints(
                ship, map, Point.of(p.x()-1, p.y()-1), Point.of(p.x()-1,p.y()), Point.of(p.x()-1,p.y()+1), Point.of(p.x(),p.y()-1), Point.of(p.x(),p.y()+1), Point.of(p.x()+1,p.y()-1), Point.of(p.x()+1,p.y()), Point.of(p.x()+1,p.y()+1));
            if (violated.size() > 0) result = false;
        }
        return result;
    }

    private List<Point> violatedWaterPoints(List<Point> ship, String[] map, Point... pointToCheck) {
        var result = new ArrayList<Point>();
        for (var p: pointToCheck) {
            if (!isPointWater(p, ship, map)) result.add(p);
        }
        return result;
    }

    private boolean isPointWater(Point p, List<Point> ship, String[] map) {
        if (p.x() < 0 || p.x() > 9 || p.y() < 0 || p.y() > 9) return true;
        if (ship.contains(p)) return true;
        return map[p.x()].charAt(p.y()) == WATER;
    }

}

record Point(int x, int y) {
    public static Point of(int i, int j) {
        return new Point(i,j);
    }
}
