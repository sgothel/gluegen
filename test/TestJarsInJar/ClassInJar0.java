
public class ClassInJar0 {
    static {
        System.err.println("ClassInJar0.init<>");
    }

    public static void ping() {
        System.err.println("ClassInJar0.ping()");
    }

    public static void main(String args[]) {
        System.err.println("ClassInJar0.main() start");
        // ClassInJar1.ping();
        // ClassInJar2.ping();
        System.err.println("ClassInJar0.main() end");
    }
}
