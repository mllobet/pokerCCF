// This file is part of the 'texasholdem' project, an open source
// Texas Hold'em poker application written in Java.
//
// Copyright 2009 Oscar Stigter
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// This file is part of the 'texasholdem' project, an open source
// Texas Hold'em poker application written in Java.
//
// Copyright 2009 Oscar Stigter
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package lo.wolo.pokerengine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import android.util.Log;
import lo.wolo.pokerccf.CCFManager;
import lo.wolo.pokerccf.RemoteUser;
import lo.wolo.pokerccf.ServerController;
import lo.wolo.pokerengine.actions.Action;
import lo.wolo.pokerengine.actions.BetAction;
import lo.wolo.pokerengine.actions.CallAction;
import lo.wolo.pokerengine.actions.CheckAction;
import lo.wolo.pokerengine.actions.FoldAction;
import lo.wolo.pokerengine.actions.RaiseAction;

/**
 * Limit Texas Hold'em poker table. <br />
 * <br />
 * 
 * This class forms the heart of the poker engine. It controls the game flow for a single poker table.
 * 
 * @author Oscar Stigter
 */
public class Table {
    
	private static final String TAG = "Table"; 
	
    /** In fixed-limit games, the maximum number of raises per betting round. */
    private static final int MAX_RAISES = 3;
    
    /** Whether players will always call the showdown, or fold when no chance. */
    private static final boolean ALWAYS_CALL_SHOWDOWN = false;
    
    /** Table type (poker variant). */
    private final TableType tableType;
    
    /** The size of the big blind. */
    private final int bigBlind;
    
    /** The players at the table. */
    private final List<Player> players;
    
    /** The active players in the current hand. */
    private final List<Player> activePlayers;
    
    /** The deck of cards. */
    private final Deck deck;
    
    /** The community cards on the board. */
    private final List<Card> board;
    
    /** The current dealer position. */
    private int dealerPosition;

    /** The current dealer. */
    private Player dealer;

    /** The position of the acting player. */
    private int actorPosition;
    
    /** The acting player. */
    private Player actor;

    /** The minimum bet in the current hand. */
    private int minBet;
    
    /** The current bet in the current hand. */
    private int bet;
    
    /** All pots in the current hand (main pot and any side pots). */
    private final List<Pot> pots;
    
    /** The player who bet or raised last (aggressor). */
    private Player lastBettor;
    
    /** Number of raises in the current betting round. */
    private int raises;
    
    private CCFManager serviceManager;
    private ArrayList<RemoteUser> remoteUsers;
    private ServerController serverController;
    
    /**
     * Constructor.
     * 
     * @param bigBlind
     *            The size of the big blind.
     */
    public Table(TableType type, int bigBlind, CCFManager serviceManager, ServerController serverController) {
        this.tableType = type;
        this.bigBlind = bigBlind;
        players = new ArrayList<Player>();
        activePlayers = new ArrayList<Player>();
        deck = new Deck();
        board = new ArrayList<Card>();
        pots = new ArrayList<Pot>();
        
        //initialize remote users
        this.serviceManager = serviceManager;
        this.remoteUsers = serviceManager.getRemoteUsers();
        this.serverController = serverController;
        
    }
    
    /**
     * Adds a player.
     * 
     * @param player
     *            The player.
     */
    public void addPlayer(Player player) {
        players.add(player);
    }
    
    /**
     * Main game loop.
     */
    public void run() {
        Log.i("lolbug", "Displaying all the players...");
        for (Player p : players) {
        	int id = Integer.parseInt(p.getName());
        	ArrayList<RemoteUser> remoteUserList = serviceManager.getRemoteUsers();
        	RemoteUser u = remoteUserList.get(id);
        	String s = u.getSession().getUserName();
        	serverController.setPlayer(s, id+1);
        	Log.i("lolbug", "Username " + id + " = " + s);
        }
        Log.i("lolbug", "Finished displaying players");
        
    	
        for (Player player : players) {
            player.getClient().joinedTable(tableType, bigBlind, players);
        }
        Log.d(TAG,"joined tables");
        dealerPosition = -1;
        actorPosition = -1;
        while (true) {
            int noOfActivePlayers = 0;
            for (Player player : players) {
                if (player.getCash() >= bigBlind) {
                    noOfActivePlayers++;
                }
            }
            Log.d(TAG,"Number of Active players: " + Integer.toString(noOfActivePlayers));
            if (noOfActivePlayers > 1) {
            	Log.d(TAG,"Start Play hand");
                playHand();
            } else {
                break;
            }
        }
        Log.d(TAG,"Game Over");
        // Game over.

        //REMOVED SET CARDS TO -1 HERE
                
        board.clear();
        pots.clear();
        bet = 0;
        notifyBoardUpdated();
        for (Player player : players) {
            player.resetHand();
        }
        notifyPlayersUpdated(false);
        notifyMessage("Game over.");
    }
    
