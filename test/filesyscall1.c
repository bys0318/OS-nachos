/* 
 * filesyscall1.c
 *
 * test syscall creat / close / unlink
 * 
 */

#include "stdio.h"
#include "stdlib.h"

int main(int argc, char *argv[]) { 
    char *file = "test.txt";
    printf("filesyscall1: test syscall creat / close / unlink\n");
    
    printf("1. syscall creat file %s\n", file);
    int fd = creat(file);
    if (fd == -1) {
        printf("failed to create %s \n", file);
        exit(-1);
    }
    printf("syscall close\n");
    close(fd);

    printf("2. syscall unlink to delete %s\n", file);
    int returnValue = unlink(file);
    if (returnValue == -1){
        printf("failed to delete %s \n", file);
        exit(-1);
    }
    
    printf("3. syscall creat again to create file %s\n", file);
    fd = creat(file);
    if (fd == -1){
        printf("failed to create %s again\n", file);
        exit(-1);
    }
    
    printf("4. syscall unlink to delete %s without close\n", file);
    returnValue = unlink(file);
    if (returnValue == -1){
        printf("failed to delete %s \n", file);
        exit(-1);
    }
    
    printf("5. syscall unlink again to see if %s still existed.\n", file);
    returnValue = unlink(file);
    if (returnValue != -1) {
        printf("failed, %s should not exist anymore\n", file);
        exit(-1);
    }

    printf("filesyscall1: SUCCESS\n");
             
}             
        


