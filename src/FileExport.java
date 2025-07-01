package utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class FileExport {

    private String fileName;
    private String fileType;
    private byte[] content;

    public FileExport(String fileName, String fileType, byte[] content) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.content = content;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public byte[] getContent() {
        return content;
    }

    public static FileExport exportToCSV(String fileName, List<?> data) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); PrintWriter writer = new PrintWriter(outputStream)) {

            if (!data.isEmpty()) {
                Object firstItem = data.get(0);
                Field[] fields = firstItem.getClass().getDeclaredFields();
                String[] headers = Arrays.stream(fields)
                        .map(Field::getName)
                        .toArray(String[]::new);
                writer.println(String.join(",", headers));
            }

            for (Object item : data) {
                Field[] fields = item.getClass().getDeclaredFields();
                String[] row = new String[fields.length];
                for (int i = 0; i < fields.length; i++) {
                    fields[i].setAccessible(true);
                    Object value = fields[i].get(item);
                    row[i] = (value != null) ? value.toString() : "";
                }
                writer.println(String.join(",", row));
            }

            writer.flush();
            return new FileExport(fileName, "text/csv", outputStream.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du CSV", e);
        }
    }

    public static FileExport exportToPDF(String fileName, List<?> data, PDFConfig config) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);

            document.open();

            if (config.getTitle() != null) {
                Font titleFont = new Font(FontFamily.HELVETICA,
                        config.getTitleSize(),
                        Font.BOLD,
                        parseColor(config.getTitleColor()));
                document.add(new Paragraph(config.getTitle(), titleFont));
                document.add(Chunk.NEWLINE);
            }

            int columns = data.isEmpty() ? 1 : data.get(0).getClass().getDeclaredFields().length;
            PdfPTable table = new PdfPTable(columns);
            table.setWidthPercentage(100);

            if (!data.isEmpty()) {
                for (Field field : data.get(0).getClass().getDeclaredFields()) {
                    PdfPCell cell = new PdfPCell(new Phrase(field.getName()));
                    cell.setBackgroundColor(parseColor(config.getHeaderColor()));
                    table.addCell(cell);
                }
            }

            for (Object item : data) {
                for (Field field : item.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    Object value = field.get(item);
                    table.addCell(new Phrase(value != null ? value.toString() : ""));
                }
            }

            document.add(table);
            document.close();

            return new FileExport(fileName, "application/pdf", outputStream.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }

    private static BaseColor parseColor(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new BaseColor(r, g, b);
    }
}
