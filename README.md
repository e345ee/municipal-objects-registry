# Municipal Objects Registry

![React](https://img.shields.io/badge/React-18.3.1-61DAFB?logo=react&logoColor=111111)
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=ffffff)
![Spring](https://img.shields.io/badge/Spring-6.1.6-6DB33F?logo=spring&logoColor=ffffff)
![Maven](https://img.shields.io/badge/Maven-WAR-C71A36?logo=apachemaven&logoColor=ffffff)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-42.7.3-4169E1?logo=postgresql&logoColor=ffffff)
![WildFly](https://img.shields.io/badge/WildFly-deploy-1F5EAA?logo=wildfly&logoColor=ffffff)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-CI/CD-2088FF?logo=githubactions&logoColor=ffffff)

Municipal Objects Registry — небольшой веб-сервис для спокойной работы с реестром городских объектов: можно просматривать, создавать и редактировать записи, работать со связанными сущностями, импортировать данные и видеть изменения почти сразу через realtime-обновления.

Архитектура простая и разделенная: фронтенд лежит в `front` и собран на React с React Router, React Query, React Hook Form, Zod, Axios и STOMP/WebSocket-клиентом, а бэкенд живет в `backend` как Java 17 WAR-приложение на Spring Web MVC, Spring Data JPA и Hibernate; данные хранятся в PostgreSQL, файлы импорта складываются через MinIO, кеширование поддерживается Ehcache, а внешний API разложен по контроллерам, сервисам, репозиториям, DTO и доменным моделям. Деплой идет через GitHub Actions при пуше в `main` или ручном запуске: backend workflow собирает WAR через Maven, применяет `backend/db/setup.sql` на удаленной PostgreSQL, копирует артефакт на сервер Helios по SCP и перезапускает WildFly скриптом `scripts/restart-wildfly.sh`; frontend workflow ставит npm-зависимости, собирает статический React-бандл, упаковывает его в архив, отправляет на тот же сервер и перезапускает httpd через `scripts/restart-httpd.sh`.
