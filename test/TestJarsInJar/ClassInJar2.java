
public class ClassInJar2 {
    static {
        System.err.println("ClassInJar2.init<>");
    }

    public static void ping() {
        System.err.println("ClassInJar2.ping()");
    }
}
