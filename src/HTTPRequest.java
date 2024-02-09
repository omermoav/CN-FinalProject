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
    private StringBuilder rawRequest = new StringBuilder();
    private StringBuilder requestHeaders = new StringBuilder();
    private String requestLine;

    public HTTPRequest(BufferedReader inFromClient) throws IOException {
        this.parameters = new HashMap<>();
        readFullRequest(inFromClient);
        parseRequest();
    }

    private void readFullRequest(BufferedReader inFromClient) throws IOException {
        String line;
        int requestBodyLength = 0;
        while ((line = inFromClient.readLine()) != null && !line.isEmpty()) {
            rawRequest.append(line + "\n");

            if (line.startsWith("Content-Length:")) {
                requestBodyLength = Integer.parseInt(line.substring("Content-Length: ".length()));
            }
        }

        rawRequest.append("\n");

        // Read body content if exists
        if (requestBodyLength > 0) {
            char[] body = new char[requestBodyLength];
            inFromClient.read(body, 0, requestBodyLength);
            rawRequest.append(new String(body));
        }
    }

    private void parseRequest() throws IOException {
        String[] requestByLines = rawRequest.toString().split("\n");
        if (requestByLines.length < 1) {
            throw new IOException("Empty request");
        }

        // Parse the request line
        this.requestLine = requestByLines[0];
        if (this.requestLine == null || !this.requestLine.matches("^(GET|POST|PUT|DELETE|HEAD|OPTIONS|PATCH|TRACE) /.* HTTP/1\\.[01]$")) {
            throw new IOException("Invalid request: " + this.requestLine);
        }

        String[] requestLineParts = this.requestLine.split("\\s+");
        this.type = requestLineParts[0];
        parseURL(requestLineParts[1]);

        // Parse headers and possibly a body if it's a POST request
        for (int i = 1; i < requestByLines.length; i++) {
            String line = requestByLines[i];
            if (!line.trim().isEmpty()) {
                parseHeaderLine(line);
            } else {
                // Handle POST body here if contentLength > 0
                // Assuming the body directly follows an empty line after headers
                if ("POST".equalsIgnoreCase(type) && contentLength > 0 && i < requestByLines.length - 1) {
                    parseParameters(requestByLines[i + 1]);
                }
                break;
            }
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

    public String getHeaders() {return this.requestLine + "\r\n" + requestHeaders.toString();
    }

    public String getRawRequest() {
        return rawRequest.toString();
    }
}
