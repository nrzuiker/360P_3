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
	public void EmptyTest() throws InterruptedException, ExecutionException {
		ExecutorService threadpool = Executors.newCachedThreadPool();
		PQueue pq = new PQueue(0);
		TestGetFirst gf = new TestGetFirst(pq);
		TestSearch s = new TestSearch(pq, "n1");
		Future<?> f1 = threadpool.submit(s);

		f1.get();
		assertEquals(-1, s.getPosition());
		
		threadpool.shutdown();
	}
	
	@Test
	public void SamePriorityTest() throws InterruptedException, ExecutionException {
		ExecutorService threadpool = Executors.newCachedThreadPool();
		PQueue pq = new PQueue(2);
		TestSearch s1 = new TestSearch(pq, "n1");
		TestSearch s2 = new TestSearch(pq, "n2");
		
		//Should add n1 and then n2 after n1
		threadpool.submit(new TestInsert(pq, "n1", 5));
		threadpool.submit(new TestInsert(pq, "n2", 5));
		
		Future<?> f1 = threadpool.submit(s1);
		Future<?> f2 = threadpool.submit(s2);

		f1.get();
		f2.get();
		
		assertEquals(2, pq.getSize());
		assertEquals(0, s1.getPosition());
		assertEquals(1, s2.getPosition());
		
		threadpool.shutdown();
	}
	
	@Test
	public void LowThenHighPriorityTest() throws InterruptedException, ExecutionException {
		ExecutorService threadpool = Executors.newCachedThreadPool();
		PQueue pq = new PQueue(2);
		TestSearch s1 = new TestSearch(pq, "n1");
		TestSearch s2 = new TestSearch(pq, "n2");
		
		//Should add n1 and then n2 before n1
		Future<?> f1 = threadpool.submit(new TestInsert(pq, "n1", 5));		
		Future<?> f2 = threadpool.submit(new TestInsert(pq, "n2", 6));
		
		f1.get();
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
	public void SimpleGetFirstTest() throws InterruptedException, ExecutionException {
		ExecutorService threadpool = Executors.newCachedThreadPool();
		PQueue pq = new PQueue(2);
		TestGetFirst g1 = new TestGetFirst(pq);
		TestInsert t1 = new TestInsert(pq, "n1", 5);
		
		//Should block making 'cancelled' true
		Future<?> f1 = threadpool.submit(new TestGetFirst(pq));
		Thread.sleep(1000);
		boolean done = f1.isDone();
		assertEquals(false, done);
		
		//Insert a value first this time
		Future<?> f2 = threadpool.submit(t1);
		f2.get();
		Future<?> f3 = threadpool.submit(new TestGetFirst(pq));
		Thread.sleep(1000);
		done = f3.isDone();
		//Didn't cancel because the thread completed during its 1 second
		assertEquals(true, done);
		
				
		threadpool.shutdown();
	}

		
}
