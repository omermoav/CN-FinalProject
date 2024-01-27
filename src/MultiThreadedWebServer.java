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
    private final ServerSocket serverSocket;
    private final Set<Socket> connectedClients;
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
        this.serverSocket = new ServerSocket(Integer.parseInt(serverConfig.getProperty("port")));
        this.rootDirectory = serverConfig.getProperty("root");
        this.defaultPage = serverConfig.getProperty("defaultPage");
        this.connectedClients = Collections.synchronizedSet(new HashSet<Socket>());
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public String getWelcomeResponse() {
        return hardCodedResponse;
    }

    public void run() throws IOException {
        while (true) {
            Socket clientSessionSocket = this.serverSocket.accept();
            this.connectedClients.add(clientSessionSocket);
            ClientHandler clientHandler = new ClientHandler(clientSessionSocket, this);
            this.executorService.execute(clientHandler);
        }
    }

    public void disconnectClient(Socket clientSessionSocket) {
        this.connectedClients.remove(clientSessionSocket);
    }
}