    /**
     * Plays a single hand.
     */
    private void playHand() {
        for (int i = 1; i <= 5; i++) serverController.setCard(-1, i);
        resetHand();
        
        int id = Integer.parseInt(actor.getName());
        serverController.sendMessage(id, "role dealer");
        // Small blind.
        if (activePlayers.size() > 2) {
            rotateActor();
        }
        id = Integer.parseInt(actor.getName());
        serverController.sendMessage(id, "role smallBlind");
        postSmallBlind();
        
        // Big blind.
        
        rotateActor();
        id = Integer.parseInt(actor.getName());
        serverController.sendMessage(id, "role bigBlind");
        postBigBlind();
        
        // Pre-Flop.
        dealHoleCards();
        doBettingRound();
        
        // Flop.
        if (activePlayers.size() > 1) {
        	Log.d(TAG,"ActivePlayers: " + Integer.toString(activePlayers.size()));
            bet = 0;
            dealCommunityCards("Flop", 3);
            
            for (Card cd :board) Log.i("lolbug", Integer.toString(cd.hashCode()));
            
            serverController.setCard(board.get(0).hashCode(), 1);
            serverController.setCard(board.get(1).hashCode(), 2);
            serverController.setCard(board.get(2).hashCode(), 3);
            
            minBet = bigBlind;
            doBettingRound();

            // Turn.
            if (activePlayers.size() > 1) {
                bet = 0;
                dealCommunityCards("Turn", 1);
                
                Log.i("lolbug", Integer.toString(board.get(3).hashCode()));
                serverController.setCard(board.get(3).hashCode(), 4);
                
                if (tableType == TableType.FIXED_LIMIT) {
                    minBet = 2 * bigBlind;
                } else {
                    minBet = bigBlind;
                }
                doBettingRound();

                // River.
                if (activePlayers.size() > 1) {
                    bet = 0;
                    dealCommunityCards("River", 1);
                    
                    Log.i("lolbug", Integer.toString(board.get(4).hashCode()));
                    serverController.setCard(board.get(4).hashCode(), 5);
                    
                    if (tableType == TableType.FIXED_LIMIT) {
                        minBet = 2 * bigBlind;
                    } else {
                        minBet = bigBlind;
                    }
                    doBettingRound();

                    // Showdown.
                    if (activePlayers.size() > 1) {
                        bet = 0;
                        minBet = bigBlind;
                        doShowdown();
                    }
                }
            }
        }
    }
    
    /**
     * Resets the game for a new hand.
     */
    private void resetHand() {
        // Clear the board.
        board.clear();
        pots.clear();
        notifyBoardUpdated();
        
        // Determine the active players.
        activePlayers.clear();
        for (Player player : players) {
            player.resetHand();
            // Player must be able to afford at least the big blind.
            if (player.getCash() >= bigBlind) {
                activePlayers.add(player);
            }
        }
        
        // Rotate the dealer button.
        dealerPosition = (dealerPosition + 1) % activePlayers.size();
        dealer = activePlayers.get(dealerPosition);
                
        // Shuffle the deck.
        deck.shuffle();

        // Determine the first player to act.
        actorPosition = dealerPosition;
        actor = activePlayers.get(actorPosition);
        
        // Set the initial bet to the big blind.
        minBet = bigBlind;
        bet = minBet;
        
        // Notify all clients a new hand has started.
        for (Player player : players) {
            player.getClient().handStarted(dealer);
        }
        notifyPlayersUpdated(false);
        notifyMessage("New hand, %s is the dealer.", dealer);
    }

    /**
     * Rotates the position of the player in turn (the actor).
     */
    private void rotateActor() {
        actorPosition = (actorPosition + 1) % activePlayers.size();
        actor = activePlayers.get(actorPosition);
        for (Player player : players) {
            player.getClient().actorRotated(actor);
        }
    }
    
