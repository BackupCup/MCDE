package net.backupcup.mcde.screen.util;

public record TexturePos(int x, int y) {
    public static TexturePos of(int x, int y) {
        return new TexturePos(x, y);
    }

    public TexturePos add(TexturePos other) {
        return new TexturePos(x() + other.x(), y() + other.y());
    }

    public TexturePos add(int x, int y) {
        return new TexturePos(x() + x, y() + y);
    }
}
