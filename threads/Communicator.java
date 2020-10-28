package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 * 
 * @author Jeremy Rios
 * 
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        lock = new Lock();
        okToComm = new Condition2(lock);
        okToSpeak = new Condition2(lock);
        okToListen = new Condition2(lock);
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     * <p/>
     * <p/>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param    word    the integer to transfer.
     */
    public void speak(int word) {
        lock.acquire();
        while (communicating){ // previous speaker in communication
            okToSpeak.sleep();
        }
        message = word;
        communicating = true; // speaker speaks up
        while (waitingToListen == 0){ // no listener, stuck in communication
            okToComm.sleep();
        }
        // always ensure that speaker finishs after listener
        okToListen.wake();
        okToComm.sleep();
        communicating = false;
        okToSpeak.wake();
        lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return the integer transferred.
     */
    public int listen() {
        lock.acquire();
        waitingToListen++;
        if (waitingToListen == 1 && communicating){ // previous speaker speaking, wake it
            okToComm.wake();
        }
        okToListen.sleep();
        okToComm.wake();
        waitingToListen--;
        int word = message;
        lock.release();
        return word;
    }

    private boolean communicating = false;
    private int waitingToListen = 0;
    private int message = 0;
    private Lock lock;
    private Condition2 okToComm;
    private Condition2 okToSpeak;
    private Condition2 okToListen;
}