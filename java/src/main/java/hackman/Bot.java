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
import java.util.stream.IntStream;

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

    private Field prevField = null;
    private Random rand;

    Bot() {
        this.rand = new Random();
    }

    Set<Point> getPreviousEnemyPositions() {
        Set<Point> prevThreatPositions = new HashSet<>();
        if (this.prevField != null) {
            List<Point> enemies = this.prevField.getEnemyPositions();
            prevThreatPositions.addAll(enemies);
        }
        return prevThreatPositions;
    }

    /**
     * Does a move action.
     *
     * @param state The current state of the game
     * @return A Move object
     */
    Move doMove(State state) {
        //System.err.println("\n" + state.getField());

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
        //System.err.println("myPath2=" + myPaths.get(0));

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

        this.prevField = new Field(state.getField());
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

        List<Path> pathsToThreats = findShortestPaths(field, origin, threats, null, true, 0);

        // Is the enemy moving away? It's unlikely he will come back this way
        Set<Point> prevThreatPositions = getPreviousEnemyPositions();
        pathsToThreats = pathsToThreats.stream()
                .filter(toThreat -> {
                    Point penultimatePos = toThreat.position(toThreat.nrMoves() - 1);
                    return !prevThreatPositions.contains(penultimatePos);
                })
                .collect(Collectors.toList());
        //System.err.println("toThreats=" + pathsToThreats);

        // Revise threats after filter
        threats = pathsToThreats.stream()
                .map(toThreat -> toThreat.end())
                .collect(Collectors.toSet());
        //System.err.println("threats=" + threats);

        // Threats that are 2 moves away can still harm us
        // by moving to the position that we want to move to.
        Set<Point> immediateThreats = pathsToThreats.stream()
                .filter(path -> path.nrMoves() <= 2)
                .map(path -> path.position(1))
                .collect(Collectors.toSet());
        //System.err.println("immediate=" + immediateThreats);

        Set<Point> traps = findTraps(field, pathsToThreats, prevThreatPositions);
        //System.err.println("traps=" + traps);

        List<Path> paths = null;
        if (!a.hasWeapon()) {
            // Safe strategy: Avoid all threats and traps
            {
                Set<Point> avoid = new HashSet<>();
                avoid.addAll(threats);
                avoid.addAll(immediateThreats);
                avoid.addAll(traps);
                //System.err.println("avoid1=" + avoid);

                paths = findShortestPaths(field, origin, targets, avoid, true, maxMoves);
                //if (!paths.isEmpty()) System.err.println("safe1=" + paths.get(0));
            }

            //Fallback: Avoid immediate threats and traps
            if (paths.isEmpty()) {
                Set<Point> avoid = new HashSet<>();
                avoid.addAll(immediateThreats);
                avoid.addAll(traps);

                paths = findShortestPaths(field, origin, targets, avoid, true, maxMoves);
                //if (!paths.isEmpty()) System.err.println("safe2=" + paths.get(0));
            }

            // Fallback: Avoid immediate threats only
            if (paths.isEmpty()) {
                paths = findShortestPaths(field, origin, targets, immediateThreats, true, maxMoves);
                //if (!paths.isEmpty()) System.err.println("safe3=" + paths.get(0));
            }
        }
        else {
            // With weapon: Allowed to avoid one threat
            Set<Point> avoid = new HashSet<>();
            avoid.addAll(immediateThreats);
            avoid.addAll(traps);

            paths = findShortestPaths(field, origin, targets, avoid, false, 0);
            //if (!paths.isEmpty()) System.err.println("unsafe=" + paths.get(0));
        }
        return paths;
    }

    /**
     * Finds the intersections that can be reached by a bug before you.
     * If they can, then they can trap you in.
     *
     * @param field The game field
     * @param pathsToThreats A set of paths from you to each threat
     * @return The set of intersection points where you can be trapped
     */
    private Set<Point> findTraps(Field field, List<Path> pathsToThreats, Set<Point> prevThreatPositions) {
        Set<Point> traps = new HashSet<>();
        Map<Point, Integer> intersectionOptions = new HashMap<>();

        for (Path toThreat : pathsToThreats) {
            int maxMoves = toThreat.nrMoves();

            // These have already been detected as immediate threats and we want to
            // avoid double counting them because they are positioned differently
            if (maxMoves <= 2) continue;

            // Find any intersections between you and the bug
            Deque<Point> intersectionStack = new ArrayDeque<>();
            for (int i = 1; i <= maxMoves; i++) {
                Point pos = toThreat.position(i);

                // Has the threat already trapped you in?
                if (i == maxMoves && intersectionStack.isEmpty()) {
                    //System.err.println("trap1=" + pos);
                    traps.add(pos);
                    break;
                }

                // Is it an intersection?
                // (Don't count the point we come from)
                int nrMoveOptions = field.getValidMoves(pos).size() - 1;
                if (nrMoveOptions > 1) {
                    if (!intersectionOptions.containsKey(pos))
                        intersectionOptions.put(pos, nrMoveOptions);
                    else
                        nrMoveOptions = intersectionOptions.get(pos);

                    int movesToIntersection = i;
                    int threatToIntersection = maxMoves - i;

                    // Can the threat reach the intersection before you?
                    if (movesToIntersection >= threatToIntersection) {
                        //System.err.println("trap2[" + movesToIntersection + ">=" + threatToIntersection + "]=" + pos);
                        traps.add(pos);

                        // If all paths from an intersection lead to traps then
                        // the intersection should also be considered a trap
                        while (intersectionStack.peekFirst() != null) {
                            Point lastIntersection = intersectionStack.removeFirst();
                            int options = intersectionOptions.get(lastIntersection);

                            intersectionOptions.put(lastIntersection, options - 1);

                            if (options == 1) {
                                //System.err.println("trap3=" + pos);
                                traps.add(lastIntersection);
                            } else {
                                break;
                            }
                        }
                    }
                    else if (nrMoveOptions == 1) {
                        Point closedIntersection = IntStream.rangeClosed(i + 1, maxMoves)
                                .mapToObj(j -> toThreat.position(j))
                                .filter(p -> field.getValidMoves(p).size() > 2)
                                .findFirst()
                                .orElse(null);
                        if (closedIntersection == null && !traps.contains(closedIntersection)) {
                            //System.err.println("trap4=" + pos);
                            traps.add(pos);
                        }
                        break;
                    }
                    intersectionStack.addFirst(pos);
                }
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
    private List<Path> findShortestPaths(Field field, Point origin, Set<Point> targets, Set<Point> avoid, boolean strictMode, int maxMoves) {
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

                if (avoid.contains(nextPosition)) {
                    if (!strictMode && unavoided == 0)
                        unavoided++;
                    else
                        continue;
                }

                if (targets.contains(nextPosition))
                    paths.add(next);

                visited.add(nextPosition);
                queue.add(next);
                encounters.add(unavoided);
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
