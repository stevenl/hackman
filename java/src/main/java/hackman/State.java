/**
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

public class State extends Field {

    private int roundId;
    private List<Player> players;

    State(int roundId, String field, List<Player> players, Game game) {
        super(game.getFieldWidth(), game.getFieldHeight(), field);

        this.roundId = roundId;
        this.players = new ArrayList<>(2);

        for (Player player : players) {
            int playerId = player.getId();
            player.setPosition(this.getPlayerPosition(playerId));

            int index = playerId == game.getMyId() ? 0 : 1;
            this.players.add(index, player);
        }
    }

    public Player getMyPlayer() {
        return this.players.get(0);
    }

    public Player getOpponentPlayer() {
        return this.players.get(1);
    }
}
