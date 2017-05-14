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

/**
 * hackman.Point
 *
 * A point representing a location in (x,y) coordinate space, specified in
 * integer precision.
 *
 * @author Steven Lee - stevenwh.lee@gmail.com
 */
public class Point extends java.awt.Point {

    public Point(int x, int y) {
        super(x, y);
    }

    public Point(Point point, Move toApply) {
        this((int) point.getX(), (int) point.getY());

        Point delta = toApply.delta();
        translate((int) delta.getX(), (int) delta.getY());
    }
}
