import java.io.*;
import java.util.*;

public class HTTPResponse {
    private final Map<String, String> headers = new HashMap<>();
    private byte[] body;
    private String statusLine;
    private boolean chunkedResponse;
    private static final String CRLF = "\r\n";
    private static final int CHUNK_SIZE = 500;
    private boolean isHeadResponse = false;

    public HTTPResponse() {}

    public void setChunkedResponse(boolean chunkedResponse) {
        this.chunkedResponse = chunkedResponse;
        if (chunkedResponse) {
            addHeader("Transfer-Encoding", "chunked");
        }
    }

    public void setHeadResponse(boolean isHeadResponse) {
        this.isHeadResponse = isHeadResponse;
    }

    public void setStatus(int statusCode) {
        String statusMessage = getStatusMessage(statusCode);
        this.statusLine = "HTTP/1.1 " + statusCode + " " + statusMessage;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public void setBody(byte[] body) {
        this.body = body;
        if (!this.chunkedResponse) {
            addHeader("Content-Length", String.valueOf(body.length));
        }
    }

    public void send(DataOutputStream outToClient) throws IOException {
        outToClient.writeBytes(this.getHeaders() + "\r\n");
        if (body != null && !isHeadResponse) {
            if (this.chunkedResponse) {
                // Handle chunked response
                int offset = 0;
                while (offset < body.length) {
                    int length = Math.min(CHUNK_SIZE, body.length - offset);
                    outToClient.writeBytes(Integer.toHexString(length) + CRLF);
                    outToClient.write(body, offset, length);
                    outToClient.writeBytes(CRLF);
                    offset += length;
                }
                outToClient.writeBytes("0" + CRLF + CRLF);
            } else {
                // Handle regular response
                outToClient.write(body);
            }
        }
        outToClient.flush();
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


