package nachos.threads;

import nachos.ag.BoatGrader;
import nachos.machine.Machine;

public class ThreadTests {
    public ThreadTests(){ }
    
    private static class PingTest implements Runnable {
        PingTest(int which) {
            this.which = which;
        }
        
        public void run() {
            for (int i=0; i<5; i++) {
                System.out.println("*** thread " + which + " looped "
                        + i + " times");
                KThread.yield();
            }
        }

        private int which;
    }

    public static void joinTest1(){
        // test join
        System.out.println("joinTest1: Start!");
        KThread ping0 = new KThread(new PingTest(0));
        ping0.fork();
        ping0.join();
        new PingTest(1).run();
        System.out.println("joinTest1: End!");
    }
        
    public static void joinTest2(){
        // test joining a thread after finished
        System.out.println("joinTest2: Start!");
        KThread ping0 = new KThread(new Runnable(){
            public void run() {
                System.out.println("Thread finished");
            }
        });
        ping0.fork();
        KThread.yield();
        ping0.join();
        new PingTest(1).run();
        System.out.println("joinTest2: End!");
    }

    public static void conditionTest1(){
		// test condition2 by cooperation between two threads
		System.out.println("conditionTest1: Start");
		final Lock lock = new Lock();
		final Condition2 condition = new Condition2(lock);
		KThread thread1 = new KThread(new Runnable(){
			public void run(){
				lock.acquire();
				System.out.println("Thread1 is going to sleep");
				condition.sleep();
                System.out.println("Thread1 has been woken up, wake up thread2!");
                condition.wake();
				lock.release();
			}
		});	
		KThread thread2 = new KThread(new Runnable(){
			public void run(){
                lock.acquire();
                System.out.println("Thread2 wake all threads up!");
                condition.wakeAll();
				System.out.println("Thread2 is going to sleep");
				condition.sleep();
				System.out.println("Thread2 has been woken up");
				lock.release();
			}
		});	
		thread1.fork();
		thread2.fork();
        new PingTest(0).run();
		System.out.println("conditionTest1: End");
    }

    private static class PingAlarmTest implements Runnable {
        PingAlarmTest(int which, Alarm alarm, long ticks){
            this.which = which;
            this.alarm = alarm;
            this.ticks = ticks;
        }

        public void run(){
            System.out.println("Thread " + which + " waking up in " + ticks + " ticks");
            alarm.waitUntil(ticks);
            System.out.println("Thread " + which + " wake up"); 
        }

        Alarm alarm;
        private int which;
        private long ticks;
    }

    public static void alarmTest1(){
        // test three threads on alarm in order
		Alarm alarm = new Alarm();

        System.out.println("alarmTest1: Start!");
        KThread thread1 = new KThread(new PingAlarmTest(1, alarm, 3000));
        thread1.fork();

        KThread thread2 = new KThread(new PingAlarmTest(2, alarm, 2000));
        thread2.fork();

        new PingAlarmTest(0, alarm, 5000).run();
        System.out.println("alarmTest1: End!");
    }

    private static class Listener implements Runnable {
        static int id = 0;
        // listen for a word
        Listener(Communicator comm){
            this.comm = comm;
            this.myID = id;
            id++;
        }
        public void run(){
            System.out.println("L" + myID + ": I'm listening for a word");
            System.out.println("L" + myID + ": I got the word -> " + comm.listen());
        }
        private Communicator comm;
        private int myID = 0;
    }

    private static class Speaker implements Runnable {
        static int id = 0;
        // speaks a word
        Speaker(Communicator comm){
            this.comm = comm;
            this.myID = id;
            id++;
        }
        public void run(){
            System.out.println("S" + myID + ":I'm Speaking my ID number -> " + myID);
            comm.speak(myID);
            System.out.println("S" + myID + ":I finished Speaking");
        }
        private Communicator comm;
        private int myID = 0;
    }

    public static void communicatorTest1(){
        // test 1 listener and 1 speaker, listener first
        System.out.println("communicatorTest1: Start!");
		Communicator comm = new Communicator();
		new KThread(new Listener(comm)).fork();
		new KThread(new Speaker(comm)).fork();
        new PingTest(0).run();
        System.out.println("communicatorTest1: End!");
    }
    
	public static void communicatorTest2(){
        // test 1 speaker and 1 listener, speaker first
        System.out.println("communicatorTest2: Start!");
		Communicator comm = new Communicator();
		new KThread(new Speaker(comm)).fork();
		new KThread(new Listener(comm)).fork();
        new PingTest(0).run();
        System.out.println("communicatorTest2: End!");
    }
    
