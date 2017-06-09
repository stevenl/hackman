/**
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

public class Game {

    public static void main(String[] args) {
        Game game = new Game();
        game.run();
        System.err.print(game.toString());
    }

    private Scanner scanner;
    private Map<String, Map<String, String>> updates;
    private LinkedList<State> states;
    private Bot bot;

    private int timebank;
    private int timePerMove;
    private int fieldWidth;
    private int fieldHeight;
    private int maxRounds;

    private String[] playerNames;
    private String myName;
    private int myId;

    public Game() {
        this.scanner = new Scanner(System.in).useDelimiter("\n");
        this.updates = new HashMap<>();
        this.states  = new LinkedList<>();
        this.bot     = new Bot();
    }

    // For testing purposes
    Game(int fieldWidth, int fieldHeight, int myId) {
        this();
        this.fieldWidth  = fieldWidth;
        this.fieldHeight = fieldHeight;
        this.myId = myId;
    }

    Bot getBot() {
        return this.bot;
    }

    int getFieldWidth() {
        return this.fieldWidth;
    }

    int getFieldHeight() {
        return this.fieldHeight;
    }

    int getMyId() {
        return this.myId;
    }

    public void run() {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            if (line.length() == 0) continue;

            String[] parts = line.split(" ");
            switch (parts[0]) {
                case "settings":
                    this.parseSetting(parts[1], parts[2]);
                    break;
                case "update":
                    this.updates.putIfAbsent(parts[1], new HashMap<>());
                    this.updates.get(parts[1]).put(parts[2], parts[3]);
                    break;
                case "action":
                    this.timebank = Integer.parseInt(parts[2]);

                    State state = parseUpdates(updates);
                    this.states.addFirst(state);
                    this.updates.clear();

                    Move move = this.bot.doMove(state);
                    if (move != null) move = Move.PASS;
                    System.out.println(move.toString());
                    break;
                default:
                    System.err.println("unknown command");
                    break;
            }
        }
    }

    /**
     * Parses all the game settings given by the engine
     * @param key Type of setting given
     * @param value Value
     */
    private void parseSetting(String key, String value) {
        switch (key) {
            case "timebank":
                this.timebank = Integer.parseInt(value);
                break;
            case "time_per_move":
                this.timePerMove = Integer.parseInt(value);
                break;
            case "field_width":
                this.fieldWidth = Integer.parseInt(value);
                break;
            case "field_height":
                this.fieldHeight = Integer.parseInt(value);
                break;
            case "max_rounds":
                this.maxRounds = Integer.parseInt(value);
                break;
            case "player_names":
                this.playerNames = value.split(",");
                break;
            case "your_bot":
                this.myName = value;
                break;
            case "your_botid":
                this.myId = Integer.parseInt(value);
                break;
            default:
                System.err.println(String.format("Cannot parse settings input with key '%s'", key));
        }
    }

    private State parseUpdates(Map<String, Map<String, String>> updates) {
        // Players
        List<Player> players = new ArrayList<>();
        for (String playerName : this.playerNames) {
            Map<String, String> playerData = updates.get(playerName);
            int id              = Integer.parseInt(playerName.substring(playerName.length() - 1));
            int snippets        = Integer.parseInt(playerData.get("snippets"));
            boolean hasWeapon   = Boolean.parseBoolean(playerData.get("has_weapon"));
            boolean isParalyzed = Boolean.parseBoolean(playerData.get("is_paralyzed"));;
            Player player = new Player(id, playerName, snippets, hasWeapon, isParalyzed);

            players.add(player);
        }

        // State
        Map<String, String> roundData = updates.get("game");
        int roundId  = Integer.parseInt(roundData.get("round"));
        String field = roundData.get("field");

        return new State(roundId, field, players, this.states.peekFirst(), this);
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();

        output.append("timebank=" + timebank + "\n");
        output.append("timePerMove=" + timePerMove + "\n");
        output.append("fieldWidth=" + fieldWidth + "\n");
        output.append("fieldHeight=" + fieldHeight + "\n");
        output.append("maxRounds=" + maxRounds + "\n");

        output.append("playerNames=" + playerNames + "\n");
        output.append("myName=" + myName + "\n");
        output.append("myId=" + myId + "\n");

        return output.toString();
    }
}
