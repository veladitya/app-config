package nitro.plc.map;

import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link TimerTask} implementation that removes its associated entry
 * (identified by a key K) from the internal {@link Map}.
 * 
 * @author 5288873
 *
 * @param <K>
 *            The object key
 * @param <V>
 */
@SuppressWarnings("hiding")
class ExpiryTask<K, V> extends TimerTask {
	private final Lock writeLock = new ReentrantLock();
	private final K key;
	private ConcurrentMap<K, ExpiredObject<K, V>> map;
	public ExpiryTask(K key) {
		this.key = key;
	}

	public K getKey() {
		return key;
	}

	@SuppressWarnings({ "unlikely-arg-type", "unchecked" })
	@Override
	public void run() {
		System.out.println("Expiring element with key [" + key + "]");
		final ExpiredObject<K, V> object;
		try {
			writeLock.lock();
			if (map.containsKey(key)) {
				object = (ExpiredObject<K, V>) map.remove(getKey());
				object.execute(object.getValue());
			}
		} finally {
			writeLock.unlock();
		}
	}
}
