import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {
    private final Socket clientSessionSocket;
    private final MultiThreadedWebServer server;
    private final BufferedReader clientInputStream;
    private final DataOutputStream clientOutputStream;

    public ClientHandler(Socket clientSessionSocket, MultiThreadedWebServer server) throws IOException {
        this.clientSessionSocket = clientSessionSocket;
        this.server = server;
        this.clientInputStream = new BufferedReader(new InputStreamReader(this.clientSessionSocket.getInputStream()));
        this.clientOutputStream = new DataOutputStream(this.clientSessionSocket.getOutputStream());
    }

    @Override
    public void run() {
        try {
            this.handleRequest();
        } catch (Exception e) {
            // Could not send response to client
            System.out.println("Could not send response to client");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void handleRequest() throws IOException {
        HTTPRequest request = null;
        HTTPResponse response = new HTTPResponse();

        try {
            // Parse the client request
            try {
                request = new HTTPRequest(this.clientInputStream);
                System.out.println("HTTP request Headers: \n" + request.getHeaders());
            } catch (IOException e) {
                // Handle 400 code request
                response.setStatus(400);
                response.addHeader("Content-Type", "text/html");
                String body = "<html><body><h1>400 Bad Request</h1></body></html>";
                response.setBody(body.getBytes());
                System.out.println("Response Headers: \n" + response.getHeaders());
                response.send(this.clientOutputStream);
                return;
            }

            response.setChunkedResponse(request.isChunkedResponse());
            boolean isValidType = verifyRequestType(request);
            if (!isValidType) {
                // Handle 501 code request
                response.setStatus(501);
                response.addHeader("Content-Type", "text/html");
                String body = "<html><body><h1>501 Not Implemented</h1></body></html>";
                response.setBody(body.getBytes());
                System.out.println("HTTP Response Headers: \n" + response.getHeaders());
                response.send(this.clientOutputStream);
                return;
            }

            // TODO: change back to using ~ (?)
            String requestedFilePath = System.getProperty("user.home") + this.server.getRootDirectory() + (request.getRequestedPage().equals("/") ? this.server.getDefaultPage() : request.getRequestedPage());
            //String requestedFilePath = '~' + this.server.getRootDirectory() + (request.getRequestedPage().equals("/") ? this.server.getDefaultPage() : request.getRequestedPage());
            File file = new File(requestedFilePath);

            if (!file.exists() || file.isDirectory()) {
                // Handle 404 code request
                response.setStatus(404);
                response.addHeader("Content-Type", "text/html");
                String body = "<html><body><h1>404 Page Not Found</h1></body></html>";
                response.setBody(body.getBytes());
                System.out.println("Response Headers: \n" + response.getHeaders());
                response.send(this.clientOutputStream);
                return;
            }

            // Read the file content
            byte[] fileContent = readFile(file);
            // Handle 200 code request
            response.setStatus(200);
            response.addHeader("Content-Type", HTTPResponse.getContentTypeByFileName(requestedFilePath).getValue());

            // Handle HEAD request
            if (request.getType().equals("HEAD")) {
                response.addHeader("Content-Length", String.valueOf(fileContent.length));
                System.out.println("Response Headers: \n" + response.getHeaders());
                response.send(this.clientOutputStream);
                return;
            }

            // Handle TRACE request
            if (request.getType().equals("TRACE")) {
                // TODO: check response content-type for trace
                response.addHeader("Content-Type", HTTPResponse.getContentTypeByFileName("trace").getValue());
                response.setBody(request.getRawRequest().getBytes());
                System.out.println("Response Headers: \n" + response.getHeaders());
                response.send(this.clientOutputStream);
                return;
            }

            // Handle params_info file
            if (request.getRequestedPage().equals("/params_info.html") && request.getType().equals("POST")) {
                fileContent = addParamsToFileContent(fileContent, request.getParameters());
            }

            // Regular GET or POST request
            response.setBody(fileContent);
            System.out.println("Response Headers: \n" + response.getHeaders());
            response.send(this.clientOutputStream);

        } catch (Exception e) {
            // Handle 500 code request
            response.setStatus(500);
            response.addHeader("Content-Type", "text/html");
            String body = "<html><body><h1>500 Internal Server Error</h1></body></html>";
            response.setBody(body.getBytes());
            System.out.println("Response Headers: \n" + response.getHeaders());
            response.send(this.clientOutputStream);
        } finally {
            try {
                this.clientSessionSocket.close();
                this.clientInputStream.close();
                this.clientOutputStream.close();
            } catch (IOException e) {
                System.out.println("Failed to close client resources");
                e.printStackTrace();
            }
        }
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
            e.printStackTrace();
        }
        return new byte[1];
    }

    private static byte[] addParamsToFileContent (byte[] fileContent, Map<String, String> requestParams) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(fileContent);

        // TODO: handle empty values of params
        if (requestParams.values().stream().anyMatch(s -> !s.isEmpty())) {
            outputStream.write("\nThe parameters of your request: ".getBytes());
            String paramsAsString = requestParams.toString();
            outputStream.write(paramsAsString.getBytes());
        } else {
            outputStream.write("\nYou did not insert any params values".getBytes());
        }

        fileContent = outputStream.toByteArray();
        return fileContent;
    }
}
