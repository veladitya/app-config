package nitro.plc.map;

/**
 * A visitor interface.
 */
public interface Callback<V> {
	public void execute(V v);
}