    /**
     * Posts the small blind.
     */
    private void postSmallBlind() {
        final int smallBlind = bigBlind / 2;
        actor.postSmallBlind(smallBlind);
        contributePot(smallBlind);
        notifyBoardUpdated();
        notifyPlayerActed();
    }
    
    /**
     * Posts the big blind.
     */
    private void postBigBlind() {
        actor.postBigBlind(bigBlind);
        contributePot(bigBlind);
        notifyBoardUpdated();
        notifyPlayerActed();
    }
    
    /**
     * Deals the Hole Cards.
     */
    private void dealHoleCards() {
        for (Player player : activePlayers) {
            player.setCards(deck.deal(2));
            int id = Integer.parseInt(player.getName());
            serverController.sendCards(id, player.getCards());
            serverController.sendMessage(id, "money " + player.getCash());
        }
        
        System.out.println();
        notifyPlayersUpdated(false);
        notifyMessage("%s deals the hole cards.", dealer);
    }
    
    /**
     * Deals a number of community cards.
     * 
     * @param phaseName
     *            The name of the phase.
     * @param noOfCards
     *            The number of cards to deal.
     */
    private void dealCommunityCards(String phaseName, int noOfCards) {
        for (int i = 0; i < noOfCards; i++) {
            board.add(deck.deal());
        }
        notifyPlayersUpdated(false);
        notifyMessage("%s deals the %s.", dealer, phaseName);
    }
    
