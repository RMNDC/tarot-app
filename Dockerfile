FROM eclipse-temurin:17-jdk

RUN apt-get update && apt-get install -y g++ && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . .

# Compile C++ server
RUN g++ -o cardpicker cardpicker.cpp

# Compile Java server
RUN javac *.java

# Start C++ server in background, then Java server
CMD ./cardpicker & java -cp /app TarotServer
