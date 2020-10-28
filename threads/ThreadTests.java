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
        System.out.println("JOIN TEST #1: Start");
        KThread ping0 = new KThread(new PingTest(0));
        ping0.fork();
        ping0.join();
        new PingTest(1).run();
        System.out.println("JOIN TEST #1: Finished");
    }
        
    public static void joinTest2(){
        // test joining a thread after finished
        System.out.println("JOIN TEST #2: Start");
        KThread ping0 = new KThread(new Runnable(){
            public void run() {
                System.out.println("Thread finished");
            }
        });
        ping0.fork();
        KThread.yield();
        ping0.join();
        new PingTest(1).run();
        System.out.println("JOIN TEST #2: Finished");
    }

    public static void conditionTest1(){
		// test condition2 by cooperation between two threads
		System.out.println("Condition TEST #1: Start");
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
		System.out.println("Condition TEST #1: End");
    }

    private static class PingAlarmTest implements Runnable {
        PingAlarmTest(int which, Alarm alarm, long ticks){
            this.which = which;
            this.alarm = alarm;
            this.ticks = ticks;
        }

        public void run(){
            System.out.println("thread " + which + " waking up in " + ticks + " ticks");
            alarm.waitUntil(ticks);
            System.out.println("thread " + which + " wake up"); 
        }

        Alarm alarm;
        private int which;
        private long ticks;
    }

    public static void alarmTest1(){
        // test three threads on alarm in order
		Alarm alarm = new Alarm();

        System.out.println("Alarm TEST #1: Start");
        KThread thread1 = new KThread(new PingAlarmTest(1, alarm, 3000));
        thread1.fork();

        KThread thread2 = new KThread(new PingAlarmTest(2, alarm, 2000));
        thread2.fork();

        new PingAlarmTest(0, alarm, 5000).run();

        System.out.println("Alarm TEST #1: End");
    }

    private static class Listener implements Runnable {
        Listener(Communicator comm){
            communicator = comm;
            localID = id++;
        }
        public void run(){
            System.out.println("L" + localID + ": I'm listening for a word");
            System.out.println("L" + localID + ": I got the word -> " + communicator.listen());
        }
        private Communicator communicator;
        static int id = 0;
        private int localID;
    }

    private static class Speaker implements Runnable {
        Speaker(Communicator comm){
            localID= id++;
            communicator = comm;
        }
        public void run(){
            System.out.println("S" + localID + ":I'm Speaking my ID number -> " + localID);
            communicator.speak(localID);
            System.out.println("S" + localID + ":I finished Speaking");
        }
        private Communicator communicator;
        static int id = 0;
        private int localID;
    }

    public static void communicatorTest1(){
		// test 1 listener and 1 speaker, listener first
		Communicator communicator = new Communicator();
		new KThread(new Listener(communicator)).fork();
		new KThread(new Speaker(communicator)).fork();
        new PingTest(0).run();
    }
    
	public static void communicatorTest2(){
		// test 1 speaker and 1 listener, speaker first
		Communicator communicator = new Communicator();
		new KThread(new Speaker(communicator)).fork();
		new KThread(new Listener(communicator)).fork();
        new PingTest(0).run();
    }
    
	public static void communicatorTest3(){
		// test multiple listeners and speakers
		Communicator communicator = new Communicator();
		new KThread(new Speaker(communicator)).fork();
		new KThread(new Listener(communicator)).fork();
		new KThread(new Listener(communicator)).fork();
		new KThread(new Speaker(communicator)).fork();
        new KThread(new Listener(communicator)).fork();
        new PingTest(0).run();
        new PingTest(1).run();
        new KThread(new Listener(communicator)).fork();
        new KThread(new Speaker(communicator)).fork();
        new KThread(new Speaker(communicator)).fork();
        new PingTest(2).run();
        new PingTest(3).run();
	}
}
