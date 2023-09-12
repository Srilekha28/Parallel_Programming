# Parallel_Programming
I have created this repository to keep track of the projects that i am doing in the field of parallel progarmming.

**Travelling Sales Person (TSP)**

Traveling Salesman Problem is a classic optimization problem in the field of computer science and mathematics. It can be described as :

Given a list of cities and the distances between each pair of cities, the problem is to find the shortest possible route that visits each city exactly once and returns to the original city (the salesman's starting point).

In mathematical terms, if there are N cities, there are N factorial (N!) possible routes to consider. As N increases, the number of possible routes grows exponentially, making the problem computationally challenging for a large number of cities.

Traveling Salesman Problem has important applications in various real-world scenarios beyond just salespeople, such as logistics, transportation planning, and circuit design. Finding an optimal solution to the TSP is a computationally complex task, and various algorithms and heuristics have been developed to approximate solutions for large instances of the problem.

To tackle this problem at hand i have decided to parallelize the execution of finding paths on multiple servers using "OpenMp" and below is the strategy that i have applied:

**Parallelization Strategy:**
GA based TSP algorithm for finding a reasonable travel path involves,
1. Finding shortest path from 50k paths
2. Using genetics algorithms and generating half new paths from shortest 25k paths from previous step
3. Randomly mutating new paths (we call off-springs)
4. Repeating from step 1 to step 3 for 150 times

Every GA based algorithm eassentially follows 3 steps : _Evaluate, CrossOver and Mutation_.
Overall, in the algorithm, finding shortest paths (evaluate method) and creating new off- springs (crossover method) are computation intensive operations. I have used various OpenMP parallelization techniques to reduce the runtime depending on the number of the threads used.

**Evaluate method:**
In this method I have calculated distance for every itinerary in chromosomes.txt file. To do this method I have implemented parallelization over the for loop which is used to calculate distance of every trip. I implemented parallelization because calculating distance of each trip is not dependent on other trips. So, dividing the itineraries among different threads will reduce the computational time complexity by calculating the distance parallelly on multiple threads.
During distance calculation for each trip, first 25K trips from generation 2-150 will already have distances calculated. Adding check before calculating distance would improve runtime of TSA algorithm. But it reduces single thread runtime more than multi-thread runtime, consequently overall performance ratio i.e. (single thread runtime) to (multi-thread (4) runtime) reduces.
After calculating distance for all itineraries, we should sort them in ascending order of their total distances. To do this, If I were to use a traditional sorting functionality provided by library, I cannot make use of OpenMP parallelization features. So, I have implemented a custom sorting algorithm. Algorithm starts with dividing total trips array into num_of_threads contiguous blocks and sorting them in place independently. We would be sorting all blocks in parallel using ` #pragma omp parallel`. Instead of incurring O(n*log(n)) time complexity, we would be incurring O(n*log(n)/num_of_threads).
In the second step, algorithm merges already sorted blocks. This is also parallelized using `#pragma omp parallel for` as there is no overlap between merge operation of blocks in distinct places in array. Here merge operation at each level should take approximately take O(level * n/num_of_threads). With large enough number of threads, overall merge operation would tend towards O(n).
   
Consider if there were 6 threads then there would be 6 blocks and will be merged in the following order (without parallelization): Bi - represents block i
• Level 1: (B1, B2), (B3, B4) and (B5, B6) will be merged in parallel in the first step
• Level 2: B1B2 will be merged with B3B4, and in the next step B1B2B3B4 will be
merged with B5B6, which is where the sorting ends.
Theoretically with large enough number of threads, algorithm should take O(n) timeJ

**Crossover method:**
We started cross-over method by taking the top 25k itineraries with shortest distances which are now known as parents. For each parent [i] and [i+1] I have generated a new offsprings [i] and [i+1] using genetic cross over algorithm. To be able to parallelize off-springs generation by using `#pragma omp parallel for`, I have structured for loop using loop fusion technique. Each loop, starts by creating an inversion index search array to search for a city location in a trip in O(1) time. Initializes child[i]’s first city as parent[i]’s first city. From here on, I took leaving cities in both parents, picked the ones with shorter distance to extend child[i]’s trip. If one of the cities is already present in itinerary, we would pick remaining one. Incase if both are picked, I would sequentially find first unassigned city and use that to extend child[i]’s trip. Child[i+1]’s trip is calculated as complement to child[i]’s trip.
Clearly there is no overlap between child[i] and child[i+1] generation to child[i+2] and child[i+3] so on and so forth, I have parallelized entire master for loop of cross-over method.

**Mutate method:**
 In this method I implemented parallelization using OpenMP on the for loop and I have mutated the offsprings itinerary by randomly swapping two cities. Using rand() method, generated random integers and taking modulo of 100, converted into percentages. Checking these random number against MUTATE_RATE to select off-springs to be mutated. Mutation rate of roughly 50 on mac and ~30 on linux helped in reaching optimal paths.

 Overall by following the above strategies i was able to achieve a performance improvement of over 4x on 4 threads.
 i have attached the outputs for parallelized version in the path: https://github.com/Srilekha28/Parallel_Programming/tree/main/TSP/PP%20Assignment1%20Outputs 