    /**
     * Performs a betting round.
     */
    private void doBettingRound() {
    	Log.d(TAG,"Betting Round started");
        // Determine the number of active players.
        int playersToAct = activePlayers.size();
        // Determine the initial player and bet size.
        if (board.size() == 0) {
            // Pre-Flop; player left of big blind starts, bet is the big blind.
            bet = bigBlind;
        } else {
            // Otherwise, player left of dealer starts, no initial bet.
            actorPosition = dealerPosition;
            bet = 0;
        }
        
        /*if (playersToAct == 2) {  //removed, fix by IW
            // Heads Up mode; player who is not the dealer starts.
            actorPosition = dealerPosition;
        }*/
        
        lastBettor = null;
        raises = 0;
        notifyBoardUpdated();
        
        while (playersToAct > 0) {
        	
            rotateActor();
            Action action = null;
            if (actor.isAllIn()) {
                // Player is all-in, so must check.
                action = Action.CHECK;
                playersToAct--;
            } else {
                // Otherwise allow client to act.
                Set<Action> allowedActions = getAllowedActions(actor);
                //Send Allowed Actions to remoteUsers
                int actorID = Integer.parseInt(actor.getName());
                Log.d(TAG,"actorID " + actorID);
                RemoteUser actorRemoteUser = remoteUsers.get(actorID);
                
                if (actorRemoteUser.getsessionState() == CCFManager.SessionState.CONNECTED) {
                	Log.d(TAG,actorRemoteUser.toString() + " isConected");
                	serverController.sendActionsAllowed(actorID,allowedActions);
                	// Highlight the user name
                	serverController.highlightPlayer(actorID);
                } else {
                	Log.d("Table","Actor: " + actor.getName() + " was NOTCONECTED");
                }
                
                //Sends money message
                serverController.sendMessage(actorID, "money " + actor.getCash());
                
                //Pool player's next action
                ClientCCF actorClient = ((ClientCCF)actor.getClient());
                while (actorClient.getNextAction() == null) {
                	try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                	actorClient.setNextAction(serviceManager.actionList.get(actorID));
                	serviceManager.actionList.set(actorID,null);
                }
                //Action has been set
                
                action = actor.getClient().act(minBet, bet, allowedActions);
                // Verify chosen action to guard against broken clients (accidental or on purpose).
                Log.d(TAG,"Action is: " + action.getName());
                Log.d(TAG,"allowedActions: ");
                for (Action a : allowedActions) {
                	Log.d(TAG,a.getName());
                }
                
                Iterator<Action> it = allowedActions.iterator();
                boolean found = false;
                while (it.hasNext() && !found)
                	found = it.next().equals(action);
                if (!found)
                	throw new IllegalStateException(String.format("Player '%s' acted with illegal %s action", actor, action));

                // TODO check Illegal actions
//                if (!allowedActions.contains(action)) {
//                    if (!(action instanceof BetAction && allowedActions.contains(Action.BET)) && !(action instanceof RaiseAction && allowedActions.contains(Action.RAISE))) {
//                        throw new IllegalStateException(String.format("Player '%s' acted with illegal %s action", actor, action));
//                    }
//                }
                playersToAct--;
                if (action instanceof CheckAction) {
                    // Do nothing.
                } else if (action instanceof CallAction) {
                    int betIncrement = bet - actor.getBet();
                    if (betIncrement > actor.getCash()) {
                        betIncrement = actor.getCash();
                    }
                    actor.payCash(betIncrement);
                    actor.setBet(actor.getBet() + betIncrement);
                    contributePot(betIncrement);
                } else if (action instanceof BetAction) {
                    int amount = (tableType == TableType.FIXED_LIMIT) ? minBet : action.getAmount();
                    if (amount < minBet && amount < actor.getCash()) {
                        throw new IllegalStateException("Illegal client action: bet less than minimum bet!");
                    }
                    if (amount > actor.getCash() && actor.getCash() >= minBet) {
                        throw new IllegalStateException("Illegal client action: bet more cash than you own!");
                    }
                    bet = amount;
                    serverController.setBetText(bet);
                    minBet = amount;
                    int betIncrement = bet - actor.getBet();
                    if (betIncrement > actor.getCash()) {
                        betIncrement = actor.getCash();
                    }
                    actor.setBet(bet);
                    actor.payCash(betIncrement);
                    contributePot(betIncrement);
                    lastBettor = actor;
                    playersToAct = (tableType == TableType.FIXED_LIMIT) ? activePlayers.size() : (activePlayers.size() - 1);
                } else if (action instanceof RaiseAction) {
                    int amount = (tableType == TableType.FIXED_LIMIT) ? minBet : action.getAmount();
                    if (amount < minBet && amount < actor.getCash()) {
                        throw new IllegalStateException("Illegal client action: raise less than minimum bet!");
                    }
                    if (amount > actor.getCash() && actor.getCash() >= minBet) {
                        throw new IllegalStateException("Illegal client action: raise more cash than you own!");
                    }
                    bet += amount;
                    serverController.setBetText(bet);
                    minBet = amount;
                    int betIncrement = bet - actor.getBet();
                    if (betIncrement > actor.getCash()) {
                        betIncrement = actor.getCash();
                    }
                    actor.setBet(bet);
                    actor.payCash(betIncrement);
                    contributePot(betIncrement);
                    lastBettor = actor;
                    raises++;
                    if (tableType == TableType.FIXED_LIMIT && (raises < MAX_RAISES || activePlayers.size() == 2)) {
                        // All players get another turn.
                        playersToAct = activePlayers.size();
                    } else {
                        // Max. number of raises reached; other players get one more turn.
                        playersToAct = activePlayers.size() - 1;
                    }
                } else if (action instanceof FoldAction) {
                    actor.setCards(null);
                    activePlayers.remove(actor);
                    actorPosition--;
                    if (activePlayers.size() == 1) {
                        // Only one player left, so he wins the entire pot.
                        notifyBoardUpdated();
                        notifyPlayerActed();
                        Player winner = activePlayers.get(0);
                        int amount = getTotalPot();
                        winner.win(amount);
                        notifyBoardUpdated();
                        notifyMessage("%s wins $ %d.", winner, amount);
                        playersToAct = 0;
                    }
                } else {
                    // Programming error, should never happen.
                    throw new IllegalStateException("Invalid action: " + action);
                }
            }
            actor.setAction(action);
            if (activePlayers.size() > 1) {
                notifyBoardUpdated();
                notifyPlayerActed();
            }
        }
        
        // Reset player's bets.
        for (Player player : activePlayers) {
            player.resetBet();
        }
        notifyBoardUpdated();
        notifyPlayersUpdated(false);
    }
    
    /**
     * Returns the allowed actions of a specific player.
     * 
     * @param player
     *            The player.
     * 
     * @return The allowed actions.
     */
    private Set<Action> getAllowedActions(Player player) {
        Set<Action> actions = new HashSet<Action>();
        if (player.isAllIn()) {
            actions.add(Action.CHECK);
        } else {
            int actorBet = actor.getBet();
            if (bet == 0) {
                actions.add(Action.CHECK);
                if (tableType == TableType.NO_LIMIT || raises < MAX_RAISES || activePlayers.size() == 2) {
                    actions.add(Action.BET);
                }
            } else {
                if (actorBet < bet) {
                    actions.add(Action.CALL);
                    if (tableType == TableType.NO_LIMIT || raises < MAX_RAISES || activePlayers.size() == 2) {
                        actions.add(Action.RAISE);
                    }
                } else {
                    actions.add(Action.CHECK);
                    if (tableType == TableType.NO_LIMIT || raises < MAX_RAISES || activePlayers.size() == 2) {
                        actions.add(Action.RAISE);
                    }
                }
            }
            actions.add(Action.FOLD);
        }
        return actions;
    }
    
