backend-grpc:
	echo "PWD: $(pwd)"
	docker run \
	   -v $$(pwd)/es.um.sisdist.backend.grpc.GrpcService:/var/maven \
	   -v ~/.m2:/var/maven/.m2 -t \
	   -w /var/maven \
	   --rm -u $$(id -u):$$(id -g) \
	   -e MAVEN_CONFIG=/var/maven/.m2 maven:3-eclipse-temurin-17 \
	   mvn -B -e -C -Duser.home=/var/maven -DskipTests clean install
	docker run \
	   -v $$(pwd)/es.um.sisdist.backend.grpc.GrpcServiceImpl:/var/maven \
	   -v ~/.m2:/var/maven/.m2 -t \
	   -w /var/maven \
	   --rm -u $$(id -u):$$(id -g) \
	   -e MAVEN_CONFIG=/var/maven/.m2 maven:3-eclipse-temurin-17 \
	   mvn -B -e -C -T 4 -Duser.home=/var/maven -DskipTests clean package

containers:
	(cd es.um.sisdist.backend.grpc.GrpcServiceImpl \
	      	&& docker build -t dsevilla/ssdd-backend-grpc:1.0 .)

clean:
	docker rmi -f dsevilla/ssdd-backend-grpc:1.0
