import java.io.*;
import java.util.Properties;

public class Main {
    public static void main(String[] argv) {
        Properties config = new Properties();
        try {
            config.load(new FileInputStream("src/config.ini"));
            MultiThreadedWebServer server = new MultiThreadedWebServer(config);
            server.run();
        } catch (IOException e) {
            System.out.println("Failed to load data from configuration file: " + e.getMessage());
        } catch (InternalServerException e) {
            System.out.println("An internal server error occurred: " + e.getMessage());
        }
    }
}
