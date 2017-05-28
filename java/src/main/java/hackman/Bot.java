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
import java.util.stream.Collectors;

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

        //System.err.println("\n" + field);

        List<Path> myPaths  = getPaths(state, state.getMyPlayer(), state.getOpponentPlayer());
        List<Path> oppPaths = getPaths(state, state.getOpponentPlayer(), state.getMyPlayer());

        //System.err.println("myPath=" + myPaths.get(0));
        //System.err.println("oppPath=" + oppPaths.get(0));

        // Don't go for the target if the opponent is closer to it
        if (myPaths.size() > 1 && !oppPaths.isEmpty()) {
            Path myPath  = myPaths.get(0);
            Path oppPath = oppPaths.get(0);

            if (myPath.end().equals(oppPath.end()) && myPath.nrMoves() > oppPath.nrMoves())
                myPaths.remove(0);
        }

        Move move = null;
        if (!myPaths.isEmpty()) {
            Map<Move, Float> moveScores = calculateMoveScores(myPaths);
            //System.err.println("scores=" + moveScores);

            // Choose the move with the highest score
            move = moveScores.entrySet().stream()
                    .max((e1, e2) -> Float.compare(e1.getValue(), e2.getValue()))
                    .map(e -> e.getKey())
                    .orElse(null);
            //System.err.println("move=" + move);
        }

        return move != null ? move : Move.PASS;
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
        Field field  = state.getField();
        Point origin = field.getPlayerPosition(a.getId());

        Set<Point> targets = new HashSet<>(field.getSnippetPositions());

        // Get sword since we don't already have one
        if (!a.hasWeapon()) targets.addAll(field.getWeaponPositions());

        int maxMoves = 0;
        if (targets.isEmpty()) maxMoves = 8;

        Set<Point> threats = new HashSet<>(field.getEnemyPositions());
        if (!a.hasWeapon() && b.hasWeapon())
            threats.add(field.getPlayerPosition(b.getId()));

        List<Path> pathsToThreats = findShortestPaths(field, origin, threats, null, 0);

        // Detect threats that are 2 steps away such that they can harm us
        // by moving to the position that we want to move to.
        Set<Point> immediateThreats = pathsToThreats.stream()
                .filter(path -> path.nrMoves() == 2)
                .map(path -> path.position(1))
                .collect(Collectors.toSet());
        //System.err.println("immediate=" + immediateThreats);

        Set<Point> traps = findTraps(field, pathsToThreats);
        //System.err.println("traps=" + traps);

        // Avoid immediate threats and traps
        List<Path> paths = null;
        {
            Set<Point> avoid = new HashSet<>();
            avoid.addAll(immediateThreats);
            avoid.addAll(traps);
            //System.err.println("avoid=" + avoid);

            paths = findShortestPaths(field, origin, targets, avoid, maxMoves);
            //if (!paths.isEmpty()) System.err.println("safe2=" + paths.get(0));
        }

        if (paths.isEmpty()) {
            if (!a.hasWeapon()) {
                // Fallback: Avoid immediate threats only
                paths = findShortestPaths(field, origin, targets, immediateThreats, maxMoves);
                //if (!paths.isEmpty()) System.err.println("safe3=" + paths.get(0));
            } else {
                // Fallback: Don't try to avoid threats
                paths = findShortestPaths(field, origin, targets, null, 0);
                //if (!paths.isEmpty()) System.err.println("unsafe=" + paths.get(0));
            }
        }

        return paths;
    }

    /**
     * Finds the intersections that can be reached by a bug before you.
     * If they can, then they can trap you in.
     *
     * @param field The game field
     * @param toThreats A set of paths from you to each threat
     * @return The set of intersection points where you can be trapped
     */
    private Set<Point> findTraps(Field field, List<Path> toThreats) {
        Set<Point> traps = new HashSet<>();

        for (Path path : toThreats) {
            // Find any intersections between you and the bug
            boolean seenIntersection = false;
            for (int i = 1; i <= path.nrMoves(); i++) {
                Point pos = path.position(i);
                List<Move> moves = field.getValidMoves(pos);

                // Is it an intersection?
                if (moves.size() > 2) {
                    seenIntersection = true;
                    int movesToIntersection = i;
                    int threatToIntersection = path.nrMoves() - i;

                    // Can the threat reach the intersection before you?
                    if (movesToIntersection >= threatToIntersection) {
                        traps.add(pos);
                        break;
                    }
                }

                // Has the threat already trapped you in?
                if (!seenIntersection && i == path.nrMoves())
                    traps.add(pos);
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
