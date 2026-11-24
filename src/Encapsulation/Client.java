import java.util.ArrayList;
import java.util.List;

public class Client extends User {
    private final ArrayList<Appointment> myBookings = new ArrayList<>();

    public Client(String userId, String name, String email) { super(userId, name, email); }

    /** Reserves the slot and records a confirmed appointment. */
    public Appointment bookSlot(Slot slot, String apptId) throws SlotUnavailableException {
        slot.reserve();
        Appointment a = new Appointment(apptId, this, slot);
        a.confirm();
        myBookings.add(a);
        return a;
    }

    public void cancelBooking(String apptId) throws BookingNotFoundException {
        for (Appointment a : myBookings) {
            if (a.getId().equals(apptId)) { a.cancel(); return; }
        }
        throw new BookingNotFoundException("No booking " + apptId + " found.");
    }

    public List<Appointment> getMyBookings() { return myBookings; }

    @Override public String getRole() { return "Client"; }
    @Override public String displayProfile() { return "Client: " + name + " <" + email + ">, bookings: " + myBookings.size(); }
}
