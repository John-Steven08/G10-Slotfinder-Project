public class Appointment extends BookingEntity {
    private final Client client;
    private final Slot slot;
    private String status = "PENDING";

    public Appointment(String apptId, Client client, Slot slot) {
        super(apptId);
        this.client = client;
        this.slot = slot;
    }

    public Client getClient() { return client; }
    public Slot getSlot() { return slot; }
    public String getStatus() { return status; }
    public boolean isActive() { return !status.equals("CANCELLED"); }

    public void confirm() { if (status.equals("PENDING")) status = "CONFIRMED"; }

    public void cancel() {
        if (isActive()) { status = "CANCELLED"; slot.release(); }
    }

    public String generateReceipt() {
        return "SLOTFINDER RECEIPT\nBooking: " + id + "\nName: " + client.getName()
            + "\nDate: " + slot.getDate() + " at " + slot.getTime()
            + "\nService: " + slot.getType()
            + "\nTotal: PHP " + String.format("%.2f", slot.calculatePrice())
            + "\nStatus: " + status;
    }

    @Override public String displayDetails() { return id + " - " + slot.displayDetails() + " - " + status; }
}
