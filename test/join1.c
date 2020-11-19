/*
 * join1.c
 *
 * test join when invoked more than once
 *
 */

#include "syscall.h"

int main (int argc, char *argv[]){
    printf("1. test join when invoked more than once\n");
    char * _argv[20];
    char *prog = "exit.coff";
    _argv[0] = prog;
    _argv[1] = "test";
    printf("exec %s...\n", prog);
    int pid = exec("exit.coff", 1, _argv);
    printf("child process id is %d\n", pid);
    printf("issue join to get exit status of chile process\n");
    int exitstatus;
    int returnValue = join(pid, &exitstatus);
    if (returnValue == 1){
        printf("first time invoke join successfully!\n");
    }
    else{
        printf("first time joined FAIL!\n");
    }
    printf("issue join again to get exit status of chile process\n");
    returnValue = join(pid, &exitstatus);
    if (returnValue == 1) {
        printf("second time joined, FAIL!\n");
    }
    else{
        printf("failed to invoke join second time as exptected, SUCCESS\n");
    }
    
    printf("2. test join to a nonchild\n");
    returnValue = join(0, &exitstatus);
    if (returnValue == 1){
        printf("joined to nonchild, FAIL\n");
    }
    else if (returnValue == -1){
        printf("detect notchild, SUCCESS\n");
    }
    printf("join to myself with pid = 1\n");
    returnValue = join(1, &exitstatus);
    if (returnValue == 1){
        printf("joined to self, FAIL\n");
    }
    else if (returnValue == -1){
        printf("detect self, SUCCESS\n");
    }
}