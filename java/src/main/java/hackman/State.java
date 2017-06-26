/*
 * Copyright 2017 Steven Lee (stevenwh.lee@gmail.com)
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 *     For the full copyright and license information, please view the LICENSE
 *     file that was distributed with this source code.
 */

package hackman;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * hackman.State
 *
 * @author Steven Lee - stevenwh.lee@gmail.com
 */
public class State {

    private State prevState;
    private Player myPlayer;
    private Player oppPlayer;
    private Map<Integer, Player> players; // id : player

    private static final Set<Point> BUG_ENTRANCE_1 = new HashSet<>(Arrays.asList(
            new Point(9, 6), new Point(9, 7), new Point(8, 7)));
    private static final Set<Point> BUG_ENTRANCE_2 = new HashSet<>(Arrays.asList(
            new Point(10, 6), new Point(10, 7), new Point(11, 7)));

    private int width;
    private int height;
    private String[][] field = null;

    private Map<Integer, Point> playerPositions; // <id, position>
    private Map<Point, Integer> enemyPositions;  // <position, count>
    private Set<Point> snippetPositions;
    private Set<Point> weaponPositions;

    private State() {
        this.players = new HashMap<>(2);
        this.playerPositions  = new HashMap<>();
        this.enemyPositions   = new HashMap<>();
        this.snippetPositions = new HashSet<>();
        this.weaponPositions  = new HashSet<>();
    }

    public State(String field, List<Player> players, State prevState, Game game) {
        this();

        this.prevState = prevState;

        this.width  = game.getFieldWidth();
        this.height = game.getFieldHeight();
        this.field  = new String[this.width][this.height];
        this.parseFromString(field);

        for (Player player : players) {
            int playerId = player.getId();
            player.setPosition(this.playerPositions.get(playerId));
            player.setState(this);

            if (playerId == game.getMyId())
                this.myPlayer = player;
            else
                this.oppPlayer = player;

            this.players.put(playerId, player);
        }
    }

