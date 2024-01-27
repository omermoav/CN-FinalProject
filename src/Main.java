import java.io.*;
import java.util.Properties;

public class Main {
    public static void main(String[] argv) throws IOException {
        Properties config = new Properties();
        try {
            config.load(new FileInputStream("src/config.ini"));
            MultiThreadedWebServer tcpMultithreadedChatServer = new MultiThreadedWebServer(config);
            tcpMultithreadedChatServer.run();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
