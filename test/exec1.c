/*
 * exec1.c
 *
 * testing exec with error arguments
 * 
 */

#include "syscall.h"
#include "stdio.h"

int main (int argc, char *argv[]){
    printf("invoke exec with wrong filename\n");
    char *prog = "inexistent.coff";
    int returnValue = exec(prog, 0, 0);
    if (returnValue == -1)
        printf("SUCCESSFULLY detected error filename\n");
    else
        printf("FAILED to detect error filename\n");
}