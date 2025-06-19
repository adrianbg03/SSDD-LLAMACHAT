.PHONY: all build run stop clean logs testclient

# Compilar módulos Java desde subdirectorios
build:
	cd backend/es.um.sisdist.backend.dao && mvn clean install -DskipTests
	cd backend-rest/es.um.sisdist.backend.Service && mvn clean install -DskipTests
	cd backend-grpc/es.um.sisdist.backend.grpc.GrpcServiceImpl && mvn clean install -DskipTests
	cd backend-rest-externo && mvn clean package -DskipTests
	cd TestClient && mvn clean install -DskipTests

# Levantar todos los servicios usando el archivo por defecto
run:
	docker compose -f docker-compose-devel.yml up --build

# Detener todos los contenedores
stop:
	docker compose -f docker-compose-devel.yml down

# Mostrar logs
logs:
	docker compose -f docker-compose-devel.yml logs -f

# Limpiar los targets de Maven
clean:
	cd backend/es.um.sisdist.backend.dao && mvn clean
	cd backend-rest/es.um.sisdist.backend.Service && mvn clean
	cd backend-grpc/es.um.sisdist.backend.grpc.GrpcServiceImpl && mvn clean
	cd backend-rest-externo && mvn clean
	cd TestClient && mvn clean

# Ejecutar TestClient
testclient:
	cd TestClient && mvn exec:java -Dexec.mainClass="testclient.TestClient"
