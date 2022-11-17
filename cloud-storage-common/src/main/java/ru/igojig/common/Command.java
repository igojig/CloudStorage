package ru.igojig.common;

// команда, идет после Header.COMMAND
public enum Command {
    RENAME((byte)1), // [length oldFileName&newFileName]
    DELETE((byte)2), // [lenght fileName]
    GET_FILE((byte)3),   // [length fileName]
    GET_FILE_LIST((byte)4) ; // содержимое директории [length file1&file2&...]

    private final byte command;

    Command(byte command) {
        this.command = command;
    }

    public byte getCommand() {
        return command;
    }
}
