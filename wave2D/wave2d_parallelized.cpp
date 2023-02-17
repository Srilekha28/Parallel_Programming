#include "mpi.h"
#include <math.h>
#include <iostream>
#include "Timer.h"
#include <stdlib.h>   // atoi
#include "omp.h"

int constant = 10;
int default_size = 10*constant;  // the default system size
int defaultCellWidth = 8;
double c = 1.0;      // wave speed
double dt = 0.1;     // time quantum
double dd = 2.0;     // change in system
bool alwaysBlockSend = false;

using namespace std;

int main( int argc, char *argv[] ) {
    // verify arguments
    if ( argc != 5 ) {
        cerr << "usage: Wave2D size max_time interval" << endl;
        return -1;
    }
    int my_rank;                        // rank of process
    int numOfHosts;                     // total # of hosts
    int size = atoi( argv[1] );         //# of processes
    int max_time = atoi( argv[2] );     //# of times the heights should be calculated
    int interval  = atoi( argv[3] );    // when prints should happen
    int numOfThreads = atoi ( argv[4]); // total # of input threads
    omp_set_num_threads(numOfThreads);
    MPI_Status status;                  //receive

    if ( size < 100 || max_time < 3 || interval < 0 ) {
        cerr << "usage: Wave2D size max_time interval" << endl;
        cerr << "       where size >= 100 && time >= 3 && interval >= 0" << endl;
        return -1;
    }

    MPI_Init( &argc, &argv ); // start MPI

    MPI_Comm_rank(MPI_COMM_WORLD , &my_rank);      // find out process rank
    MPI_Comm_size(MPI_COMM_WORLD , &numOfHosts);   // find out # of processes

    // create a simulation space
    double z[3][size][size];
    for ( int p = 0; p < 3; p++ )
        for ( int i = 0; i < size; i++ )
            for ( int j = 0; j < size; j++ )
                z[p][i][j] = 0.0; // no wave

    // start a timer
    Timer time;
    time.start( );
  

    // time = 0;
    // initialize the simulation space: calculate z[0][i][j]
    int weight = size / default_size;
    for( int i = 0; i < size; i++ ) {
        for( int j = 0; j < size; j++ ) {
            if( i > 4 * weight * constant && i < 6 * weight * constant &&
                j > 4 * weight * constant && j < 6 * weight * constant)
            {
                z[0][i][j] = 20.0;
            }
            else
            {
                z[0][i][j] = 0.0;
            }
        }
    }

    //time = 1;
    // calculate the heights of cells at level 1 for t=1
    for (int i = 1 ; i < size-1 ; i++) {
        for (int j = 1 ; j < size-1 ; j++) {
            z[1][i][j] = z[0][i][j] + pow(c,2)/2 * (pow((dt/dd),2)) * (z[0][i+1][j] + z[0][i-1][j] + z[0][i][j+1] + z[0][i][j-1] - 4.0 * z[0][i][j]);
        }
    }

    // find the values of quotients and remainder
    int remainder = size % numOfHosts;
    int quotient =  size / numOfHosts;

    // initialize the values of iStart(start point of i), iEnd(end point of i) and rowsAllocated
    int iStart, iEnd, rowsAllocated;

    iStart = my_rank * quotient;
    iStart += my_rank < remainder ? my_rank : remainder;

    iEnd = (my_rank + 1) * quotient;
    iEnd += my_rank < remainder ? my_rank + 1 : remainder;

    rowsAllocated = quotient;
    rowsAllocated += my_rank < remainder ? 1 : 0;

    //used to print the ranges of all the ranks
    cerr << "rank[" << my_rank << "]'s range = " << iStart << " ~ " << iEnd - 1 << endl;

    iStart += my_rank == 0 ? 1 : 0;
    iEnd -= my_rank == (numOfHosts - 1) ? 1 : 0;

    // we will run schrodingers wave equation parallelly on multiple hosts
    for (int t = 2; t < max_time; ++t ) {

        #pragma omp for schedule(static) 
        for (int i = iStart ; i < iEnd ; i++ ) {
            for (int j = 1 ; j < size - 1 ; j++) {
                z[t%3][i][j] = 2.0 * z[(t-1)%3][i][j]
                                - z[(t-2)%3][i][j]
                                + pow(c,2)*(pow((dt/dd),2))
                                * (
                                   z[(t-1)%3][i+1][j]
                                   + z[(t-1)%3][i-1][j]
                                   + z[(t-1)%3][i][j+1]
                                   + z[(t-1)%3][i][j-1]
                                   - 4.0 * z[(t-1)%3][i][j]
                                  );
            }  
        }

        /*
         * if current host is master then master will calculate
         * the start point and ranks of all the partitions and use the
         * calculated values to findout the height at specific point
         */

        if (my_rank == 0)
        {
            if (alwaysBlockSend || (interval != 0 && t % interval == 0))
            {
                for (int r = 1; r < numOfHosts; r++)
                {
                    int rAllocated = quotient;
                    if (r < remainder) {
                        rAllocated += 1;
                    }
                    int s;
                    if (r < remainder)
                    {
                        s = (r * quotient) + r;
                    }
                    else {
                        s = r * quotient + remainder;
                    }
                    MPI_Recv(&(z[t%3][s][0]), size * rAllocated, MPI_DOUBLE, r, r,  MPI_COMM_WORLD, &status);
                }
            }
                
        }
        /* if current host is a worker host other than master then,
         * the host will send the calculated block of data bact to master
         */
        else
        {
            if (alwaysBlockSend || (interval != 0 && t % interval == 0))
            {
                MPI_Send(&(z[t%3][iStart][0]), size * rowsAllocated, MPI_DOUBLE, 0, my_rank, MPI_COMM_WORLD );
            }
        }

        for (int partition = 0; partition < numOfHosts - 1; partition++)
        {
            /* when rank equals partition, then rank will send 
             * last row in its partition to rank+1, and will receive
             * first row from rank+1
             */
            if (partition == my_rank)
            {
                MPI_Send(&(z[t%3][iEnd-1][0]), size, MPI_DOUBLE, my_rank + 1, my_rank, MPI_COMM_WORLD );

                MPI_Recv(&(z[t%3][iEnd][0]), size, MPI_DOUBLE, my_rank + 1, my_rank + 1,  MPI_COMM_WORLD, &status);
            }

            /* when rank is one less than partition number, then
             * first we will receive last row from rank - 1 and
             * later we will send first row to rank - 1
             */
            if (partition == (my_rank - 1))
            {
                MPI_Recv(&(z[t%3][iStart - 1][0]), size, MPI_DOUBLE, my_rank - 1, my_rank - 1,  MPI_COMM_WORLD, &status);

                MPI_Send(&(z[t%3][iStart][0]), size, MPI_DOUBLE, my_rank - 1, my_rank, MPI_COMM_WORLD );
                
            }
        }

        /* master host will print output only when interval is not 0
         * and when mod of time and interval is 0
         */       
        if (my_rank == 0) {
                if (interval != 0 && t % interval == 0)
                {
                        cout << t << endl;
                        for (int i = 0 ; i < size ; i++) {
                                for(int j = 0; j < size ; j++) {
                                        cout << z[t%3][j][i] << " ";
                                }
                        cout << endl;
                        }
                }
        }
    }
  if(my_rank == 0)
{
    cerr << "Elapsed time = " << time.lap( ) << endl;
}

  MPI_Finalize( ); // Shut down MPI
  return 0;
 } // end of simulation
