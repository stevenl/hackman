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
    private String name;
    private int snippets;
    private boolean hasWeapon;
    private boolean isParalyzed;
    private Point position = null;

    Player(int id, String name, int snippets, boolean hasWeapon, boolean isParalyzed) {
        this.id          = id;
        this.name        = name;
        this.snippets    = snippets;
        this.hasWeapon   = hasWeapon;
        this.isParalyzed = isParalyzed;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
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

    @Override
    public String toString() {
        return String.format("hackman.Player[%d] at %s", this.id, this.position);
    }
}
