![Static Badge](https://img.shields.io/badge/Java-17-blue)
![Static Badge](https://img.shields.io/badge/Netty-yellow)
![Static Badge](https://img.shields.io/badge/JavaFX-17-blue)
![Static Badge](https://img.shields.io/badge/Log4j-blue)
![Static Badge](https://img.shields.io/badge/Sqlite-blue)
![Static Badge](https://img.shields.io/badge/Maven-blue)

## CloudStorage
Облачное хранилище - учебный проект GeekBrains

Многомодульный проект, представляющий собой многопользовательскую упрощенную систему облачного хранения пользовательских файлов

Написан с использованием фреймворков `Netty` (сервер/клиент) и `JavaFX` (клиент)

https://github.com/igojig/CloudStorage/assets/103119162/a256941f-3163-4d80-8d49-953607e86131

### Состав проекта

  - `cloud-storage-client` - клиентская часть
  - `cloud-storage-server` - серверная часть
  - `cloud-storage-common` - совместно используемые библиотеки

  `client-repository` - директория для хранения пользовательских файлов клиентского приложения

  `server-repository` - директория для хранения пользовательских файлов серверного приложения

   `logs` - директория для лог-файлов

### Основные возможности
 - аутентификация пользователей
 - копирование файлов между клиентом и сервером
 - отображение прогресса при копировании
 - отображение списка файлов
 - удаление файлов
 - переименование файлов
 - ведение логов



### Особенности
 - реализовано полностью протокольное решение передачи файлов используя механизм `zero-copy`

### Установка
````
git clone https://github.com/igojig/CloudStorage
mvn clean install
````

### Запуск сервера
`
mvn exec:java -pl cloud-storage-server
`

### Запуск клиента
`
mvn javafx:run -pl cloud-storage-client
`

### Параметры для входа
| Login | Password | Username |
|:-----:|:--------:|:--------:|
|   1   |    1     |  Ivanov  |
|   2   |    2     |  Petrov  |
|   3   |    3     | Sidorov  |
|   3   |    4     | Smirnov  |


### Запуск из среды разработки

 - внести в параметры старта VM клиента и сервера:
      ```
        --add-opens java.base/jdk.internal.misc=io.netty.all
        --add-opens java.base/java.nio=io.netty.all
        -Dio.netty.tryReflectionSetAccessible=true
      ```
 - для запуска нескольких экземпляров клиента установить параметр

    `[Edit configuration]->[Modify options]->[Allow multiple instances]`
    
