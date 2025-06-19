.PHONY: build run clean

build:
    ./mvnw clean package

run:
    ./mvnw spring-boot:run

clean:
    ./mvnw clean

docker-build:
    docker build -t backend-rest-externo .

docker-run:
    docker run -p 8180:8180 --rm backend-rest-externo