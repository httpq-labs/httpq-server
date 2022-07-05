# HTTPQ

HTTPQ is a multitenant, zero-ops, horizontally scalable webhooks sending server.

It's open-source under a GPLv3 license and production grade.

Its [OpenAPI specification](https://httpq-labs.github.io/httpq-api-spec/) makes it easy to use and administer from any programming language


## Quick Links

- [Community](#community)
- [Features](#features)
- [Dependencies](#dependencies)
- [Benchmark](#benchmark)
- [Install](#install)
- [Speedrun: Sending your first webhook](#speedrun)
- [Managing Webhook schema evolution](#webhook-schema-evolution)
- [Authentication and Authorization](#security-keys)
- [OpenAPI Specification](#openapi-specification)
- [Generating API Clients](#api-clients)
- [FAQ](#faq)
- [Support](#support)
- [Contributing](#contributing)
- [Build from Source](#build-from-source)

### Community
- Gitter
- Google Group

### Features
- Multitenancy
- Server Authentication and Authorization
- OpenAPI
- Async I/O
- Payload authentication and verification
- Webhook Schema Versioning
- Topic based event routing


### Dependencies

### Benchmark

### Install

### Speedrun

### Webhook Schema Evolution

### Security Keys

### OpenAPI specification

### API Clients

### FAQ

### Support

### Contributing

### Build from source
#### Build with Docker
```bash
$ docker build -t dev.httpq/httpq-server:latest .
```


#### Build on your machine
HTTPQ requires the following dependencies:

- Java 17+ with Maven 3+
- PostgreSQL 14+

Clone this repository and run the following commands:
```bash
$ ./mvnw clean package
$ java -jar target/httpq-server-far.jar
```

© 2022-present Edouard Swiac
