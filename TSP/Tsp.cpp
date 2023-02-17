#include <iostream>  // cout
#include <fstream>   // ifstream
#include <string.h>  // strncpy
#include <stdlib.h>  // rand
#include<cstdlib>
#include <math.h>    // sqrt, pow
#include <algorithm>
#include "omp.h"
#include "Timer.h"
#include "Trip.h"

using namespace std;

// Already implemented. see the actual implementations below
void initialize( Trip trip[CHROMOSOMES], int coordinates[CITIES][2] );
void select( Trip trip[CHROMOSOMES], Trip parents[TOP_X] );
void populate( Trip trip[CHROMOSOMES], Trip offsprings[TOP_X] );

// need to implement for your program 1
void evaluate( Trip trip[CHROMOSOMES], int coordinates[CITIES][2] );
void crossover( Trip parents[TOP_X], Trip offsprings[TOP_X], int coordinates[CITIES][2] );
void mutate( Trip offsprings[TOP_X] );

/*
 * MAIN: usage: Tsp #threads
 */
int main( int argc, char* argv[] ) {
  Trip trip[CHROMOSOMES];       // all 50000 different trips (or chromosomes)
  Trip shortest;                // the shortest path so far
  int coordinates[CITIES][2];   // (x, y) coordinates of all 36 cities:
  int nThreads = 1;
  
  // verify the arguments
  if ( argc == 2 )
    nThreads = atoi( argv[1] );
  else {
    cout << "usage: Tsp #threads" << endl;
    if ( argc != 1 )
      return -1; // wrong arguments
  }
  cout << "# threads = " << nThreads << endl;

  // shortest path not yet initialized
  shortest.itinerary[CITIES] = 0;  // null path
  shortest.fitness = -1.0;         // invalid distance

  // initialize 50000 trips and 36 cities' coordinates
  initialize( trip, coordinates );

  // TODO create distance mapping instead of storing coorditions
  // 36c2 possibilities 36 * 36 array
  // float cityDistances[CITIES][CITIES]; // stores distance between each city, to avoid recomputing multiple times

  // start a timer 
  Timer timer;
  timer.start( );

  // change # of threads
  omp_set_num_threads( nThreads );

  // random seed
  srand(12346);  

  // find the shortest path in each generation
  for ( int generation = 0; generation < MAX_GENERATION; generation++ ) {

    // evaluate the distance of all 50000 trips
    evaluate( trip, coordinates );

    // just print out the progress
    if ( generation % 20 == 0 )
      cout << "generation: " << generation << endl;

    // whenever a shorter path was found, update the shortest path
    if ( shortest.fitness < 0 || shortest.fitness > trip[0].fitness ) {

      strncpy( shortest.itinerary, trip[0].itinerary, CITIES );
      shortest.fitness = trip[0].fitness;

      cout << "generation: " << generation 
	   << " shortest distance = " << shortest.fitness
	   << "\t itinerary = " << shortest.itinerary << endl;
    }
    

    // define TOP_X parents and offsprings.
    Trip parents[TOP_X], offsprings[TOP_X];

    // choose TOP_X parents from trip
    select( trip, parents );

    // generates TOP_X offsprings from TOP_X parenets
    crossover( parents, offsprings, coordinates );

    // mutate offsprings
    mutate( offsprings );

    // populate the next generation.
    populate( trip, offsprings );
  }

  // stop a timer
  cout << "elapsed time = " << timer.lap( ) << endl;
  return 0;
}

int getCityIndex(char city) {
    return ( city >= 'A' ) ? city - 'A' : city - '0' + 26;
}

/**
  This method can be used to calcualte distance of complete itinerary/path,
  and will be called in evaluate() method.
  For every path, we will start calculating distance from point (0, 0)

  @param trip Trip object of a particular trip
  @param coordinates

  @return total distance between all cities of itinerary starting from (0, 0)
*/
float calculateDistance(Trip trip, int coordinates[CITIES][2])
{
    float totalDistance = 0.0;
    int prev_x = 0;
    int prev_y = 0;
    for(int i = 0 ; i < CITIES ; i++) {
        char city = trip.itinerary[i];
        int index = getCityIndex(city);
        int x = coordinates[index][0];
        int y = coordinates[index][1];
        totalDistance += sqrt((pow((x - prev_x) , 2) + pow((y - prev_y), 2)));
        prev_x = x;
        prev_y = y;
    }

    return totalDistance;
}

