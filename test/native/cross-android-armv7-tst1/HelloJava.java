
public class HelloJava {
    public static void main(String args[]) {
        System.out.print("HelloJava main(");
        for(int i=0; i<args.length; i++) {
            if(0<i) {
                System.out.print(", ");
            }
            System.out.print(args[i]);
        }
        System.out.println(")");
    }
    public static void test(int i) {
        System.out.println("HelloJava test("+i+")");
    }
}
