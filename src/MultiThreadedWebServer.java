import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiThreadedWebServer {
    private final int portNumber;
    private final ExecutorService executorService;
    private final String rootDirectory;
    private final String defaultPage;

    public MultiThreadedWebServer(Properties serverConfig) {
        this.portNumber = Integer.parseInt(serverConfig.getProperty("port"));
        this.rootDirectory = serverConfig.getProperty("root");
        this.defaultPage = serverConfig.getProperty("defaultPage");
        int maxThreads = Integer.parseInt(serverConfig.getProperty("maxThreads"));
        this.executorService = Executors.newFixedThreadPool(maxThreads);
    }

    public void run() throws InternalServerException {
        String clientAddress;
        try (ServerSocket serverSocket = new ServerSocket(this.portNumber)) {
            System.out.println("Server is listening on port " + this.portNumber);
            while (true) {
                Socket clientSessionSocket = serverSocket.accept();
                clientSessionSocket.setSoTimeout(10000);
                clientAddress = clientSessionSocket.getInetAddress() + ":" + clientSessionSocket.getPort();
                System.out.println("Accepted new connection from " + clientAddress);
                try {
                    ClientHandler clientHandler = new ClientHandler(clientSessionSocket, this);
                    this.executorService.execute(clientHandler);
                } catch (ClientHandlerException e) {
                    System.out.println("[" + clientAddress + "]: Failed to initialize client socket streams");
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to start server: " + e.getMessage());
            throw new InternalServerException(e);
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
