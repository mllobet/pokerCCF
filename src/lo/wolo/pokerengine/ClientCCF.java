package lo.wolo.pokerengine;

import java.util.List;
import java.util.Set;

import lo.wolo.pokerengine.actions.Action;

public class ClientCCF implements Client {

	static final int NO_OF_HOLE_CARDS = 2;
	
	/** Table type. */
    private TableType tableType;
    
    /** The hole cards. */
    private Card[] cards;
    
    private Action nextAction;
    
	@Override
	public void messageReceived(String message) {
		// TODO Auto-generated method stub

	}

	@Override
	public void joinedTable(TableType type, int bigBlind, List<Player> players) {
		this.tableType = type;
	}

	@Override
	public void handStarted(Player dealer) {
		cards = null;

	}

	@Override
	public void actorRotated(Player actor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void playerUpdated(Player player) {
		if (player.getCards().length == NO_OF_HOLE_CARDS) {
            this.cards = player.getCards();
        }
	}

	@Override
	public void boardUpdated(List<Card> cards, int bet, int pot) {
		// TODO Auto-generated method stub

	}

	@Override
	public void playerActed(Player player) {
		// TODO Auto-generated method stub

	}
	
	public void setNextAction(Action a) {
		this.nextAction = a;
	}

	@Override
	public Action act(int minBet, int currentBet, Set<Action> allowedActions) {
		if (nextAction != null) {
			Action tmp = nextAction;
			nextAction = null;
			return tmp;
		}
		return null;
	}

}
