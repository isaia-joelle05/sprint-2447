package utils;

public class PDFConfig {
    private String title;
    private int titleSize;
    private String titleColor;
    private String headerColor; 

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public int getTitleSize() {
        return titleSize;
    }
    public void setTitleSize(int titleSize) {
        this.titleSize = titleSize;
    }

    public String getTitleColor() {
        return titleColor;
    }
    public void setTitleColor(String titleColor) {
        this.titleColor = titleColor;
    }

    public String getHeaderColor() {
        return headerColor;
    }
    public void setHeaderColor(String headerColor) {
        this.headerColor = headerColor;
    }
}
