import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiThreadedWebServer {
    private final int portNumber;
    private final ExecutorService executorService;
    private final String rootDirectory;
    private final String defaultPage;
    private static final String hardCodedResponse = "HTTP/1.1 200 OK\r\n" +
            "\r\n" +
            "<html>\n" +
            "<body>\n" +
            "<h1>Hello, World!</h1>\n" +
            "</body>\n" +
            "</html>";
    private static int THREAD_POOL_SIZE;

    public MultiThreadedWebServer(Properties serverConfig) throws IOException {
        THREAD_POOL_SIZE = Integer.parseInt(serverConfig.getProperty("maxThreads"));
        this.portNumber = Integer.parseInt(serverConfig.getProperty("port"));
        this.rootDirectory = serverConfig.getProperty("root");
        this.defaultPage = serverConfig.getProperty("defaultPage");
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public void run() throws IOException {
        try {
            ServerSocket serverSocket = new ServerSocket(portNumber);
            System.out.println("Server is listening on port " + portNumber);
            while (true) {
                Socket clientSessionSocket = serverSocket.accept();
                System.out.println("Accepted new connection from " + clientSessionSocket.getInetAddress() + ":" + clientSessionSocket.getPort());
                ClientHandler clientHandler = new ClientHandler(clientSessionSocket, this);
                clientHandler.run();
                //this.executorService.execute(clientHandler);
            }
        } catch (Exception e) {
            // TODO: Handle this exception
            System.out.println("An unexpected error occurred");
            e.printStackTrace();
        } finally {
            this.executorService.shutdown();
        }
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public String getDefaultPage() {
        return defaultPage;
    }
}
