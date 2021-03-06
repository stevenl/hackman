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

import hackman.Point;

/**
 * hackman.Move
 *
 * All move types
 *
 * @author Jim van Eeden - jim@riddles.io
 */
public enum Move {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    PASS;

    private static final Point up    = new Point(0, -1);
    private static final Point down  = new Point(0, +1);
    private static final Point left  = new Point(-1, 0);
    private static final Point right = new Point(+1, 0);
    private static final Point pass  = new Point(0, 0);

    public Point delta() {
        switch (this) {
            case UP:    return up;
            case DOWN:  return down;
            case LEFT:  return left;
            case RIGHT: return right;
            default:    return pass;
        }
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
