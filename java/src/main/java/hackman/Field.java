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

import java.util.ArrayList;
import java.util.HashMap;

/**
 * hackman.Field
 *
 * Stores all information about the playing field and
 * contains methods to perform calculations about the field
 *
 * @author Jim van Eeden - jim@riddles.io
 */
public class Field {

    private int width;
    private int height;
    private String myId;
    private String opponentId;

    private String[][] field;
    private HashMap<Integer, Point> playerPositions; // <id, position>
    private ArrayList<Point> enemyPositions;
    private ArrayList<Point> snippetPositions;
    private ArrayList<Point> weaponPositions;

    public Field() {
        this.playerPositions  = new HashMap<>();
        this.enemyPositions   = new ArrayList<>();
        this.snippetPositions = new ArrayList<>();
        this.weaponPositions  = new ArrayList<>();
    }

    /**
     * Initializes field
     * @throws Exception: exception
     */
    public void initField() throws Exception {
        try {
            this.field = new String[this.width][this.height];
        } catch (Exception e) {
            throw new Exception("Error: trying to initialize field while field "
                    + "settings have not been parsed yet.");
        }
        clearField();
    }

    /**
     * Clears the field
     */
    public void clearField() {
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
    public void parseFromString(String input) {
        clearField();

        String[] cells = input.split(",");

        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                String cell = cells[x + (y * this.width)];
                this.field[x][y] = cell;

                for (char c : cell.toCharArray()) {  // Multiple things can be on same position
                    if (c == this.myId.charAt(0)) {
                        int id = Character.getNumericValue(c);
                        Point position = new Point(x, y);
                        this.playerPositions.put(id, position);
                    } else if (c == this.opponentId.charAt(0)) {
                        int id = Character.getNumericValue(c);
                        Point position = new Point(x, y);
                        this.playerPositions.put(id, position);
                    } else if (c == 'C') {
                        this.snippetPositions.add(new Point(x, y));
                    } else if (c == 'E') {
                        this.enemyPositions.add(new Point(x, y));
                    } else if (c == 'W') {
                        this.weaponPositions.add(new Point(x, y));
                    }
                }
            }
        }
    }

    /**
     * Returns a string representation of the field that can be printed
     * @return String representation of the current field
     */
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
    public ArrayList<Move> getValidMoves() {
        return getValidMoves(getMyPosition());
    }

    public ArrayList<Move> getValidMoves(Point p) {
        ArrayList<Move> validMoves = new ArrayList<>();
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
        int x = p.x;
        int y = p.y;

        return x >= 0 && x < this.width && y >= 0 && y < this.height &&
                !this.field[x][y].contains("x");
    }

    public void setMyId(int id) {
        this.myId = id + "";
    }

    public void setOpponentId(int id) {
        this.opponentId = id + "";
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
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

    public ArrayList<Point> getEnemyPositions() {
        return this.enemyPositions;
    }

    public ArrayList<Point> getSnippetPositions() {
        return this.snippetPositions;
    }

    public ArrayList<Point> getWeaponPositions() {
        return this.weaponPositions;
    }

    public int getNrArtifacts() {
        return this.snippetPositions.size() + this.weaponPositions.size();
    }
}
