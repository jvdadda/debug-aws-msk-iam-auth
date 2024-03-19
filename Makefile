build/native:
	./gradlew build -Dquarkus.package.type=native -Dquarkus.native.container-build=true
	docker build -f src/main/docker/Dockerfile.native-micro -t quarkus/debug-aws-msk-iam-auth .

build/jvm:
	./gradlew build
	docker build -f src/main/docker/Dockerfile.jvm -t quarkus/debug-aws-msk-iam-auth .
start:
	docker run -e AWS_ACCESS_KEY_ID=TO_REPLACE -e AWS_SECRET_ACCESS_KEY=TO_REPLACE -e AWS_REGION=TO_REPLACE -i --rm -p 8080:8080 quarkus/debug-aws-msk-iam-auth

.PHONY: build/native build/jvm start
