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

package io.riddles.bookinggame.game.enemy;

import java.awt.Point;

import io.riddles.bookinggame.game.move.MoveType;
import io.riddles.bookinggame.game.state.BookingGameState;

/**
 * io.riddles.catchfrauds.game.enemy.Enemy - Created on 3-6-16
 *
 * [description]
 *
 * @author Joost de Meij - joost@riddles.io, Jim van Eeden - jim@riddles.io
 */
public class BookingGameEnemy {

    private Point coordinate;
    private Point prevCoordinate;
    private MoveType direction;
    private EnemyAIInterface enemyAI;
    private int id;

    public BookingGameEnemy(int id, Point coordinate, MoveType direction,
                            EnemyAIInterface enemyAI) {
        this.coordinate = coordinate;
        this.direction = direction;
        this.enemyAI = enemyAI;
        this.id = id;
    }

    public BookingGameEnemy(int id, Point coordinate, Point prevCoordinate,
                            MoveType direction, EnemyAIInterface enemyAI) {
        this.coordinate = coordinate;
        this.prevCoordinate = prevCoordinate;
        this.direction = direction;
        this.enemyAI = enemyAI;
        this.id = id;
    }

    public BookingGameEnemy clone() {
        Point clonedCoordinate = new Point(this.coordinate);
        Point clonedPrevCoordinate = null;

        if (this.prevCoordinate != null) {
            clonedPrevCoordinate = new Point(this.prevCoordinate);
        }

        return new BookingGameEnemy(this.id, clonedCoordinate,
                clonedPrevCoordinate, this.direction, this.enemyAI);
    }

    public void performMovement(BookingGameState state) {
        Point newCoordinate = this.enemyAI.transform(this, state);
        setCoordinate(newCoordinate);
    }

    public void setCoordinate(Point coordinate) {
        this.prevCoordinate = this.coordinate;
        this.coordinate = coordinate;
        setDirection(getDirection(this.prevCoordinate, this.coordinate));
    }

    private MoveType getDirection(Point oldCoordinate, Point newCoordinate) {
        if (newCoordinate.x > oldCoordinate.x) return MoveType.RIGHT;
        if (newCoordinate.x < oldCoordinate.x) return MoveType.LEFT;
        if (newCoordinate.y > oldCoordinate.y) return MoveType.DOWN;
        if (newCoordinate.y < oldCoordinate.y) return MoveType.UP;
        return null;
    }

    public MoveType getDirection() {
        return this.direction;
    }

    public void setDirection(MoveType d) {
        this.direction = d;
    }

    public String toString() {
        return "Enemy " + this.coordinate;
    }

    public Point getCoordinate() {
        return this.coordinate;
    }

    public int getId() {
        return this.id;
    }
}
