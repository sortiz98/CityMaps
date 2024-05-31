import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.lang.Double.*;

/**
 * This class provides a shortestPath method for finding routes between two points
 * on the map using A*.
 */
public class Router {
    /**
     * Return a LinkedList of <code>Long</code>s representing the shortest path from st to dest, 
     * where the longs are node IDs.
     */
    public static LinkedList<Long> shortestPath(GraphDB g, double stlon, double stlat,
                                                double destlon, double destlat) {
        long pointA = g.closest(stlon, stlat);
        long pointB = g.closest(destlon, destlat);
        Entry curr = new Entry(pointA, pointA, pointB, null, g);
        PriorityQueue<Entry> next = new PriorityQueue<>();
        LinkedList<Long> route = new LinkedList<>();
        HashMap<Long, Double> m = new HashMap<>();
        m.put(curr.id, curr.priority);
        while (curr.id != pointB) {
            for (long adj : g.adjacent(curr.id)) {
                Entry e = new Entry(adj, pointA, pointB, curr, g);
                if (m.containsKey(adj)) {
                    if (m.get(adj) > e.priority) {
                        next.add(e);
                        m.put(adj, e.priority);
                    }
                } else {
                    next.add(e);
                }
            }
            curr = next.poll();
            m.put(curr.id, curr.priority);
        }
        Entry temp = curr;
        while (temp != null) {
            route.addFirst(temp.id);
            temp = temp.previous;
        }
        return route;
    }


    static class Entry implements Comparable<Entry> {
        long id;
        double g;
        double priority;
        Entry previous;

        Entry(long id, long a, long b, Entry previous, GraphDB g) {
            this.id = id;
            this.previous = previous;
            if (previous != null) {
                this.g = previous.g + g.distance(previous.id, id);
                this.priority = this.g + g.distance(id, b);
            } else {
                this.g = 0;
                this.priority = g.distance(id, b);
            }
        }


        public int compareTo(Entry e) {
            return Double.compare(this.priority, e.priority);
        }

    }
}
