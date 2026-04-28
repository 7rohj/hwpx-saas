package com.khwajeong.hwpx_saas.imagefix.dto;

public class ImageFixResponse {

    private String backgroundImageDataUrl;
    private TextLayer textLayer;

    public ImageFixResponse() {
    }

    public ImageFixResponse(String backgroundImageDataUrl, TextLayer textLayer) {
        this.backgroundImageDataUrl = backgroundImageDataUrl;
        this.textLayer = textLayer;
    }

    public String getBackgroundImageDataUrl() {
        return backgroundImageDataUrl;
    }

    public void setBackgroundImageDataUrl(String backgroundImageDataUrl) {
        this.backgroundImageDataUrl = backgroundImageDataUrl;
    }

    public TextLayer getTextLayer() {
        return textLayer;
    }

    public void setTextLayer(TextLayer textLayer) {
        this.textLayer = textLayer;
    }

    public static class TextLayer {
        private String text;
        private int x;
        private int y;
        private int fontSize;
        private String color;
        private String fontFamily;

        public TextLayer() {
        }

        public TextLayer(String text, int x, int y, int fontSize, String color, String fontFamily) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.fontSize = fontSize;
            this.color = color;
            this.fontFamily = fontFamily;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getFontSize() {
            return fontSize;
        }

        public void setFontSize(int fontSize) {
            this.fontSize = fontSize;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public String getFontFamily() {
            return fontFamily;
        }

        public void setFontFamily(String fontFamily) {
            this.fontFamily = fontFamily;
        }
    }
}