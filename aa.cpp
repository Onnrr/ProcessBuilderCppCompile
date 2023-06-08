#include <iostream>
#include <string>

int main() {
    for (int i = 0; i < 3; i++) {
        std::string input;
        std::cout << "Enter a string: ";
        std::getline(std::cin, input);

        std::cout << "You entered: " << input << std::endl;
    }
    std::cout << "Program finished" << std::endl;

    return 0;
}