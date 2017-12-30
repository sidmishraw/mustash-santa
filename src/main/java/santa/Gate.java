/**
 * Project: SantaClaus-java
 * Package: santa
 * File: Gate.java
 * 
 * @author sidmishraw
 *         Last modified: Dec 29, 2017 2:31:41 PM
 */
package santa;

import lombok.Getter;

/**
 * Gate represents the gate held by Santa. A Gate has a fixed `capacity` which we need to specify
 * while making a new Gate, and a mutable `remaining` capacity. The `remaining` capacity is
 * decremented whenever a helper calls `passGate` to go through the gate.
 * If the capacity is 0, passGate blocks.
 * A Gate is created with zero remaining capacity, so that no helpers can pass through it.
 * Santa opens the gate with `operateGate`, which sets its remaining capacity back to n.
 * 
 * @author sidmishraw
 *
 *         Qualified Name: santa.Gate
 *
 */
public class Gate {
    
    /**
     * The maximum capacity of the Gate
     */
    private @Getter int capacity;
    
    /**
     * The number of helpers allowed to pass through the gate. The gate will block if remaining is 0.
     */
    private @Getter int remaining;
    
    /**
     * 
     */
    public Gate(int capacity) {
        this.capacity = capacity;
        this.remaining = 0;
    }
    
    /**
     * A helper passed through the gate.
     */
    public void passThrough() {
        this.remaining = this.remaining - 1;
    }
    
    /**
     * Resets the remaining count to max capacity.
     */
    public void operate() {
        this.remaining = this.capacity;
    }
}
