# Memrise parser

Parser for web site https://www.memrise.com

#### Usage

1. Write in application.properties your memrise session id from cookie "sessionid_2" (auth from google), property - "memrise.session.id=_your_sessionid_".
Write via a semicolon your memrise lessons urls in property "memrise.lesson.urls=".
Write via a semicolon count parts in memrise lesson course in property "memrise.lesson.parts=".
Write via a semicolon file name for memrise lesson course in property "memrise.lesson.titles=".
1. ```mvn clean package```
1. ```mvn spring-boot:run```
