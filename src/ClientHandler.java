import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

public class ClientHandler implements Runnable {
    private final Socket clientSessionSocket;
    private final MultiThreadedWebServer server;
    private final BufferedReader clientInputStream;
    private final DataOutputStream clientOutputStream;
    private final String clientAddress;

    public ClientHandler(Socket clientSessionSocket, MultiThreadedWebServer server) throws ClientHandlerException {
        try {
            this.clientSessionSocket = clientSessionSocket;
            this.clientAddress = clientSessionSocket.getInetAddress() + ":" + clientSessionSocket.getPort();
            this.server = server;
            this.clientInputStream = new BufferedReader(new InputStreamReader(this.clientSessionSocket.getInputStream()));
            this.clientOutputStream = new DataOutputStream(this.clientSessionSocket.getOutputStream());
        } catch (IOException e) {
            throw new ClientHandlerException(e);
        }
    }

    @Override
    public void run() {
        try {
            this.handleRequest();
        } catch (Exception e) {
            System.out.println("[" + this.clientAddress + "]: Failed to respond to client");
        }
    }

    private void handleRequest() throws IOException {
        HTTPRequest request = null;
        HTTPResponse response = new HTTPResponse();

        try {
            try {
                // Parse the client request
                request = new HTTPRequest();
                request.readFullRequest(this.clientInputStream);
                System.out.println("[" + this.clientAddress + "]: HTTP request Headers: \n" + request.getRawHeaders());
                request.parseRequest();
            } catch (IOException e) {
                // Could not read client's request
                System.out.println("[" + this.clientAddress + "]: Server failed to read client's request");
                throw e;
            } catch (BadRequestException e) {
                System.out.println(e.getMessage() + "!!!!");
                if ("HEAD".equals(request.getType())) {
                    response.setHeadResponse(true);
                }
                // 400
                handleBadRequestError(response);
                return;
            }

            // Request is valid
            // Check if the request is a HEAD request
            if ("HEAD".equals(request.getType())) {
                // In order to exclude the response body
                response.setHeadResponse(true);
            }
            response.setChunkedResponse(request.isChunkedResponse());

            boolean isValidType = verifyRequestType(request);
            // 501
            if (!isValidType) {
                handleNotImplementedError(response);
                return;
            }

            String requestedFilePath = System.getProperty("user.home") + this.server.getRootDirectory().substring(1) + (request.getRequestedPage().equals("/") ? this.server.getDefaultPage() : request.getRequestedPage().substring(1));
            File file = new File(requestedFilePath);

            // 404
            if (!file.exists() || file.isDirectory()) {
                handleNotFoundError(response);
                return;
            }

            // Read the file content
            byte[] fileContent = readFile(file);

            // 200
            response.setStatus(200);
            response.addHeader("Content-Type", HTTPResponse.getContentTypeByFileName(requestedFilePath).getValue());

            // Handle TRACE request
            if (request.getType().equals("TRACE")) {
                handleTraceRequest(response, request);
                return;
            }

            // Handle params_info file
            if (request.getRequestedPage().equals("/params_info.html") && request.getType().equals("POST")) {
                fileContent = addParamsToFileContent(fileContent, request.getParameters());
            }

            // Regular GET or POST request
            response.setBody(fileContent);
            sendResponseToClient(response);

        } catch (Exception e) {
            // 500
            handleInternalServerError(response);
        } finally {
            try {
                System.out.println("[" + this.clientAddress + "]: Closing client connection ...\n");
                this.clientSessionSocket.close();
                this.clientInputStream.close();
                this.clientOutputStream.close();
            } catch (IOException e) {
                System.out.println("[" + this.clientAddress + "]: Failed to close client sockets streams");
            }
        }
    }

    private void handleBadRequestError(HTTPResponse response) throws IOException {
        response.setStatus(400);
        response.addHeader("Content-Type", "text/html");
        String body = "<html><body><h1>400 Bad Request</h1></body></html>";
        response.setBody(body.getBytes());
        sendResponseToClient(response);
    }

    private void handleNotImplementedError(HTTPResponse response) throws IOException {
        response.setStatus(501);
        response.addHeader("Content-Type", "text/html");
        String body = "<html><body><h1>501 Not Implemented</h1></body></html>";
        response.setBody(body.getBytes());
        sendResponseToClient(response);
    }

    private void handleNotFoundError(HTTPResponse response) throws IOException {
        response.setStatus(404);
        response.addHeader("Content-Type", "text/html");
        String body = "<html><body><h1>404 Not Found</h1></body></html>";
        response.setBody(body.getBytes());
        sendResponseToClient(response);
    }

    private void handleInternalServerError(HTTPResponse response) throws IOException {
        response.setStatus(500);
        response.addHeader("Content-Type", "text/html");
        String body = "<html><body><h1>500 Internal Server Error</h1></body></html>";
        response.setBody(body.getBytes());
        sendResponseToClient(response);
    }

    private void handleTraceRequest(HTTPResponse response, HTTPRequest request) throws IOException {
        response.addHeader("Content-Type", HTTPResponse.getContentTypeByFileName("trace").getValue());
        response.setBody(request.getRawRequest().getBytes());
        sendResponseToClient(response);
    }

    private boolean verifyRequestType(HTTPRequest request) {
        return (request.getType().equals("GET") || request.getType().equals("POST") ||
                request.getType().equals("HEAD") || request.getType().equals("TRACE"));
    }

    private byte[] readFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bFile = new byte[(int)file.length()];

            // Read until the end of the stream
            while (fis.available() != 0) {
                fis.read(bFile, 0, bFile.length);
            }
            return bFile;
        } catch (IOException e) {
            System.out.println("Failed to read requested file");
        }
        return new byte[1];
    }

    private static byte[] addParamsToFileContent (byte[] fileContent, Map<String, String> requestParams) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(fileContent);
        outputStream.write("</br>The parameters of your request: ".getBytes());
        outputStream.write(requestParams.toString().getBytes());

        fileContent = outputStream.toByteArray();
        return fileContent;
    }

    private void sendResponseToClient(HTTPResponse response) throws IOException {
        System.out.println("[" + this.clientAddress + "]: Response Headers: \n" + response.getHeaders());
        response.send(this.clientOutputStream);
    }
}
