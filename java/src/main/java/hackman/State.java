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
 * hackman.State
 *
 * @author Steven Lee - stevenwh.lee@gmail.com
 */
public class State extends Field {

    private Player myPlayer;
    private Player oppPlayer;
    private Map<Integer, Player> players; // id : player
    private State prevState;

    State(String field, List<Player> players, State prevState, Game game) {
        super(game.getFieldWidth(), game.getFieldHeight(), field);

        this.prevState = prevState;
        this.players   = new HashMap<>(2);

        for (Player player : players) {
            int playerId = player.getId();
            player.setPosition(this.getPlayerPosition(playerId));
            player.setState(this);

            if (playerId == game.getMyId())
                this.myPlayer = player;
            else
                this.oppPlayer = player;

            this.players.put(playerId, player);
        }
    }

    public Player getPlayer(int playerId) {
        return this.players.get(playerId);
    }

    public Player getMyPlayer() {
        return this.myPlayer;
    }

    public Player getOpponentPlayer() {
        return this.oppPlayer;
    }

    public Set<Point> getPreviousEnemyPositions() {
        Set<Point> prevEnemyPositions = new HashSet<>();

        if (this.prevState != null) {
            Set<Point> enemies = this.prevState.getEnemyPositions();
            prevEnemyPositions.addAll(enemies);
        }
        return prevEnemyPositions;
    }
}
