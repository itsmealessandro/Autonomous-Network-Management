FROM ubuntu:latest

RUN apt update && \
    apt install -y default-jdk

WORKDIR .

COPY ./ ./

RUN chmod +x setup.sh

CMD ["./setup.sh"]

