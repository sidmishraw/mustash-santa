/**
 * Project: SantaClaus-java
 * Package: santa
 * File: Simulation.java
 * 
 * @author sidmishraw
 *         Last modified: Dec 29, 2017 2:24:42 PM
 */
package santa;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import stm.STM;
import stm.TVar;
import stm.Transaction;
import stm.utils.Transactions;

/**
 * <p>
 * 
 * <h3>Problem Statement</h3>
 * 
 * Santa repeatedly sleeps until awakened by either all of his nine reindeer, back from their holidays, or by a group of
 * three of his ten elves. If awakened by the reindeer, he harnesses each of them to his sleigh, delivers toys with them
 * and finally unharnesses them (allowing them to go off on holiday). If awakened by a group of elves, he shows each of
 * the group into his study, consults with them on toy R&D and finally shows them each out (allowing them to go back to
 * work). Santa should give priority to the reindeer in the case that there is both a group of elves and a group of
 * reindeer waiting.
 * 
 * <p>
 * {@link [1] S. P. Jones, "Beautiful concurrency", ch. 4, [Online] Available.
 * https://www.schoolofhaskell.com/school/advanced-haskell/beautiful-concurrency/4-the-santa-claus-problem}
 * </p>
 * </p>
 * 
 * @author sidmishraw
 *
 *         Qualified Name: santa.Simulation
 *
 */
public class Simulation {
    
    private static final Logger logger = LoggerFactory.getLogger(Simulation.class);
    
    /**
     * <p>
     * According to S.P. Jones, Santa makes 2 `Group`s: Elves and Reindeers. Each Elf and Reindeer tries to
     * join their respective group. Upon successful joining, two `Gates` are returned. `EntryGate` and `ExitGate`.
     * The `EntryGate` allows Santa to control when the elf can enter the study, and also lets Santa know when
     * they are all inside. The `ExitGate` controls the elves leaving the study.
     * </p>
     * 
     * <p>
     * Santa waits for either of his two groups to be ready, and then uses that Group's Gate's to marshal
     * his helpers(elves or reindeers) through their task.
     * </p>
     * 
     * <p>
     * The helpers spend their time in an infinite loop: try to join a group, move through the gates under Santa's
     * control, and then delay for a random interval before trying to join a group again.
     * </p>
     * 
     * <h4>My interpretation</h4>
     * 
     * <p>
     * From the last description, it can be inferred that the helpers are threads that do `join a group`,
     * `move through gates`, and `delay for some time` actions in sequence infinitely.
     * From my STM model, I can represent each of these sequences as a transaction?
     * </p>
     * 
     * <p>
     * Elves `MeetInStudy` and Reindeers `DeliverToys`
     * </p>
     * 
     * <p>
     * loggerMutex will used for taking lock on the logger
     * </p>
     */
    
    /**
     * The single state store
     */
    private static final STM    stm    = new STM();
    
    /**
     * @param args
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        TVar<Group> elfGroup = newGroup(3);
        TVar<Group> reindeerGroup = newGroup(9);
        logger.info("Groups ready!");
        IntStream.range(1, 11).sequential().forEach(i -> {
            new Thread(() -> {
                while (true) {
                    stm.exec(newHelper(i, elfGroup, Simulation::meetInStudy));
                }
            }).start();
        });
        IntStream.range(1, 10).sequential().forEach(i -> {
            new Thread(() -> {
                while (true) {
                    stm.exec(newHelper(i, reindeerGroup, Simulation::deliverToys));
                }
            }).start();
        });
        Map<String, Object> santaP = santa(elfGroup, reindeerGroup);
        // # Santa simulation
        for (int i = 0; i < 1; i++) {
            stm.forkAndExec((Transaction) santaP.get("REINDEER"), (Transaction) santaP.get("ELF"));
            LinkedBlockingQueue<Pair<TVar<Gate>, TVar<Gate>>> eQ = (LinkedBlockingQueue<Pair<TVar<Gate>, TVar<Gate>>>) santaP
                    .get("ELF_CHANNEL");
            LinkedBlockingQueue<Pair<TVar<Gate>, TVar<Gate>>> rQ = (LinkedBlockingQueue<Pair<TVar<Gate>, TVar<Gate>>>) santaP
                    .get("REINDEER_CHANNEL");
            try {
                Pair<TVar<Gate>, TVar<Gate>> eGates = null;
                Pair<TVar<Gate>, TVar<Gate>> rGates = null;
                if (null != (rGates = rQ.take())) {
                    logger.info("Ho! Ho! Hoo! Let's go deliver some toys! :D");
                    logger.info("-------------------------------------------");
                    operateGate(rGates.getFirst());
                    operateGate(rGates.getSecond());
                    Thread.sleep(5000);
                } else {
                    logger.error("Null gates for reindeers");
                }
                if (null != (eGates = eQ.take())) {
                    logger.info("Ho! Ho! Hoo! Let's hold the meeting in the study! :D");
                    logger.info("----------------------------------------------------");
                    operateGate(eGates.getFirst());
                    operateGate(eGates.getSecond());
                    Thread.sleep(5000);
                } else {
                    logger.error("Null gates for elves");
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        // # Santa simulation
        logger.info("Santa is done for the day. Bye Santa ~~~");
        System.exit(0); // kill all threads and exit
    }
    
    /**
     * Simulates Santa.
     * 
     * @param elfGroup
     *            The group elves must join.
     * @param reindeerGroup
     *            The group reindeers must join.
     * @return The elf and reindeer related props.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> santa(TVar<Group> elfGroup, TVar<Group> reindeerGroup) {
        Map<String, Object> helperStuff = new HashMap<>();
        helperStuff.put("REINDEER_CHANNEL", new LinkedBlockingQueue<Pair<TVar<Gate>, TVar<Gate>>>());
        helperStuff.put("ELF_CHANNEL", new LinkedBlockingQueue<Pair<TVar<Gate>, TVar<Gate>>>());
        helperStuff.put("REINDEER", Transactions.newT(stm).begin(t -> {
            Pair<TVar<Gate>, TVar<Gate>> gates = awaitGroup(reindeerGroup);
            ((LinkedBlockingQueue<Pair<TVar<Gate>, TVar<Gate>>>) helperStuff.get("REINDEER_CHANNEL")).add(gates);
            return true;
        }).end().done());
        helperStuff.put("ELF", Transactions.newT(stm).begin(t -> {
            Pair<TVar<Gate>, TVar<Gate>> gates = awaitGroup(elfGroup);
            ((LinkedBlockingQueue<Pair<TVar<Gate>, TVar<Gate>>>) helperStuff.get("ELF_CHANNEL")).add(gates);
            return true;
        }).end().done());
        return helperStuff;
    }
    
    /**
     * Creates a new transactional variables holding a gate of the given capacity.
     * 
     * @param capacity
     *            The capacity of the gate
     * @return the transactional variable holding the gate
     */
    private static TVar<Gate> newGate(int capacity) {
        return stm.newTVar(new Gate(capacity));
    }
    
