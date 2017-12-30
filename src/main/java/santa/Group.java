/**
 * Project: SantaClaus-java
 * Package: santa
 * File: Group.java
 * 
 * @author sidmishraw
 *         Last modified: Dec 29, 2017 2:56:18 PM
 */
package santa;

import lombok.Getter;
import stm.TVar;

/**
 * Group represents the group of helpers. It is created empty with a specified capacity.
 * A helper (Elf of reindeer) may join a group by calling the `JoinGroup` function.
 * The call to `JoinGroup` blocks if the group is full. Santa calls `AwaitGroup` function to
 * wait for the group to be full, when it is full he gets the Group's gates and the group is
 * immediately re-initialized with fresh gates so that another group of eager elves can start
 * assembling.
 * 
 * @author sidmishraw
 *
 *         Qualified Name: santa.Group
 *
 */
public class Group {
    
    /**
     * The maximum capacity of the group.
     */
    private @Getter int        capacity;
    
    /**
     * The number of spots left in the group.
     */
    private @Getter int        spacesLeft;
    
    /**
     * The entry gate of the group. The helpers need to pass through this gate to meet with Santa.
     */
    private @Getter TVar<Gate> inGate;
    
    /**
     * The exit gate of the group. The helpers need to pass through this gate when the helpers are done meeting with
     * Santa.
     */
    private @Getter TVar<Gate> outGate;
    
    /**
     * A new group.
     * 
     * @param capacity
     *            The capacity of the group.
     * @param spacesLeft
     *            The number of spots left in the group.
     * @param inGate
     *            The entry gate.
     * @param outGate
     *            The exit gate.
     */
    public Group(int capacity, int spacesLeft, TVar<Gate> inGate, TVar<Gate> outGate) {
        this.capacity = capacity;
        this.spacesLeft = spacesLeft;
        this.inGate = inGate;
        this.outGate = outGate;
    }
    
    /**
     * Adds a helper to the group.
     */
    public void addHelper() {
        this.spacesLeft = this.spacesLeft - 1;
    }
    
    /**
     * Resets the spaces left in the group to max capacity.
     */
    public void resetSpaces() {
        this.spacesLeft = this.capacity;
    }
}
