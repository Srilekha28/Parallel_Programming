import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import scala.Tuple2;

/**                                                                                                                        
 * Vertex Attributes
 */
public class Data implements Serializable {
    List<Tuple2<String,Integer>> neighbors; // <neighbor0, weight0>, <neighbor1, weight1>, ...
    String status;                          // "INACTIVE" or "ACTIVE"
    Integer distance;                       // the distance so far from source to this vertex
    Integer prev;                           // the distance calculated in the previous iteration

    public Data(){
        neighbors = new ArrayList<>();
        status = "INACTIVE";
        distance = 0;
    }

    public Data( List<Tuple2<String,Integer>> neighbors, Integer dist, Integer prev, String status ){
        if ( neighbors != null ) {
            this.neighbors = new ArrayList<>( neighbors );
        } else {
            this.neighbors = new ArrayList<>( );
        }
        this.distance = dist;
	    this.prev = prev;
        this.status = status;
    }

    /**
     * This method will take keys with same values as input and reduce the values to single key
     * @param d1
     * @param d2
     * @return Data object
     */
    public static Data merge(final Data d1, final Data d2)
    {
        List<Tuple2<String, Integer>> n = !d1.neighbors.isEmpty() ? d1.neighbors : d2.neighbors;
        Integer distance = d1.distance < d2.distance ? d1.distance : d2.distance;
        Integer prev = d1.prev < d2.prev ? d1.prev : d2.prev;

        return new Data(n, distance, prev, "INACTIVE");
    }

    public String getStatus()
    {
        return this.status;
    }

    @Override
    public String toString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("status = ")
                .append(this.status)
                .append("distance ")
                .append(this.distance)
                .append("prev ")
                .append(this.prev)
                .append("neighbors " );
        neighbors.forEach((tuple -> {
            stringBuilder.append("key: ").append(tuple._1()).append("link weight ").append(tuple._2());
        }));

        return stringBuilder.toString();
    }
}
