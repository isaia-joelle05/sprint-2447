package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class MultipartParser {
    private static final int BUFFER_SIZE = 40000;

    public static Map<String, FilePart> parseMultipartRequest(HttpServletRequest request) throws IOException {
        Map<String, FilePart> fileDataMap = new HashMap<>();

        String contentType = request.getContentType();
        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            throw new IOException("Invalid content type, expected multipart/form-data");
        }

        String boundary = contentType.split("boundary=")[1];
        InputStream inputStream = request.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains(boundary)) {
                parsePart(reader, fileDataMap);
            }
        }
        return fileDataMap;
    }

    private static void parsePart(BufferedReader reader, Map<String, FilePart> fileDataMap) throws IOException {
        String contentDisposition = reader.readLine();
        String contentType = reader.readLine();
        
        reader.readLine();
        
        String fieldName = getFieldName(contentDisposition);
        String fileName = getFileName(contentDisposition);
        byte[] data = readPartData(reader);

        fileDataMap.put(fieldName, new FilePart(fileName, data));
    }

    private static String getFieldName(String contentDisposition) {
        for (String part : contentDisposition.split(";")) {
            if (part.trim().startsWith("name=")) {
                return part.split("=")[1].replace("\"", "").trim();
            }
        }
        return null;
    }

    private static String getFileName(String contentDisposition) {
        for (String part : contentDisposition.split(";")) {
            if (part.trim().startsWith("filename=")) {
                return part.split("=")[1].replace("\"", "").trim();
            }
        }
        return null;
    }

    private static byte[] readPartData(BufferedReader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null && !line.startsWith("--")) {
            builder.append(line).append("\n");
        }
        return builder.toString().getBytes();
    }
}
