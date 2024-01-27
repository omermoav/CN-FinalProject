import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HTTPRequest {
    private String type;
    private String requestedPage;
    private boolean isImage;
    private int contentLength;
    private String referer;
    private String userAgent;
    private String transferEncoding;
    private boolean chunkedResponse;
    private Map<String, String> parameters;
    private StringBuilder requestHeaders = new StringBuilder();


    public HTTPRequest(BufferedReader inFromClient) throws IOException {
        this.parameters = new HashMap<>();
        parseRequest(inFromClient);
    }

    private void parseRequest(BufferedReader inFromClient) throws IOException {
        String inputLine = inFromClient.readLine();
        if (inputLine == null) {
            throw new IOException("Input from client is null");
        }

        String[] requestLineParts = inputLine.split("\\s+");
        if (requestLineParts.length < 2) {
            throw new IOException("Invalid request line: " + inputLine);
        }

        this.type = requestLineParts[0].toUpperCase(); // we get the HTTP method of the client
        String url = requestLineParts[1].toLowerCase();

        // Parse the requested URL and extract parameters if present
        parseURL(url);

        // Continue reading and parsing the headers
        while ((inputLine = inFromClient.readLine()) != null && !inputLine.equals("\r\n")) {
            parseHeaderLine(inputLine);
        }

        // If it's a POST request, parse the body to get parameters
        if ("POST".equalsIgnoreCase(this.type)) {
            char[] body = new char[this.contentLength];
            if (inFromClient.read(body, 0, this.contentLength) != this.contentLength) {
                throw new IOException("[POST ERROR] :: Couldn't read all request content body!!!");
            };
            String requestBody = new String(body);
            parseParameters(requestBody);
        }
    }

    private void parseURL(String url) {
        String[] urlParts = url.split("\\?");
        this.requestedPage = urlParts[0];

        // Check if the requested resource is an image
        this.isImage = this.requestedPage.matches(".*\\.(jpg|bmp|gif|png)$");

        // If there are parameters in the URL, parse them
        if (urlParts.length > 1) {
            parseParameters(urlParts[1]);
        }
    }

    private void parseHeaderLine(String line) {
        requestHeaders.append(line).append("\n");
        String[] header = line.split(": ", 2);
        if (header.length > 1) {
            String headerName = header[0].toLowerCase();
            String headerValue = header[1];
            switch (headerName) {
                case "content-length" -> this.contentLength = Integer.parseInt(headerValue);
                case "transfer-encoding" -> this.transferEncoding = headerValue;
                case "chunked" -> this.chunkedResponse = "yes".equalsIgnoreCase(headerValue);
                case "referer" -> this.referer = headerValue;
                case "user-agent" -> this.userAgent = headerValue;
            }
        }
    }

    private void parseParameters(String parametersLine) {
        String[] pairs = parametersLine.split("&");
        for (String pair : pairs) {
            String[] param = pair.split("=", 2);
            if (param.length > 1) {
                this.parameters.put(param[0], param[1]);
            }
        }
    }

    public boolean shouldUseChunkedEncoding() {
        // Use chunked encoding if the header is set to yes, and the transfer-encoding allows it
        return "chunked".equalsIgnoreCase(this.transferEncoding) && this.chunkedResponse;
    }

    // Getters
    public String getType() {
        return type;
    }

    public String getRequestedPage() {
        return requestedPage;
    }

    public boolean isImage() {
        return isImage;
    }

    public int getContentLength() {
        return contentLength;
    }

    public String getReferer() {
        return referer;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getRequestHeaders() {
        return requestHeaders.toString();
    }
}