    /**
     * Allows the helper to pass through the gate. Internally uses a transaction to modify the state of the STM.
     * 
     * @param gate
     *            The gate the helper will pass through.
     */
    private static void passGate(TVar<Gate> gate) {
        stm.exec(Transactions.newT(stm).begin(t -> {
            Gate g = t.read(gate);
            if (g.getRemaining() <= 0) {
                return false;
            }
            g.passThrough();
            return t.write(gate, g);
        }).end().done());
    }
    
    /**
     * Santa resets the gate to its full capacity allowing his helpers to pass through it.
     * 
     * @param gate
     *            The gate Santa operates on.
     */
    private static void operateGate(TVar<Gate> gate) {
        stm.exec(Transactions.newT(stm).begin(t -> {
            Gate g = t.read(gate);
            g.operate();
            return t.write(gate, g);
        }).end().done());
    }
    
    /**
     * Creates a new group of the given capacity.
     * 
     * @param capacity
     *            The max capacity of the group.
     * @return The transactional variable holding the group.
     */
    private static TVar<Group> newGroup(int capacity) {
        return stm.newTVar(new Group(capacity, capacity, newGate(capacity), newGate(capacity)));
    }
    
    /**
     * JoinGroup lets the helpers join the group. This is a transactional operation. It updates the
     * group in the STM. JoinGroup first checks if the Group is full. If the group is full, the call
     * blocks. Otherwise, it updates its member count and the member is added.
     * 
     * @param group
     *            The group the helper intends to join.
     * @return The gates of the group. [inGate, outGate]
     */
    private static Pair<TVar<Gate>, TVar<Gate>> joinGroup(TVar<Group> group) {
        Pair<TVar<Gate>, TVar<Gate>> gates = new Pair<TVar<Gate>, TVar<Gate>>(null, null);
        stm.exec(Transactions.newT(stm).begin(t -> {
            Group grp = t.read(group);
            if (grp.getSpacesLeft() <= 0) {
                return false;
            }
            grp.addHelper();
            gates.setFirst(grp.getInGate());
            gates.setSecond(grp.getOutGate());
            return t.write(group, grp);
        }).end().done());
        return gates;
    }
    
    /**
     * AwaitGroup makes new Gates when it re-initializes the Group. This ensures that
     * a new group can assemble while the old one is still talking to Santa in the study,
     * with no danger of an elf from the new group overtaking a sleepy elf from the old one.
     * 
     * @param group
     *            The group to await for.
     * @return The gates of the group. [inGate, outGate]
     */
    private static Pair<TVar<Gate>, TVar<Gate>> awaitGroup(TVar<Group> group) {
        Pair<TVar<Gate>, TVar<Gate>> gates = new Pair<TVar<Gate>, TVar<Gate>>(null, null);
        stm.exec(Transactions.newT(stm).begin(t -> {
            Group grp = t.read(group);
            gates.setFirst(grp.getInGate());
            gates.setSecond(grp.getOutGate());
            if (grp.getSpacesLeft() > 0) {
                return false;
            }
            return t.write(group, new Group(grp.getCapacity(), grp.getCapacity(), newGate(grp.getCapacity()),
                    newGate(grp.getCapacity())));
        }).end().done());
        return gates;
    }
    
    /**
     * Creates a new helper with the given ID and the group it must join.
     * 
     * @param id
     *            The ID of the helper.
     * @param group
     *            The group the helper must join.
     * 
     * @param task
     *            The task the helper needs to perform
     * @return The helper -- which is a transaction.
     */
    private static Transaction newHelper(int id, TVar<Group> group, Consumer<Integer> task) {
        return Transactions.newT(stm).begin(t -> {
            Pair<TVar<Gate>, TVar<Gate>> gates = joinGroup(group);
            passGate(gates.getFirst());
            task.accept(id);
            passGate(gates.getSecond());
            return true;
        }).end().done();
    }
    
    /**
     * The elf with the given ID meets Santa in his study.
     * 
     * @param elfID
     *            The elf's ID
     */
    private static void meetInStudy(int elfID) {
        logger.info(String.format("Elf #%d meeting in the study.", elfID));
    }
    
    /**
     * The reindeer with the given ID meets Santa to deliver toys.
     * 
     * @param reinID
     *            The reindeer's ID
     */
    private static void deliverToys(int reinID) {
        logger.info(String.format("Reindeer #%d delivering toys.", reinID));
    }
}
