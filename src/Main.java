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
            e.printStackTrace();
        }
    }
}
