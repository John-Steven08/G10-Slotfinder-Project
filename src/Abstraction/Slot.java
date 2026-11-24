/** Base class for time slots. Fields are private (Encapsulation). */
public abstract class Slot extends BookingEntity implements Schedulable {
    private final int slotId;
    private final String date;   // YYYY-MM-DD
    private final String time;   // HH:mm
    private boolean isBooked;
    private double basePrice;

    protected Slot(int slotId, String date, String time, double basePrice) {
        super("S" + slotId);
        this.slotId = slotId;
        this.date = date;
        this.time = time;
        setBasePrice(basePrice);
    }

    public int getSlotId() { return slotId; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public double getBasePrice() { return basePrice; }

    public void setBasePrice(double basePrice) {
        if (basePrice < 0) throw new IllegalArgumentException("Price cannot be negative.");
        this.basePrice = basePrice;
    }

    @Override public boolean isAvailable() { return !isBooked; }

    @Override public void reserve() throws SlotUnavailableException {
        if (isBooked) throw new SlotUnavailableException("That slot was just booked. Pick another open time.");
        isBooked = true;
    }

    @Override public void release() { isBooked = false; }

    public String getStatus() { return isBooked ? "BOOKED" : "OPEN"; }

    public abstract double calculatePrice();   // Polymorphism
    public abstract String getType();

    @Override public String displayDetails() {
        return date + " " + time + " [" + getType() + "] " + getStatus() + " PHP " + calculatePrice();
    }
}
