import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/** Holds all data in memory (ArrayList) and applies the booking rules. */
public class BookingService {
    public static final String DEFAULT_KEY = "admin123";

    private final ArrayList<Slot> slots = new ArrayList<>();
    private final ArrayList<Appointment> appointments = new ArrayList<>();
    private final Map<String, Client> clients = new HashMap<>();
    private final Admin admin = new Admin("A1", "Service Provider", "admin@slotfinder.local", DEFAULT_KEY);
    private int nextSlot = 1, nextAppt = 1;

    private void requireAdmin(String key) {
        if (key == null || !admin.verify(key)) throw new SecurityException("Admin key is incorrect.");
    }

    public static String validateDate(String d) throws InvalidTimeFormatException {
        try { return LocalDate.parse(d.trim()).toString(); }
        catch (RuntimeException e) { throw new InvalidTimeFormatException("Date must use the format YYYY-MM-DD."); }
    }

    public static String validateTime(String t) throws InvalidTimeFormatException {
        if (t == null || !t.trim().matches("\\d{2}:\\d{2}"))
            throw new InvalidTimeFormatException("Time must use the 24-hour format HH:mm.");
        try { return LocalTime.parse(t.trim()).toString(); }
        catch (RuntimeException e) { throw new InvalidTimeFormatException("Time must use the 24-hour format HH:mm."); }
    }

    private static double num(String s) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException("Enter a price.");
        return Double.parseDouble(s.trim());
    }

    private Slot add(String d, String t, String type, double base, double fee) throws SlotUnavailableException {
        int id = nextSlot;
        Slot s = "Priority".equalsIgnoreCase(type)
            ? new VipSlot(id, d, t, base, fee)
            : new StandardSlot(id, d, t, base, "Consultation".equalsIgnoreCase(type) ? "Consultation" : "Standard");
        admin.addSlot(slots, s);
        nextSlot++;
        return s;
    }

    public synchronized Slot addSlot(String key, String date, String time, String type, String price, String fee)
            throws InvalidTimeFormatException, SlotUnavailableException {
        requireAdmin(key);
        String d = validateDate(date), t = validateTime(time);
        double base = num(price), extra = (fee == null || fee.isBlank()) ? 0 : num(fee);
        if (base < 0 || extra < 0) throw new IllegalArgumentException("Prices cannot be negative.");
        return add(d, t, type == null ? "Standard" : type, base, extra);
    }

    public synchronized void removeSlot(String key, int slotId) throws BookingNotFoundException {
        requireAdmin(key);
        for (Appointment a : appointments) if (a.getSlot().getSlotId() == slotId) a.cancel();
        admin.removeSlot(slots, slotId);
    }

    private static boolean has(String s) { return s != null && !s.isBlank(); }

    public synchronized List<Slot> search(String date, String from, String to, String type, boolean openOnly) {
        List<Slot> out = new ArrayList<>();
        for (Slot s : slots) {
            if (has(date) && !s.getDate().equals(date)) continue;
            if (has(from) && s.getTime().compareTo(from) < 0) continue;
            if (has(to) && s.getTime().compareTo(to) > 0) continue;
            if (has(type) && !s.getType().equalsIgnoreCase(type)) continue;
            if (openOnly && !s.isAvailable()) continue;
            out.add(s);
        }
        out.sort(Comparator.comparing(Slot::getDate).thenComparing(Slot::getTime));
        return out;
    }

    public synchronized Appointment book(String slotId, String name, String email)
            throws SlotUnavailableException, BookingNotFoundException {
        Slot slot = null;
        int id = Integer.parseInt(String.valueOf(slotId).trim());
        for (Slot s : slots) if (s.getSlotId() == id) slot = s;
        if (slot == null) throw new BookingNotFoundException("That slot no longer exists.");

        Client c = new Client("C" + (clients.size() + 1), name, email);  // validates name and email
        Client known = clients.get(c.getEmail());
        if (known == null) { clients.put(c.getEmail(), c); known = c; }

        Appointment a = known.bookSlot(slot, "A" + nextAppt);
        nextAppt++;
        appointments.add(a);
        return a;
    }

    public synchronized Appointment cancel(String apptId, String email, String key) throws BookingNotFoundException {
        Appointment found = null;
        for (Appointment a : appointments) if (a.getId().equals(apptId)) found = a;
        boolean isAdmin = key != null && admin.verify(key);
        if (found == null || (!isAdmin && (email == null || !found.getClient().getEmail().equalsIgnoreCase(email.trim()))))
            throw new BookingNotFoundException("No booking " + apptId + " found for that email.");
        found.getClient().cancelBooking(apptId);
        return found;
    }

    public synchronized List<Appointment> appointmentsFor(String email) {
        Client c = clients.get(email == null ? "" : email.trim().toLowerCase());
        List<Appointment> out = c == null ? new ArrayList<>() : new ArrayList<>(c.getMyBookings());
        Collections.reverse(out);
        return out;
    }

    public synchronized List<Appointment> allAppointments(String key) {
        requireAdmin(key);
        List<Appointment> out = new ArrayList<>(appointments);
        Collections.reverse(out);
        return out;
    }

    /** Sample data so the calendar is not empty on first run. */
    public synchronized void seed() {
        LocalDate today = LocalDate.now();
        String[][] plan = {{"09:00", "Standard"}, {"10:30", "Consultation"}, {"13:00", "Standard"}, {"15:00", "Priority"}};
        for (int d = 0; d < 5; d++)
            for (String[] p : plan) {
                try { add(today.plusDays(d).toString(), p[0], p[1], p[1].equals("Consultation") ? 450 : 300, p[1].equals("Priority") ? 200 : 0); }
                catch (SlotUnavailableException ignored) { }
            }
    }
}
