/*
 * exec.c
 *
 * Simple program for testing exec. It does not pass any arguments to
 * the child.
 */

#include "syscall.h"

int main (int argc, char *argv[]){
    char *prog = "exit.coff";
    int pid = exec(prog, 0, 0);
    printf("child process id is %d\n", pid);
    char *prog1 = "exit.coff";
    pid = exec(prog1, 0, 0);
    printf("another child process id is %d\n", pid);
    // the exit status of this process is the pid of the child process
    exit(pid);
}