/*
 * Initializes trip[CHROMOSOMES] with chromosome.txt and coordiantes[CITIES][2] with cities.txt
 *
 * @param trip[CHROMOSOMES]:      50000 different trips
 * @param coordinates[CITIES][2]: (x, y) coordinates of 36 different cities: ABCDEFGHIJKLMNOPQRSTUVWXYZ
 */
void initialize( Trip trip[CHROMOSOMES], int coordinates[CITIES][2] ) {
  // open two files to read chromosomes (i.e., trips)  and cities
  ifstream chromosome_file( "chromosome.txt" );
  ifstream cities_file( "cities.txt" );

   
  // read data from the files
  // chromosome.txt:                                                                                           
  //   T8JHFKM7BO5XWYSQ29IP04DL6NU3ERVA1CZG                                                                    
  //   FWLXU2DRSAQEVYOBCPNI608194ZHJM73GK5T                                                                    
  //   HU93YL0MWAQFIZGNJCRV12TO75BPE84S6KXD
  for ( int i = 0; i < CHROMOSOMES; i++ ) {
    chromosome_file >> trip[i].itinerary;
    trip[i].fitness = 0.0;
  }

  // cities.txt:                                                                                               
  // name    x       y                                                                                         
  // A       83      99                                                                                        
  // B       77      35                                                                                        
  // C       14      64                                                                                        
  for ( int i = 0; i < CITIES; i++ ) {
    char city;
    cities_file >> city;
    int index = getCityIndex(city);
    cities_file >> coordinates[index][0] >> coordinates[index][1];
  }

  // close the files.
  chromosome_file.close( );
  cities_file.close( );

  // just for debugging
  if ( DEBUG ) {
    for ( int i = 0; i < CHROMOSOMES; i++ )
      cout << trip[i].itinerary << endl;
    for ( int i = 0; i < CITIES; i++ )
      cout << coordinates[i][0] << "\t" << coordinates[i][1] << endl;
  }
}

/* 
  To compare two trips using fitness/distance attributes
 */
bool compareTrips(Trip const & a, Trip const & b) {
    return a.fitness < b.fitness;
}

/*
 * This method is used to calculate the distance of the complete itenerary
 * After calculating the distance for all trips it sorts the iteneraries
 * based on their fitness in ascending order.
 */