	public static void communicatorTest3(){
        // test multiple listeners and speakers
        System.out.println("communicatorTest3: Start!");
		Communicator comm = new Communicator();
		new KThread(new Speaker(comm)).fork();
		new KThread(new Listener(comm)).fork();
		new KThread(new Listener(comm)).fork();
		new KThread(new Speaker(comm)).fork();
        new KThread(new Listener(comm)).fork();
        new PingTest(0).run();
        new PingTest(1).run();
        new KThread(new Listener(comm)).fork();
        new KThread(new Speaker(comm)).fork();
        new KThread(new Speaker(comm)).fork();
        new PingTest(2).run();
        new PingTest(3).run();
        System.out.println("communicatorTest3: End!");
    }
    
    private static class Yielder implements Runnable{
        public boolean flag = true;
        public void run(){
            while (flag)
                KThread.yield();
        }
    }

    // Test the priority
    public static void PriorityTest1() {
        System.out.println("Prioirty Queue Test 1: Start");
        KThread thread1 = new KThread(new Runnable(){
			public void run(){
				System.out.println("I'm thread1");
			}
		});
		KThread thread2 = new KThread(new Runnable(){
			public void run(){
				System.out.println("I'm thread2");
			}
		});
		KThread thread3 = new KThread(new Runnable(){
			public void run(){
				System.out.println("I'm thread3");
			}
        });
        Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(thread1, 4);
		ThreadedKernel.scheduler.setPriority(thread2, 3);
		ThreadedKernel.scheduler.setPriority(thread3, 2);
        Machine.interrupt().enable();
        thread3.fork();
        thread2.fork();
        thread1.fork();
        KThread.yield();
        System.out.println("Priority Queue Test 1: End");
    }

    // test donate 
    public static void PriorityTest2() {
        System.out.println("Priority Queue Test 2: Start");
        // main thread holds the lock
        final Lock lock = new Lock();
        System.out.println("main holds the lock");
        lock.acquire();
        KThread thread1 = new KThread(new Runnable(){
            public void run(){
                System.out.println("thread1 wants the lock");
                lock.acquire();
                System.out.println("thread1 gets the lock");
                lock.release();
            }
        });
        Yielder yielder = new Yielder();
        KThread thread2 = new KThread(yielder);
        Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(thread1, 3);
		ThreadedKernel.scheduler.setPriority(thread2, 2);
        Machine.interrupt().enable();
        thread1.fork();
        thread2.fork();
        // only if it donate can main keep running and not blocked by thread2
        KThread.yield();
        System.out.println("main releases the lock");
        lock.release();
        yielder.flag = false; // stop yielding
        KThread.yield();
        System.out.println("Priority Queue Test 2: End");
    }

    public static void PriorityTest3() {
        System.out.println("Priority Queue TEST 3: START");
		final Lock mainlock = new Lock();
		final Lock smalllock = new Lock();
		final Lock smalllock2 = new Lock();
		smalllock.acquire();
		smalllock2.acquire();
		KThread thread1 = new KThread(new Runnable(){
			public void run(){
				System.out.println("Important thread wants the lock");
				mainlock.acquire();
				System.out.println("Important thread got what it wanted");
				mainlock.release();
				smalllock2.acquire();
				smalllock2.release();
			}
		});
		Yielder yielder = new Yielder();
		KThread thread2 = new KThread(yielder);
		KThread thread3 = new KThread(new Runnable(){
			public void run(){
				System.out.println("I am thread 3 and hold the lock");
				mainlock.acquire();
				smalllock.acquire();
				smalllock.release();
				System.out.println("I am thread 3 and release the lock");
				mainlock.release();
				KThread.yield();
				System.out.println("I am thread 3");
			}
		});
		Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(thread1, 4);
		ThreadedKernel.scheduler.setPriority(thread2, 3);
		ThreadedKernel.scheduler.setPriority(thread3, 2);
		Machine.interrupt().enable();
		thread3.fork();
		KThread.yield();
		smalllock.release();
		thread2.fork();
		thread1.fork();
		KThread.yield();
		smalllock2.release();
		yielder.flag = false;
		KThread.yield();
		System.out.println("Priority Queue TEST 3: END");
    }

//	test boat with at least 2 children
	public static void boatTest()
	{
		System.out.println("boatTest: Start!");
		System.out.println("2 children");
		Boat.begin(0,2,new BoatGrader());
		System.out.println("5 children");
		Boat.begin(0,5,new BoatGrader());
		System.out.println("5 children and 3 adults");
		Boat.begin(3,5,new BoatGrader());
		System.out.println("boatTest: End!");
	}
}
