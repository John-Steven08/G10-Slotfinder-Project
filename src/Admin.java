import java.util.List;

public class Admin extends User {
    private final String adminKey;

    public Admin(String userId, String name, String email, String adminKey) {
        super(userId, name, email);
        this.adminKey = adminKey;
    }

    public boolean verify(String key) { return adminKey.equals(key); }

    /** Conflict guard: no two slots may share the same date and time. */
    public void addSlot(List<Slot> slots, Slot slot) throws SlotUnavailableException {
        for (Slot s : slots) {
            if (s.getDate().equals(slot.getDate()) && s.getTime().equals(slot.getTime()))
                throw new SlotUnavailableException("A slot already exists on " + s.getDate() + " at " + s.getTime() + ".");
        }
        slots.add(slot);
    }

    public void removeSlot(List<Slot> slots, int slotId) throws BookingNotFoundException {
        if (!slots.removeIf(s -> s.getSlotId() == slotId)) throw new BookingNotFoundException("Slot " + slotId + " not found.");
    }

    @Override public String getRole() { return "Administrator"; }
    @Override public String displayProfile() { return "Admin: " + name; }
}
