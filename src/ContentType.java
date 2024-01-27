public enum ContentType {
    HTML("text/html"),
    IMAGE("image"),
    ICON("icon"),
    OCTET_STREAM("application/octet-stream");

    private String value;

    ContentType(String contentTypeValue) {
        this.value = contentTypeValue;
    }

    public String getValue() {
        return this.value;
    }
}