    /**
     * Contributes to the pot.
     * 
     * @param amount
     *            The amount to contribute.
     */
    private void contributePot(int amount) {
        for (Pot pot : pots) {
            if (!pot.hasContributer(actor)) {
                int potBet = pot.getBet();
                if (amount >= potBet) {
                    // Regular call, bet or raise.
                    pot.addContributer(actor);
                    amount -= pot.getBet();
                } else {
                    // Partial call (all-in); redistribute pots.
                    pots.add(pot.split(actor, amount));
                    amount = 0;
                }
            }
            if (amount <= 0) {
                break;
            }
        }
        if (amount > 0) {
            Pot pot = new Pot(amount);
            pot.addContributer(actor);
            pots.add(pot);
        }
        int tamount = 0;
        for (int i = 0; i < pots.size(); i++) {
        	tamount += pots.get(i).getValue();
        }
        
        if (pots.size() >= 1) 
        	serverController.setPotText(tamount);
    }
    
    /**
     * Performs the showdown.
     */
    private void doShowdown() {
//        System.out.println("\n[DEBUG] Pots:");
//        for (Pot pot : pots) {
//            System.out.format("  %s\n", pot);
//        }
//        System.out.format("[DEBUG]  Total: %d\n", getTotalPot());
        
        // Determine show order; start with all-in players...
        List<Player> showingPlayers = new ArrayList<Player>();
        for (Pot pot : pots) {
            for (Player contributor : pot.getContributors()) {
                if (!showingPlayers.contains(contributor) && contributor.isAllIn()) {
                    showingPlayers.add(contributor);
                }
            }
        }
        // ...then last player to bet or raise (aggressor)...
        if (lastBettor != null) {
            if (!showingPlayers.contains(lastBettor)) {
                showingPlayers.add(lastBettor);
            }
        }
        //...and finally the remaining players, starting left of the button.
        int pos = (dealerPosition + 1) % activePlayers.size();
        while (showingPlayers.size() < activePlayers.size()) {
            Player player = activePlayers.get(pos);
            if (!showingPlayers.contains(player)) {
                showingPlayers.add(player);
            }
            pos = (pos + 1) % activePlayers.size();
        }
        
        // Players automatically show or fold in order.
        boolean firstToShow = true;
        int bestHandValue = -1;
        for (Player playerToShow : showingPlayers) {
            Hand hand = new Hand(board);
            hand.addCards(playerToShow.getCards());
            HandValue handValue = new HandValue(hand);
            boolean doShow = ALWAYS_CALL_SHOWDOWN;
            if (!doShow) {
                if (playerToShow.isAllIn()) {
                    // All-in players must always show.
                    doShow = true;
                    firstToShow = false;
                } else if (firstToShow) {
                    // First player must always show.
                    doShow = true;
                    bestHandValue = handValue.getValue();
                    firstToShow = false;
                } else {
                    // Remaining players only show when having a chance to win.
                    if (handValue.getValue() >= bestHandValue) {
                        doShow = true;
                        bestHandValue = handValue.getValue();
                    }
                }
            }
            if (doShow) {
                // Show hand.
                for (Player player : players) {
                    player.getClient().playerUpdated(playerToShow);
                }
                notifyMessage("%s has %s.", playerToShow, handValue.getDescription());
            } else {
                // Fold.
                playerToShow.setCards(null);
                activePlayers.remove(playerToShow);
                for (Player player : players) {
                    if (player.equals(playerToShow)) {
                        player.getClient().playerUpdated(playerToShow);
                    } else {
                        // Hide secret information to other players.
                        player.getClient().playerUpdated(playerToShow.publicClone());
                    }
                }
                notifyMessage("%s folds.", playerToShow);
            }
        }
        
        // Sort players by hand value (highest to lowest).
        Map<HandValue, List<Player>> rankedPlayers = new TreeMap<HandValue, List<Player>>();
        for (Player player : activePlayers) {
            // Create a hand with the community cards and the player's hole cards.
            Hand hand = new Hand(board);
            hand.addCards(player.getCards());
            // Store the player together with other players with the same hand value.
            HandValue handValue = new HandValue(hand);
//            System.out.format("[DEBUG] %s: %s\n", player, handValue);
            List<Player> playerList = rankedPlayers.get(handValue);
            if (playerList == null) {
                playerList = new ArrayList<Player>();
            }
            playerList.add(player);
            rankedPlayers.put(handValue, playerList);
        }

        // Per rank (single or multiple winners), calculate pot distribution.
        int totalPot = getTotalPot();
        Map<Player, Integer> potDivision = new HashMap<Player, Integer>();
        for (HandValue handValue : rankedPlayers.keySet()) {
            List<Player> winners = rankedPlayers.get(handValue);
            for (Pot pot : pots) {
                // Determine how many winners share this pot.
                int noOfWinnersInPot = 0;
                for (Player winner : winners) {
                    if (pot.hasContributer(winner)) {
                        noOfWinnersInPot++;
                    }
                }
                if (noOfWinnersInPot > 0) {
                    // Divide pot over winners.
                    int potShare = pot.getValue() / noOfWinnersInPot;
                    for (Player winner : winners) {
                        if (pot.hasContributer(winner)) {
                            Integer oldShare = potDivision.get(winner);
                            if (oldShare != null) {
                                potDivision.put(winner, oldShare + potShare);
                            } else {
                                potDivision.put(winner, potShare);
                            }
                            
                        }
                    }
                    // Determine if we have any odd chips left in the pot.
                    int oddChips = pot.getValue() % noOfWinnersInPot;
                    if (oddChips > 0) {
                        // Divide odd chips over winners, starting left of the dealer.
                        pos = dealerPosition;
                        while (oddChips > 0) {
                            pos = (pos + 1) % activePlayers.size();
                            Player winner = activePlayers.get(pos);
                            Integer oldShare = potDivision.get(winner);
                            if (oldShare != null) {
                                potDivision.put(winner, oldShare + 1);
//                                System.out.format("[DEBUG] %s receives an odd chip from the pot.\n", winner);
                                oddChips--;
                            }
                        }
                        
                    }
                    pot.clear();
                }
            }
        }
        
        // Divide winnings.
        StringBuilder winnerText = new StringBuilder();
        int totalWon = 0;
        for (Player winner : potDivision.keySet()) {
            int potShare = potDivision.get(winner);
            winner.win(potShare);
            totalWon += potShare;
            if (winnerText.length() > 0) {
                winnerText.append(", ");
            }
            winnerText.append(String.format("%s wins $ %d", winner, potShare));
            notifyPlayersUpdated(true);
        }
        winnerText.append('.');
        notifyMessage(winnerText.toString());
        
        // Sanity check.
        if (totalWon != totalPot) {
            throw new IllegalStateException("Incorrect pot division!");
        }
    }
    
