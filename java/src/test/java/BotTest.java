package hackman;

import org.junit.Test;
import static org.junit.Assert.assertNotEquals;

public class BotTest {

    private Bot bot;

    public BotTest() {
        bot = new Bot();
    }

    @Test
    public void dontFollowOpponent() throws Exception {
        State s = new State();
        Field f = s.getField();

        // Settings
        s.setMyId(0);
        s.setOpponentId(1);
        s.setMyName("player0");
        s.getPlayers().put("player0", new Player("player0"));
        s.getPlayers().put("player1", new Player("player1"));
        s.getMyPlayer().setId(0);
        s.getOpponentPlayer().setId(1);

        f.setMyId(0);
        f.setOpponentId(1);
        f.setHeight(14);
        f.setWidth(20);

        // Update
        f.parseFromString(".,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,x,x,x,x,x,.,x,x,x,x,x,x,.,x,x,x,x,x,.,.,x,.,.,.,.,.,x,x,x,x,x,x,.,.,C,.,.,x,.,.,x,.,x,x,x,.,.,.,x,x,.,.,.,x,x,x,.,x,.,.,.,.,.,.,x,x,x,.,x,x,.,x,x,x,.,.,.,.,.,.,x,x,x,.,x,C,.,.,.,.,.,.,.,x,C,x,x,x,.,.,.,.,x,.,x,1,x,x,x,x,x,x,.,x,.,x,.,.,.,x,x,.,x,.,.,.,x,x,x,x,x,x,.,.,.,x,.,x,x,.,.,.,x,x,x,.,x,x,x,x,x,x,.,x,x,x,.,.,.,.,x,.,.,.,.,0,.,.,.,.,.,.,.,.,.,.,.,x,.,.,x,x,x,.,x,x,x,x,x,x,x,x,x,x,.,x,x,x,.,.,x,x,x,.,.,.,.,.,.,.,.,.,.,.,.,x,x,x,.,.,x,x,x,.,x,x,x,.,x,x,.,x,x,x,.,x,x,x,.,.,.,.,.,.,.,.,.,.,x,x,.,.,.,.,.,.,.,.,.");

        Move m  = bot.doMove(s);
        assertNotEquals(Move.UP, m);
    }
}
