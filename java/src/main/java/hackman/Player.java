/*
 * Copyright 2016 riddles.io (developers@riddles.io)
 * Modifications copyright 2017 Steven Lee (stevenwh.lee@gmail.com)
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
import java.util.stream.IntStream;

/**
 * hackman.Player
 *
 * Stores all information about a player
 *
 * @author Jim van Eeden - jim@riddles.io
 * @author Steven Lee - stevenwh.lee@gmail.com
 */
public class Player {

    private int id;
    private int snippets;
    private boolean hasWeapon;
    private boolean isParalyzed;
    private Point position = null;
    private State state = null;

    Player(int id, int snippets, boolean hasWeapon, boolean isParalyzed) {
        this.id          = id;
        this.snippets    = snippets;
        this.hasWeapon   = hasWeapon;
        this.isParalyzed = isParalyzed;
    }

    public int getId() {
        return this.id;
    }

    public int getSnippets() {
        return this.snippets;
    }

    public boolean hasWeapon() {
        return this.hasWeapon;
    }

    public void setWeapon(boolean hasWeapon) {
        this.hasWeapon = hasWeapon;
    }

    public boolean isParalyzed() {
        return this.isParalyzed;
    }

    public Point getPosition() {
        return this.position;
    }

    void setPosition(Point position) {
        this.position = position;
    }

    void setState(State state) {
        this.state = state;

        // Reset
        this.pathsToThreats = null;
        this.immediateThreats = null;
    }

    /*****************************************************/

    private Player getOpponent() {
        int oppId = (this.id + 1) % 2;
        return this.state.getPlayer(oppId);
    }

    /**
     * Gets the positions that the player can aim for. This includes all
     * snippets, as well as weapons if the player does not have one.
     *
     * @return The positions of the targets
     */
    private Set<Point> getTargets() {
        Set<Point> targets = new HashSet<>();
        targets.addAll(state.getSnippetPositions());

        // Get sword since we don't already have one
        if (!this.hasWeapon)
            targets.addAll(state.getWeaponPositions());

        //System.err.println(String.format("[%d] targets=%s", id, targets));
        return targets;
    }

    /**
     * Get the positions of any threats. This includes all enemy bugs, as
     * well as the opponent player if it has a weapon. These are counted
     * as potential threats in that we do not take into account whether
     * they are moving away, in which case they are no longer threats.
     *
     * @return The positions of the threats
     */
    private Set<Point> getPotentialThreats() {
        Set<Point> threats = new HashSet<>();
        threats.addAll(state.getEnemyPositions());

        Player opponent = getOpponent();
        if (opponent.hasWeapon())
            threats.add(opponent.getPosition());

        //System.err.println(String.format("[%d] potentialThreats=%s", id, threats));
        return threats;
    }

    /**
     * Like getPotentialThreats() except that it does not include enemies
     * that are not threats because they are moving away.
     *
     * @return The positions of the threats.
     */
    private Set<Point> getThreats() {
        Set<Point> threats = getPathsToThreats().stream()
                .map(toThreat -> toThreat.end())
                .collect(Collectors.toSet());

        //System.err.println(String.format("[%d] threats=%s", id, threats));
        return threats;
    }

    /**
     * Gets the threats that are 1-2 moves away. These are the threats that
     * can harm us in the next move. Those that are 2 steps away can harm us
     * by moving to the position that we want to move to.
     *
     * @return The positions of the immediate threats
     */
    private Set<Point> immediateThreats = null;
    private Set<Point> getImmediateThreats() {
        if (this.immediateThreats == null) {
            this.immediateThreats = getPathsToThreats().stream()
                    .filter(path -> path.nrMoves() <= 2)
                    .map(path -> path.position(1))
                    .collect(Collectors.toSet());
            //System.err.println(String.format("[%d] immediate=%s", id, immediateThreats));
        }
        return this.immediateThreats;
    }

    private Set<Point> getNearbyThreats() {
        Set<Point> nearbyThreats = getPathsToThreats().stream()
                .filter(path -> 2 < path.nrMoves() && path.nrMoves() <= 8)
                .map(path -> path.end())
                .collect(Collectors.toSet());

        //System.err.println(String.format("[%d] nearby=%s", id, nearbyThreats));
        return nearbyThreats;
    }