    /**
     * Notifies listeners with a custom game message.
     * 
     * @param message
     *            The formatted message.
     * @param args
     *            Any arguments.
     */
    private void notifyMessage(String message, Object... args) {
        message = String.format(message, args);
        for (Player player : players) {
            player.getClient().messageReceived(message);
        }
    }
    
    /**
     * Notifies clients that the board has been updated.
     */
    private void notifyBoardUpdated() {
        int pot = getTotalPot();
        for (Player player : players) {
            player.getClient().boardUpdated(board, bet, pot);
        }
    }
    
    /**
     * Returns the total pot size.
     * 
     * @return The total pot size.
     */
    private int getTotalPot() {
        int totalPot = 0;
        for (Pot pot : pots) {
            totalPot += pot.getValue();
        }
        return totalPot;
    }

    /**
     * Notifies clients that one or more players have been updated. <br />
     * <br />
     * 
     * A player's secret information is only sent its own client; other clients
     * see only a player's public information.
     * 
     * @param showdown
     *            Whether we are at the showdown phase.
     */
    private void notifyPlayersUpdated(boolean showdown) {
        for (Player playerToNotify : players) {
            for (Player player : players) {
                if (!showdown && !player.equals(playerToNotify)) {
                    // Hide secret information to other players.
                    player = player.publicClone();
                }
                playerToNotify.getClient().playerUpdated(player);
            }
        }
    }
    
    /**
     * Notifies clients that a player has acted.
     */
    private void notifyPlayerActed() {
        for (Player p : players) {
            Player playerInfo = p.equals(actor) ? actor : actor.publicClone();
            p.getClient().playerActed(playerInfo);
        }
    }
    
}
