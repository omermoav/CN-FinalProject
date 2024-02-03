import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSessionSocket;
    private final MultiThreadedWebServer server;

    public ClientHandler(Socket clientSessionSocket, MultiThreadedWebServer server) {
        this.clientSessionSocket = clientSessionSocket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            this.handleRequest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleRequest() {
        String clientRequest;
        try (BufferedReader inFromClient = new BufferedReader(new InputStreamReader(this.clientSessionSocket.getInputStream()));
             DataOutputStream outToClient = new DataOutputStream(this.clientSessionSocket.getOutputStream())) {

            // Parse the client request
            HTTPRequest request = new HTTPRequest(inFromClient);
            System.out.println("HTTP request: \n" + request.getRawRequest());

            // Build response
            HTTPResponse response = createResponse(request);
            System.out.println("Response Headers: \n" + response.getHeaders());

            response.send(outToClient);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                this.clientSessionSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private HTTPResponse createResponse(HTTPRequest request) {
        HTTPResponse response = new HTTPResponse();
        // Logic to determine the content and status of the response
        // This is a simplified example. You'd typically check httpRequest's properties
        // and decide on the response's content and status code.
        if (request.getRequestedPage().equals("/")) {
            // Example for the root page request
            response.setStatusCode(200); // OK
            response.addHeader("Content-Type", "text/html");
            String body = "<html><body><h1>Welcome to My Server</h1></body></html>";
            response.setBody(body.getBytes());
        } else {
            // Example for not found
            response.setStatusCode(404); // Not Found
            response.addHeader("Content-Type", "text/html");
            String body = "<html><body><h1>404 Not Found</h1></body></html>";
            response.setBody(body.getBytes());
        }
        return response;
    }
}
