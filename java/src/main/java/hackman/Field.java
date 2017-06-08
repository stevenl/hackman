/*
 * Copyright 2016 riddles.io (developers@riddles.io)
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

/**
 * hackman.Field
 *
 * Stores all information about the playing field and
 * contains methods to perform calculations about the field
 *
 * @author Jim van Eeden - jim@riddles.io
 */
class Field {

    private static final Set<Point> BUG_ENTRANCE_1 = new HashSet<>(Arrays.asList(
            new Point(9, 6), new Point(9, 7), new Point(8, 7)));
    private static final Set<Point> BUG_ENTRANCE_2 = new HashSet<>(Arrays.asList(
            new Point(10, 6), new Point(10, 7), new Point(11, 7)));

    private int width;
    private int height;
    private int myId;
    private int opponentId;

    private String[][] field = null;
    private Map<Integer, Point> playerPositions; // <id, position>
    private Set<Point> enemyPositions;
    private Set<Point> snippetPositions;
    private Set<Point> weaponPositions;

    private Field() {
        this.playerPositions  = new HashMap<>();
        this.enemyPositions   = new HashSet<>();
        this.snippetPositions = new HashSet<>();
        this.weaponPositions  = new HashSet<>();
    }

    Field(int width, int height, String field) {
        this();
        this.width  = width;
        this.height = height;

        initField();
        this.parseFromString(field);
    }

    /**
     * Initializes field
     */
    void initField() {
        if (this.width == 0)
            throw new IllegalStateException("Field 'width' has not been set");
        if (this.height == 0)
            throw new IllegalStateException("Field 'height' has not been set");

        this.field = new String[this.width][this.height];
        clearField();
    }

    /**
     * Clears the field
     */
    private void clearField() {
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                this.field[x][y] = "";
            }
        }

        this.playerPositions.clear();
        this.enemyPositions.clear();
        this.snippetPositions.clear();
        this.weaponPositions.clear();
    }

    /**
     * Parses input string from the engine and stores it in this.field.
     * Also stores several interesting points.
     * @param input String input from the engine
     */
    void parseFromString(String input) {
        if (this.field == null)
            initField();
        else
            clearField();

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
                            this.enemyPositions.add(position);
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

    private String unparseToString() {
        StringBuilder str = new StringBuilder(this.width * this.height * 2 - 1);

        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                str.append(this.field[x][y]);
                if (y != this.height - 1 || x != this.width - 1)
                    str.append(",");
            }
        }
        return str.toString();
    }

    /**
     * Returns a string representation of the field that can be printed
     * @return String representation of the current field
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

    /**
     * Return a list of valid moves for my bot, i.e. moves does not bring
     * player outside the field or inside a wall
     * @return A list of valid moves
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
     * @param p Point to test
     * @return True if point is valid to stand on, false otherwise
     */
    public boolean isPointValid(Point p) {
        // Special handling for the bugs source (which is marked as a wall)
        if (BUG_ENTRANCE_1.contains(p)) {
            return this.enemyPositions.stream().anyMatch(e -> BUG_ENTRANCE_1.contains(e));
        }
        else if (BUG_ENTRANCE_2.contains(p)) {
            return this.enemyPositions.stream().anyMatch(e -> BUG_ENTRANCE_2.contains(e));
        }

        int x = p.x;
        int y = p.y;

        return x >= 0 && x < this.width && y >= 0 && y < this.height &&
                !this.field[x][y].contains("x");
    }

    public void setMyId(int id) {
        this.myId = id;
    }

    public void setOpponentId(int id) {
        this.opponentId = id;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Map<Integer, Point> getPlayerPositions() {
        return this.playerPositions;
    }

    public Point getPlayerPosition(int playerId) {
        return playerPositions.get(playerId);
    }

    public Point getMyPosition() {
        return this.playerPositions.get(this.myId);
    }

    public Point getOpponentPosition() {
        return this.playerPositions.get(this.opponentId);
    }

    public Set<Point> getEnemyPositions() {
        return this.enemyPositions;
    }

    public Set<Point> getSnippetPositions() {
        return this.snippetPositions;
    }

    public Set<Point> getWeaponPositions() {
        return this.weaponPositions;
    }

    /**
     * Does a breadth-first search to find an optimal path from the given
     * origin to each of the targets. The resulting paths will never pass
     * through any of the points to avoid.
     *
     * @param origin The starting position (e.g. my current position)
     * @param targets The set of end positions to aim for (e.g. snippet positions)
     * @param avoid The set of positions to avoid (e.g. threats)
     * @param strictMode If false, then we allow one encounter with a point that should be avoided
     * @param maxMoves The maximum number of moves to make before terminating the search
     * @return a list of Paths to each of the targets. The list is in increasing order of distance.
     */
    public List<Path> findShortestPaths(Point origin, Set<Point> targets, Set<Point> avoid, boolean strictMode, int maxMoves) {
        if (avoid == null) avoid = new HashSet<>();
        if (maxMoves <= 0) maxMoves = Integer.MAX_VALUE;

        List<Path> paths          = new ArrayList<>();
        Queue<Path> queue         = new LinkedList<>();
        Queue<Integer> encounters = new LinkedList<>();
        Set<Point> visited        = new HashSet<>();

        queue.add(new Path(origin));
        encounters.add(0);
        visited.add(origin);

        // Do a breadth-first search
        search:
        while (!queue.isEmpty()) {
            Path path = queue.remove();
            int unavoided = encounters.remove();
            Set<Move> validMoves = this.getValidMoves(path.end());

            for (Move nextMove : validMoves) {
                Path nextPath = new Path(path, nextMove, this);

                if (nextPath.nrMoves() > maxMoves) {
                    if (targets.isEmpty()) {
                        paths.add(path);
                        paths.addAll(queue);
                    }
                    break search;
                }

                Point nextPosition = nextPath.end();
                if (visited.contains(nextPosition))
                    continue;

                if (avoid.contains(nextPosition)) {
                    if (!strictMode && unavoided == 0)
                        unavoided++;
                    else
                        continue;
                }

                if (targets.contains(nextPosition))
                    paths.add(nextPath);

                visited.add(nextPosition);
                queue.add(nextPath);
                encounters.add(unavoided);
            }
        }
        return paths;
    }

}
