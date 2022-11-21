package ru.igojig.common;

// что к нам может прилететь в заголовке
public enum Header {
    FILE((byte) 1), // файл
    FILE_LIST((byte) 2), // список файлов(содержимое директории)
    COMMAND((byte) 3), // команда

    AUTH_REQUEST((byte) 5), // [login&password]

    AUTH_OK((byte) 6), //[username]
    AUTH_ERR((byte) 7);

    private final byte header;

    public byte getHeader() {
        return header;
    }

    Header(byte header) {
        this.header = header;
    }

}
