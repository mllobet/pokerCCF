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

package lo.wolo.pokerengine.actions;

import lo.wolo.pokerccf.Constants;

/**
 * Player action.
 * 
 * @author Oscar Stigter
 */
public abstract class Action {
    
    /** Player went all-in. */
    public static final Action ALL_IN = new AllInAction();

    /** Bet. */
    public static final Action BET = new BetAction(0);
    
    /** Posting the big blind. */
    public static final Action BIG_BLIND = new BigBlindAction();
    
    /** Call. */
    public static final Action CALL = new CallAction();
    
    /** Check. */
    public static final Action CHECK = new CheckAction();
    
    /** Continue. */
    public static final Action CONTINUE = new ContinueAction();
    
    /** Fold. */
    public static final Action FOLD = new FoldAction();
    
    /** Raise. */
    public static final Action RAISE = new RaiseAction(0);
    
    /** Posting the small blind. */
    public static final Action SMALL_BLIND = new SmallBlindAction();
    
    /** The action's name. */
    private final String name;
    
    /** The action's verb. */
    private final String verb;
    
    /** The amount (if appropriate). */
    private final int amount;
    
    /**
     * Constructor.
     * 
     * @param name
     *            The action's name.
     * @param verb
     *            The action's verb.
     */
    public Action(String name, String verb) {
        this(name, verb, 0);
    }
    
    /**
     * Constructor.
     * 
     * @param name
     *            The action's name.
     * @param verb
     *            The action's verb.
     * @param amount
     *            The action's amount.
     */
    public Action(String name, String verb, int amount) {
        this.name = name;
        this.verb = verb;
        this.amount = amount;
    }
    
    /**
     * Returns the action's name.
     * 
     * @return The action's name.
     */
    public final String getName() {
        return name;
    }
    
    /**
     * Returns the action's verb.
     * 
     * @return The action's verb.
     */
    public final String getVerb() {
        return verb;
    }
    
    /**
     * Returns the action's amount.
     * 
     * @return The action's amount.
     */
    public final int getAmount() {
        return amount;
    }
    
    /** returns encoded value of the action */
    public final int getEncode() {
    	if (name.equals("All-in")) {
    		return Constants.ALL_IN;
    	} else if (name.equals("Bet")) {
    		return Constants.BET;
    	} else if (name.equals("Big blind")) {
    		return Constants.BIG_BLIND;
    	} else if (name.equals("Call")) {
    		return Constants.CALL;
    	} else if (name.equals("Check")) {
    		return Constants.CHECK;
    	} else if (name.equals("Continue")) {
    		return Constants.CONTINUE;
    	} else if (name.equals("Fold")) {
    		return Constants.FOLD;
    	} else if (name.equals("Raise")) {
    		return Constants.RAISE;
    	} else {
    		return Constants.SMALL_BLIND;
    	}
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return name;
    }
    
    public boolean equals(Action a) {
    	return this.getName().equals(a.getName());
    }

}
