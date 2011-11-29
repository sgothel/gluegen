
public class ClassInJar1 {
    static {
        System.err.println("ClassInJar1.init<>");
    }

    public static void ping() {
        System.err.println("ClassInJar1.ping()");
    }
}
