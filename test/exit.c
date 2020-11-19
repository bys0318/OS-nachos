/* 
 * exit.c
 *
 * test simple exit functionality
 */
   
#include "syscall.h"

int main(int argc, char *argv[]){
    exit(123);
    // next line should not be executed
    assert(0);
}