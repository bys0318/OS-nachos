/* 
 * filesyscall2.c
 *
 * test syscall read and write
 * 
 */

#include "stdio.h"
#include "stdlib.h"

int main(int argc, char *argv[]) { 
    char *file = "mv.c";
    printf("filesyscall2: test syscall read\n");
    printf("open %s\n", file);
    int fd = open(file);
    if (fd == -1) {
        printf("failed to open %s \n", file); 
        exit(-1);
    }
    printf("file handle %d \n", fd);
    printf("looping between read / write\n");
    int length = 0;
    int BUFSIZE = 100;
    char buffer[101];
    while((length = read(fd, buffer, BUFSIZE)) > 0) {
        write(1, buffer, length);
    }
    close(fd); 
    printf("filesyscall2: SUCCESS\n");
}             
        


