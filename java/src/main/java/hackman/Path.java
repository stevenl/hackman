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
import java.util.stream.Collectors;

/**
 * hackman.Path
 *
 * @author Steven Lee - stevenwh.lee@gmail.com
 */

public class Path {

    private Point start;
    private Point end;
    private List<Move> moves;
    private List<Point> positions;
    private Map<Integer, Integer> threatsByMove; // moveNr : nrThreats
    private List<Integer> intersectionMoves;

    private Path() {
        this.start = null;
        this.end   = null;

        this.moves     = new ArrayList<>();
        this.positions = new ArrayList<>();
        this.threatsByMove = new HashMap<>();
        this.intersectionMoves = new ArrayList<>();
    }

    public Path(Point start) {
        this();
        this.start = start;
        this.end   = start;
        this.positions.add(start);
    }

    public Path(Path path, Point nextPosition, Move nextMove, int nrThreats, State state) {
        this(path);

        if (!state.isPointValid(nextPosition))
            throw new RuntimeException("Invalid point: " + nextPosition);

        this.end = nextPosition;
        this.positions.add(nextPosition);

        this.moves.add(nextMove);

        if (nrThreats > 0) this.threatsByMove.put(this.moves.size(), nrThreats);

        boolean isIntersection = state.getValidMoves(nextPosition).size() > 2;
        if (isIntersection) this.intersectionMoves.add(this.moves.size());
    }

    private Path(Path toCopy) {
        this(toCopy.start, toCopy.end, toCopy.moves, toCopy.positions, toCopy.threatsByMove, toCopy.intersectionMoves);
    }

    private Path(Point start, Point end, List<Move> moves, List<Point> positions, Map<Integer, Integer> threatsByMove, List<Integer> intersectionMoves) {
        this();
        this.start = start;
        this.end   = end;

        this.moves.addAll(moves);
        this.positions.addAll(positions);
        this.threatsByMove.putAll(threatsByMove);
        this.intersectionMoves.addAll(intersectionMoves);
    }

    Path subPath(Point start, Point end) {
        List<Move> moves = new ArrayList<>();
        List<Point> positions = new ArrayList<>();
        List<Integer> intersectionMoves = new ArrayList<>();
        Map<Integer, Integer> threatsByMove = new HashMap<>();

        // Find the starting point
        int i = 0;
        int intNr = 0;
        for (Point pos : this.positions) {
            if (intNr < this.intersectionMoves.size()) {
                if (i == this.intersectionMoves.get(intNr))
                    intNr++;
            }
            if (pos.equals(start))
                break;
            i++;
        }
        if (i == nrMoves())
            throw new RuntimeException("start point is not in the path: " + start);

        // Find the ending point
        int j = i;
        for (Point pos : this.positions.subList(i, this.positions.size() - 1)) {
            positions.add(pos);

            if (intNr < this.intersectionMoves.size()) {
                if (j == this.intersectionMoves.get(intNr)) {
                    intersectionMoves.add(j);
                    intNr++;
                }
            }

            if (j > i && this.threatsByMove.containsKey(j))
                threatsByMove.put(j - i, this.threatsByMove.get(j));

            if (pos.equals(end))
                break;

            moves.add(this.moves.get(j));
            j++;
        }
        if (j == nrMoves())
            throw new RuntimeException("end point is not in the path: " + end);

        Path subPath = new Path(start, end, moves, positions, threatsByMove, intersectionMoves);

        return subPath;
    }

    public Point start() {
        return this.start;
    }

    public Point end() {
        return this.end;
    }

    public List<Point> getPositions() {
        return this.positions;
    }

    public Point position(int n) {
        if (n < 0)
            throw new IndexOutOfBoundsException("'n' must not be less than 0");
        else if (n > this.moves.size())
            throw new IndexOutOfBoundsException("'n' must not exceed the number of moves");

        int i = this.positions.size();
        while (i <= n) {
            if (i > 0) {
                Point last = this.positions.get(i - 1);
                Point curr = new Point(last, this.moves.get(i - 1));
                this.positions.add(curr);
            } else {
                this.positions.add(this.start);
            }
            i++;
        }
        return this.positions.get(n);
    }

    public Move move(int n) {
        return this.moves.get(n);
    }

    public int nrMoves() {
        return this.moves.size();
    }

    public int getMoveNr(Point pos) {
        int i = 0;
        for (Point p : this.positions) {
            if (p.equals(pos))
                return i;
            i++;
        }
        throw new RuntimeException("Path does not contains the position: " + pos);
    }

    int nrThreats() {
        return this.threatsByMove.size();
    }

    float getThreatScore() {
        float score = 0;
        for (Map.Entry<Integer, Integer> e : this.threatsByMove.entrySet())
            score += (float) e.getValue() / e.getKey(); // nrThreat / nrMoves

        return score;
    }

    public List<Integer> getIntersectionMoves() {
        return this.intersectionMoves;
    }

    //public int getIntersectionMove(int intersectionIndex) {
    //    return this.intersectionMoves.get(intersectionIndex);
    //}

    public int nrIntersections() {
        return this.intersectionMoves.size();
    }

    public List<Point> getIntersectingPoints(Path other) {
        Set<Point> otherPositions = new HashSet<>(other.positions);
        List<Point> intersectingPoints = this.positions.stream()
                .filter(p -> otherPositions.contains(p))
                .collect(Collectors.toList());

        return intersectingPoints;
    }

    @Override
    public String toString() {
        return String.format("%s, %s: [%d]%s [%d]", this.start, this.end, this.nrMoves(), this.moves, this.nrThreats());
    }
}
