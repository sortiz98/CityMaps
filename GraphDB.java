import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.*;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. 
 * Code includes the vertices, adjacent, distance, closest, lat, and lon
 * methods as well as instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 * */
public class GraphDB {
    /** Instance variables for storing the graph.  */
    HashMap<Long, Node> nodes = new HashMap<>();
    HashMap<Long, Set<Long>> graph = new HashMap<>();
    LinkedList<Map<String, Node>> locations = new LinkedList<>();
    Trie t;
    HashTable tl;
    /** 
     * @param dbPath Path to the XML file to be parsed.
     */
    public GraphDB(String dbPath) {
        try {
            File inputFile = new File(dbPath);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputFile, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }


    public void addNode(Node n) {
        nodes.put(n.id, n);
    }


/*
For each node in Edge, checks if node already in graph. If so, add adjacent
nodes in Edge to neighbors set in graph. Otherwise, add adjacent nodes in Edge
to node's adjacent set (if already added, previous found adj nodes already in adj set)
then add node and adjacent set to graph.
*/
    public void addEdge(Edge e) {
        int size = e.nodes.size();
        Long key;
        Set<Long> value;
        boolean added;
        Node n;
        if (size > 1) {

            for (int i = 0; i < size; i++) {
                key = e.nodes.get(i);
                n = nodes.get(key);
                value = graph.get(key);
                added = graph.containsKey(key);
                if (i == 0) {
                    if (added) {
                        value.add(e.nodes.get(i + 1));
                    } else {
                        n.adjacent.add(e.nodes.get(i + 1));
                        graph.put(key, n.adjacent);
                    }
                } else if (i == size - 1) {
                    if (added) {
                        value.add(e.nodes.get(i - 1));
                    } else {
                        n.adjacent.add(e.nodes.get(i - 1));
                        graph.put(key, n.adjacent);
                    }
                } else {

                    if (added) {
                        value.add(e.nodes.get(i + 1));
                        value.add(e.nodes.get(i - 1));
                    } else {
                        n.adjacent.add(e.nodes.get(i - 1));
                        n.adjacent.add(e.nodes.get(i + 1));
                        graph.put(key, n.adjacent);


                    }
                }
            }
        }
    }

    public static class Node {
        double lat;
        double lon;
        long id;
        String name;
        Set<Long> adjacent;

        Node(String lat, String lon, String id) {
            this.lat = Double.parseDouble(lat);
            this.lon = Double.parseDouble(lon);
            this.id = Long.parseLong(id);
            this.name = null;
            adjacent = new HashSet<>();
        }
        void setName(String name) {
            this.name = name;
        }


    }

    static class Edge {
        LinkedList<Long> nodes;
        String highway;
        String name;
        String maxSpeed;

        Edge() {
            this.nodes = null;
            this.highway = null;
            this.name = null;
            this.maxSpeed = null;
        }

    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        Map<Long, Node> copy = new HashMap<>();
        copy.putAll(nodes);
        Map<String, Node> location = new HashMap<>();
        Long node;
        for (Map.Entry<Long, Node> entry : copy.entrySet()) {
            node = entry.getKey();
            if (!graph.containsKey(node)) {
                nodes.remove(node);
            }
            Node n = entry.getValue();
            if (n.name !=  null) {
                location.put(n.name, n);
                locations.add(location);
                location = new HashMap<>();
            }
        }
        t = new Trie(locations);
        tl = new HashTable(locations);

    }

    /** Returns an iterable of all vertex IDs in the graph. */
    Iterable<Long> vertices() {
        LinkedList<Long> ids = new LinkedList<>();
        for (Map.Entry<Long, Set<Long>> entry : graph.entrySet()) {
            ids.addLast(entry.getKey());
        }
        return ids;
    }

    /** Returns ids of all vertices adjacent to v. */
    Iterable<Long> adjacent(long v) {
        return graph.get(v);
    }

    /** Returns the Euclidean distance between vertices v and w, where Euclidean distance
     *  is defined as sqrt( (lonV - lonV)^2 + (latV - latV)^2 ). */
    double distance(long v, long w) {
        if (v == w) {
            return 0;
        }
        double latV = nodes.get(v).lat;
        double latW = nodes.get(w).lat;
        double lonV = nodes.get(v).lon;
        double lonW = nodes.get(w).lon;
        return Math.sqrt((latV - latW) * (latV - latW) + (lonV - lonW) * (lonV - lonW));

    }

    /** Returns the vertex id closest to the given longitude and latitude. */
    long closest(double lon, double lat) {
        long closest = 0;
        double min = Integer.MAX_VALUE;
        Node n;
        double distance;
        for (Map.Entry<Long, Set<Long>> entry : graph.entrySet()) {
            n = nodes.get(entry.getKey());
            distance = Math.sqrt((lon - n.lon) * (lon - n.lon) + (lat - n.lat) * (lat - n.lat));
            if (distance < min) {
                min = distance;
                closest = n.id;
            }
        }
        return closest;
    }

    /** Longitude of vertex v. */
    double lon(long v) {
        return nodes.get(v).lon;
    }

    /** Latitude of vertex v. */
    double lat(long v) {
        return nodes.get(v).lat;

    }

    public List<String> getLocationsByPrefixHelper(String prefix) {

        return t.locationsByPrefix(prefix);
    }

    public List<Map<String, Object>> getLocationsHelper(String location) {
        return tl.locations(location);
    }

}
