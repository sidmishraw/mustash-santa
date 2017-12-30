/**
 * Project: SantaClaus-java
 * Package: santa
 * File: Pair.java
 * 
 * @author sidmishraw
 *         Last modified: Dec 29, 2017 3:07:30 PM
 */
package santa;

import lombok.Getter;
import lombok.Setter;

/**
 * A pair of 2 things
 * 
 * @author sidmishraw
 *
 *         Qualified Name: santa.Pair
 *
 */
public class Pair<X, Y> {
    
    private @Getter @Setter X first;
    private @Getter @Setter Y second;
    
    /**
     * Creates a new pair of 2 things.
     * 
     * @param first
     *            The first item of the pair.
     * @param second
     *            The second item of the pair.
     */
    public Pair(X first, Y second) {
        this.first = first;
        this.second = second;
    }
}
