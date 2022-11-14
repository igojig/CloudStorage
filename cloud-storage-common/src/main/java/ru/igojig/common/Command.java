package ru.igojig.common;

public enum Command {
    RENAME((byte)1), // [oldFileName&newFileName]
    DELETE((byte)2), // [fileName]
    GET_FILE((byte)3),   // [fileName]
    GET_FILE_LIST((byte)4) ;

    private final byte command;

    Command(byte command) {
        this.command = command;
    }

    public byte getCommand() {
        return command;
    }
}
