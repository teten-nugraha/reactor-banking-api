
# Spring Boot Simple Banking App with Reactor

Project ini meliputi fitur

- Transfer
- Withdraw
- Deposit
- Cek Balance
- Payment



## Prerequisites

Berikut adalah beberapa software yang harus sudah disediakan  
di local komputer Anda.

- Open JDK Versi 17
- Gradle  (Optional karena bisa menggunakan gradle wrapper)


## Installation

### Running di local

masuk ke root direktori project ini

```bash  
 ./gradlew clean  // mendownload dependency
 ./gradlew build // build program
 ./gradlew bootRun // running aplikasi  
```  


## List Endpoints

Import file spring kotlin api blog.postman_collection.json ke aplikasi Postman kalian.

List Endpoint

## Access H2 Memory Db

Jika server sudah running maka anda juga bisa mengakes database nya menggunakan  
credentials dibawah ini :

- Host : http://localhost:8080/h2-console
- Driver Class : org.h2.Driver
- JDBC Url : jdbc:h2:mem:testdb
- Username : sa
- Password :

## Authors

- [@teten_nugraha](https://twitter.com/backendherodev)


## Tech Stack

**Server:** Java, Spring Framework, Spring Boot