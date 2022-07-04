docker:
	docker build -t dev.httpq/httpq-server:latest .

push_ecr: docker
	docker tag dev.httpq/httpq-server:latest 710053959384.dkr.ecr.us-west-2.amazonaws.com/dev.httpq/httpq-server:latest
	docker push 710053959384.dkr.ecr.us-west-2.amazonaws.com/dev.httpq/httpq-server:latest

test:
	./mvnw test
