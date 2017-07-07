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

package io.riddles.bookinggame.game.player;

import java.awt.*;

import io.riddles.bookinggame.BookingGame;
import io.riddles.javainterface.game.player.AbstractPlayer;

/**
 * io.riddles.catchfrauds.game.player.BookingGameMovePlayer - Created on 3-6-16
 *
 * [description]
 *
 * @author Joost de Meij - joost@riddles.io, Jim van Eeden - jim@riddles.io
 */
public class BookingGamePlayer extends AbstractPlayer {

    private int snippets;
    private boolean weapon;
    private Point coordinate;
    private int paralysis;
    private boolean isAlive;

    public BookingGamePlayer(int id) {
        super(id);
        this.snippets = 0;
        this.weapon = false;
        this.paralysis = 0;
        this.coordinate = new Point(0, 0);
        this.isAlive = true;
    }

    public BookingGamePlayer(int id, int snippets, boolean weapon,
                             int paralysis, boolean isAlive, Point coordinate) {
        super(id);
        this.snippets = snippets;
        this.weapon = weapon;
        this.paralysis = paralysis;
        this.coordinate = coordinate;
        this.isAlive = isAlive;
    }

    public BookingGamePlayer clone() {
        Point clonedCoordinate = new Point(this.coordinate);
        return new BookingGamePlayer(this.getId(), this.snippets, this.weapon,
                this.paralysis, this.isAlive, clonedCoordinate);
    }

    public void updateSnippets(int delta) {
        this.snippets += delta;

        if (this.snippets < 0) {
            this.snippets = 0;
            this.isAlive = false;
        }
    }

    public void setWeapon(boolean weapon) {
        this.weapon = weapon;
    }

    public boolean hasWeapon() {
        return this.weapon;
    }

    public int getSnippets() {
        return this.snippets;
    }

    public Point getCoordinate() { return this.coordinate; }

    public void setCoordinate(Point c) {
        this.coordinate = c;
    }

    public String toString() {
        return "Player " + this.getId() + " coord " + this.getCoordinate() + " snippets " + this.snippets + " weapons " + weapon + " paralysis " + paralysis;
    }

    public void updateParalysis() {
        if (this.paralysis > 0) {
            this.paralysis--;
        }
    }

    public Boolean isParalyzed() {
        return (this.paralysis > 0);
    }

    public void paralyse(int p) {
        this.paralysis += p;
    }

    public boolean isAlive() {
        return this.isAlive;
    }
}
