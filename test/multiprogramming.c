#include"syscall.h"
char sort[10]="sort.coff";
char sort2[10]="sort2.coff";
char*argv[10];
int main()
{
	argv[0]=sort;
	argv[1]=0;
	int pid1=exec(sort,1,argv);
	argv[0]=sort2;
	argv[1]=0;
	int pid2=exec(sort2,1,argv);
	int status;
	join(pid1,&status);
	argv[0]=sort2;
	argv[1]=0;
	int pid3=exec(sort2,1,argv);
	return 0;
}
