/* 
 * filesyscall3.c
 *
 * test stdin / stdout
 * 
 */

#include "stdio.h"
#include "stdlib.h"

int main(int argc, char *argv[]) { 
    printf("filesyscall: test stdin / stdout\n");
    printf("1. test stdin\n");
    printf("invoke fgetc to input a character:\n");
    int fg = fgetc(0);
    char buffer[101];
    fgetc(0);
    printf("\ninput character is %c\n", fg);
    printf("invoke readline to input a line :\n");
    readline(buffer, 80); 
    printf("input line is %s\n", buffer);

    printf("2. test stdout\n");
    strcpy(buffer, "test stdout\n");
    write(1, buffer, strlen(buffer));
    int i;
    for (i = 0; i < 10; i++){
        printf("%d test stdout\n", i);
    }

    printf("filesyscall3: SUCCESS\n");
}
        