    /**
     * Gets the most direct path to each of the potential threats and filters
     * out the enemies that are moving away and are therefore not threats.
     *
     * @return A list of paths to each threat
     */
    private List<Path> pathsToThreats = null;
    private List<Path> getPathsToThreats() {
        if (this.pathsToThreats == null) {
            Set<Point> threats = getPotentialThreats();
            this.pathsToThreats = state.findShortestPaths(this.position, threats, null, true, null);

            // Is the enemy moving away? It's unlikely he will come back this way
            Set<Point> prevEnemyPositions = state.getPreviousEnemyPositions();
            if (!prevEnemyPositions.isEmpty()) {
                this.pathsToThreats = this.pathsToThreats.stream()
                        .filter(toThreat -> {
                            Point penultimatePos = toThreat.position(toThreat.nrMoves() - 1);
                            return !prevEnemyPositions.contains(penultimatePos);
                        })
                        .collect(Collectors.toList());
            }
            //System.err.println(String.format("[%d] toThreats=%s", id, pathsToThreats));
        }
        return this.pathsToThreats;
    }


    /**
     * Finds the intersections that can be reached by a bug before you.
     * If they can, then they can trap you in.
     *
     * @return The set of intersection points where you can be trapped
     */
    private Set<Point> getTraps() {
        List<Path> pathsToThreats = this.getPathsToThreats();
        Set<Point> targets = this.getTargets();

        Set<Point> traps = new HashSet<>();
        Map<Point, Integer> intersectionOptions = new HashMap<>();
        for (Path toThreat : pathsToThreats) {
            int maxMoves = toThreat.nrMoves();

            // These have already been detected as immediate threats and we want to
            // avoid double counting them because they are positioned differently
            if (maxMoves <= 2) continue;

            // Don't be too cautious: far-away threats may move somewhere else
            if (toThreat.nrMoves() > 10 && toThreat.nrIntersections() > 3)
                continue;

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
                int nrMoveOptions = state.getValidMoves(pos).size() - 1;
                if (nrMoveOptions > 1 || targets.contains(pos)) {
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
                                .filter(p -> state.getValidMoves(p).size() > 2)
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
        //System.err.println(String.format("[%d] traps=%s", id, traps));
        return traps;
    }

    public List<Path> getPaths() {
        Point origin = this.getPosition();

        Set<Point> targets = getTargets();
        Set<Point> immediateThreats = getImmediateThreats();
        Set<Point> nearbyThreats = getNearbyThreats();
        Set<Point> traps = getTraps();

        // Don't be a sitting duck if there are no targets:
        // Get any safe paths within 8 moves
        Predicate<Path> searchWhile = null;
        if (targets.isEmpty())
            searchWhile = (p -> p.nrIntersections() < 8);

        List<Path> paths = null;
        if (!this.hasWeapon) {
            // Safe strategy: Avoid all threats and traps
            {
                Set<Point> avoid = new HashSet<>();
                avoid.addAll(immediateThreats);
                avoid.addAll(nearbyThreats);
                avoid.addAll(traps);
                //System.err.println(String.format("[%d] avoid1=%s", id, avoid));

                paths = state.findShortestPaths(origin, targets, avoid, true, searchWhile);
                //if (!paths.isEmpty()) System.err.println(String.format("[%d] safe1=%s", id, paths.get(0)));
            }

            //Fallback: Avoid immediate threats and traps
            if (paths.isEmpty()) {
                Set<Point> avoid = new HashSet<>();
                avoid.addAll(immediateThreats);
                avoid.addAll(traps);

                paths = state.findShortestPaths(origin, targets, avoid, true, searchWhile);
                //if (!paths.isEmpty()) System.err.println(String.format("[%d] safe2=%s", id, paths.get(0)));
            }

            // Fallback: Avoid immediate threats only
            if (paths.isEmpty()) {
                paths = state.findShortestPaths(origin, targets, immediateThreats, true, searchWhile);
                //if (!paths.isEmpty()) System.err.println(String.format("[%d] safe3=%s", id, paths.get(0)));
            }

            if (paths.isEmpty()) {
                paths = state.findShortestPaths(origin, targets, immediateThreats, false, searchWhile);
                //if (!paths.isEmpty()) System.err.println(String.format("[%d] safe4=%s", id, paths.get(0)));
            }
        }
        else {
            // With weapon: Allowed to avoid one threat
            Set<Point> avoid = new HashSet<>();
            avoid.addAll(immediateThreats);
            avoid.addAll(traps);
            //System.err.println(String.format("[%d] avoid=%s", id, avoid));

            paths = state.findShortestPaths(origin, targets, avoid, false, searchWhile);
            //if (!paths.isEmpty()) System.err.println(String.format("[%d] unsafe=%s", id, paths.get(0)));
        }
        return paths;
    }

    @Override
    public String toString() {
        return String.format("hackman.Player[%d] at %s", this.id, this.position);
    }
}
