import java.util.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        Path currentDirectory = Paths.get(System.getProperty("user.dir")); // as normally changing pwd will not work directly so we need a variable for that 

        while (true) {
            System.out.print("$ ");
            System.out.flush(); // show whatever i have print right now 

            String input = sc.nextLine();

            if (input.equals("exit")) {
                break;
            }

            if (input.isEmpty()) {
                continue;
            }

            /*  String[] parts = input.split(" ");
             String command = parts[0]; // get thefirst word/token in input string , cmd like cat hi -> command = cat
             changed this to below code as above code would not handle it correctly because it splits only on spaces and if there are quotes in the input string then it would not handle it correctly
            */
           
            List<String> parts = parseCommand(input);
            String command = parts.get(0);

            String stdoutFile = null;
            String stderrFile = null;

            for (int i = 0; i < parts.size(); i++) {
                String token = parts.get(i);

                if (token.equals(">") || token.equals("1>")) {
                    stdoutFile = parts.get(i + 1);
                    parts = new ArrayList<>(parts.subList(0, i));
                    break;
                }

                if (token.equals("2>")) {
                    stderrFile = parts.get(i + 1);
                    parts = new ArrayList<>(parts.subList(0, i));
                    break;
                }
            }

            if (parts.isEmpty()) { // if we only have redirections and no command
                continue;
            }


            command = parts.get(0);

            /*
             * if we use contains then if other command has echo then this will also execute
             * which is incorrect
             */

            if (command.equals("echo")) {
                String output = String.join(" ", parts.subList(1, parts.size()));
                try {
                    if (stdoutFile != null) {
                        Files.writeString(Paths.get(stdoutFile),output + System.lineSeparator());
                    } else {    
                        System.out.println(output);
                    }
                } catch (Exception e) {   // as file can also have some error
                    e.printStackTrace();
                }

                continue;
            }
        
            // we cant use path here as path means where can i find executables and this tells about directory where we are currently present
            if (input.equals("pwd")) {
                System.out.println(currentDirectory);
                continue;
            }
            //[Absolute Path] change the current directory (getting output of pwd) to where we want to go in new directory 
            if(input.startsWith("cd ")) {
                String targetDirectory = input.substring(3);

                // to check ~ should be home directory 
                if(targetDirectory.equals("~")) {
                    targetDirectory = System.getenv("HOME");
                }

                Path newPath;
                // for Relative Path we use currentDirectory and resolve() to append target to it
                if (targetDirectory.startsWith("/")) {
                    newPath = Paths.get(targetDirectory); // absolute path (starting from the root of the file system)
                }else{
                    newPath = currentDirectory.resolve(targetDirectory); // relative path (starting from the current directory)
                }

                newPath = newPath.normalize(); // cleans up the path by removing unnecessary parts like . or .. 
                if(Files.exists(newPath) && Files.isDirectory(newPath)) {
                    currentDirectory = newPath;
                }else{
                    System.out.println("cd: " + targetDirectory + ": No such file or directory");
                }
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
                if (target.equals("echo") || target.equals("exit") || target.equals("type") || target.equals("pwd") || target.equals("cd")) {
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

            /*
             * External command execution
             * Example: input = "myprogram alice bob" --> command = "myprogram"
             * Search PATH for executable. If found: run executable with arguments. Else:
             * command not found.
             */

            

            String pathEnv = System.getenv("PATH");
            String[] directories = pathEnv.split(File.pathSeparator);

            boolean found = false;

            for (String dir : directories) {
                Path filePath = Paths.get(dir, command);
                if (Files.exists(filePath) && Files.isExecutable(filePath)) {
                    found = true;
                    try {
                      // ProcessBuilder accept list of string , as its first argument as Think of it as: Prepare to run: /usr/local/bin/custom_exe alice bob and Nothing is executed yet.
                      ProcessBuilder pb = new ProcessBuilder(parts);

                      if (stdoutFile != null) {
                          pb.redirectOutput(new File(stdoutFile)); // stdout -> file
                      } else {
                          pb.redirectOutput(ProcessBuilder.Redirect.INHERIT); // stdout -> terminal
                      }

                      if (stderrFile != null) {
                          pb.redirectError(new File(stderrFile)); // stderr -> file
                      } else {
                          pb.redirectError(ProcessBuilder.Redirect.INHERIT); // stderr -> terminal
                      }

                      Process process = pb.start(); // start the process - only after this line program runs - Send the worker to do the job.
                      process.waitFor(); // wait for the process to complete - wait until worker returns  

                    } catch (Exception e) {
                        e.printStackTrace(); // print full details of the error in the console 
                    }

                    break;
                }
            }

            if (!found) {
                System.out.println(input + ": command not found");
            }
        }

        sc.close();
    }

    // This is to handle single and double quotes in input string
    private static List<String> parseCommand(String input) {

        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);

            // Handle backslashes
            if (ch == '\\') {
                // Inside single quotes: backslash is literal
                if (inSingleQuote) {
                    current.append('\\');
                    continue;
                }

                // Inside double quotes
                if (inDoubleQuote) {
                    if (i + 1 < input.length()) {
                        char next = input.charAt(i + 1);
                        // Only \" and \\ are special
                        if (next == '"' || next == '\\') {
                            current.append(next);
                            i++;
                        }else{
                            current.append('\\');
                        }
                    }

                    continue;
                }

                // Outside quotes
                if (i + 1 < input.length()) {
                    current.append(input.charAt(i + 1));
                    i++;
                }

                continue;
            }


            // Handle single quotes
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }

            // Handle double quotes
            if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
             
            // Handle spaces (only when NOT inside quotes)
            if (ch == ' ' && !inSingleQuote && !inDoubleQuote) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
                 
                continue;
            }
            // Add normal characters
                current.append(ch);
        }
        // Add last token
        if (current.length() > 0) {
            parts.add(current.toString());
        }

        return parts;
    }
}