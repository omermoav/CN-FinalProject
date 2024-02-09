import java.io.*;
import java.util.*;

public class HTTPResponse {
    private int statusCode;
    private String statusMessage;
    private final Map<String, String> headers = new HashMap<>();
    private byte[] body;
    private String statusLine;

    public HTTPResponse() {
        // Default constructor
    }

    public void setStatus(int statusCode) {
        this.statusCode = statusCode;
        this.statusMessage = getStatusMessage(statusCode);
        this.statusLine = "HTTP/1.1 " + this.statusCode + " " + this.statusMessage;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public void setBody(byte[] body) {
        this.body = body;
        addHeader("Content-Length", String.valueOf(body.length));
    }

    public void send(DataOutputStream outToClient) throws IOException {
        outToClient.writeBytes(this.statusLine + "\r\n");
        outToClient.writeBytes(this.getHeaders() + "\r\n");
        if (body != null) {
            outToClient.write(body);
        }
        outToClient.flush(); // TODO: Check on this!
    }

    private String getStatusMessage(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 501 -> "Not Implemented";
            case 500 -> "Internal Server Error";
            default -> "Unknown Status";
        };
    }

    public String getHeaders() {
        StringBuilder responseHeaders = new StringBuilder(this.statusLine).append("\r\n");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            String headerLine = header.getKey() + ": " + header.getValue() + "\r\n";
            responseHeaders.append(headerLine);
        }
        return responseHeaders.toString();
    }

    public static ContentType getContentTypeByFileName(String fileName) {
        if (fileName.endsWith(".html")) {
            return ContentType.HTML;
        } else if (fileName.matches(".*\\.(jpg|png|gif|bmp)$")) {
            return ContentType.IMAGE;
        } else if (fileName.endsWith(".ico")) {
            return ContentType.ICON;
        } else {
            return ContentType.OCTET_STREAM;
        }
    }
}


