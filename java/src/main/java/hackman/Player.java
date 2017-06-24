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
import java.util.stream.IntStream;

/**
 * hackman.Player
 *
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

        // Reset cached attributes
        this.toOpponent = null;
        this.pathsToThreats = null;
        this.threats = null;
        this.immediateThreats = null;
        this.traps = null;
    }

    /*****************************************************/

    /**
     * Gets the opponent of this player.
     *
     * @return The opponent player
     */
    private Player getOpponent() {
        int oppId = (this.id + 1) % 2;
        return this.state.getPlayer(oppId);
    }

    /**
     * Gets a safe path to the opponent player
     * or null if no such path can be found.
     *
     * @return A path to the opponent player.
     */
    private List<Path> toOpponent = null;
    private Path getPathToOpponent() {
        if (this.toOpponent == null) {
            Set<Point> targets = new HashSet<>();
            targets.add(getOpponent().getPosition());
            this.toOpponent = state.findShortestPaths(this.position, targets, null, true, null);
        }

        if (!this.toOpponent.isEmpty())
            return this.toOpponent.get(0);
        else
            return null;
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
    private Map<Point, Integer> getPotentialThreats() {
        Map<Point, Integer> threats = new HashMap<>();
        threats.putAll(state.getEnemyPositions());

        Player opponent = getOpponent();
        if (opponent.hasWeapon()) {
            Point pos = opponent.getPosition();
            if (!threats.containsKey(pos))
                threats.put(pos, 1);
            else
                threats.compute(pos, (k, v) -> v + 1);
        }

        //System.err.println(String.format("[%d] potentialThreats=%s", id, threats));
        return threats;
    }

    /**
     * Like getPotentialThreats() except that it does not include enemies
     * that are not threats because they are moving away.
     *
     * @return The positions of the threats.
     */
    private Map<Point, Integer> threats = null;
    private Map<Point, Integer> getThreats() {
        if (this.threats == null) {
            this.threats = new HashMap<>();
            Map<Point, Integer> potentialThreats = getPotentialThreats();
            Map<Point, Integer> prevEnemyPositions = state.getPreviousEnemyPositions();

            for (Path toThreat : getPathsToThreats()) {
                Point pos = toThreat.end();
                int nrThreats = potentialThreats.get(pos);

                // Don't count the threats that are moving away
                Point penultimatePos = toThreat.position(toThreat.nrMoves() - 1);
                nrThreats -= prevEnemyPositions.getOrDefault(penultimatePos, 0);

                this.threats.put(pos, nrThreats);
            }
            //System.err.println(String.format("[%d] threats=%s", id, threats));
        }
        return this.threats;
    }

    /**
     * Gets the threats that are 1-2 moves away. These are the threats that
     * can harm us in the next move. Those that are 2 steps away can harm us
     * by moving to the position that we want to move to.
     *
     * @return The positions of the immediate threats
     */
    private Map<Point, Integer> immediateThreats = null;
    private Map<Point, Integer> getImmediateThreats() {
        if (this.immediateThreats == null) {
            this.immediateThreats = new HashMap<>();
            Map<Point, Integer> threats = getThreats();

            for (Path toThreat : getPathsToThreats()) {
                if (toThreat.nrMoves() > 2)
                    continue;

                Point pos = toThreat.position(1);
                int nrThreats = threats.get(toThreat.end());

                if (!this.immediateThreats.containsKey(pos))
                    this.immediateThreats.put(pos, nrThreats);
                else
                    this.immediateThreats.compute(pos, (k, v) -> v + nrThreats);
            }
            //System.err.println(String.format("[%d] immediate=%s", id, immediateThreats));
        }
        return this.immediateThreats;
    }

    /**
     * Gets the threats that are less than two intersection away from
     * the player. These threats are close enough such that they will
     * often not be detected as traps (because the player can reach
     * the first intersection before them).
     *
     * @return The positions of the nearby threats
     */
    private Map<Point, Integer> getNearbyThreats() {
        Map<Point, Integer> threats = getThreats();
        Map<Point, Integer> nearbyThreats = getPathsToThreats().stream()
                .filter(path -> path.nrMoves() > 2 && path.nrIntersections() < 2)
                .map(path -> path.end())
                .collect(Collectors.toMap(pos -> pos, pos -> threats.get(pos)));
        nearbyThreats.putAll(getImmediateThreats());

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
            Map<Point, Integer> threats = getPotentialThreats();
            this.pathsToThreats = state.findShortestPaths(this.position, threats.keySet(), null, true, null);

            // Is the threat at an intersection? Consider alternatives routes to reach them
            Set<Point> threatsAtIntersections = new HashSet<>();
            Map<Point, Integer> pathsToAvoid = new HashMap<>();
            for (Path toThreat : this.pathsToThreats) {
                Point threat = toThreat.end();
                boolean isThreatAtIntersection = state.getValidMoves(threat).size() > 2;

                if (isThreatAtIntersection) {
                    threatsAtIntersections.add(threat);

                    Point penultimatePos = toThreat.position(toThreat.nrMoves() - 1);
                    pathsToAvoid.put(penultimatePos, 1);
                }
            }
            List<Path> altPathsToThreats = state.findShortestPaths(this.position, threatsAtIntersections, pathsToAvoid, true, null);
            this.pathsToThreats.addAll(altPathsToThreats);

            // Is the enemy moving away? It's unlikely he will come back this way
            Map<Point, Integer> prevEnemyPositions = state.getPreviousEnemyPositions();
            if (!prevEnemyPositions.isEmpty()) {
                this.pathsToThreats.removeIf(toThreat -> {
                    Point pos = toThreat.end();
                    Point penultimatePos = toThreat.position(toThreat.nrMoves() - 1);

                    return prevEnemyPositions.containsKey(penultimatePos) &&
                            prevEnemyPositions.get(penultimatePos) == threats.get(pos);
                });
            }
            //System.err.println(String.format("[%d] toThreats=%s", id, pathsToThreats));
        }
        return this.pathsToThreats;
    }

    /**
     * Determines whether this player is trapped in by threats with
     * no way of escaping harm.
     *
     * @return True if the player is trapped.
     */
    boolean isTrapped() {
        // Am I surrounded by immediate threats?
        List<Path> escapePaths = state.findShortestPaths(this.position, null, getImmediateThreats(), true, p -> p.nrMoves() < 3);
        if (escapePaths.isEmpty())
            return true;

        Map<Point, Integer> avoid = new HashMap<>();
        getNearbyThreats().forEach((k, v) -> avoid.merge(k, v, Integer::sum));

        // Are the paths to the closest intersections blocked?
        escapePaths = state.findShortestPaths(this.position, null, avoid, true, p -> p.nrIntersections() == 0);

        //System.err.println(String.format("[%d] escape=%s", id, escapePaths));
        return escapePaths.isEmpty();
    }

    /**
     * Determines whether this player can be trapped in by threats.
     *
     * @return True if the player can be trapped.
     */
    boolean canBeTrapped() {
        Map<Point, Integer> avoid = new HashMap<>();
        getNearbyThreats().forEach((k, v) -> avoid.merge(k, v, Integer::sum));
        getTraps().forEach((k, v) -> avoid.merge(k, v, Integer::sum));

        List<Path> escapePaths = state.findShortestPaths(this.position, null, avoid, !hasWeapon(), p -> p.nrIntersections() < 2);
        return escapePaths.isEmpty();
    }

    /**
     * Finds the intersections that can be reached by a bug before you.
     * If they can, then they can trap you in.
     *
     * @return The set of intersection points where you can be trapped
     */
    private Map<Point, Integer> traps = null;
    private Map<Point, Integer> getTraps() {
        if (this.traps == null)
            initTraps();

        return this.traps;
    }

    private void initTraps() {
        this.traps = new HashMap<>();
        Set<Point> targets = this.getTargets();

        Map<Point, Integer> intersectionOptions = new HashMap<>();
        for (Path toThreat : getPathsToThreats()) {
            int maxMoves = toThreat.nrMoves();

            // These have already been detected as immediate threats and we want to
            // avoid double counting them because they are positioned differently
            if (maxMoves <= 2) continue;

            // Don't be too cautious: far-away threats may move somewhere else
            if (toThreat.nrMoves() > 10 && toThreat.nrIntersections() > 3)
                continue;

            // Find any intersections between you and the bug
            //System.err.println(String.format("[%d] toThreat=%s", id, toThreat));
            Deque<Point> intersectionStack = new ArrayDeque<>();
            for (int i = 1; i <= maxMoves; i++) {
                Point pos = toThreat.position(i);

                // Is it an intersection?
                // (Don't count the point we come from)
                int nrMoveOptions = state.getValidMoves(pos).size() - 1;
                boolean isIntersection = nrMoveOptions > 1;
                if (targets.contains(pos) && intersectionStack.isEmpty() && !isIntersection) {
                    int movesToTarget = i;
                    int threatToTarget = maxMoves - i;

                    // Can the threat reach the target before you?
                    if (movesToTarget >= threatToTarget) {
                        //System.err.println(String.format("trap1=[%d >= %d]=%s", movesToTarget, threatToTarget, pos));
                        this.traps.put(pos, 1);
                    } else {
                        // ... or can the threat the block player after reaching the target
                        intersectionOptions.put(pos, nrMoveOptions);
                        intersectionStack.addFirst(pos);
                    }
                }
                else if (isIntersection) {
                    if (!intersectionOptions.containsKey(pos))
                        intersectionOptions.put(pos, nrMoveOptions);
                    else
                        nrMoveOptions = intersectionOptions.get(pos);

                    int movesToIntersection = i;
                    int threatToIntersection = maxMoves - i;

                    // Can the threat reach the intersection before you?
                    if (movesToIntersection >= threatToIntersection) {
                        //System.err.println(String.format("trap2=[%d >= %d]=%s", movesToIntersection, threatToIntersection, pos));
                        this.traps.put(pos, 1);

                        // If all paths from an intersection lead to traps then
                        // the intersection should also be considered a trap
                        while (intersectionStack.peekFirst() != null) {
                            Point lastIntersection = intersectionStack.removeFirst();
                            int options = intersectionOptions.get(lastIntersection);

                            intersectionOptions.put(lastIntersection, options - 1);

                            if (options == 1) {
                                //System.err.println("trap3=" + lastIntersection);
                                this.traps.put(lastIntersection, 1);
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
                        if (closedIntersection == null && !this.traps.containsKey(closedIntersection)) {
                            //System.err.println("trap4=" + pos);
                            this.traps.put(pos, 1);
                        }
                        break;
                    }
                    intersectionStack.addFirst(pos);
                }
            }
        }
        //System.err.println(String.format("[%d] traps=%s", id, traps));
    }

    private List<Path> getPaths() {
        // Don't be a sitting duck if there are no targets:
        // Get any safe paths within 8 moves
        Predicate<Path> searchWhile = null;
        if (getTargets().isEmpty())
            searchWhile = (p -> p.nrMoves() <= 8);

        List<Path> paths = null;
        Map<Point, Integer> avoid = new HashMap<>();
        getNearbyThreats().forEach((k, v) -> avoid.merge(k, v, Integer::sum));
        getTraps().forEach((k, v) -> avoid.merge(k, v, Integer::sum));
        //System.err.println(String.format("[%d] avoid1=%s", id, avoid));

        if (!this.hasWeapon) {
            if (isTrapped()) {
                // Minimise damage since we can't avoid them completely
                searchWhile = (p -> p.nrMoves() <= 8 || p.nrIntersections() < 2);
                paths = state.findShortestPaths(this.position, null, avoid, false, searchWhile);
                //if (!paths.isEmpty()) System.err.println(String.format("[%d] safe0=%s", id, paths.get(0)));
            }

            // Safe strategy: Avoid all threats and traps
            if (paths == null) {
                paths = state.findShortestPathsPerDirection(this.position, getTargets(), avoid, true, searchWhile);
                //if (!paths.isEmpty()) System.err.println(String.format("[%d] safe1=%s", id, paths.get(0)));
            }

            // Fallback: Going after the target is too dangerous
            if (paths.isEmpty()) {
                searchWhile = (p -> p.nrMoves() <= 8);
                paths = state.findShortestPaths(this.position, null, avoid, true, searchWhile);
                //if (!paths.isEmpty()) System.err.println(String.format("[%d] safe2=%s", id, paths.get(0)));
            }

            // Fallback: Minimise damage since we are likely trapped (just don't freeze)
            if (paths.isEmpty()) {
                searchWhile = (p -> p.nrMoves() <= 8 || p.nrIntersections() < 2);
                paths = state.findShortestPaths(this.position, null, avoid, false, searchWhile);
                //if (!paths.isEmpty()) System.err.println(String.format("[%d] safe3=%s", id, paths.get(0)));
            }
        }
        else {
            // With weapon: Allowed to avoid one threat
            // Trap the opponent
            Player opponent = getOpponent();
            if (!opponent.hasWeapon() && !opponent.isTrapped() && opponent.canBeTrapped()) {
                Path toOpponent = getPathToOpponent();

                // Check if an enemy stands in the way of setting the trap
                Map<Point, Integer> threats = getThreats();
                boolean pathHasEnemy = toOpponent.getPositions().stream()
                        .anyMatch(pos -> threats.containsKey(pos));

                if (toOpponent != null && !pathHasEnemy) {
                    Map<Point, Integer> oppTraps = opponent.getTraps();

                    Point destination = oppTraps.containsKey(this.position) ? this.position : null;
                    for (int n : toOpponent.getIntersectionMoves()) {
                        Point pos = toOpponent.position(n);
                        if (oppTraps.containsKey(pos))
                            destination = pos;
                    }

                    if (destination != null) {
                        paths = new ArrayList<>();
                        Path toSetTrap = toOpponent.subPath(this.position, destination);
                        paths.add(toSetTrap);
                        //if (!paths.isEmpty()) System.err.println(String.format("[%d] weapon1=%s", id, paths.get(0)));
                    }
                }
            }

            // Go for a target
            if (paths == null) {
                // Is it worth using up the weapon? Not if it doesn't save many steps to reach the target
                paths = state.findShortestPathsPerDirection(this.position, getTargets(), avoid, false, searchWhile);
                //if (!paths.isEmpty()) System.err.println(String.format("[%d] weapon2=%s", id, paths.get(0)));
            }
        }
        return paths;
    }

    /**
     * Does a move action.
     *
     * @return A Move object
     */
    public Move doMove() {
        //System.err.println("\n" + state);

        List<Path> myPaths  = this.getPaths();
        List<Path> oppPaths = this.getOpponent().getPaths();

        //System.err.println("myPath=" + myPaths.get(0));
        //System.err.println("oppPath=" + oppPaths.get(0));
        //for (Path p : myPaths) System.err.println(String.format("[%d] myPath=%s\n", id, p));
        //for (Path p : oppPaths) System.err.println(String.format("[%d] oppPath=%s\n", id, p));

        Map<Point, Path> oppPathsByTarget = new HashMap<>();
        Map<Point, Integer> oppPathRank = new HashMap<>();
        int i = 1;
        for (Path oppPath : oppPaths) {
            Point target = oppPath.end();

            // Consider the first (shortest) path only
            if (oppPathsByTarget.containsKey(target)) continue;

            oppPathsByTarget.put(target, oppPath);
            oppPathRank.put(target, i++);
        }

        Path toOpponent = getPathToOpponent();
        int nrMovesToOpponent = toOpponent != null ? toOpponent.nrMoves() : 0;

        Map<Move, Float> moveScores = new HashMap<>();
        for (Path myPath : myPaths) {
            Point target = myPath.end();
            Path oppPath = oppPathsByTarget.get(target);

            float score = 1.0f / myPath.nrMoves();

            // More threats encountered means lower score
            if (myPath.nrThreats() > 0)
                score *= Math.pow(0.9, myPath.nrThreats());

            if (oppPath != null) {
                List<Point> intersectingPoints = myPath.getIntersectingPoints(oppPath);
                Point meetingPoint = intersectingPoints.size() > 0 ? intersectingPoints.get(0) : null;

                // Cut our losses: Ditch targets that opponent can get to first
                if (oppPath.nrMoves() < myPath.nrMoves()) {
                    int pathRank = oppPathRank.get(target);
                    score *= 1.0f - (0.98f / pathRank);
                    // 0.98 is so we don't have equal 0 scores in each direction when there is only one target
                }
                // Don't let him have any: Prefer targets nearer to opponent
                else if (meetingPoint != null && oppPath.getMoveNr(meetingPoint) < nrMovesToOpponent) {
                    score *= 1.55;
                }
            }

            Move move = myPath.nrMoves() > 0 ? myPath.move(0) : Move.PASS;
            float newScore = moveScores.getOrDefault(move, 0.0f);
            newScore += score;

            moveScores.put(move, newScore);
        }
        //System.err.println("scores=" + moveScores);

        // Choose the move with the highest score
        // If multiple moves have the same max score, choose one at random
        // (this is intentionally non-deterministic to avoid deadlock moves)
        float maxScore = moveScores.values().stream()
                .max((v1, v2) -> Float.compare(v1, v2))
                .orElse(0.0f);
        Move move = moveScores.entrySet().stream()
                .filter(entry -> entry.getValue() == maxScore)
                .findAny()
                .map(entry -> entry.getKey())
                .orElse(null);

        //System.err.println("move=" + move);
        return move != null ? move : Move.PASS;
    }

    @Override
    public String toString() {
        return String.format("hackman.Player[%d] at %s", this.id, this.position);
    }
}
