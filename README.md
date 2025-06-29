
# 📚 OnlineBookShop

A simple Java Spring Boot application for managing a bookstore.

---

## 🚀 Getting Started

Follow these steps to get the project up and running on your local machine.

---

## 📥 Clone the Repository

Clone the project from GitHub:

git clone https://github.com/eslamomar/BookShop.git
cd BookShop

---

## 🛠️ Set Up the Database (MySQL)

1. Open your MySQL shell:

mysql -u root -p

2. Create the database:

SHOW DATABASES;
CREATE DATABASE `bookstore`;
USE bookstore;
SHOW TABLES;

> 🔸 'SHOW TABLES;' is optional — the database will be empty initially.

---

## ⚙️ Configure Application Properties

Edit the following file:

src/main/resources/application.properties

Update the database configuration with your MySQL username and password:

spring.datasource.url = jdbc:mysql://localhost:3306/bookstore

spring.datasource.username = root

spring.datasource.password = rootroot

# Hibernate ddl auto options: create, create-drop, validate, update
spring.jpa.hibernate.ddl-auto = create

---

## ▶️ Run the Application

You can run the project using your IDE (e.g. IntelliJ, Eclipse), or via the terminal with Maven:

./mvnw spring-boot:run

> 💡 If you're on Windows, use 'mvnw.cmd spring-boot:run'

---

## 🌐 Access the App

Once the application is running, open your browser and navigate to:

http://localhost:8080/

---

## ✅ Requirements

Before running this project, ensure you have the following installed:

- Java 17 or higher (I use 21)
- Maven
- MySQL Server
