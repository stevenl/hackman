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
 * hackman.Bot
 *
 * @author Steven Lee - stevenwh.lee@gmail.com
 */

public class Bot {

    public static void main(String[] args) {
        Parser parser = new Parser(new Bot());
        parser.run();
    }

    private Random rand;

    private Bot() {
        this.rand = new Random();
    }

    /**
     * Does a move action.
     *
     * @param state The current state of the game
     * @return A Move object
     */
    public Move doMove(State state) {
        Field field = state.getField();
        Player me   = state.getMyPlayer();

//        System.err.println("\n" + field);

        Path path = null;
        if (field.getNrArtifacts() == 0) {
            path = findSafePath(state, 20);
        } else {
            List<Path> myPaths  = getPaths(state, state.getMyPlayer(), state.getOpponentPlayer());
            List<Path> oppPaths = getPaths(state, state.getOpponentPlayer(), state.getMyPlayer());

//            System.err.println("myPath=" + myPaths.get(0));
//            System.err.println("oppPath=" + oppPaths.get(0));

            // Don't go for the target if the opponent is closer to it
            if (myPaths.size() > 1 && !oppPaths.isEmpty()) {
                Path myPath  = myPaths.get(0);
                Path oppPath = oppPaths.get(0);

                if (myPath.end().equals(oppPath.end()) && myPath.nrMoves() > oppPath.nrMoves())
                    myPaths.remove(0);
            }

            if (!myPaths.isEmpty())
                path = myPaths.get(0);
        }

        if (path == null)
            return randomMove(state);

        return path.moves().get(0);
    }

    private List<Path> getPaths(State state, Player a, Player b) {
        Field field = state.getField();
        Point origin = field.getPlayerPosition(a.getId());

        List<Path> paths = null;
        if (!a.hasWeapon()) {
            Set<Point> targets = new HashSet<>();
            targets.addAll(field.getSnippetPositions());
            // Get sword since we don't already have one
            targets.addAll(field.getWeaponPositions());

            Set<Point> threats = new HashSet<>(field.getEnemyPositions());
            if (b.hasWeapon()) {
                Point position = field.getPlayerPosition(b.getId());
                threats.add(position);
            }
            threats.addAll(findImmediateThreats(field, origin, threats));

            paths = findShortestPaths(field, origin, targets, threats);
        }

        // Fall back on an unsafe route if there is no safe route (if no weapon)
        if (paths == null || paths.isEmpty()) {
            Set<Point> targets = new HashSet<>();
            targets.addAll(field.getSnippetPositions());
            if (!a.hasWeapon())
                targets.addAll(field.getWeaponPositions());

            paths = findShortestPaths(field, origin, targets);
        }

        return paths;
    }

    private Set<Point> findImmediateThreats(Field field, Point origin, Set<Point> threats) {
        Set<Point> dangers = new HashSet<>();

        List<Path> pathsToThreats = findShortestPaths(field, origin, threats, null, 2);
        for (Path p : pathsToThreats) {
            Move m = p.moves().get(0);
            dangers.add(new Point(origin, m));
        }
        return dangers;
    }

    /**
     * Does a breadth-first search to find an optimal path from the given
     * origin to the closest target.
     *
     * @param field The game field
     * @param origin The starting point (e.g. my current position)
     * @param targets The end points to aim for (e.g. snippet positions)
     * @return a list of Paths to each of the targets. The list is in increasing order of distance.
     */
    private List<Path> findShortestPaths(Field field, Point origin, Set<Point> targets) {
        return findShortestPaths(field, origin, targets, null, 0);
    }

    private List<Path> findShortestPaths(Field field, Point origin, Set<Point> targets, Set<Point> avoid) {
        return findShortestPaths(field, origin, targets, avoid, 0);
    }

    /**
     * Does a breadth-first search to find an optimal path from the given
     * origin to each of the targets. The resulting paths will never pass
     * through any of the points to avoid.
     *
     * @param field The game field
     * @param origin The starting point (e.g. my current position)
     * @param targets The end points to aim for (e.g. snippet positions)
     * @param avoid The points to avoid (e.g. threats)
     * @return a list of Paths to each of the targets. The list is in increasing order of distance.
     */
    private List<Path> findShortestPaths(Field field, Point origin, Set<Point> targets, Set<Point> avoid, int maxMoves) {
        if (avoid == null) avoid = new HashSet<>();
        if (maxMoves <= 0) maxMoves = Integer.MAX_VALUE;

        List<Path> paths   = new ArrayList<>();
        Queue<Path> queue  = new LinkedList<>();
        Set<Point> visited = new HashSet<>();

        queue.add(new Path(origin));
        visited.add(origin);

        // Ensure that paths that encounter threats are not counted
        // in which case alternative routes to the targets will be found
        visited.addAll(avoid);

        // Do a breadth-first search
        search:
        while (!queue.isEmpty()) {
            Path path = queue.remove();
            List<Move> validMoves = field.getValidMoves(path.end());

            for (Move m : validMoves) {
                Path next = new Path(path, m);
                if (next.nrMoves() > maxMoves)
                    break search;

                Point nextPosition = next.end();
                if (visited.contains(nextPosition))
                    continue;

                if (targets.contains(nextPosition))
                    paths.add(next);

                visited.add(nextPosition);
                queue.add(next);
            }
        }
        return paths;
    }

    private Path findSafePath(State state, int maxIterations) {
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
                Path next = new Path(path, m);
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

        if (paths.isEmpty())
            return null;

        return paths.get(0);
    }

    private Set<Point> threats(State state) {
        Field field = state.getField();

        Set<Point> threats = new HashSet<>();
        threats.addAll(field.getEnemyPositions());

        if (state.getOpponentPlayer().hasWeapon())
            threats.add(field.getOpponentPosition());

        return threats;
    }

    private Move randomMove(State state) {
        ArrayList<Move> validMoves = state.getField().getValidMoves();

        if (validMoves.size() <= 0) return Move.PASS; // No valid moves

        int random = rand.nextInt(validMoves.size());

        return validMoves.get(random);
    }
}
