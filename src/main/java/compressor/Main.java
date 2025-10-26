package compressor;

import compressor.services.APIController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        // 1. Check for minimum required arguments (4, as determined by APIController)
        if (args.length < 4) {
            // Updated usage message to reflect expected arguments
            System.err.println("Usage: java Main <type> <method> <sourceFile> <destinationFile> [optional_flags...]");
            return;
        }

        try {
            // 2. Convert the args array to a List<String>
            List<String> argList = Arrays.asList(args);
            // 3. Instantiate the APIController using the List
            APIController controller = new APIController((ArrayList<String>) argList);
            // 4. Execute the command
            controller.run();

        } catch (IllegalArgumentException e) {
            // Catch configuration errors (e.g., bad detail level, missing flags)
            System.err.println("CONFIGURATION ERROR: " + e.getMessage());
        } catch (IOException e) {
            // Catch file I/O errors from the run() method
            System.err.println("I/O ERROR: Failed during file processing: " + e.getMessage());
        } catch (IllegalStateException e) {
            // Catch state errors (e.g., get index missing)
            System.err.println("EXECUTION ERROR: " + e.getMessage());
        } catch (Exception e) {
            // Catch any unexpected runtime exceptions
            System.err.println("UNEXPECTED ERROR: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }
}

