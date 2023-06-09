import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        CppComp c = null;
        Scanner scan = new Scanner(System.in);
        try {
            c = new CppComp();
        } catch (Exception e) {
            System.out.println("Compiler is not installed");
        }

        try {
            c.executeFile("bb.cpp");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            String s = scan.nextLine();
            c.sendInput(s);
            s = scan.nextLine();
            c.sendInput(s);
            s = scan.nextLine();
            c.sendInput(s);
        } catch (Exception e) {
            
            e.printStackTrace();
        }
        scan.close();
    }
}
