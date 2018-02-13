FROM maven:latest

MAINTAINER David Chaves <dachafra@gmail.com>

WORKDIR /barcelona-tram-2-gtfsrt

COPY . .

RUN mvn clean package

EXPOSE 8080

ENTRYPOINT ["/barcelona-tram-2-gtfsrt/run.sh"]