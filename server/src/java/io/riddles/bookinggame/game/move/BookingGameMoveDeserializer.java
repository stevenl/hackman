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

package io.riddles.bookinggame.game.move;

import io.riddles.bookinggame.game.player.BookingGamePlayer;
import io.riddles.javainterface.exception.InvalidInputException;
import io.riddles.javainterface.serialize.Deserializer;

/**
 * io.riddles.bookinggame.game.move.BookingGameMoveDeserializer - Created on 6/27/16
 *
 * [description]
 *
 * @author Joost de Meij - joost@riddles.io, Jim van Eeden - jim@riddles.io
 */
public class BookingGameMoveDeserializer implements Deserializer<BookingGameMove> {

    private BookingGamePlayer player;

    public BookingGameMoveDeserializer(BookingGamePlayer player) {
        this.player = player;
    }

    @Override
    public BookingGameMove traverse(String string) {
        if (this.player.isParalyzed()) {
            return new BookingGameMove(this.player, MoveType.PARALYZED);
        }

        try {
            return visitMove(string);
        } catch (InvalidInputException ex) {
            this.player.sendWarning(ex.getMessage());
            return new BookingGameMove(this.player, ex);
        } catch (Exception ex) {
            this.player.sendWarning(ex.getMessage());
            return new BookingGameMove(
                this.player, new InvalidInputException("Failed to parse move"));
        }
    }

    private BookingGameMove visitMove(String input) throws InvalidInputException {
        String[] split = input.split(" ");

        MoveType type = visitAssessment(split[0]);

         return new BookingGameMove(this.player, type);
    }

    private MoveType visitAssessment(String input) throws InvalidInputException {
        switch (input) {
            case "up":
                return MoveType.UP;
            case "down":
                return MoveType.DOWN;
            case "left":
                return MoveType.LEFT;
            case "right":
                return MoveType.RIGHT;
            case "pass":
                return MoveType.PASS;
            default:
                throw new InvalidInputException("Move isn't valid");
        }
    }
}
