package ru.igojig.common;

// что к нам может прилететь в заголовке
public enum Header {
        FILE((byte)1), // файл
        FILE_LIST((byte)2), // список файлов(содержимое директории)
        COMMAND((byte)3); // команда

        private final byte header;

        public byte getHeader() {
            return header;
        }

        Header(byte header) {
            this.header = header;
        }

    }
