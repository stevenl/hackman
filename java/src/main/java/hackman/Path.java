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
    private List<Integer> intersectionMoves;

    private Path() {
        this.start = null;
        this.end   = null;

        this.moves     = new ArrayList<>();
        this.positions = new ArrayList<>();
        this.intersectionMoves = new ArrayList<>();
    }

    public Path(Point start) {
        this();
        this.start = start;
        this.end   = start;
        this.positions.add(start);
    }

    public Path(Path path, Move nextMove, Field field) {
        this();

        this.start = path.start;
        this.end   = new Point(path.end, nextMove);

        if (!field.isPointValid(this.end))
            throw new RuntimeException("Invalid point: " + this.end);

        this.moves.addAll(path.moves);
        this.moves.add(nextMove);

        this.positions.addAll(path.positions);
        this.positions.add(this.end);

        this.intersectionMoves.addAll(path.intersectionMoves);

        boolean isIntersection = field.getValidMoves(this.end).size() > 2;
        if (isIntersection) this.intersectionMoves.add(this.moves.size());
    }

    public Point start() {
        return this.start;
    }

    public Point end() {
        return this.end;
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

    public List<Integer> getIntersectionMoves() {
        return this.intersectionMoves;
    }

    public int nrIntersections() {
        return this.intersectionMoves.size();
    }

    @Override
    public String toString() {
        return String.format("%s, %s: %s", this.start, this.end, this.moves);
    }
}
