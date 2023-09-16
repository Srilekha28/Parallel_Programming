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
**Paralallelied the program using OpenMP**
Overall, in the algorithm, finding shortest paths (evaluate method) and creating new off- springs (crossover method) are computation intensive operations. I have used various OpenMP parallelization techniques to reduce the runtime depending on the number of the threads used.

 Overall by following the above strategy i was able to achieve a performance improvement of over 4x on 4 threads.
 i have attached the outputs for parallelized version in the path: https://github.com/Srilekha28/Parallel_Programming/tree/main/TSP/PP%20Assignment1%20Outputs 

 **Wave2D**
 
 Created a Wave2D simulation using MPI on multiple remote hosts to showcase the speedup compared to sequential exection.

 **Word Count**
 
 This project intends to replicate the idea behind how google search works when we enter the data how google searches for related document in the backend.

 So i have utilized **Map Reduce** to speed up the search process .

 I have used Mapper, Reducer, Combiner and Partitioner to parallelize the search on **24 remote hosts.**

 **Breadth Fist Search**
 
Implemented it using Dijkstras algorithm and parallelized the search process using **Spark** specific transformations like **JavaRDD** and flatmap. 
