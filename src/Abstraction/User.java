/** Abstract base for everyone who uses the system. */
public abstract class User {
    protected String userId;
    protected String name;
    protected String email;

    protected User(String userId, String name, String email) {
        this.userId = userId;
        setName(name);
        setEmail(email);
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }

    public void setName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Enter your name.");
        this.name = name.trim();
    }

    public void setEmail(String email) {
        if (email == null || !email.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))
            throw new IllegalArgumentException("Enter a valid email address.");
        this.email = email.trim().toLowerCase();
    }

    public abstract String displayProfile();
    public abstract String getRole();
}
