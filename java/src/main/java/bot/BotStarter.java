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

package bot;

import field.Field;
import hackman.Path;
import move.Move;
import player.Player;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Queue;

/**
 * bot.BotStarter
 *
 * @author Steven Lee - stevenwh.lee@gmail.com
 */

public class BotStarter {

    private Random rand;

    private BotStarter() {
        this.rand = new Random();
    }

    /**
     * Does a move action.
     *
     * @param state The current state of the game
     * @return A Move object
     */
    public Move doMove(BotState state) {
        Field field = state.getField();
        Player me   = state.getMyPlayer();

//      System.err.println("\n" + field);

        List<Path> paths = null;
        if (field.getNrArtifacts() == 0) {
            paths = findSafePaths(state, 10);
        } else {
            if (!me.hasWeapon())
                paths = findShortestPaths(state, true, 0);

            // Fall back on an unsafe route if there is no safe route (if no weapon)
            if (paths == null || paths.isEmpty())
                paths = findShortestPaths(state, false, 0);
        }

        if (paths.isEmpty())
            return randomMove(state);

        Path path = paths.get(0);
        return path.moves().get(0);
    }

    private List<Path> findShortestPaths(BotState state, boolean safeMode, int maxPaths) {
        Field field = state.getField();
        Point start = field.getMyPosition();

        List<Path> paths   = new ArrayList<>();
        Set<Point> targets = new HashSet<>();
        Queue<Path> queue  = new LinkedList<>();
        Set<Point> visited = new HashSet<>();

        targets.addAll(field.getSnippetPositions());
        targets.addAll(field.getWeaponPositions());
//      System.err.println("targets=" + targets);

        queue.add(new Path(start));
        visited.add(start);

        if (safeMode) {
            // Ensure that paths that encounter threats are not counted
            // in which case alternative routes to the targets will be found
            visited.addAll(threats(state));
        }

        // Do a breadth-first search
        while (!queue.isEmpty()) {
            Path path = queue.remove();
            List<Move> validMoves = field.getValidMoves(path.end());

            for (Move m : validMoves) {
                Path next = path.addMove(m);
                Point nextPosition = next.end();

                if (visited.contains(nextPosition))
                    continue;

                if (targets.contains(nextPosition))
                    paths.add(next);

                if (maxPaths > 0 && maxPaths == paths.size())
                    return paths;

                visited.add(nextPosition);
                queue.add(next);
            }
        }
        return paths;
    }

    private List<Path> findSafePaths(BotState state, int maxIterations) {
        if (maxIterations < 1) maxIterations = 10;

        Field field        = state.getField();
        Point start        = field.getMyPosition();
        Set<Point> threats = threats(state);
        List<Path> paths   = new ArrayList<>();
        Queue<Path> queue  = new LinkedList<>();
        Set<Point> visited = new HashSet<>();

        queue.add(new Path(start));
        visited.add(start);

        // Do a breadth-first search
        search:
        for (int i = 0; i < maxIterations; i++) {
            Path path = queue.remove();
            List<Move> validMoves = field.getValidMoves(path.end());

            for (Move m : validMoves) {
                Path next = path.addMove(m);
                Point nextPosition = next.end();

                if (visited.contains(nextPosition))
                    continue;

                if (threats.contains(nextPosition)) {
                    threats.remove(nextPosition);
                    continue;
                }

                visited.add(nextPosition);
                queue.add(next);

                if (!queue.isEmpty() && !threats.isEmpty())
                    break search;
            }
        }
        return paths;
    }

    private Set<Point> threats(BotState state) {
        Field field = state.getField();

        Set<Point> threats = new HashSet<>();
        threats.addAll(field.getEnemyPositions());

        if (state.getOpponentPlayer().hasWeapon())
            threats.add(field.getOpponentPosition());

        return threats;
    }

    private Move randomMove(BotState state) {
        ArrayList<Move> validMoves = state.getField().getValidMoves();

        if (validMoves.size() <= 0) return Move.PASS; // No valid moves

        int random = rand.nextInt(validMoves.size());

        return validMoves.get(random);
    }

    public static void main(String[] args) {
        BotParser parser = new BotParser(new BotStarter());
        parser.run();
    }
}
