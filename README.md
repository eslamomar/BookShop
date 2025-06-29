# BookShop

Setup the Database
# 1. Open MySQL shell
mysql -u root -p

-- 2. Show existing databases
SHOW DATABASES;

-- 3. Create a new database named 'bookstore'
CREATE DATABASE bookstore;

-- 4. Select the new database
USE bookstore;

-- 5. (Optional) View existing tables (will be empty at this point)
SHOW TABLES;


in /src/main/resources/application.properties

update database username and password

spring.datasource.url = jdbc:mysql://localhost:3306/bookstore
spring.datasource.username = root
spring.datasource.password = rootroot

## Hibernate Properties
# Hibernate ddl auto (create, create-drop, validate, update)
spring.jpa.hibernate.ddl-auto = update