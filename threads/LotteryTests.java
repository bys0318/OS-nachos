package nachos.threads;

import java.util.HashMap;
import java.util.Iterator;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.threads.LotteryScheduler.LotteryThreadState;
import nachos.threads.LotteryScheduler.LotteryQueue;
import nachos.threads.PriorityScheduler.ThreadState;
import nachos.threads.PriorityScheduler.PriorityQueue;

/**This class is the test suit for the LotteryScheduler.
 * There are 4 tests. The testAll function runs all 4 tests
 */
public class LotteryTests {

	/* The grand daddy method to test the entire suit */
	public static void testAll() {
		testlotteryrule();
		testdonate();
		testsetpriority();
	}

	// Test the functionality of picking random threads proportional to the
	// number of tickets
	public static void testlotteryrule() {
        System.out.println("Lottery Test #1: Start");
        
		Integer val;
		LotteryThreadState thread = null;
		LotteryScheduler scheduler = new LotteryScheduler();
		LotteryQueue lotteryqueue = (LotteryQueue) scheduler.newThreadQueue(false); //no donation
		KThread thread1 = new KThread().setName("Thread1");
		KThread thread2 = new KThread().setName("Thread2");
        KThread thread3 = new KThread().setName("Thread3");
        Integer times1 = 0, times2 = 0, times3 = 0;

		Machine.interrupt().disable();
		scheduler.setPriority(thread1, 3);
		scheduler.setPriority(thread2, 4);
		scheduler.setPriority(thread3, 5);
		lotteryqueue.waitForAccess(thread1);
		lotteryqueue.waitForAccess(thread2);
		lotteryqueue.waitForAccess(thread3);
		Machine.interrupt().enable();
        
		for (int i = 0; i < 10000; i++) {
            thread = lotteryqueue.pickNextThread();
            if (thread == null) return;
            switch (thread.getPriority()) {
                case 3:
                    times1 ++;
                    break;
                case 4:
                    times2 ++;
                    break;
                case 5:
                    times3 ++;
                    break;
                default:
                    break;
            }
		}

		System.out.println("Thread1 is picked "	+ times1 + " times");
        System.out.println("Thread2 is picked "	+ times2 + " times");
        System.out.println("Thread3 is picked "	+ times3 + " times");
        
		System.out.println("Lottery Test #1: End");
	}

	public static void testdonate() {
        System.out.println("Lottery Test #2: Start");
		final boolean[] yielding = {true};
		final Lock lock = new Lock();
		final KThread Thread1 = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				Machine.interrupt().disable();
				System.out.println("Thread1 " + ThreadedKernel.scheduler.getEffectivePriority());
				Machine.interrupt().enable();
				System.out.println("Thread1 finishes.");
				lock.release();
			}
		});
		final KThread Thread2 = new KThread(new Runnable() {
			public void run() {
				while (yielding[0]) {
					KThread.yield();
					System.out.println("Thread2 wakes up");
				}
				Machine.interrupt().disable();
				System.out.println("Thread2 " + ThreadedKernel.scheduler.getEffectivePriority());
				Machine.interrupt().enable();
				System.out.println("Thread2 finishes.");
			}
		});
		final KThread Thread3 = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				Machine.interrupt().disable();
				Thread1.fork();
				Thread2.fork();
				System.out.println("Thread3 Before " + ThreadedKernel.scheduler.getEffectivePriority());
				Machine.interrupt().enable();
				KThread.yield();
				yielding[0] = false;
				Machine.interrupt().disable();
				System.out.println("Thread3 After " + ThreadedKernel.scheduler.getEffectivePriority());
				Machine.interrupt().enable();
				System.out.println("Thread3 finishes.");
				lock.release();
			}
		});
		Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(Thread1, 1000);
		ThreadedKernel.scheduler.setPriority(Thread2, 100);
		ThreadedKernel.scheduler.setPriority(Thread3, 10);
		Machine.interrupt().enable();
        Thread3.fork();
		Machine.interrupt().disable();
		Machine.interrupt().enable();
		ThreadedKernel.alarm.waitUntil(1000000);
		System.out.println("Lottery Test #2: End");
	}

	public static void testsetpriority() {
		System.out.println("Lottery Test #3: Start");
		final Lock lock = new Lock();
		final KThread Thread1 = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				for (int i = 0; i < 10; ++i) {
					Machine.interrupt().disable();
					ThreadedKernel.scheduler.decreasePriority();
					System.out.println("Thread1 Current Priority = " + ThreadedKernel.scheduler.getPriority());
					Machine.interrupt().enable();
				}
				lock.release();
			}
		});

		final KThread Thread2 = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				for (int i = 0; i < 10; ++i) {
					Machine.interrupt().disable();
					ThreadedKernel.scheduler.increasePriority();
					System.out.println("Thread2 Current Priority = " + ThreadedKernel.scheduler.getPriority());
					Machine.interrupt().enable();
				}
				lock.release();
			}
		});
		Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(Thread1, 5);
		ThreadedKernel.scheduler.setPriority(Thread2, Integer.MAX_VALUE - 5);
		Machine.interrupt().enable();

		Thread1.fork();
		Thread2.fork();

		ThreadedKernel.alarm.waitUntil(100000);
		System.out.println("Lottery Test #3: End");
	}
}
