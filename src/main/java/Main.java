import java.util.Scanner;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
             * Don't use startsWith() + endsWith() for type command checks.
             * Example: type myecho : endsWith("echo") returns true, which incorrectly
             * treats "myecho" as the builtin command "echo".
             * Instead, extract the target command and compare it using equals().
             */

            /*
             * type:
             * - Builtin command? -> print "<cmd> is a shell builtin"
             * - Else search PATH for an executable file.
             * - Found? -> print full path.
             * - Not found? -> print "<cmd>: not found".
             */

            if (input.startsWith("type ")) {
                String target = input.substring(5);
                // check shell buildins first
                if (target.equals("echo") || target.equals("exit") || target.equals("type")) {
                    System.out.println(target + " is a shell builtin");
                    continue;
                }

                // Get PATH variable
                String pathEnv = System.getenv("PATH");

                if (pathEnv != null) {
                    // PATH looks like: /usr/bin:/usr/local/bin:/some/other/folder
                    String[] directories = pathEnv.split(File.pathSeparator);

                    boolean found = false;

                    // Search every directory in PATH
                    for (String dir : directories) {
                        // Build full path dir = "/usr/bin" , target = "ls" then result = "/usr/bin/ls"
                        Path filePath = Paths.get(dir, target);
                        // Check if file exists and is executable
                        if (Files.exists(filePath) && Files.isExecutable(filePath)) {

                            System.out.println(target + " is " + filePath);

                            found = true;
                            break;
                        }
                    }

                    // If not found anywhere
                    if (!found) {
                        System.out.println(target + ": not found");
                    }
                }
                continue;

            }

            System.out.println(input + ": command not found");
        }

        sc.close();
    }
}