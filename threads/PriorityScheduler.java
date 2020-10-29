package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
		       
		return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
		       
		return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());
		       
		Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum);
	
		getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		       
		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
	    	return false;

		setPriority(thread, priority+1);

		Machine.interrupt().restore(intStatus);
		return true;
    }

    public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		       
		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
	 		return false;

		setPriority(thread, priority-1);

		Machine.interrupt().restore(intStatus);
		return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
			this.ThreadStateQueue = new LinkedList<ThreadState>();
		}

		public void waitForAccess(KThread thread) {
			//System.out.println("queue waitforaccess");
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			ThreadState nextThreadState = pickNextThread(); 
			if (nextThreadState != null) {
				//System.out.println(nextThreadState.getEffectivePriority());
				this.ThreadStateQueue.remove(nextThreadState); // pick the thread out
				acquire(nextThreadState.thread);
				return nextThreadState.thread;
			}
			else return null;
		}

	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
		protected ThreadState pickNextThread() {
			if (this.ThreadStateQueue.isEmpty()) return null; // If there is no thread waiting, I don't know what to do
			ThreadState nextThread = ThreadStateQueue.peek(); // get the first thread
			for (ThreadState threadstate:ThreadStateQueue) // go over all threads in the queue
			/*
				if (threadstate.getEffectivePriority() > nextThread.getEffectivePriority() || // current thread has higher effectivepriority
						(threadstate.getEffectivePriority() == nextThread.getEffectivePriority() // same effectivepriority
							&& threadstate.getdelay() < nextThread.getdelay())) {				// but has waited longer 
					nextThread = threadstate;  // choose it! 
				}
			*/
			// assume the queue is FCFS, the previous one comes first
				if (threadstate.getEffectivePriority() > nextThread.getEffectivePriority())
					nextThread=threadstate;
			return nextThread;
		}
	
		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
			// No I don't want ^_^
		}

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
		public boolean transferPriority;
		protected ThreadState currentThreadState;
		protected LinkedList<ThreadState> ThreadStateQueue;
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
	/**
	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
		public ThreadState(KThread thread) {
			this.thread = thread;
			this.waitforme = new LinkedList<PriorityQueue>();
			setPriority(priorityDefault);
		}


	/**
	 * Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
		public int getPriority() {
			return priority;
		}

	/**
	 * Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */
		public int getEffectivePriority() { // do not update when get, update when some donate (when necessary)
			/**
			if (true) {
				for (ThreadState threadstate:waitQueue) {
					this.effectivepriority = Math.max(threadstate.getEffectivePriority(), this.effectivepriority);
				}
			}
			return this.effectivepriority;
			*/
			return this.effectivepriority;
		}

	/**
	 * Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
		public void setPriority(int priority) {
			if (this.priority == priority)
			return;
			
			this.priority = priority;
			
			// implement me
			if (priority > this.effectivepriority) {
				this.effectivepriority = priority;
				donate(); // when update the effective priority, recompute donated priority
			}

		}

		public void setEffectivePriority(int effectivepriority) {
			if (this.effectivepriority == effectivepriority)
			return;

			this.effectivepriority = effectivepriority;
		}

	/**
	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
	 * the associated thread) is invoked on the specified priority queue.
	 * The associated thread is therefore waiting for access to the
	 * resource guarded by <tt>waitQueue</tt>. This method is only called
	 * if the associated thread cannot immediately obtain access.
	 *
	 * @param	waitQueue	the queue that the associated thread is
	 *				now waiting on.
	 *
	 * @see	nachos.threads.ThreadQueue#waitForAccess
	 */
		public void waitForAccess(PriorityQueue waitQueue) {// I need to release the resource I hold
			//System.out.println("true waitforaccess");
			if (this.iwaitfor == waitQueue)
				return;
			//System.out.println("does not return");
			if (this.iwaitfor != null) {
				this.iwaitfor.ThreadStateQueue.remove(this);
				if (this.iwaitfor.currentThreadState == this) {
					this.iwaitfor.currentThreadState = null;
				}
			}
			this.iwaitfor = waitQueue;
			waitQueue.ThreadStateQueue.add(this);
			if (waitQueue.transferPriority) {
				donate();
				//System.out.println("successfully enqueue");
			}
			//else System.out.println("successfully enqueue with no donation");
		}

	/**
	 * Called when the associated thread has acquired access to whatever is
	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
	 * <tt>thread</tt> is the associated thread), or as a result of
	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
	 *
	 * @see	nachos.threads.ThreadQueue#acquire
	 * @see	nachos.threads.ThreadQueue#nextThread
	 */
		public void acquire(PriorityQueue waitQueue) {
			if (waitQueue.currentThreadState != null) {
				waitQueue.currentThreadState.waitforme.remove(waitQueue);
				waitQueue.currentThreadState.setEffectivePriority(waitQueue.currentThreadState.getPriority());
				for (PriorityQueue queue:waitQueue.currentThreadState.waitforme)
					if (queue.transferPriority)
						waitQueue.currentThreadState.getdonated(queue);
				// maybe here need to recompute holder's effectivepriority
			}
			waitQueue.ThreadStateQueue.remove(this);
			waitQueue.currentThreadState = this;
			this.waitforme.add(waitQueue);
			this.iwaitfor = null;
			if (waitQueue.transferPriority) {
				getdonated(waitQueue);
				donate();
			}
		}	

		public void getdonated(PriorityQueue waitQueue) {
			for (ThreadState threadstate:waitQueue.ThreadStateQueue) {
				this.effectivepriority = Math.max(this.effectivepriority, threadstate.effectivepriority);
			}
		}

		public void donate() {
			if (this.iwaitfor == null) return;
			if (this.iwaitfor.transferPriority == false) return;
			if (this.iwaitfor.currentThreadState != null){
				//System.out.println("donate");
				if (this.iwaitfor.currentThreadState.getEffectivePriority() < this.effectivepriority){
					this.iwaitfor.currentThreadState.setEffectivePriority(this.effectivepriority);
					this.iwaitfor.currentThreadState.donate();
				}
			}
		}

		/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority = priorityDefault;
		protected int effectivepriority = priorityDefault;
		// protected long waittime = 0;
		/** The queue of thread waiting for this' resources */
		protected LinkedList<PriorityQueue> waitforme;
		protected PriorityQueue iwaitfor = null;
    }
}
