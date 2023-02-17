import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static java.lang.Math.floor;
import static java.lang.Math.pow;

public class Point implements Serializable {
    public double x;
    public double y;

    Point(final double x, final double y)
    {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString()
    {
        return "(" + this.x + ", " + this.y + ")";
    }

    //Here BigDecimal is used to maintain precission.
    @Override
    public boolean equals(Object point)
    {
        Point secondPoint = (Point) point;
        Double point1_x = BigDecimal.valueOf(this.x)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();

        Double point1_y = BigDecimal.valueOf(this.y)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();

        Double point2_x = BigDecimal.valueOf(secondPoint.x)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();

        Double point2_y = BigDecimal.valueOf(secondPoint.y)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();

        return point1_x.equals(point2_x) && point1_y.equals(point2_y);
    }

    /**
     * Ideally Java lists and most of the data structures will be stored in the form of iterables which implement HashCode method
     * This method provides a numeric representation of an object's content so as to provide an alternate mechanism to identify it.
     * This returns an integer which represents the address of the variable, As we have points which are almost very similar
     * i implemented this hashCode method so that none of two different X and Y coordinates with very minute difference in the end
     * map to the same address, in that case there is a possibility that there might be two points with same values but point
     * to different addresses which might result in a mismatch.
     */

    @Override
    public int hashCode()
    {
//        HashCodeBuilder builder = new HashCodeBuilder();
//        builder.append(x);
//        builder.append(y);
//        return builder.toHashCode();

        return (int) floor(17*x + 29*y);
    }

    /**
     * Helper method to calculate distance between 2 points
     * @param p1 point1
     * @param p2 point2
     * @return distance between given 2 points
     */
    public static double calculateDistance(Point p1, Point p2)
    {
        return Math.sqrt((pow((p1.x - p2.x),2)) + (pow((p1.y - p2.y),2)));
    }
}