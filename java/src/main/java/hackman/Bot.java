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

    Bot() {
        this.rand = new Random();
    }

    /**
     * Does a move action.
     *
     * @param state The current state of the game
     * @return A Move object
     */
    Move doMove(State state) {
        Field field = state.getField();
        Player me   = state.getMyPlayer();

//        System.err.println("\n" + field);

        Path path = null;
        List<Path> myPaths  = getPaths(state, state.getMyPlayer(), state.getOpponentPlayer());
        List<Path> oppPaths = getPaths(state, state.getOpponentPlayer(), state.getMyPlayer());

//        System.err.println("myPath=" + myPaths.get(0));
//        System.err.println("oppPath=" + oppPaths.get(0));

        // Don't go for the target if the opponent is closer to it
        if (myPaths.size() > 1 && !oppPaths.isEmpty()) {
            Path myPath  = myPaths.get(0);
            Path oppPath = oppPaths.get(0);

            if (myPath.end().equals(oppPath.end()) && myPath.nrMoves() > oppPath.nrMoves())
                myPaths.remove(0);
        }

        Move move = null;
        if (!myPaths.isEmpty()) {
            float maxScore = -1;
            Map<Move, Float> moveScores = calculateMoveScores(myPaths);
//            System.err.println("scores=" + moveScores);

            for (Map.Entry<Move, Float> e : moveScores.entrySet()) {
                float score = e.getValue();
                if (score <= maxScore)
                    continue;

                move = e.getKey();
                maxScore = score;
            }
        }

        if (move == null)
            return randomMove(field, field.getMyPosition());

        return move;
    }

    private Map<Move, Float> calculateMoveScores(List<Path> paths) {
        Map<Move, Float> moveScores = new HashMap<>();
        for (Path p : paths) {
            Move move = p.moves().get(0);

            float score;
            if (moveScores.containsKey(move))
                score = moveScores.get(move);
            else
                score = 0;

            score += 1.0 / p.nrMoves();
            moveScores.put(move, score);
        }
        return moveScores;
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
            if (b.hasWeapon()) threats.add(field.getPlayerPosition(b.getId()));

            Set<Point> immediateThreats = findImmediateThreats(field, origin, threats);
            Set<Point> traps = findTraps(field, origin, threats);

            Set<Point> avoid = new HashSet<>(threats);
            avoid.addAll(traps);
            avoid.addAll(immediateThreats);

            int maxMoves = 0;
            if (targets.isEmpty()) maxMoves = 8;

            paths = findShortestPaths(field, origin, targets, avoid, maxMoves);
//            if (!paths.isEmpty()) System.err.println("safe=" + paths.get(0));

            if (paths.isEmpty()) {
                Set<Point> threats2 = new HashSet<>();
                threats2.addAll(immediateThreats);
                threats2.addAll(traps);
                paths = findShortestPaths(field, origin, targets, threats2, maxMoves);
//                if (!paths.isEmpty()) System.err.println("safe2=" + paths.get(0));
            }
            if (paths.isEmpty()) {
                paths = findShortestPaths(field, origin, targets, immediateThreats, maxMoves);
//                if (!paths.isEmpty()) System.err.println("safe3=" + paths.get(0));
            }
        }

        // Fall back on an unsafe route if there is no safe route (if no weapon)
        if (paths == null || paths.isEmpty()) {
            Set<Point> targets = new HashSet<>();
            targets.addAll(field.getSnippetPositions());
            if (!a.hasWeapon()) targets.addAll(field.getWeaponPositions());

            paths = findShortestPaths(field, origin, targets, null, 0);
//            if (!paths.isEmpty()) System.err.println("unsafe=" + paths.get(0));
        }

        return paths;
    }

    /**
     * Detect threats that are 2 steps away such that they can harm us by
     * moving to the position that we want to move to. Such threats are not
     * detected by the normal bread-first search when they are not directly
     * in our path to reach a target.
     *
     * @param field The game field
     * @param origin the starting position (my current position)
     * @param threats The set of positions that have threats
     * @return A set of adjacent positions that are dangerous to move to
     */
    private Set<Point> findImmediateThreats(Field field, Point origin, Set<Point> threats) {
        Set<Point> dangers = new HashSet<>();

        List<Path> pathsToThreats = findShortestPaths(field, origin, threats, null, 0);
        for (Path p : pathsToThreats) {
            if (p.nrMoves() == 2) {
                Move m = p.moves().get(0);
                dangers.add(new Point(origin, m));
            }
        }
        return dangers;
    }

    private Set<Point> findTraps(Field field, Point origin, Set<Point> avoid) {
        List<Path> toIntersections = new ArrayList<>();
        Queue<Path> queue  = new LinkedList<>();
        Set<Point> visited = new HashSet<>();

        queue.add(new Path(origin));
        visited.add(origin);

        // Do a breadth-first search to find the intersections
        while (!queue.isEmpty()) {
            Path path = queue.remove();
            List<Move> validMoves = field.getValidMoves(path.end());

            // Have we reached an intersection?
            if (path.nrMoves() > 0 && validMoves.size() > 2) {
                toIntersections.add(path);
                continue;
            }

            for (Move m : validMoves) {
                Path next = new Path(path, m);
                Point nextPosition = next.end();

                if (visited.contains(nextPosition))
                    continue;

                visited.add(nextPosition);

                if (!avoid.contains(nextPosition))
                    queue.add(next);
            }
        }

        // Now find the threat nearest to each intersection
        // i.e. how many moves before they can trap you in?
        Map<Point, Path> intersectionPaths = new HashMap<>();
        for (Path p : toIntersections) {
            intersectionPaths.put(p.end(), p);
            queue.add(new Path(p.end())); // start new path from intersection
        }

        Map<Path, Path> intersectionThreats = new HashMap<>();
        while (!queue.isEmpty()) {
            Path path = queue.remove();
            List<Move> validMoves = field.getValidMoves(path.end());

            for (Move m : validMoves) {
                Path next = new Path(path, m);
                Point nextPosition = next.end();

                if (avoid.contains(nextPosition)) {
                    Path intPath = intersectionPaths.get(path.start());
                    intersectionThreats.put(intPath, next);
                    break;
                }

                if (visited.contains(nextPosition))
                    continue;

                visited.add(nextPosition);
                queue.add(next);
            }
        }

        Set<Point> traps = new HashSet<>();
        for (Map.Entry<Path, Path> e : intersectionThreats.entrySet()) {
            Path intersection = e.getKey();
            Path trapper = e.getValue();

            if (intersection.nrMoves() >= trapper.nrMoves()) {
                traps.add(intersection.end());
            }
        }
        return traps;
    }

    /**
     * Does a breadth-first search to find an optimal path from the given
     * origin to each of the targets. The resulting paths will never pass
     * through any of the points to avoid.
     *
     * @param field The game field
     * @param origin The starting position (e.g. my current position)
     * @param targets The set of end positions to aim for (e.g. snippet positions)
     * @param avoid The set of positions to avoid (e.g. threats)
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

                if (next.nrMoves() > maxMoves) {
                    if (targets.isEmpty()) {
                        paths.add(path);
                        paths.addAll(queue);
                    }
                    break search;
                }

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

    /**
     * Returns a random but valid Move
     * @param field The game field
     * @return A move decided at random
     */
    private Move randomMove(Field field, Point origin) {
        ArrayList<Move> validMoves = field.getValidMoves(origin);

        if (validMoves.size() <= 0) return Move.PASS; // No valid moves

        int random = rand.nextInt(validMoves.size());

        return validMoves.get(random);
    }
}
