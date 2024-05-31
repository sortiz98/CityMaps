import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    // Built own QuadTree since there is no built-in quadtree in Java.

    private QuadTree quadTree;

    /** imgRoot is the name of the directory containing the images.
     *  You may not actually need this for your class. */
    public Rasterer(String imgRoot) {
        quadTree = new QuadTree(imgRoot);
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     * <p>
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     * </p>
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified:
     * "render_grid"   -> String[][], the files to display
     * "raster_ul_lon" -> Number, the bounding upper left longitude of the rastered image <br>
     * "raster_ul_lat" -> Number, the bounding upper left latitude of the rastered image <br>
     * "raster_lr_lon" -> Number, the bounding lower right longitude of the rastered image <br>
     * "raster_lr_lat" -> Number, the bounding lower right latitude of the rastered image <br>
     * "depth"         -> Number, the 1-indexed quadtree depth of the nodes of the rastered image.
     *                    Can also be interpreted as the length of the numbers in the image
     *                    string. <br>
     * "query_success" -> Boolean, whether the query was able to successfully complete. Don't
     *                    forget to set this to true! <br>
     * @see //#REQUIRED_RASTER_REQUEST_PARAMS
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
        Map<String, Object> results = new HashMap<>();
        results.put("query_success", false);
        Double queryLonDPP = (params.get("lrlon") - params.get("ullon")) / params.get("w");
        LinkedList<LinkedList<QuadTree>> grid = new LinkedList<>();
        grid.add(new LinkedList<>());
        grid.get(0).add(quadTree);
        //if (params.get("ullat") >= MapServer.ROOT_ULLAT || MapServer)
        while (lonDPP(grid) > queryLonDPP && grid.get(0).get(0).getTile().length() < 7) {
            grid = zoomGrid(grid, params);
        }
        results = exctractResults(grid);
        return results;
    }

    public LinkedList<LinkedList<QuadTree>> zoomGrid(LinkedList<LinkedList<QuadTree>> qts,
             Map<String, Double> params) {

        LinkedList<LinkedList<QuadTree>> zoomed = new LinkedList<>();
        QuadTree curr;
        boolean addCol = true;
        boolean addCol2 = true;
        int col = 0;
        for (int i = 0; i < qts.size(); i += 1) {
            addCol = true;
            addCol2 = true;
            for (int k = 0; k < qts.get(i).size(); k += 1) {
                curr = qts.get(i).get(k);
                if (intersecting(curr.upperLeft(), params)) {
                    if (addCol) {
                        zoomed.add(new LinkedList<>());
                        addCol = false;
                        col = zoomed.size() - 1;
                    }
                    zoomed.get(col).add(curr.upperLeft());
                }
                if (intersecting(curr.upperRight(), params)) {
                    if (addCol) {
                        zoomed.add(new LinkedList<>());
                        addCol = false;
                        col = zoomed.size() - 1;
                    }
                    zoomed.get(col).add(curr.upperRight());
                }
                if (intersecting(curr.lowerLeft(), params)) {
                    if (!addCol && addCol2 || addCol) {
                        zoomed.add(new LinkedList<>());
                        addCol2 = false;
                        addCol = false;
                    }
                    zoomed.get(zoomed.size() - 1).add(curr.lowerLeft());
                }
                if (intersecting(curr.lowerRight(), params)) {
                    if (!addCol && addCol2 || addCol) {
                        zoomed.add(new LinkedList<>());
                        addCol2 = false;
                        addCol = false;
                    }
                    zoomed.get(zoomed.size() - 1).add(curr.lowerRight());
                }
            }
        }
        return zoomed;
    }

    private boolean intersecting(QuadTree q, Map<String, Double> params) {
        if (q == null) {
            return false;
        }
        if (q.getULLon() > params.get("lrlon") || q.getLRLon() < params.get("ullon")
                || q.getULLat() < params.get("lrlat") || q.getLRLat() > params.get("ullat")) {
            return false;
        }

        return true;
    }

    private Double lonDPP(LinkedList<LinkedList<QuadTree>> tiles) {
        Double ullon = tiles.get(0).get(0).getULLon();
        Double lrlon =  tiles.get(0).get(0).getLRLon();
        Double w = 256.0; //tiles.get(0).size() * 256;
        return (lrlon - ullon) / w;
    }

     /* @return A map of results for the front end as specified:
            * "render_grid"   -> String[][], the files to display
     * "raster_ul_lon" -> Number, the bounding upper left longitude of the rastered image <br>
     * "raster_ul_lat" -> Number, the bounding upper left latitude of the rastered image <br>
     * "raster_lr_lon" -> Number, the bounding lower right longitude of the rastered image <br>
     * "raster_lr_lat" -> Number, the bounding lower right latitude of the rastered image <br>
     * "depth"         -> Number, the 1-indexed quadtree depth of the nodes of the rastered image.
            *                    Can also be interpreted as the length of the numbers in the image
     *                    string. <br>
     * "query_success" -> Boolean, whether the query was able to successfully complete. Don't
            *                    forget to set this to true! <br>
            */
    private Map<String, Object> exctractResults(LinkedList<LinkedList<QuadTree>> tiles) {
        Map<String, Object> results = new HashMap<>();
        String[][] grid = new String[tiles.size()][tiles.get(0).size()];
        for (int i = 0; i < grid.length; i++) {
            for (int k = 0; k < grid[0].length; k++) {
                if (tiles.get(i).get(k).getTile().equals("")) {
                    grid[i][k] = "img/root.png";
                } else {
                    grid[i][k] = "img/" + tiles.get(i).get(k).getTile() + ".png";
                }
            }
        }
        results.put("render_grid", grid);
        results.put("raster_ul_lon", tiles.get(0).get(0).getULLon());
        results.put("raster_ul_lat", tiles.get(0).get(0).getULLat());
        QuadTree q = tiles.get(tiles.size() - 1).get(tiles.get(0).size() - 1);
        results.put("raster_lr_lon", q.getLRLon());
        results.put("raster_lr_lat", q.getLRLat());
        results.put("depth", grid[0][0].length() - 8);
        results.put("query_success", true);
        return results;
    }


}
