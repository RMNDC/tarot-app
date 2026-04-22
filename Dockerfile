FROM openjdk:17

RUN apt-get update && apt-get install -y g++

WORKDIR /app
COPY . .

# Compile C++ into a shared library
RUN g++ -shared -fPIC -o libcardpicker.so cardpicker.cpp \
    -I$(find /usr/lib/jvm -name "jni.h" | head -1 | xargs dirname) \
    -I$(find /usr/lib/jvm -name "jni_md.h" | head -1 | xargs dirname)

# Compile Java
RUN javac TarotServer.java CardFetcher.java CardPickerNative.java

CMD ["java", "-Djava.library.path=.", "-cp", ".", "TarotServer"]
