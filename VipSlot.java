/** Priority slot: base price plus a priority fee. */
public class VipSlot extends Slot {
    private final double priorityFee;

    public VipSlot(int slotId, String date, String time, double basePrice, double priorityFee) {
        super(slotId, date, time, basePrice);
        this.priorityFee = priorityFee;
    }

    @Override public double calculatePrice() { return getBasePrice() + priorityFee; }
    @Override public String getType() { return "Priority"; }
}
