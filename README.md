<p align="left">
<img src="./static/logo.png"/>
<img src="https://www.gnu.org/graphics/gplv3-with-text-136x68.png"/>
</p> 

[![Gitter](https://badges.gitter.im/httpq-labs/community.svg)](https://gitter.im/httpq-labs/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge) 


**HTTPQ** is a multitenant, API driven, zero-ops, horizontally scalable webhooks sending server.

It's free and open-source software under a [GPLv3 license](https://www.gnu.org/licenses/gpl-3.0.en.html).

Its [OpenAPI specification](https://httpq-labs.github.io/httpq-api-spec/) makes it easy to use and administer from the programming language of your choice.

## Webhooks
[Wikipedia](https://en.wikipedia.org/wiki/Webhook) says:
> Webhooks are "user-defined HTTP callbacks". They are usually triggered by some event, such as pushing code to a repository or a comment being posted to a blog. When that event occurs, the source site makes an HTTP request to the URL configured for the webhook. Users can configure them to cause events on one site to invoke behavior on another.
>
> Common uses are to trigger builds with continuous integration systems or to notify bug tracking systems. Because webhooks use HTTP, they can be integrated into web services without adding new infrastructure.

**HTTPQ** is the server that sits between your application and webhooks consumers. Your application triggers webhooks sending by making non-blocking API calls to **HTTPQ**. **HTTPQ** does the heavy lifting for you, such as queueing and making the HTTP requests, retrying errors multiple times, so your application won't have to. 

## Why HTTPQ
Sending webhooks is not hard per se, but doing it yourself commits you to maintaining a critical piece of infrastructure that's unrelated to your main project, which can be distracting and even painful if you quickly need to scale up sending.

Developers who roll their own usually quickly reach a ceiling in scaling sending efficiently (compute resources, database, queueing, network I/O, monitoring...). 

That's why we created **HTTPQ**. It's aimed to be an off-the-shelf solution that just works, is simple, easy to self-host, and scale nicely along your volume, so you can keep your users happy with snappy webhooks delivery.

## Quick Links

- [Community](#community)
- [Features](#features)
- [Dependencies](#dependencies)
- [Benchmark](#benchmark)
- [Documentation](#documentation)
- [OpenAPI Specification](#openapi-specification)
- [Contributing](#contributing)
- [Build from Source](#build-from-source)

### Community
- [![Gitter](https://badges.gitter.im/httpq-labs/community.svg)](https://gitter.im/httpq-labs/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
- [Google Group](https://groups.google.com/u/3/g/httpq)

### Features
- Multitenancy
- Authentication and Authorization
- OpenAPI
- Async I/O
- Payload authentication and verification
- Webhook Schema Versioning
- Topic based routing


### Dependencies
- PostgreSQL 14+
- Java 17

### Benchmark
- In progress 

### Documentation
<https://httpq-labs.github.io/httpq-docs/>

### OpenAPI specification
<https://httpq-labs.github.io/httpq-api-spec/>

### Contributing
Please submit a PR with appropriate testing.

### Build from source
#### Build with Docker
```bash
$ docker build -t org.httpq/httpq-server:latest .
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
