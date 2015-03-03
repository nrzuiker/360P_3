import static org.junit.Assert.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class PQueueTest {

	
	public static class TestInsert implements Runnable {
		PQueue p;
		String name;
		int priority;
		int position;
		
		public TestInsert(PQueue p, String n, int pri) {
			this.p = p;
			name = n;
			priority = pri;
			position = -1;
		}
		
		public int getPosition() {
			return position;
		}
		
		@Override
		public void run() {
			try {
				position = p.insert(name, priority);
				
			} catch (Exception e) {
				System.out.println("Exception: " + e.getMessage());
			}
		}
	}
	
	public static class TestSearch implements Runnable {
		PQueue p;
		String name;
		int position;
		
		public TestSearch(PQueue p, String name) {
			this.p = p;
			this.name = name;
			position = -2; //Actual output can be >= -1
		}
		
		public int getPosition() {
			return position;
		}
		
		@Override
		public void run() {
			try {
				position = p.search(name);
				
			} catch (Exception e) {
				System.out.println("Exception: " + e.getMessage());
			}
		}
	}
	
	public static class TestGetFirst implements Runnable {
		PQueue p;
		String name;
		
		public TestGetFirst(PQueue p) {
			this.p = p;
			name = "";
		}
		
		public String getName() {
			return name;
		}
		
		@Override
		public void run() {
			try {
				name = p.getFirst();
				
			} catch (Exception e) {
				System.out.println("Exception: " + e.getMessage());
			}
		}
	}
	
	@Test
	public void InsertEmptyBlockTest() throws InterruptedException, ExecutionException {
		ExecutorService threadpool = Executors.newCachedThreadPool();
		PQueue pq = new PQueue(0);
		TestInsert i1 = new TestInsert(pq, "n1", 1);
		
		//Insert a value, wait a second, make sure it hasn't finished meaning it's blocked
		Future<?> f1 = threadpool.submit(i1);
		Thread.sleep(1000);
		boolean done = f1.isDone();
		assertFalse(done);
		
		threadpool.shutdown();
	}
	
	@Test
	public void InsertNonEmptyBlockTest() throws InterruptedException, ExecutionException {
		ExecutorService threadpool = Executors.newCachedThreadPool();
		PQueue pq = new PQueue(1);
		
		Future<?> f1 = threadpool.submit(new TestInsert(pq, "n1", 5));
		f1.get();
		assertEquals(1, pq.getSize());
		//The queue is now full
		
		TestInsert i1 = new TestInsert(pq, "n2", 6);
		Future<?> f2 = threadpool.submit(i1);
		Thread.sleep(1000);
		boolean done = f2.isDone();
		assertEquals(false, done); //Should be blocked because the queue is full
		
		//Remove the first value allowing f2 to finish
		TestGetFirst g1 = new TestGetFirst(pq);
		Future<?> f3 = threadpool.submit(g1);
		f3.get();
		assertEquals("n1", g1.getName());//Make sure we got the one that was in the queue
		
		while(!done) {
			done = f2.isDone();
		}
		assertEquals(1, pq.getSize()); //Now f2 should put ("n2", 6) into the queue
		threadpool.shutdown();
	}
	
	@Test
	public void InsertSamePriorityTest() throws InterruptedException, ExecutionException {
		ExecutorService threadpool = Executors.newCachedThreadPool();
		PQueue pq = new PQueue(2);
		
		TestSearch s1 = new TestSearch(pq, "n1");
		TestSearch s2 = new TestSearch(pq, "n2");
		
		//Should add n1 and then n2 after n1
		Future<?> f1 = threadpool.submit(new TestInsert(pq, "n1", 5));
		f1.get(); //Make sure n1 has been added before adding n2
		assertEquals(1, pq.getSize());
		Future<?> f2 = threadpool.submit(new TestInsert(pq, "n2", 5));
		f2.get();
		assertEquals(2, pq.getSize());
		
		Future<?> f3 = threadpool.submit(s1);
		Future<?> f4 = threadpool.submit(s2);
		f3.get();
		f4.get();
		
		assertEquals(0, s1.getPosition());
		assertEquals(1, s2.getPosition());
		
		threadpool.shutdown();
	}
	
	@Test
	public void InsertLowThenHighPriorityTest() throws InterruptedException, ExecutionException {
		ExecutorService threadpool = Executors.newCachedThreadPool();
		PQueue pq = new PQueue(2);
		TestSearch s1 = new TestSearch(pq, "n1");
		TestSearch s2 = new TestSearch(pq, "n2");
		
		//Should add n1 and then n2 before n1
		Future<?> f1 = threadpool.submit(new TestInsert(pq, "n1", 5));
		f1.get();
		Future<?> f2 = threadpool.submit(new TestInsert(pq, "n2", 6));
		f2.get();
		
		Future<?> f3 = threadpool.submit(s1);
		Future<?> f4 = threadpool.submit(s2);
		f3.get();
		f4.get();
		
		assertEquals(2, pq.getSize());
		assertEquals(1, s1.getPosition());		
		assertEquals(0, s2.getPosition());
				
		threadpool.shutdown();
	}
	
	@Test
	public void GetFirstBlockTest() throws InterruptedException, ExecutionException {
		ExecutorService threadpool = Executors.newCachedThreadPool();
		PQueue pq = new PQueue(2);
		
		//Should block making 'done' false
		Future<?> f1 = threadpool.submit(new TestGetFirst(pq));
		Thread.sleep(1000);
		boolean done = f1.isDone();
		assertEquals(false, done);
		
		//Insert a value and check that it gets removed
		Future<?> f2 = threadpool.submit(new TestInsert(pq, "n1", 5));
		f2.get();
		while(!done) {
			done = f1.isDone();
		}
		assertEquals(0, pq.getSize()); //Queue is empty again
				
		threadpool.shutdown();
	}

	@Test
	public void GetFirstRemoveTest() throws InterruptedException, ExecutionException {
		ExecutorService threadpool = Executors.newCachedThreadPool();
		PQueue pq = new PQueue(3);
		
		Future<?> f1 = threadpool.submit(new TestInsert(pq, "n1", 1));
		Future<?> f2 = threadpool.submit(new TestInsert(pq, "n2", 2));
		Future<?> f3 = threadpool.submit(new TestInsert(pq, "n3", 3));
		f1.get();
		f2.get();
		f3.get();
		assertEquals(3, pq.getSize());
		
		TestGetFirst g1 = new TestGetFirst(pq);
		TestGetFirst g2 = new TestGetFirst(pq);
		TestGetFirst g3 = new TestGetFirst(pq);
		
		Future<?> f4 = threadpool.submit(g1);
		f4.get();
		//Size should now be 2 and the name returned should be n3
		assertEquals(2, pq.getSize());
		assertEquals("n3", g1.getName());
		
		Future<?> f5 = threadpool.submit(g2);
		f5.get();
		//Size should now be 1 and the name returned should be n2
		assertEquals(1, pq.getSize());
		assertEquals("n2", g2.getName());
		
		Future<?> f6 = threadpool.submit(g3);
		f6.get();
		//Size should now be 0 and the name returned should be n1
		assertEquals(0, pq.getSize());
		assertEquals("n1", g3.getName());
				
		threadpool.shutdown();
	}	
}
