#include <iostream>
#include <sstream>
#include <string>
#include <vector>
#include <algorithm>
#include <random>
#include <ctime>
#include <cstring>
#include <unistd.h>
#include <netinet/in.h>
#include <sys/socket.h>

// Picks 3 random indices and returns them as JSON
// Called by Java via HTTP GET /pick?count=78
std::string pickCards(int count) {
    std::vector<int> indices(count);
    for (int i = 0; i < count; i++) indices[i] = i;

    std::mt19937 rng(static_cast<unsigned int>(std::time(nullptr)));
    std::shuffle(indices.begin(), indices.end(), rng);

    std::ostringstream json;
    json << "{\"picks\":[" << indices[0] << "," << indices[1] << "," << indices[2] << "]}";
    return json.str();
}

// Parse ?count=N from the request line
int parseCount(const std::string& request) {
    size_t pos = request.find("count=");
    if (pos == std::string::npos) return 78;
    return std::stoi(request.substr(pos + 6));
}

int main() {
    int port = 9090;
    int server_fd = socket(AF_INET, SOCK_STREAM, 0);

    int opt = 1;
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port = htons(port);

    bind(server_fd, (sockaddr*)&addr, sizeof(addr));
    listen(server_fd, 10);
    std::cout << "C++ picker server running on port " << port << std::endl;

    while (true) {
        int client = accept(server_fd, nullptr, nullptr);
        char buffer[1024] = {};
        read(client, buffer, sizeof(buffer));

        std::string request(buffer);
        int count = parseCount(request);
        std::string body = pickCards(count);

        std::ostringstream response;
        response << "HTTP/1.1 200 OK\r\n"
                 << "Content-Type: application/json\r\n"
                 << "Content-Length: " << body.size() << "\r\n"
                 << "\r\n"
                 << body;

        std::string res = response.str();
        write(client, res.c_str(), res.size());
        close(client);
    }
    return 0;
}
