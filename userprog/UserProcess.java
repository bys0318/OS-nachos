package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.*;

import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    private void assign(int vpn)
	{
		int ppn;
		for(ppn=0;UserKernel.usedMemory[ppn];ppn++)
			continue;
		UserKernel.usedMemory[ppn]=true;
		pageTable[numEntry++]=new TranslationEntry(vpn,ppn,true,false,false,false);
		return;
	}
	private void setReadOnly(int vpn,boolean readOnly)
	{
		for(int f1=0;f1<numEntry;f1++)
			if(pageTable[f1].valid&&(pageTable[f1].vpn==vpn))
				pageTable[f1].readOnly=readOnly;
		return;
	}
	private int translate(int vpn)
	{
		for(int f1=0;f1<numEntry;f1++)
			if(pageTable[f1].valid&&(pageTable[f1].vpn==vpn))
				return pageTable[f1].ppn;
		return -1;
	}
	private int atp(int addr)
	{
		return addr/pageSize;
	}
	private int ato(int addr)
	{
		return addr%pageSize;
	}
	private int pota(int pn,int offset)
	{
		return pn*pageSize+offset;
	}
    /**
     * Allocate a new process.
     */
    public UserProcess() {
        int numPhysPages = Machine.processor().getNumPhysPages();
        pageTable = new TranslationEntry[numPhysPages];
        for (int i=0; i<numPhysPages; i++)
            pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
        boolean intStatus=Machine.interrupt().disable();
        pID = counter++;
        processNum++;
        Machine.interrupt().restore(intStatus);
        descripters = new OpenFile[16];
        for (int i = 0; i < 16; ++i) descripters[i] = null;
        descripters[0] = UserKernel.console.openForReading();
        descripters[1] = UserKernel.console.openForWriting();
    }
    
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	    return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
        if (!load(name, args)){
            boolean intStatus = Machine.interrupt().disable();
            processNum--;
            Machine.interrupt().restore(intStatus);
            return false;
        }
        
        thread = (UThread) (new UThread(this).setName(name));
        thread.fork();

        return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	    Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
        Lib.assertTrue(maxLength >= 0);

        byte[] bytes = new byte[maxLength+1];

        int bytesRead = readVirtualMemory(vaddr, bytes);

        for (int length=0; length<bytesRead; length++) {
            if (bytes[length] == 0)
            return new String(bytes, 0, length);
        }

        return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	    return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
        if ((vaddr < 0) || (vaddr >= numPages * pageSize)) return 0;
        byte[] memory = Machine.processor().getMemory();
        
        // for now, just assume that virtual addresses equal physical addresses
        for (int f1 = 0; f1 < length; ++f1)
            {
                int ppn = translate(atp(vaddr + f1));
                if (ppn == -1)
                    return f1;
                data[offset + f1] = memory[pota(ppn, ato(vaddr + f1))];
            }
        return length;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	    return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
        if((vaddr < 0) || (vaddr >= numPages * pageSize)) return 0;
        byte[] memory = Machine.processor().getMemory();
        
        // for now, just assume that virtual addresses equal physical addresses
        for (int f1 = 0; f1 < length; ++f1)
            {
                int ppn = translate(atp(vaddr + f1));
                if (ppn == -1)
                    return f1;
                memory[pota(ppn, ato(vaddr + f1))] = data[offset + f1];
            }
            return length;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args){
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
        
        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        if (executable == null) {
            Lib.debug(dbgProcess, "\topen failed");
            return false;
        }

        try {
            coff = new Coff(executable);
        }
        catch (EOFException e){
            executable.close();
            Lib.debug(dbgProcess, "\tcoff load failed");
            return false;
        }

        // make sure the sections are contiguous and start at page 0
        numPages = 0;
        for (int s=0; s<coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages) {
            coff.close();
            Lib.debug(dbgProcess, "\tfragmented executable");
            return false;
            }
            numPages += section.getLength();
        }

        // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for (int i=0; i<args.length; i++) {
            argv[i] = args[i].getBytes();
            // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }
        if (argsSize > pageSize) {
            coff.close();
            Lib.debug(dbgProcess, "\targuments too long");
            
            return false;
        }
        // program counter initially points at the program entry point
        initialPC = coff.getEntryPoint();	

        // next comes the stack; stack pointer initially points to top of it
        numPages += stackPages;
        initialSP = numPages*pageSize;

        // and finally reserve 1 page for arguments
        numPages++;

        if (!loadSections())
            return false;
        // store arguments in last page
        int entryOffset = (numPages-1)*pageSize;
        int stringOffset = entryOffset + args.length*4;

        this.argc = args.length;
        this.argv = entryOffset;
        
        for (int i=0; i<argv.length; i++) {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
            Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
                argv[i].length);
            stringOffset += argv[i].length;
            Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
            stringOffset += 1;
        }
        return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
        boolean intStatus=Machine.interrupt().disable();
		int cnt=0;
        int numPhysPages=Machine.processor().getNumPhysPages();
		for(int f1=0;f1<numPhysPages;f1++)
			if(!UserKernel.usedMemory[f1])
				cnt++;
		if(numPages>cnt)
		{
			Machine.interrupt().restore(intStatus);
			coff.close();
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}
		for(int f1=0;f1<numPages;f1++)
			assign(f1);
		Machine.interrupt().restore(intStatus);
		for(int s=0;s<coff.getNumSections();s++)
		{
			CoffSection section=coff.getSection(s);
			Lib.debug(dbgProcess,"\tinitializing "+section.getName()+" section ("+section.getLength()+" pages)");
			for(int i=0;i<section.getLength();i++)
			{
				int vpn=section.getFirstVPN()+i;
				setReadOnly(vpn,section.isReadOnly());
				section.loadPage(i,translate(vpn));
			}
		}
		return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        for (int i = 0; i < 16; ++i) 
            if (descripters[i] != null) {
                descripters[i].close();
                descripters[i] = null;
            }
        boolean intStatus=Machine.interrupt().disable();
        for(int f1 = 0; f1 < numEntry; f1++)
            {
                UserKernel.usedMemory[pageTable[f1].ppn] = false;
                pageTable[f1] = new TranslationEntry(pageTable[f1].vpn, 0, false, false, false, false);
            }
        Machine.interrupt().restore(intStatus);
        numPages = 0;
        coff.close();
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
        Processor processor = Machine.processor();

        // by default, everything's 0
        for (int i=0; i<processor.numUserRegisters; i++)
            processor.writeRegister(i, 0);

        // initialize PC and SP according
        processor.writeRegister(Processor.regPC, initialPC);
        processor.writeRegister(Processor.regSP, initialSP);

        // initialize the first two argument registers to argc and argv
        processor.writeRegister(Processor.regA0, argc);
        processor.writeRegister(Processor.regA1, argv);
    }

    private int avail_des() {
        for (int i = 2; i < 16; ++i) if (descripters[i] == null) return i;
        return -1;
    }
    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {
        if (pID != 0) return 0; // only root is allowed

        Machine.halt();
        
        Lib.assertNotReached("Machine.halt() did not halt machine!");
        return 0;
    }

    private int handleCreate(int address) {
        int available = avail_des();
        if (address < 0 || available == -1) return -1;
        String name = readVirtualMemoryString(address, 256);
        if (name == null) return -1;
        OpenFile file = ThreadedKernel.fileSystem.open(name, true);
        if (file == null) return -1;
        descripters[available] = file;
        return available;
    }
    
    private int handleOpen(int address) {
        int available = avail_des();
        if (address < 0 || available == -1){
            Lib.debug(dbgProcess, "Address fault or no space!");
            return -1;
        }
        String name = readVirtualMemoryString(address, 256);
        if (name == null){
            Lib.debug(dbgProcess, "Name not Found!");
            return -1;
        }
        OpenFile file = ThreadedKernel.fileSystem.open(name, false);
        if (file == null){
            Lib.debug(dbgProcess, "File not Found!");
            return -1;
        }
        descripters[available] = file;
        return available;
    }

    private int handleRead(int descripter, int bufferaddr, int count) {
        if (descripter < 0 || descripter > 15 || bufferaddr < 0 || count < 0) return -1;
        if (descripters[descripter] == null) return -1;
        OpenFile file = descripters[descripter];
        byte[] buf = new byte[count];
        int bytes_read = file.read(buf, 0, count);
        if (bytes_read != count) return -1;
        int bytes_write = writeVirtualMemory(bufferaddr, buf, 0, bytes_read);
        if (bytes_write != bytes_read) return -1;
        return bytes_read;
    }

    private int handleWrite(int descripter, int bufferaddr, int count) {
        if (descripter < 0 || descripter > 15 || bufferaddr < 0 || count < 0) return -1;
        if (descripters[descripter] == null) return -1;
        OpenFile file = descripters[descripter]; 
        byte[] buf = new byte[count];
        int bytes_read = readVirtualMemory(bufferaddr, buf, 0, count);
        if (bytes_read != count) return -1; 
        int bytes_write = file.write(buf, 0, bytes_read);
        if (bytes_read != bytes_write) return -1;
        return bytes_write;
    }

    private int handleClose(int descripter) {
        if (descripter < 0 || descripter > 15) return -1;
        if (descripters[descripter] == null) return -1;
        descripters[descripter].close();
        descripters[descripter] = null;
        return 0;
    }
    
    private int handleUnlink(int address) {
        if (address < 0) return -1;
        String name = readVirtualMemoryString(address, 256);
        if (name == null) return -1;
        boolean succ = UserKernel.fileSystem.remove(name);
        // Prof. Xu thinks we do not need to remove the descripter though it may leads to error
        /*if (succ) { 
            for (int i = 0; i < 16; ++i) {
                if (descripters[i] == null) continue;
                if (descripters[i].getName().equals(name)) descripters[i] = null;
            }
        }*/
        return succ ? 0 : -1;
    }
    
    private int handleExec(int filePtr, int argc, int argvPtr){
        // check if arguments are valid
        if (filePtr < 0 || argc < 0 || argvPtr < 0){
            Lib.debug(dbgProcess, "handleExec(): Invalid arguments!");
			return -1;
        }
        // read filename from virtual memory
        String fileName = readVirtualMemoryString(filePtr, 256);
        if (fileName == null || !fileName.endsWith(".coff")){
            Lib.debug(dbgProcess, "handleExec(): Invalid fileName!");
            return -1;
        }
        // read arguments for the new process
        String[] args = new String[argc];
        for (int i = 0; i < argc; i++) {
			byte[] buffer = new byte[4];
            int length = readVirtualMemory(argvPtr + 4 * i, buffer);
            if (length != 4) {
                Lib.debug(dbgProcess, "handleExec(): Invalid argument address!");
				return -1;
			}
			int pointer = Lib.bytesToInt(buffer, 0);
            String arg = readVirtualMemoryString(pointer, 256);
			if (arg == null) {
                Lib.debug(dbgProcess, "handleExec(): Invalid argument address!");
				return -1;
            }
			args[i] = arg;
        }
        // create new child process and set up relationship
        UserProcess newChild = new UserProcess();
        // Run child process
        boolean returnValue = newChild.execute(fileName, args);
        if (!returnValue){
            return -1;
        }
		newChild.parent = this;
        children.add(newChild);
		return newChild.pID;
    }

    private int handleJoin(int ID, int statusPtr){
        if (ID < 0 || statusPtr < 0){
            Lib.debug(dbgProcess, "handleJoin(): Invalid arguments!");
			return 1;
        }
        // find the child process with ID
        UserProcess child = null;
        int childNum = children.size();
        for (int i = 0; i < childNum; i++){
            if (children.get(i).pID == ID){
                child = children.get(i);
                break;
            }
        }
		if (child == null){
            Lib.debug(dbgProcess, "handleJoin(): Invalid process ID!");
			return 1;
        }
        child.parent = null;
        children.remove(child);
		child.thread.join();
        // check status
		statusLock.acquire();
		Integer status = childrenStatus.get(child.pID);
        statusLock.release();
        if (status == null){
            Lib.debug(dbgProcess, "handleJoin(): No sign of exit from the child process!");
			return 1;
        }
        else{
			int length = writeVirtualMemory(statusPtr, Lib.bytesFromInt(status));
			if (length != 4){
                Lib.debug(dbgProcess, "handleJoin(): Invalid exit status of child process!");
				return 1;
            }
			return 0;
		}
    }

    private void handleExit(int status){
        if (parent != null){ // exit status transferred to its parent
			parent.statusLock.acquire();
			parent.childrenStatus.put(pID, status);
			parent.statusLock.release();
        }
        // delete the relationship between the process and its children
        int childNum = children.size();
		for(int i = 0; i < childNum; i++){
			UserProcess child = children.removeFirst();
			child.parent = null;
        }
        unloadSections(); // TODO: free up memory, close open files
        boolean intStatus = Machine.interrupt().disable();
        if (--processNum == 0){ // last process, kernel terminate
            Kernel.kernel.terminate();
        }
        Machine.interrupt().restore(intStatus);
        UThread.finish();
    }

    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
        switch (syscall) {
            case syscallHalt:
                return handleHalt();
            case syscallCreate:
                return handleCreate(a0);
            case syscallOpen:
                return handleOpen(a0);
            case syscallRead:
                return handleRead(a0, a1, a2);
            case syscallWrite:
                return handleWrite(a0, a1, a2);
            case syscallClose:
                return handleClose(a0);
            case syscallUnlink:
                return handleUnlink(a0);
            case syscallExec:
                return handleExec(a0, a1, a2);
            case syscallJoin:
                return handleJoin(a0, a1);
            case syscallExit:
                handleExit(a0);
                return 0;
            default:
                Lib.debug(dbgProcess, "Unknown syscall " + syscall);
                handleExit(-1);
                Lib.assertNotReached("Unknown system call!");
        }
        return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
        Processor processor = Machine.processor();

        switch (cause) {
            case Processor.exceptionSyscall:
                int result = handleSyscall(processor.readRegister(Processor.regV0),
                            processor.readRegister(Processor.regA0),
                            processor.readRegister(Processor.regA1),
                            processor.readRegister(Processor.regA2),
                            processor.readRegister(Processor.regA3)
                            );
                processor.writeRegister(Processor.regV0, result);
                processor.advancePC();
                break;				       
                            
            default:
                Lib.debug(dbgProcess, "Unexpected exception: " +
                    Processor.exceptionNames[cause]);
                handleExit(-1);
                Lib.assertNotReached("Unexpected exception");
        }
    }
    protected UThread thread;
    protected static int counter = 0;
    protected static int processNum = 0;
    protected int pID = 0;
    /** The parent process of current process */
    protected UserProcess parent = null;
    /** The children process of current process */
    protected LinkedList<UserProcess> children = new LinkedList<UserProcess>();
    /** Status of all children of current process when exit, along with a lock */
    protected HashMap<Integer, Integer>childrenStatus = new HashMap<Integer, Integer>();
    protected Lock statusLock = new Lock();
    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages = 0;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;

	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';

    protected OpenFile[] descripters;

    private int numEntry = 0;
}
