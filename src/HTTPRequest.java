import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

public class HTTPRequest {
    private String type;
    private String requestedPage;
    private int contentLength;
    private boolean chunkedResponse;
    private final Map<String, String> parameters;
    private final StringBuilder rawFullRequest;
    private final StringBuilder rawHeaders;
    private String requestLine;
    private static final String CRLF = "\r\n";

    public HTTPRequest() {
        this.parameters = new HashMap<>();
        this.rawFullRequest = new StringBuilder();
        this.rawHeaders = new StringBuilder();
    }

    public void readFullRequest(BufferedReader inFromClient) throws IOException, BadRequestException {
        String line;
        int requestBodyLength = 0;
        while ((line = inFromClient.readLine()) != null && !line.isEmpty()) {
            this.rawFullRequest.append(line).append(CRLF);
            this.rawHeaders.append(line).append(CRLF);

            if (line.startsWith("Content-Length:")) {
                try {
                    requestBodyLength = Integer.parseInt(line.substring("Content-Length: ".length()));
                    if (requestBodyLength < 0) {
                        throw new BadRequestException("Content-Length must be non-negative");
                    }
                }  catch (NumberFormatException e) {
                    throw new BadRequestException ("Content-Length header value isn't a parsable Integer");
                }
            }
        }

        if (line == null) {
            throw new BadRequestException("Request is missing final CRLF");
        }

        // End of headers
        this.rawFullRequest.append(CRLF);

        // Read body content if exists
        if (requestBodyLength > 0) {
            char[] body = new char[requestBodyLength];
            try {
                inFromClient.read(body, 0, requestBodyLength);
            } catch (SocketTimeoutException e) {
                throw new BadRequestException("Make sure Content-Length header value isn't greater the actual content length and try again.");
            }
            this.rawFullRequest.append(new String(body));
        }
    }

    public void parseRequest() throws BadRequestException {
        String[] requestByLines = this.rawFullRequest.toString().split(CRLF);
        if (requestByLines.length < 1) {
            throw new BadRequestException("Empty request");
        }

        // Parse the request line
        this.requestLine = requestByLines[0];
        if (this.requestLine == null || !this.requestLine.matches("^.+ /.* HTTP/1\\.[01]$")) {
            throw new BadRequestException("Invalid request line: " + this.requestLine);
        }

        String[] requestLineParts = this.requestLine.split("\\s+");
        this.type = requestLineParts[0];
        parseURL(requestLineParts[1]);

        // Parse headers and possibly a body if it's a POST request
        for (int i = 1; i < requestByLines.length; i++) {
            String header = requestByLines[i];
            if (!header.trim().isEmpty()) {
                if (!header.matches("^[a-zA-Z0-9\\-]+: .+$")) {
                    throw new BadRequestException("Invalid header line: " + header);
                } else {
                    parseHeaderLine(header);
                }
            } else {
                if (this.contentLength > 0 && ("GET".equalsIgnoreCase(this.type) || "HEAD".equalsIgnoreCase(this.type))) {
                    throw new BadRequestException("Request of type " + this.type + " cannot have a body");
                }
                // Handle POST body, assuming the body directly follows an empty line after headers
                if ("POST".equalsIgnoreCase(type) && this.contentLength > 0 && i < requestByLines.length - 1) {
                    parseParameters(requestByLines[i + 1]);
                }
                break;
            }
        }
    }

    private void parseURL(String url){
        String[] urlParts = url.split("\\?", 2);

        // Ignore any ../ in the URL - don't allow directory traversal outside the root directory
        this.requestedPage = urlParts[0].replaceAll("\\.\\./", "");

        // If there are parameters in the URL, parse them
        if (urlParts.length > 1) {
            parseParameters(urlParts[1]);
        }
    }

    private void parseHeaderLine(String line) throws BadRequestException {
        String[] header = line.split(": ", 2);
        if (header.length == 2) {
            String headerName = header[0].toLowerCase();
            String headerValue = header[1];
            switch (headerName) {
                case "content-length" -> this.contentLength = Integer.parseInt(headerValue);
                case "chunked" -> this.chunkedResponse = "yes".equalsIgnoreCase(headerValue);
            }
        } else {
            throw new BadRequestException("Invalid header: " + line);
        }
    }

    private void parseParameters(String parametersLine) {
        String[] pairs = parametersLine.split("&");
        for (String pair : pairs) {
            String[] param = pair.split("=", 2);
            if (param.length == 2) {
                this.parameters.put(param[0], param[1]);
            } else if (param.length == 1){
                this.parameters.put(param[0],"");
            }
        }
    }

    public String getType() {
        return this.type;
    }

    public String getRequestedPage() {
        return this.requestedPage;
    }

    public Map<String, String> getParameters() {
        return this.parameters;
    }

    public String getRawRequest() {
        return this.rawFullRequest.toString();
    }

    public String getRawHeaders() {
        return this.rawHeaders.toString();
    }

    public boolean isChunkedResponse() {
        return this.chunkedResponse;
    }
}
