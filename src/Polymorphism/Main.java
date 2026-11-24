import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    private static final BookingService svc = new BookingService();
    private static final Scanner in = new Scanner(System.in);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH);
    private static final String[] SERVICES = {"Standard", "Priority", "Consultation"};
    private static String lastName = "", lastEmail = "";

    public static void main(String[] args) {
        svc.seed();
        System.out.println();
        System.out.println("  ==========================================");
        System.out.println("     SlotFinder - Calendar & Booking System");
        System.out.println("  ==========================================");
        System.out.println("  Type the number of an option and press Enter.");
        while (true) {
            title("MAIN MENU");
            System.out.println("  1  Find and book a time");
            System.out.println("  2  My bookings (view or cancel)");
            System.out.println("  3  Admin (service provider)");
            System.out.println("  0  Exit");
            switch (askInt("Your choice", 0, 3)) {
                case 1 -> findAndBook();
                case 2 -> myBookings();
                case 3 -> admin();
                default -> { System.out.println("\n  Thank you for using SlotFinder. Goodbye!"); return; }
            }
        }
    }

    // =============== CLIENT ===============
    private static void findAndBook() {
        title("FIND AND BOOK A TIME");
        System.out.println("  How do you want to look?");
        System.out.println("  1  See the week at a glance");
        System.out.println("  2  Pick a day");
        System.out.println("  3  Pick a service (Standard / Priority / Consultation)");
        System.out.println("  4  Pick a time range (example: 13:00 to 17:00)");
        System.out.println("  5  Show all open times");
        System.out.println("  0  Back");
        int c = askInt("Your choice", 0, 5);
        List<Slot> list;
        switch (c) {
            case 0 -> { return; }
            case 1 -> {
                weekView();
                if (!askYesNo("\n  Do you want to book one of the open times?")) return;
                list = svc.search(null, null, null, null, true);
            }
            case 2 -> {
                TreeMap<String, Integer> open = new TreeMap<>();
                for (Slot s : svc.search(null, null, null, null, true)) open.merge(s.getDate(), 1, Integer::sum);
                if (open.isEmpty()) { System.out.println("\n  There are no open times right now."); return; }
                List<String> dates = new ArrayList<>(open.keySet());
                System.out.println();
                for (int i = 0; i < dates.size(); i++)
                    System.out.printf("  %2d)  %-11s %d open time(s)%n", i + 1, day(dates.get(i)), open.get(dates.get(i)));
                int n = askInt("Pick a day (0 = back)", 0, dates.size());
                if (n == 0) return;
                list = svc.search(dates.get(n - 1), null, null, null, true);
            }
            case 3 -> {
                System.out.println();
                for (int i = 0; i < SERVICES.length; i++) System.out.println("  " + (i + 1) + ")  " + SERVICES[i]);
                int n = askInt("Pick a service (0 = back)", 0, SERVICES.length);
                if (n == 0) return;
                list = svc.search(null, null, null, SERVICES[n - 1], true);
            }
            case 4 -> {
                String from = askTime("Start time (HH:mm, example 13:00)");
                String to = askTime("End time (HH:mm, example 17:00)");
                list = svc.search(null, from, to, null, true);
            }
            default -> list = svc.search(null, null, null, null, true);
        }
        chooseAndBook(list);
    }

    private static void chooseAndBook(List<Slot> list) {
        if (list.isEmpty()) { System.out.println("\n  No open times found. Try a different search."); return; }
        System.out.println("\n  OPEN TIMES\n");
        printSlots(list, false);
        int n = askInt("\n  Type the number of the time you want (0 = back)", 0, list.size());
        if (n == 0) return;
        Slot s = list.get(n - 1);
        System.out.println("\n  You chose: " + day(s.getDate()) + " at " + s.getTime() + " - " + s.getType()
                + " - PHP " + String.format("%.2f", s.calculatePrice()));
        String name = askText("Your name", lastName);
        String email = askEmail(lastEmail);
        if (!askYesNo("Confirm this booking?")) { System.out.println("\n  Okay, nothing was booked."); return; }
        try {
            Appointment a = svc.book(String.valueOf(s.getSlotId()), name, email);
            lastName = name;
            lastEmail = email;
            System.out.println("\n  BOOKING CONFIRMED!");
            System.out.println("  ------------------------------------");
            for (String line : a.generateReceipt().split("\n")) System.out.println("  " + line);
            System.out.println("  ------------------------------------");
            System.out.println("  Keep your booking ID (" + a.getId() + "). Use your email to see or cancel it later.");
        } catch (Exception e) {
            System.out.println("\n  Sorry: " + e.getMessage());
        }
    }

    private static void myBookings() {
        title("MY BOOKINGS");
        String email = askEmail(lastEmail);
        List<Appointment> list = svc.appointmentsFor(email);
        if (list.isEmpty()) { System.out.println("\n  No bookings found for " + email + "."); return; }
        System.out.println();
        printAppointments(list, false);
        if (list.stream().noneMatch(Appointment::isActive)) return;
        int n = askInt("\n  Type a number to cancel that booking (0 = back)", 0, list.size());
        if (n == 0) return;
        Appointment a = list.get(n - 1);
        if (!a.isActive()) { System.out.println("\n  That booking is already cancelled."); return; }
        if (!askYesNo("Cancel booking " + a.getId() + " (" + day(a.getSlot().getDate()) + " " + a.getSlot().getTime() + ")?")) return;
        try {
            svc.cancel(a.getId(), email, null);
            System.out.println("\n  Booking " + a.getId() + " was cancelled. The time is open again.");
        } catch (Exception e) {
            System.out.println("\n  Sorry: " + e.getMessage());
        }
    }

    // =============== ADMIN ===============
    private static void admin() {
        title("ADMIN LOGIN");
        System.out.println("  (Demo key: " + BookingService.DEFAULT_KEY + ")");
        String key = null;
        for (int left = 3; left > 0 && key == null; left--) {
            String k = ask("Admin key (0 = back)");
            if (k.equals("0")) return;
            try { svc.allAppointments(k); key = k; }
            catch (SecurityException e) { System.out.println("  Wrong key. Tries left: " + (left - 1)); }
        }
        if (key == null) return;
        while (true) {
            title("ADMIN MENU");
            System.out.println("  1  Add a time slot");
            System.out.println("  2  Remove a time slot");
            System.out.println("  3  See all bookings");
            System.out.println("  4  Cancel a booking");
            System.out.println("  5  Week at a glance");
            System.out.println("  0  Log out");
            switch (askInt("Your choice", 0, 5)) {
                case 1 -> adminAdd(key);
                case 2 -> adminRemove(key);
                case 3 -> { System.out.println(); printAppointments(svc.allAppointments(key), true); }
                case 4 -> adminCancel(key);
                case 5 -> weekView();
                default -> { return; }
            }
        }
    }

    private static void adminAdd(String key) {
        do {
            System.out.println("\n  ADD A TIME SLOT");
            String date = askDate("Date (YYYY-MM-DD, example 2026-11-25)", LocalDate.now().toString());
            String time = askTime("Time (HH:mm, example 09:00)");
            System.out.println();
            for (int i = 0; i < SERVICES.length; i++) System.out.println("  " + (i + 1) + ")  " + SERVICES[i]);
            String type = SERVICES[askInt("Service", 1, SERVICES.length) - 1];
            double price = askMoney("Price in PHP", type.equals("Consultation") ? 450 : 300);
            double fee = type.equals("Priority") ? askMoney("Priority fee in PHP", 200) : 0;
            try {
                Slot s = svc.addSlot(key, date, time, type, String.valueOf(price), String.valueOf(fee));
                System.out.println("\n  Added: " + day(s.getDate()) + " " + s.getTime() + " - " + s.getType() + " - PHP " + String.format("%.2f", s.calculatePrice()));
            } catch (Exception e) {
                System.out.println("\n  Could not add the slot: " + e.getMessage());
            }
        } while (askYesNo("\n  Add another slot?"));
    }

    private static void adminRemove(String key) {
        List<Slot> all = svc.search(null, null, null, null, false);
        if (all.isEmpty()) { System.out.println("\n  There are no slots."); return; }
        System.out.println();
        printSlots(all, true);
        int n = askInt("\n  Type the number of the slot to remove (0 = back)", 0, all.size());
        if (n == 0) return;
        Slot s = all.get(n - 1);
        String warn = s.isAvailable() ? "" : " It is booked, so that booking will be cancelled.";
        if (!askYesNo("Remove " + day(s.getDate()) + " " + s.getTime() + "?" + warn)) return;
        try {
            svc.removeSlot(key, s.getSlotId());
            System.out.println("\n  Slot removed.");
        } catch (Exception e) {
            System.out.println("\n  Sorry: " + e.getMessage());
        }
    }

    private static void adminCancel(String key) {
        List<Appointment> all = svc.allAppointments(key);
        if (all.isEmpty()) { System.out.println("\n  There are no bookings yet."); return; }
        System.out.println();
        printAppointments(all, true);
        int n = askInt("\n  Type the number of the booking to cancel (0 = back)", 0, all.size());
        if (n == 0) return;
        Appointment a = all.get(n - 1);
        if (!a.isActive()) { System.out.println("\n  That booking is already cancelled."); return; }
        if (!askYesNo("Cancel booking " + a.getId() + " for " + a.getClient().getName() + "?")) return;
        try {
            svc.cancel(a.getId(), null, key);
            System.out.println("\n  Booking " + a.getId() + " was cancelled.");
        } catch (Exception e) {
            System.out.println("\n  Sorry: " + e.getMessage());
        }
    }

    // =============== DISPLAY ===============
    private static void weekView() {
        List<Slot> all = svc.search(null, null, null, null, false);
        if (all.isEmpty()) { System.out.println("\n  There are no time slots yet."); return; }
        TreeSet<String> dates = new TreeSet<>(), times = new TreeSet<>();
        for (Slot s : all) { dates.add(s.getDate()); times.add(s.getTime()); }
        List<String> days = new ArrayList<>(dates);
        if (days.size() > 7) days = days.subList(0, 7);
        System.out.println("\n  WEEK AT A GLANCE    open = free   FULL = taken   - = no slot\n");
        System.out.printf("  %-7s", "Time");
        for (String d : days) System.out.printf("%-9s", LocalDate.parse(d).format(SHORT));
        System.out.println();
        for (String t : times) {
            System.out.printf("  %-7s", t);
            for (String d : days) {
                String cell = "-";
                for (Slot s : all) if (s.getDate().equals(d) && s.getTime().equals(t)) cell = s.isAvailable() ? "open" : "FULL";
                System.out.printf("%-9s", cell);
            }
            System.out.println();
        }
    }

    private static void printSlots(List<Slot> list, boolean showStatus) {
        for (int i = 0; i < list.size(); i++) {
            Slot s = list.get(i);
            System.out.printf("  %2d)  %-11s %-6s %-13s PHP %8.2f%s%n", i + 1, day(s.getDate()), s.getTime(), s.getType(),
                    s.calculatePrice(), showStatus ? "  " + s.getStatus() : "");
        }
    }

    private static void printAppointments(List<Appointment> list, boolean withClient) {
        if (list.isEmpty()) { System.out.println("  No bookings yet."); return; }
        for (int i = 0; i < list.size(); i++) {
            Appointment a = list.get(i);
            Slot s = a.getSlot();
            System.out.printf("  %2d)  %-4s %-11s %-6s %-13s %-10s%s%n", i + 1, a.getId(), day(s.getDate()), s.getTime(),
                    s.getType(), a.getStatus(), withClient ? "  " + a.getClient().getName() : "");
        }
    }

    private static String day(String date) { return LocalDate.parse(date).format(DAY); }

    private static void title(String text) {
        System.out.println("\n  ------------------------------------------");
        System.out.println("  " + text);
        System.out.println("  ------------------------------------------");
    }

    // =============== INPUT (every question repeats until the answer is valid) ===============
    private static String ask(String prompt) {
        System.out.print("  " + prompt + ": ");
        if (!in.hasNextLine()) { System.out.println(); System.exit(0); }
        return in.nextLine().trim();
    }

    private static int askInt(String prompt, int min, int max) {
        while (true) {
            String s = ask(prompt + " [" + min + "-" + max + "]");
            try {
                int n = Integer.parseInt(s);
                if (n >= min && n <= max) return n;
            } catch (NumberFormatException ignored) { }
            System.out.println("  Please type a number from " + min + " to " + max + ".");
        }
    }

    private static boolean askYesNo(String prompt) {
        while (true) {
            String s = ask(prompt + " (y/n)").toLowerCase();
            if (s.equals("y") || s.equals("yes")) return true;
            if (s.equals("n") || s.equals("no")) return false;
            System.out.println("  Please type y or n.");
        }
    }

    private static String askText(String prompt, String def) {
        while (true) {
            String s = ask(def.isEmpty() ? prompt : prompt + " (Enter = " + def + ")");
            if (!s.isEmpty()) return s;
            if (!def.isEmpty()) return def;
            System.out.println("  This cannot be empty.");
        }
    }

    private static String askEmail(String def) {
        while (true) {
            String s = askText("Your email (example: ana@gmail.com)", def);
            if (s.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) return s.toLowerCase();
            System.out.println("  That does not look like an email address. Try again.");
        }
    }

    private static String askDate(String prompt, String def) {
        while (true) {
            try { return BookingService.validateDate(askText(prompt, def)); }
            catch (InvalidTimeFormatException e) { System.out.println("  " + e.getMessage()); }
        }
    }

    private static String askTime(String prompt) {
        while (true) {
            try { return BookingService.validateTime(askText(prompt, "")); }
            catch (InvalidTimeFormatException e) { System.out.println("  " + e.getMessage()); }
        }
    }

    private static double askMoney(String prompt, double def) {
        while (true) {
            String s = askText(prompt, String.valueOf((int) def));
            try {
                double v = Double.parseDouble(s);
                if (v >= 0) return v;
            } catch (NumberFormatException ignored) { }
            System.out.println("  Please type an amount such as 300.");
        }
    }
}
