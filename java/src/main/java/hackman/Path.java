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

import move.Move;

import java.util.ArrayList;
import java.util.List;

/**
 * hackman.Path
 *
 * @author Steven Lee - stevenwh.lee@gmail.com
 */

public class Path {

    private Point start;
    private Point end;
    private List<Move> moves;

    public Path(Point start) {
        this.start = start;
        this.end   = start;
        this.moves = new ArrayList<>();
    }

    public Path(Point start, Point end, List<Move> moves) {
        this.start = start;
        this.end   = end;
        this.moves = moves;
    }

    public Path(Path path, Move nextMove) {
        List<Move> moves = new ArrayList<>(path.moves());
        moves.add(nextMove);

        this.start = path.start();
        this.end   = new Point(path.end(), nextMove);
        this.moves = moves;
    }

    public Point start() {
        return this.start;
    }

    public Point end() {
        return this.end;
    }

    public List<Move> moves() {
        return this.moves;
    }

    @Override
    public String toString() {
        return String.format("%s, %s: %s", this.start, this.end, this.moves);
    }
}
