package ru.igojig.common;

public enum HandlerState {
    IDLE,

    // файл
    FILE_NAME_LENGTH,
    FILE_NAME,
    FILE_LENGTH,
    FILE,

    // список файлов
    FILE_LIST_LENGTH,
    FILE_LIST,

    // команда
    COMMAND,


    COMMAND_RENAME_LENGTH,
    COMMAND_RENAME


}
