/** Standard or consultation slot: price equals the base price. */
public class StandardSlot extends Slot {
    private final String serviceCategory;

    public StandardSlot(int slotId, String date, String time, double basePrice, String serviceCategory) {
        super(slotId, date, time, basePrice);
        this.serviceCategory = serviceCategory;
    }

    @Override public double calculatePrice() { return getBasePrice(); }
    @Override public String getType() { return serviceCategory; }
}
