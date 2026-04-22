FROM eclipse-temurin:17-jdk

RUN apt-get update && apt-get install -y g++ && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . .

ENV JAVA_HOME=/opt/java/openjdk

RUN g++ -shared -fPIC -o libcardpicker.so cardpicker.cpp \
    -I$JAVA_HOME/include \
    -I$JAVA_HOME/include/linux

RUN javac *.java

CMD ["java", "-Djava.library.path=/app", "-cp", "/app", "TarotServer"]
