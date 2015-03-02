
public class PQueueRunner {
	
	public static void main(String [] args) {
		PQueue pq = new PQueue(10);
		pq.insert("n1", 4);
		pq.printQueue();
		pq.insert("n2", 2);
		pq.printQueue();
		pq.insert("n3", 8);
		pq.printQueue();
		pq.insert("n4", 3);
		pq.printQueue();
		int loc = pq.search("n4");
		System.out.println(loc);
		pq.getFirst();
		pq.printQueue();
		loc = pq.search("n4");
		System.out.println(loc);
	}
}
