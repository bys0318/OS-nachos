package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    }
    
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new LotteryQueue(transferPriority);
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


    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 1;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = Integer.MAX_VALUE;

    protected LotteryThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null) 
            thread.schedulingState = new LotteryThreadState(thread);
        return (LotteryThreadState) thread.schedulingState;
    }

    protected class LotteryQueue extends PriorityQueue {
        
        LotteryQueue(boolean transferPriority) {
            super(transferPriority);
            this.threadStateQueue = new LinkedList<LotteryThreadState>();
        }
        public int getSum() {
            if (this.CurrentThreadState == null) {
                int sum = 0;
                for (LotteryThreadState threadstate: this.threadStateQueue) sum += threadstate.getEffectivePriority();
                return sum;
            }
            return this.CurrentThreadState.getEffectivePriority();
        }

        public void waitForAccess(KThread thread) {
			//System.out.println("queue lottery waitforaccess");
			Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).waitForAccess(this);
            //System.out.println(this.threadStateQueue.size());
		}

		public void acquire(KThread thread) {
            //System.out.println("acquire");
            //System.out.println(this.threadStateQueue.size());
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}

        public KThread nextThread() {
            //System.out.println("Next Thread");
            //System.out.println(this.threadStateQueue.size());
			Lib.assertTrue(Machine.interrupt().disabled());
			LotteryThreadState nextThreadState = pickNextThread(); 
			if (nextThreadState != null) {
                //System.out.println("return next thread");
				//System.out.println(nextThreadState.getEffectivePriority());
				this.threadStateQueue.remove(nextThreadState); // pick the thread out
                acquire(nextThreadState.thread);
                //System.out.println(nextThnextThreadStatereadState.thread.getName());
                //System.out.println(.getEffectivePriority());
				return nextThreadState.thread;
			}
			else return null;
		}

        protected LotteryThreadState pickNextThread() {
            //System.out.println("pickNextThread");
            //System.out.println(this.threadStateQueue.size());
            //if (!this.ThreadStateQueue.isEmpty()) {System.out.println("wrong queue"); return null;}
            if (this.threadStateQueue.isEmpty()) {return null;}//System.out.println("Empty Queue"); return null;}
            //System.out.println("Non-Empty!");
            int randNum = (int)(Math.random() * getTotalTickets());
			for (LotteryThreadState threadstate : threadStateQueue) {
				randNum -= threadstate.getEffectivePriority();
				if (randNum < 0) return threadstate;
			}
			return null;
        }

        protected int getTotalTickets() {
            //System.out.println("get Total Tickets");
            //System.out.println(this.threadStateQueue.size());
            if (this.CurrentThreadState == null) return getSum();
            return this.CurrentThreadState.getEffectivePriority()-this.CurrentThreadState.getPriority();
        }

        protected LotteryThreadState CurrentThreadState;
		protected LinkedList<LotteryThreadState> threadStateQueue;
    }

    protected class LotteryThreadState extends ThreadState {
        public LotteryThreadState(KThread thread) {
            super(thread);
            this.waitForme = new LinkedList<LotteryQueue>();
        }
/*
        public void setlotterychange(int lotterychange) {
            this.lotterychange = lotterychange;
        }
*/  
        public void setPriority(int priority) {
            if (this.priority == priority)
            return;

            this.donation = priority - this.priority;
            this.priority = priority;
            //System.out.println("correct function call");
            // implement me
            /*if (priority > this.effectivepriority) {
                this.effectivepriority = priority;
                donate(); // when update the effective priority, recompute donated priority
            }*/
            this.effectivepriority += donation;
            donate(this.donation);

        }

        public void setEffectivePriority(int effectivepriority) {
            if (this.effectivepriority == effectivepriority)
            return;
            this.effectivepriority = effectivepriority;
        }

        public void waitForAccess(LotteryQueue waitQueue) {// I need to release the resource I hold
			//System.out.println("true waitforaccess");
			if (this.Iwaitfor == waitQueue)
				return;
			//System.out.println("does not return");
			if (this.Iwaitfor != null) {
				this.Iwaitfor.threadStateQueue.remove(this);
				if (this.Iwaitfor.CurrentThreadState == this) {
					this.Iwaitfor.CurrentThreadState = null;
				}
			}
			this.Iwaitfor = waitQueue;
			waitQueue.threadStateQueue.add(this);
			if (waitQueue.transferPriority) {
				donate();
				//System.out.println("successfully enqueue");
			}
			//else System.out.println("successfully enqueue with no donation");
		}

        public void acquire(LotteryQueue waitQueue) {
			if (waitQueue.CurrentThreadState != null) {
				waitQueue.CurrentThreadState.waitForme.remove(waitQueue);
				waitQueue.CurrentThreadState.setEffectivePriority(waitQueue.CurrentThreadState.getPriority());
				for (PriorityQueue queue:waitQueue.CurrentThreadState.waitForme)
					if (queue.transferPriority)
						waitQueue.CurrentThreadState.getdonated(queue);
				// maybe here need to recompute holder's effectivepriority
			}
			waitQueue.threadStateQueue.remove(this);
			waitQueue.CurrentThreadState = this;
			this.waitForme.add(waitQueue);
			this.Iwaitfor = null;
			if (waitQueue.transferPriority) {
                getdonated(waitQueue);
                donate(this.donation);
			}
		}

        public void getdonated(LotteryQueue waitQueue) {
            this.donation = this.effectivepriority;
            for (LotteryThreadState threadstate : waitQueue.threadStateQueue) {
				this.effectivepriority += threadstate.effectivepriority;
            }
            this.donation = this.effectivepriority - this.donation;
        }

        public void donate() {
            if (this.Iwaitfor == null) return;
			if (this.Iwaitfor.transferPriority == false) return;
			if (this.Iwaitfor.CurrentThreadState != null){
				//System.out.println("donate");
				this.Iwaitfor.CurrentThreadState.setEffectivePriority(
                    this.Iwaitfor.CurrentThreadState.effectivepriority + this.effectivepriority);
				this.Iwaitfor.CurrentThreadState.donate(this.effectivepriority);
			}
        }
        public void donate(int donation) {
            if (this.Iwaitfor == null) return;
			if (this.Iwaitfor.transferPriority == false) return;
			if (this.Iwaitfor.CurrentThreadState != null){
				//System.out.println("donate");
				this.Iwaitfor.CurrentThreadState.setEffectivePriority(
                    this.Iwaitfor.CurrentThreadState.effectivepriority + donation);
				this.Iwaitfor.CurrentThreadState.donate(donation);
			}
        }
        protected int donation = 0;
        protected LinkedList<LotteryQueue> waitForme;
		protected LotteryQueue Iwaitfor = null;
    }
}
