/**
 *  SusiThoughts
 *  Copyright 30.06.2016 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package org.loklak.susi;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * An Argument is a series of thoughts, also known as a 'proof' in automated reasoning.
 * Within the Susi AI infrastructure this may be considered as the representation of
 * the short-time memory of thinking inside Susi.
 */
public class SusiArgument {

    private final ArrayList<SusiThought> recall;
    private final List<SusiAction> actions;
    
    /**
     * Create an empty argument
     */
    public SusiArgument() {
        this.recall = new ArrayList<>();
        this.actions = new ArrayList<>();
    }
    
    public SusiArgument clone() {
        SusiArgument c = new SusiArgument();
        this.recall.forEach(thought -> c.recall.add(thought));
        this.actions.forEach(action -> c.actions.add(action));
        return c;
    }
    
    /**
     * Get an impression of time which elapsed since the start of reasoning in this argument
     * @return the number of thoughts
     */
    public int times() {
        return this.recall.size();
    }
    
    /**
     * The mindstate is the current state of an argument
     * @return the latest thought in a series of proof steps
     */
    public SusiThought mindstate() {
        return remember(0);
    }
    
    /**
     * Remembering the thoughts is essential to recall which thoughts leads to the current mindstate
     * @param timesBack the number of thoughts backwards from the current mindstate
     * @return the thought in the past according to the elapsed time of the thoughts
     */
    public SusiThought remember(int timesBack) {
        int state = this.recall.size() - timesBack - 1;
        if (state < 0) return new SusiThought(); // empty mind!
        return this.recall.get(state);
    }
 
    /**
     * Thinking is a series of thoughts, every new thought appends another thought to the argument.
     * A special situation may (or may not) occur if one thinking step does not produce a result.
     * Depending on the inference rule set that may mean that the consideration of the rule containing
     * the inferences was wrong and should be abandoned. This happens if mindstate().equals(thought).
     * @param thought the next thought
     * @return self, the current argument
     */
    public SusiArgument think(SusiThought thought) {
        this.recall.add(thought);
        return this;
    }
    
    /**
     * to remember larger sets of thoughts, we can also think arguments
     * @param argument
     * @return self, the current argument
     */
    public SusiArgument think(SusiArgument argument) {
        argument.recall.forEach(thought -> think(thought));
        return this;
    }
    
    private static final Pattern variable_pattern = Pattern.compile("\\$.*?\\$");
    
    /**
     * Unification applies a piece of memory within the current argument to a statement
     * which creates an instantiated statement
     * TODO: this should support backtracking, thus producing optional several unifications and turning this into a choice point
     * @param statement
     * @return the instantiated statement with elements of the argument applied
     */
    public String unify(String statement) {
        remember: for (int mindstate_count = this.recall.size() - 1; mindstate_count >= 0; mindstate_count--) {
            JSONArray table = this.recall.get(mindstate_count).getData();
            if (table != null && table.length() > 0) {
                JSONObject row = table.getJSONObject(0);
                for (String key: row.keySet()) {
                    int i = statement.indexOf("$" + key + "$");
                    if (i >= 0) {
                        statement = statement.substring(0, i) + row.get(key).toString() + statement.substring(i + key.length() + 2);
                    }
                }
                if (!variable_pattern.matcher(statement).find()) break remember; //termination if no more patterns are included in statement
            }
        }
        return statement;
    }
    
    /**
     * Every argument may have a set of (re-)actions assigned.
     * Those (re-)actions are methods to do something with the argument.
     * @param action one (re-)action on this argument
     * @return the argument
     */
    public SusiArgument addAction(final SusiAction action) {
        this.actions.add(action);
        return this;
    }
    
    /**
     * To be able to apply (re-)actions to this thought, the actions on the information can be retrieved.
     * @return the (re-)actions which are applicable to this thought.
     */
    public List<SusiAction> getActions() {
        return this.actions;
    }
    
}
