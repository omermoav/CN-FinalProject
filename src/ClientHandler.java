import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSessionSocket;
    private final MultiThreadedWebServer server;
    private final String clientIPAddress;
    private final int clientPortNumber;

    public ClientHandler(Socket clientSessionSocket, MultiThreadedWebServer server) {
        this.clientSessionSocket = clientSessionSocket;
        this.server = server;
        this.clientIPAddress = clientSessionSocket.getInetAddress().getHostAddress();
        this.clientPortNumber = clientSessionSocket.getPort();
    }

    @Override
    public void run() {
        String clientRequest;
        try (BufferedReader inFromClient = new BufferedReader(new InputStreamReader(this.clientSessionSocket.getInputStream()));
             DataOutputStream outToClient = new DataOutputStream(this.clientSessionSocket.getOutputStream())) {
            // Parse the client request
            try {
                HTTPRequest request = new HTTPRequest(inFromClient);
                System.out.println("HTTP request: \n" + request); // TODO: resolve request.toString()

            } catch (Exception e) {
                //TODO: Send 400 Bad Request!!!!
            }



            //send response


            outToClient.writeBytes(this.server.getWelcomeResponse());

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                this.clientSessionSocket.close();
                this.server.disconnectClient(clientSessionSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
