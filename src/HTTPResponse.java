import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class HTTPResponse {
    private final HTTPRequest request;
    private String status;
    private int statusCode;
    private int contentLength;
    private ContentType requestedContentType;
    private boolean chunked;
    private boolean isErrorResponse;

    public HTTPResponse(HTTPRequest request){
        this.request = request;
        this.chunked = request.shouldUseChunkedEncoding();
        this.requestedContentType = getContentTypeByFileName(request.getRequestedPage());
    }

    private String buildResponseHeader() {
        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder
                .append("HTTP/1.1 ").append(this.status).append(" \r\n")
                .append("Content-Type: ").append(this.isErrorResponse ? ContentType.HTML.getValue() : this.requestedContentType).append(" \r\n");

        if (this.chunked) {
            headerBuilder.append("Transfer-Encoding: chunked\r\n");
        } else {
            headerBuilder.append("Content-Length: ").append(this.contentLength).append("\r\n");
        }

        headerBuilder.append("\r\n"); // End of headers

        return headerBuilder.toString();
    }

    private void setRequestStatus() {
        if (!request.getType().equals("GET") && !request.getType().equals("POST") &&
                !request.getType().equals("HEAD") && !request.getType().equals("TRACE")) {
            this.statusCode = 501;
            this.status = String.format("%d Not Implemented", statusCode);
            this.isErrorResponse = true;
            return;
        }

        try {
            // TODO: Change root directory (not hard-coded)
            String requestedFile = "~/www/lab/html/" + (request.getRequestedPage().equals("/") ? "index.html" : request.getRequestedPage());
            File file = new File(requestedFile);

            if (!file.exists() || file.isDirectory()) {
                this.statusCode = 404;
                this.status = String.format("%d Not Found", statusCode);
                this.isErrorResponse = true;
                return;
            }

            // Read the file content
            byte[] fileContent = readFile(file);
            this.contentLength = fileContent.length;

            this.statusCode = 200;
            this.status = String.format("%d OK", statusCode);
            this.isErrorResponse = false;

        } catch (Exception e) {
            this.statusCode = 500;
            this.status = String.format("%d Internal Server Error", statusCode);
            this.isErrorResponse = true;
        }


    }

    //TODO: move ro different class
    private byte[] readFile(File file) {
        try
        {
            FileInputStream fis = new FileInputStream(file);
            byte[] bFile = new byte[(int)file.length()];

            // read until the end of the stream.
            while(fis.available() != 0)
            {
                fis.read(bFile, 0, bFile.length);
            }
            return bFile;
        }
        catch(FileNotFoundException e)
        {
            // TODO: something
        }
        catch(IOException e)
        {
            // TODO: something
        }
        return new byte[1];
    }


    private static ContentType getContentTypeByFileName(String fileName) {
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


