import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");
            System.out.flush();

            String input = sc.nextLine();

            if (input.equals("exit")) {
                break;
            }

            if (input.isEmpty()) {
                continue;
            }

            /*
             * if we use contains then if other command has echo then this will also execute
             * which is incorrect
             */

            if (input.startsWith("echo ")) {
                System.out.println(input.substring(5));
                continue;
            }

            /*
             * if i use startwith and endswith then type myecho will run and it will give
             * output of myecho is a shell builtin
             * 
             * if(input.startsWith("type ")){
             * if(input.endsWith("echo") || input.endsWith("type") ||
             * input.endsWith("exit")){
             * System.out.println(input.substring(5) + " is a shell builtin");
             * } else {
             * System.out.println(input.substring(5) + ": not found");
             * }
             * continue;
             * }
             */

            if (input.startsWith("type ")) {
                String target = input.substring(5);

                if (target.equals("echo") || target.equals("exit") || target.equals("type")) {
                    System.out.println(target + " is a shell builtin");
                } else {
                    System.out.println(target + ": not found");
                }

                continue;
            }

            System.out.println(input + ": command not found");
        }

        sc.close();
    }
}