/** Abstraction: common base for slots and appointments. */
public abstract class BookingEntity {
    protected final String id;

    protected BookingEntity(String id) { this.id = id; }

    public String getId() { return id; }

    public abstract String displayDetails();
}
