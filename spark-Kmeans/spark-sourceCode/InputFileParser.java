import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.getProperty;

public class InputFileParser {


    public List<Point> parseInput() throws IOException {
        final File programData = new File(getProperty("user.dir") + File.separator + INPUT_FILE);
        final BufferedReader bufferedReader = new BufferedReader(new FileReader(programData));

        int totalPoints = Integer.parseInt(bufferedReader.readLine());
        List<Point> points = new ArrayList<>();
        int i = 0;
        while (i < totalPoints)
        {
            String[] coords = bufferedReader.readLine().split(SPLIT_REGEX);
            points.add(new Point(Float.parseFloat(coords[0]),
                    Float.parseFloat(coords[1])));
            i++;
        }

        bufferedReader.close();

        return points;
    }

    // regular expression that represents one of more spaces
    private static final String SPLIT_REGEX = "\\s+";

    // hardcoded input file name
    private static final String INPUT_FILE = "KMeansDataSet.txt";
}
