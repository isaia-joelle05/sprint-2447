package utils;

public class FilePart {
    private String fileName;
    private byte[] data;

    public FilePart(String fileName, byte[] data) {
        this.fileName = fileName;
        this.data = data;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getData() {
        return data;
    }
}