void evaluate(Trip trip[CHROMOSOMES], int coordinates[CITIES][2]) {
    // Each iteration of following loop is independent, hence parallelized to improve runtime of overall algorithm
    #pragma omp parallel for
    for ( int i = 0; i < CHROMOSOMES; i++) {
        // Following optimisation reduces overall runtime of algorithm, even with one thread (close to 3secs reduction in overall runtime)
        // But that reduces ratio of (runtime of single thread) to (runtime of n threads)
        // if (trip[i].fitness == 0.0) {
            float distance = calculateDistance(trip[i], coordinates);
            trip[i].fitness = distance;
        // }
    }

    int blocks = 0;     // total number of threads
    int blockSize;      // size of trips for each sort function
    int remainder;      // remainder of CHROMOSOMES to num threads

    // Here we are parallelizing the merge sort by dividing the trips into blocks
    // where number of blocks would be equal to number of threads and the trips
    // will be divided equally amongst the blocks as much as possible, any remainder
    // would be allocated to last block. Each block will be sorted in-place.
    //
    // Example: If there are 8 threads used to run code, we would be creating 8 blocks, sort them parallelly.
    // Time complexity of sort will be reduced to O(n * log(n)/(number of threads))
    #pragma omp parallel
    {
        blocks = omp_get_num_threads();
        remainder = CHROMOSOMES % blocks;
        blockSize = (CHROMOSOMES - remainder) / blocks;
        int block = omp_get_thread_num();

        int start = blockSize * block;
        int finish;  
        if ((block + 1) == blocks) {
            finish = start + blockSize + remainder;
        } else {
            finish = start + blockSize;
        }

        std::sort(trip + start, trip + finish, compareTrips );
    }

    // Post sorting individual block of trips, we will be merging them inplace by parallelizing
    // Each level of merge operation
    // Example: Continuing earlier case, (Bi - i'th block of trips sorted earlier)
    //         B1 (MERGE) B2,  B3 (MERGE) B4,  B5 (MERGE) B6,  B7 (MERGE) B8    - LEVEL 1 MERGE
    //         B1B2 (MERGE) B3B4, B5B6 (MERGE) B7B8                             - LEVEL 2 MERGE
    //         B1B2B3B4 (MERGE) B5B6B7B8                                        - LEVEL 3 MERGE
    // All levels of merge calls are run parallel using omp parallel for
    // With large number of threads and division, merge can be completed in O(n) time
    for (int merge_step = 1; merge_step < blocks; merge_step *= 2 )
    {
        #pragma omp parallel for
        for (int i = 0; i < (blocks/2/merge_step); ++i) {
            int start = i * 2 * merge_step * blockSize;
            int mid = (i * 2 * merge_step + merge_step) * blockSize;
            int end = (i * 2 * merge_step + 2*merge_step) * blockSize;
            end = ((CHROMOSOMES - end) == remainder) ? CHROMOSOMES : end;
            std::inplace_merge( trip + start, trip + mid, trip + end, compareTrips);
        }

        // For odd number of threads, last merge has to be done separately
        // Logic takes care of unmerged block size and merges that with last merge block
        // When num of threads are 15
        //      Level 1: (1+1) + (1+1) + (1+1) + (1+1) + (1+1) + (1+1) + (1+1) + 1 (unmerged) -> this will be merged to form 3 unit block
        //      Level 2: (2+2) + (2+2) + (2+2) + 3 (unmerged) -> 3 unit block will be merged with 4 unit block
        //      Level 3: (4+4) + 7 (unmerged) -> both of them will be merged to form 15unit sorted trips array
        if ((blocks > 2*merge_step) && (blocks % (2*merge_step)) > 0) {
            int i = (blocks/2/merge_step) - 1; // last iteration index value

            int prevBlockstart = i * 2 * merge_step * blockSize;
            int prevBlockEnd = (i * 2 * merge_step + 2*merge_step) * blockSize ;

            std::inplace_merge(trip + prevBlockstart,  trip + prevBlockEnd, trip + CHROMOSOMES, compareTrips);            
        }    
    }
}

/*
 * Select the first TOP_X parents from trip[CHROMOSOMES]
 *
 * @param trip[CHROMOSOMES]: all trips
 * @param parents[TOP_X]:    the firt TOP_X parents
 */
void select( Trip trip[CHROMOSOMES], Trip parents[TOP_X] ) {
  // just copy TOP_X trips to parents

  // Parallelizing for loop to improve running time of the program.
  #pragma omp parallel for
  for ( int i = 0; i < TOP_X; i++ )
    strncpy( parents[i].itinerary, trip[i].itinerary, CITIES + 1 );
}

/*
 * Function takes 2 city chars as argument along with coordinates and calculates Euclidean distance
 *
 * @param city1
 * @param city2
 * @param coordinates
 * @return distance between city1 and city2
 */
float calculateDistanceBetweenTwoCities(char city1, char city2, int coordinates[CITIES][2]) {
    int index1 = getCityIndex(city1);
    int index2 = getCityIndex(city2);

    int city1x = coordinates[index1][0];
    int city1y = coordinates[index1][1];
    
    int city2x = coordinates[index2][0];
    int city2y = coordinates[index2][1];

    return sqrt((pow((city2x - city1x), 2) + pow((city2y - city1y), 2)));
}