    /**
     * Parses input string from the engine and stores it in this.field.
     * Also stores several interesting points.
     *
     * @param input String input from the engine
     */
    private void parseFromString(String input) {
        if (this.height == 0)
            throw new RuntimeException("height must be defined");
        if (this.width == 0)
            throw new RuntimeException("width must be defined");

        String[] cells = input.split(",");

        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                String cell = cells[x + (y * this.width)];
                this.field[x][y] = cell;

                // Multiple things can be on same position
                for (char c : cell.toCharArray()) {
                    Point position = new Point(x, y);
                    switch (c) {
                        case 'C':
                            this.snippetPositions.add(position);
                            break;
                        case 'E':
                            this.enemyPositions.compute(position, (k, v) -> v != null ? v + 1 : 1);
                            if (BUG_ENTRANCE_1.contains(position) || BUG_ENTRANCE_2.contains(position))
                                this.field[x][y] = "E";
                            break;
                        case 'W':
                            this.weaponPositions.add(position);
                            break;
                        default:
                            if (Character.isDigit(c)) {
                                int id = Character.getNumericValue(c);
                                this.playerPositions.put(id, position);
                            }
                    }
                }
            }
        }
    }

    public Player getPlayer(int playerId) {
        return this.players.get(playerId);
    }

    public Player getMyPlayer() {
        return this.myPlayer;
    }

    //public Player getOpponentPlayer() {
    //    return this.oppPlayer;
    //}

    public Map<Point, Integer> getEnemyPositions() {
        return this.enemyPositions;
    }

    public Map<Point, Integer> getPreviousEnemyPositions() {
        Map<Point, Integer> prevEnemyPositions = new HashMap<>();

        if (this.prevState != null) {
            Map<Point, Integer> enemies = this.prevState.getEnemyPositions();
            prevEnemyPositions.putAll(enemies);
        }
        return prevEnemyPositions;
    }

    public Set<Point> getSnippetPositions() {
        return this.snippetPositions;
    }

    public Set<Point> getWeaponPositions() {
        return this.weaponPositions;
    }

    /**
     * Return a list of valid moves for my bot, i.e. moves does not bring
     * player outside the field or inside a wall
     *
     * @param p The point of origin
     * @return A list of valid moves from the given point
     */
    public Set<Move> getValidMoves(Point p) {
        Set<Move> validMoves = new HashSet<>();
        int x = p.x;
        int y = p.y;

        Point up    = new Point(x, y - 1);
        Point down  = new Point(x, y + 1);
        Point left  = new Point(x - 1, y);
        Point right = new Point(x + 1, y);

        if (isPointValid(up))    validMoves.add(Move.UP);
        if (isPointValid(down))  validMoves.add(Move.DOWN);
        if (isPointValid(left))  validMoves.add(Move.LEFT);
        if (isPointValid(right)) validMoves.add(Move.RIGHT);

        return validMoves;
    }

    /**
     * Returns whether a point on the field is valid to stand on.
     *
     * @param p A point to test
     * @return True if point is valid to stand on, false otherwise
     */
    public boolean isPointValid(Point p) {
        // Special handling for the bugs source (which is marked as a wall)
        if (BUG_ENTRANCE_1.contains(p)) {
            return this.enemyPositions.keySet().stream()
                    .anyMatch(e -> BUG_ENTRANCE_1.contains(e));
        }
        else if (BUG_ENTRANCE_2.contains(p)) {
            return this.enemyPositions.keySet().stream()
                    .anyMatch(e -> BUG_ENTRANCE_2.contains(e));
        }

        int x = p.x;
        int y = p.y;

        return x >= 0 && x < this.width && y >= 0 && y < this.height &&
                !this.field[x][y].contains("x");
    }

    /**
     * Does a breadth-first search to find an optimal path from the given
     * origin to each of the targets. The resulting paths will never pass
     * through any of the points to avoid.
     *
     * @param origin      The starting position (e.g. my current position)
     * @param targets     The set of end positions to aim for (e.g. snippet positions)
     * @param avoid       The set of positions to avoid (e.g. threats)
     * @param threatsAllowed The number of encounters with threats that the player is allowed
     * @param searchWhile A predicate that defines the condition for when the search can continue
     * @return A list of Paths to each of the targets. The list is in increasing order of distance.
     */
    public List<Path> findShortestPaths(Point origin, Set<Point> targets, Map<Point, Integer> avoid, int threatsAllowed, Predicate<Path> searchWhile) {
        // Parameter defaults
        if (targets == null)
            targets = new HashSet<>();
        if (avoid == null)
            avoid = new HashMap<>();
        if (searchWhile == null)
            searchWhile = (p -> true);

        List<Path> paths          = new ArrayList<>();
        Queue<Path> queue         = new LinkedList<>();
        Queue<Integer> encounters = new LinkedList<>();
        Set<Point> visited        = new HashSet<>();

        queue.add(new Path(origin));
        encounters.add(0); // keep a count of encounters in parallel with items in queue
        visited.add(origin);

        // Do a breadth-first search
        search:
        while (!queue.isEmpty()) {
            Path path = queue.remove();
            int unavoided = encounters.remove();
            Set<Move> validMoves = this.getValidMoves(path.end());

            for (Move nextMove : validMoves) {
                Point nextPosition = new Point(path.end(), nextMove);
                int nrThreats = avoid.getOrDefault(nextPosition, 0);
                Path nextPath = new Path(path, nextPosition, nextMove, nrThreats, this);

                if (!searchWhile.test(nextPath)) {
                    paths.add(path);
                    continue search;
                }

                if (visited.contains(nextPosition))
                    continue;

                int newUnavoided = unavoided;
                if (avoid.containsKey(nextPosition)) {
                    if (newUnavoided + avoid.get(nextPosition) <= threatsAllowed)
                        newUnavoided += avoid.get(nextPosition);
                    else
                        continue; // We've reached a position to avoid - Don't add this path
                }

                if (targets.contains(nextPosition))
                    paths.add(nextPath);

                visited.add(nextPosition);
                queue.add(nextPath);
                encounters.add(newUnavoided);
            }
        }
        return paths;
    }

    /**
     * In addition to findShortestPath(), this method considers the shortest
     * paths to each target for each move direction. Effectively, it will
     * produce alternative paths to the targets.
     *
     * @param origin      The starting position (e.g. my current position)
     * @param targets     The set of end positions to aim for (e.g. snippet positions)
     * @param avoid       The set of positions to avoid (e.g. threats)
     * @param threatsAllowed The number of encounters with threats that the player is allowed
     * @param searchWhile A predicate that defines the condition for when the search can continue
     * @return A list of Paths to the targets. Each target may have multiple paths. The list is in increasing order of distance.
     */

    List<Path> findShortestPathsPerDirection(Point origin, Set<Point> targets, Map<Point, Integer> avoid, int threatsAllowed, Predicate<Path> searchWhile) {
        // Parameter defaults
        if (targets == null)
            targets = new HashSet<>();
        if (avoid == null)
            avoid = new HashMap<>();
        if (searchWhile == null)
            searchWhile = (p -> true);

        List<Path> allPaths = new ArrayList<>();

        Set<Point> nextPositions = this.getValidMoves(origin).stream()
                .map(move -> new Point(origin, move))
                .collect(Collectors.toSet());

        for (Point pos : nextPositions) {
            Map<Point, Integer> avoid1 = new HashMap<>(avoid);

            // Avoid all other directions so we can consider the paths to each target if we move this way
            for (Point wantToAvoid : nextPositions) {
                if (wantToAvoid.equals(pos))
                    continue;

                if (avoid1.containsKey(wantToAvoid))
                    avoid1.compute(wantToAvoid, (k, v) -> v + 999);
                else
                    avoid1.put(wantToAvoid, 999);
            }

            List<Path> paths = this.findShortestPaths(origin, targets, avoid1, threatsAllowed, searchWhile);
            allPaths.addAll(paths);
        }

        allPaths.sort(Comparator.comparing(Path::nrMoves).thenComparing(Path::nrThreats));

        return allPaths;
    }

    /**
     * Returns a string representation of the field that can be printed
     *
     * @return String representation of this field
     */
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();

        // x-axis labels
        output.append("    ");
        for (int x = 0; x < this.width; x++) {
            // for readability, skip every 2nd label when values are 2-digits
            String s = (x < 10 || x % 2 == 1) ? String.format("%2d", x) : "  ";
            output.append(s);
        }
        output.append("\n");

        // top border
        output.append("    ");
        for (int x = 0; x < this.width; x++)
            output.append("--");
        output.append("-\n");

        for (int y = 0; y < this.height; y++) {
            output.append(String.format("%2d |", y)); // y-axis labels & left border

            for (int x = 0; x < this.width; x++) {
                output.append(" " + this.field[x][y]);
            }
            output.append(" |\n"); // right border
        }

        // bottom border
        output.append("    ");
        for (int x = 0; x < this.width; x++)
            output.append("--");
        output.append("-\n");

        return output.toString();
    }
}
