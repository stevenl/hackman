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
 * hackman.Bot
 *
 * Decides how the bot should move
 *
 * @author Steven Lee - stevenwh.lee@gmail.com
 */

public class Bot {

    private int myId;
    private State prevState = null;

    /**
     * Does a move action.
     *
     * @param state The current state of the game
     * @return A Move object
     */
    Move doMove(State state) {
        //System.err.println("\n" + state);

        Player myPlayer  = state.getMyPlayer();
        Player oppPlayer = state.getOpponentPlayer();

        List<Path> myPaths  = myPlayer.getPaths();
        List<Path> oppPaths = oppPlayer.getPaths();

        //System.err.println("myPath=" + myPaths.get(0));
        //System.err.println("oppPath=" + oppPaths.get(0));
        //System.err.println("myPaths=" + myPaths);
        //System.err.println("oppPaths=" + oppPaths);

        Move move = null;
        if (!myPaths.isEmpty()) {
            Map<Move, Float> moveScores = calculateMoveScores(state, myPaths, oppPaths);
            //System.err.println("scores=" + moveScores);

            // Choose the move with the highest score
            move = moveScores.entrySet().stream()
                    .max((e1, e2) -> Float.compare(e1.getValue(), e2.getValue()))
                    .map(e -> e.getKey())
                    .orElse(null);
            //System.err.println("move=" + move);
        }

        this.prevState = state;
        return move != null ? move : Move.PASS;
    }

    private Map<Move, Float> calculateMoveScores(State state, List<Path> myPaths, List<Path> oppPaths) {
        Map<Point, Path> oppPathsByTarget = new HashMap<>();
        Map<Point, Integer> oppPathRank = new HashMap<>();
        int i = 1;
        for (Path oppPath : oppPaths) {
            Point target = oppPath.end();
            oppPathsByTarget.put(target, oppPath);
            oppPathRank.put(target, i++);
        }

        Point origin = state.getMyPlayer().getPosition();
        Set<Point> targets = new HashSet<>();
        targets.add(state.getOpponentPlayer().getPosition());
        List<Path> toOpponent = state.findShortestPaths(origin, targets, null, true, 0);
        int nrMovesToOpponent = !toOpponent.isEmpty() ? toOpponent.get(0).nrMoves() : 0;

        Map<Move, Float> moveScores = new HashMap<>();
        for (Path myPath : myPaths) {
            Point target = myPath.end();
            Path oppPath = oppPathsByTarget.get(target);

            float score = 1.0f / myPath.nrMoves();

            if (oppPath != null) {
                // Cut our losses: Ditch targets that opponent can get to first
                if (oppPath.nrMoves() < myPath.nrMoves()) {
                    int pathRank = oppPathRank.get(target);
                    score *= 1.0f - (1.0f / pathRank);
                }
                // Don't let him have any: Prefer targets nearer to opponent
                else if (oppPath.nrMoves() < nrMovesToOpponent) {
                    score *= 1.4;
                }
            }

            Move move = myPath.move(0);
            float newScore = moveScores.getOrDefault(move, 0.0f);
            newScore += score;

            moveScores.put(move, newScore);
        }
        return moveScores;
    }
}