/*
 * This method is used to generate TOP_X offsprings from TOP_X parents. We will be using modified genetics
 * algorithm. From i'th parent and i+1'th parent, we will be generating i'th child and i+1'th child.
 * Steps:
 *      1) Start with first city in parent[i] as first city of child[i]
 *      2) For every other city, calculate distance of leaving city in parent[i] and parent[i+1]
 *      3) If either of the cities is already added to child[i], pick the other city    
 *      4) If both of them are added, pick a random unassisgned city
 *      5) Else, pick the closest to extend child[i] itinerary
 *      6) Calculate child[i+1] trip as complement of child[i]
 *          - example of complements used: A-9, B-8 etc 
 *
 * @param parents shortest CHROMOSOMES/2 trips  
 * @param offsprings
 * @param coordinates
 */
void crossover(Trip parents[TOP_X], Trip offsprings[TOP_X], int coordinates[CITIES][2]) {
    #pragma omp parallel for
    for (int i = 0; i < TOP_X; i += 2) {
        char childOneItenary[CITIES + 1];           // To store child[i] itinerary
        char childTwoItenary[CITIES + 1];           // To store child[i+1] itinerary

        int alreadyAddedCityArr[CITIES] = {0};      // To keep track of already added cities to child[i] trip
        int parentOneCityPositions[CITIES] = {0};   // Inversion array to ease city search for parent[i]
        int parentTwoCityPositions[CITIES] = {0};   // Inversion array to ease city search for parent[i+1]

        // Creating inversion array to easily search for a city in parent[i] or parent[i+1] itinarary
        // Example: for city A, at 0 index in parentOneCityPositions we will store position of city A in parent[i]'s itinerary
        for (int j = 0; j < CITIES; j++) {
            char city = parents[i].itinerary[j];
            parentOneCityPositions[getCityIndex(city)] = j;

            city = parents[i+1].itinerary[j];
            parentTwoCityPositions[getCityIndex(city)] = j;
        }

        // Initialize first city of child[i] with first city of parent[i]
        char prevCity = parents[i].itinerary[0];
        int prevIndex = ( prevCity >= 'A' ) ? prevCity - 'A' : prevCity - '0' + 26;
        childOneItenary[0] = prevCity;
        alreadyAddedCityArr[prevIndex] = 1;

        int k = 1;  // keeps track of number of cities added to child[i]'s itinerary
        for(k=0 ; k< CITIES ; k++) {
            // find next city position in parent[i], if prevCity is last one, wrapping around for simplicity
            int nextPosition = parentOneCityPositions[prevIndex] < 35 ? parentOneCityPositions[prevIndex] + 1 : 0;
            char parentOneNextCity = parents[i].itinerary[nextPosition];

            // find next city position in parent[i+1], if prevCity is last one, wrapping around for simplicity
            nextPosition = parentTwoCityPositions[prevIndex] < 35 ? parentTwoCityPositions[prevIndex] + 1 : 0;
            char parentTwoNextCity = parents[i+1].itinerary[nextPosition];

            int nextOneIndex = getCityIndex(parentOneNextCity);
            int nextTwoIndex = getCityIndex(parentTwoNextCity);

            // If both next cities are already assigned to child[i]'s itinerary, pick a random next city
            if (alreadyAddedCityArr[nextOneIndex]== 1 && alreadyAddedCityArr[nextTwoIndex] == 1) {
                bool randomCitySelectionStrategy = false; // true for random selection, false for sequential
                int cityIndex;
                if (randomCitySelectionStrategy) {
                    // Following is solution to pick a city at random, but sequential query produces better results
                    cityIndex = rand() % CITIES;
                    while (alreadyAddedCityArr[cityIndex] != 0) {
                        cityIndex = rand() % CITIES;
                    }
                    prevIndex = cityIndex;
                    prevCity = ( prevIndex < 26 ) ? char(int('A') + prevIndex) : char(int('0') + (prevIndex - 26));
                } else {
                    // Sequential random city selection
                    for (cityIndex = 0; cityIndex < CITIES; cityIndex++)
                    {
                        if (alreadyAddedCityArr[cityIndex] == 0)
                        {
                            prevIndex = cityIndex;
                            prevCity = ( prevIndex < 26 ) ? char(int('A') + prevIndex) : char(int('0') + (prevIndex - 26));
                            break;
                        }
                    }
                }
            }
            // If parent[i] next city is not assigned and parent[i+1]'s next city is assigned, assign parent[i]'s next city
            else if (alreadyAddedCityArr[nextOneIndex]== 0 && alreadyAddedCityArr[nextTwoIndex] == 1) {
                prevIndex = nextOneIndex;
                prevCity = parentOneNextCity;
            }
            // If parent[i] next city is assigned and parent[i+1]'s next city is not assigned, assign parent[i+1]'s next city
            else if (alreadyAddedCityArr[nextOneIndex]== 1 && alreadyAddedCityArr[nextTwoIndex] == 0) {
                prevIndex = nextTwoIndex;
                prevCity = parentTwoNextCity;
            }
            // If both of them are unassined, pick the one with shortest distance
            else {
                // gets precomputed distance for prevCity and nextCity instead of calculating every time
                float parentOneDistance = calculateDistanceBetweenTwoCities(prevCity, parentOneNextCity, coordinates) ;
                float parentTwoDistance = calculateDistanceBetweenTwoCities(prevCity, parentTwoNextCity, coordinates);

                if (parentOneDistance < parentTwoDistance) {
                    prevIndex = nextOneIndex;
                    prevCity = parentOneNextCity;
                }
                else {
                    prevIndex = nextTwoIndex;
                    prevCity = parentTwoNextCity;
                }
            }
            // extend child[i]'s itinerary and mark already assigned status
            childOneItenary[k] = prevCity;
            alreadyAddedCityArr[prevIndex] = 1;
        }

        // Complement itinerary of child[i] for child[i+1]
        #pragma omp parallel for
        for (k = 0; k < CITIES; k++) {
            prevIndex = ( childOneItenary[k] >= 'A' ) ? childOneItenary[k] - 'A' : childOneItenary[k] - '0' + 26;
            if (prevIndex < 10) {
                childTwoItenary[k] = char(int('9') - prevIndex);
            } else if (prevIndex >= 26) {
                childTwoItenary[k] = char(int('J') - (prevIndex - 26));
            } else if (10 <= prevIndex && prevIndex < 26) {
                childTwoItenary[k] = char(int('Z') - (prevIndex - 10));
            }
        }

        strncpy(offsprings[i].itinerary, childOneItenary, CITIES + 1);
        strncpy(offsprings[i+1].itinerary, childTwoItenary, CITIES + 1);
    }
}

