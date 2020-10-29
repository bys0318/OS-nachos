package nachos.threads;
import nachos.ag.BoatGrader;
public class Boat
{
/*
	All threads at Oahu will only visit O_*.
	All threads at Molokai will only visit M_*.
*/
	private static BoatGrader bg;
	private static Lock lock;
	private static Condition finishQueue;
//	O_*
	private static Condition O_waitingQueue;
	private static Condition O_adultQueue;
	private static Condition O_childQueue;
	private static int O_adultNum;
	private static int O_childNum;
	private static boolean O_initial;
	private static boolean O_waiting;
//	M_*
	private static Condition M_pilotQueue;
	private static int M_finishNum;
	private static void AdultItinerary()
	{
		bg.initializeAdult();
//	Reach Oahu
		lock.acquire();
		O_adultNum++;
		if(O_waiting)
			O_waitingQueue.wake();
		O_adultQueue.sleep();
		O_adultNum--;
//	Leave Oahu
		bg.AdultRowToMolokai();
//	Reach Molokai
		M_finishNum++;
		M_pilotQueue.wake();
		lock.release();
		return;
	}
	private static void ChildItinerary()
	{
		bg.initializeChild();
//	Reach Oahu
		boolean state;
		boolean mayFinish;
		lock.acquire();
		O_childNum++;
		if(O_initial&&(O_childNum==2))//The beginning
		{
			O_initial=false;
			O_childQueue.wake();
			O_childNum--;
//	Leave Oahu
			bg.ChildRowToMolokai();
//	Reach Molokai
			M_finishNum++;
			state=false;
			M_pilotQueue.sleep();
		}
		else
		{
			if(O_waiting)
			{
				O_waitingQueue.wake();
				O_waiting=false;
			}
			state=true;
			O_childQueue.sleep();
		}
		while(true)
			if(state)//At Oahu
			{
				if((O_childNum==1)&&(O_adultNum==0))
					mayFinish=true;
				else
					mayFinish=false;
				O_childNum--;
//	Leave Oahu
				bg.ChildRideToMolokai();
//	Reach Molokai
				M_finishNum++;
				if(mayFinish)
					finishQueue.wake();//Inform begin()
				else
					M_pilotQueue.wake();
				state=false;
				M_pilotQueue.sleep();
			}
			else//At Molokai
			{
				M_finishNum--;
//	Leave Molokai
				bg.ChildRowToOahu();
//	Reach Oahu
				O_childNum++;
				if((O_childNum==1)&&(O_adultNum==0))
				{
					O_waiting=true;
					O_waitingQueue.sleep();
				}
				if(O_childNum>1)
				{
					O_childQueue.wake();
					O_childNum--;
//	Leave Oahu
					bg.ChildRowToMolokai();
//	Reach Molokai
					M_finishNum++;
					state=false;
					M_pilotQueue.sleep();
				}
				else
				{
					O_adultQueue.wake();
					state=true;
					O_childQueue.sleep();
				}
			}
	}
	public static void begin(int adults,int children,BoatGrader b)
	{
		bg=b;
		lock=new Lock();
		finishQueue=new Condition(lock);
		O_waitingQueue=new Condition(lock);
		O_adultQueue=new Condition(lock);
		O_childQueue=new Condition(lock);
		M_pilotQueue=new Condition(lock);
		O_initial=true;
		O_waiting=false;
		O_adultNum=0;
		O_childNum=0;
		M_finishNum=0;
		lock.acquire();
		Runnable runnableAdult=new Runnable()
		{
			public void run()
			{
				AdultItinerary();
				return;
			}
		};
		for(int f1=0;f1<adults;f1++)
		{
			KThread t=new KThread(runnableAdult);
			t.setName("Adult Boat Thread"+f1);
			t.fork();
		}
		Runnable runnableChild=new Runnable()
		{
			public void run()
			{
				ChildItinerary();
				return;
			}
		};
		for(int f1=0;f1<children;f1++)
		{
			KThread t=new KThread(runnableChild);
			t.setName("Child Boat Thread"+f1);
			t.fork();
		}
		finishQueue.sleep();
		while(M_finishNum<adults+children)
		{
			M_pilotQueue.wake();
			finishQueue.sleep();
		}
		lock.release();
		return;
	}
}
