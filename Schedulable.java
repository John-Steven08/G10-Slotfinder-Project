/** Abstraction: the contract for anything that can be reserved. */
public interface Schedulable {
    boolean isAvailable();
    void reserve() throws SlotUnavailableException;
    void release();
}
