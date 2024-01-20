![Static Badge](https://img.shields.io/badge/Java-17-blue)
![Static Badge](https://img.shields.io/badge/Netty-yellow)
![Static Badge](https://img.shields.io/badge/JavaFX-17-blue)
![Static Badge](https://img.shields.io/badge/Log4j-blue)
![Static Badge](https://img.shields.io/badge/Sqlite-blue)
![Static Badge](https://img.shields.io/badge/Maven-blue)

## CloudStorage
Облачное хранилище - учебный проект GeekBrains

Многомодульный проект, представляющий собой упрощенную систему облачного хранения пользовательских файлов

Написан с использованием фреймворков `Netty` (сервер/клиент) и `JavaFX` (клиент)

https://github.com/igojig/CloudStorage/github-assets/demo.mp4



https://github.com/igojig/CloudStorage/assets/103119162/b100d97c-32bd-4d96-a7e4-3a56ed7fa68f


### Состав проекта

  - `cloud-storage-client` - клиентская часть
  - `cloud-storage-server` - серверная часть
  - `cloud-storage-common` - совместно используемые библиотеки
### Основные возможности
 - аутентификация пользователей
 - копирование файлов между клиентом и сервером
 - отображение прогресса при копировании
 - отображение списка файлов
 - удаление файлов
 - переименование файлов
 - ведение логов



### Реализованные задачи
 - разработал протокол передачи файлов с использованием фреймворка `Netty`
 - разработал графический интерфейс клиента
 - 


- **Login/password:** 1/1, 2/2, 3/3, 4/4

- для корректного запуска: внести в параметры старта VM клиента и сервера:
  ```
    --add-opens java.base/jdk.internal.misc=io.netty.all
    --add-opens java.base/java.nio=io.netty.all
    -Dio.netty.tryReflectionSetAccessible=true
  ```
    