/*
 * mutating offsprings itinerary at random by swapping two randomly selected cities
 * MUTATE_RATE is used as percentage of offspring trips to be mutated.
 *
 * @param offsprings
 */
void mutate(Trip offsprings[TOP_X]) {
    int mutationRandom;
    int randomIndexOne;
    int randomIndexTwo;
    
    #pragma omp parallel for
    for (int i = 0 ; i < TOP_X ; i++) {
        // generating numbers between 0 to 99 to be used for mutation decision
        mutationRandom = rand() % 100;
        // Generated mutationRandom should be <x for x% for uniformly generated random numbers
        if (mutationRandom < MUTATE_RATE) {
            randomIndexOne = rand() % CITIES ;
            randomIndexTwo = rand() % CITIES ;
            char temp = offsprings[i].itinerary[randomIndexOne];
            offsprings[i].itinerary[randomIndexOne] = offsprings[i].itinerary[randomIndexTwo];
            offsprings[i].itinerary[randomIndexTwo] = temp ;
        }
    }
}

/*
 * Replace the bottom TOP_X trips with the TOP_X offsprings
 */
void populate( Trip trip[CHROMOSOMES], Trip offsprings[TOP_X] ) {
  // just copy TOP_X offsprings to the bottom TOP_X trips.
  #pragma omp parallel for
  for ( int i = 0; i < TOP_X; i++ ) {
    strncpy( trip[ CHROMOSOMES - TOP_X + i ].itinerary, offsprings[i].itinerary, CITIES + 1 );
    trip[ CHROMOSOMES - TOP_X + i ].fitness = 0.0;
 }

  // for debugging
  if ( DEBUG ) {
    for ( int chrom = 0; chrom < CHROMOSOMES; chrom++ ) 
      cout << "chrom[" << chrom << "] = " << trip[chrom].itinerary 
	   << ", trip distance = " << trip[chrom].fitness << endl;
  }
}
