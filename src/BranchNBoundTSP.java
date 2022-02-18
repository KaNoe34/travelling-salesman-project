package Main;

import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;

public class BranchNBoundTSP {
    
    //adjacency matrix
    private final double[][] ADJ_MATRIX;
    
    //saved cost of best tour found so far
    private double bestWeight;
    
    //the complete tour holding the current best cost
    private int[] bestPath;
    
    /**
     * Creates an object ready to answer the TSP problem on the given adjacency
     * matrix using the branch-and-bound method.
     *
     * @param adjacencyMatrix a 2D array of costs where the first dimension
     * would access the city being traveled from and the second the city being
     * traveled to
     */
    public BranchNBoundTSP (double[][] adjacencyMatrix) {
        ADJ_MATRIX = adjacencyMatrix;
    }
    
    public void go () {
        
        //initialize to a value that guarantees any weight is better than it
        bestWeight = Double.POSITIVE_INFINITY;
        
        //create a TreeSet structure which will function as a priority queue
        //assigning priority based on comparisons done by the below Comparator
        //object where smaller object have higher priority
        TreeSet<Node> q = new TreeSet<>(new Comparator<Node>() {
            
            @Override
            public int compare (Node o1, Node o2) {
                
                //a node is considered smaller if it has a higher level (is
                //deeper down the tree and includes more cities) and higher
                //if it has less but an equal amount doesn't indicate equality
                if (o1.level > o2.level) return -1;
                else if (o1.level < o2.level) return 1;
                
                //a smaller lower bound indicates a smaller object, and a larger
                //one indicates a larger object but same lower bound does not
                //indicate equality
                if (o1.lowerBound < o2.lowerBound) return -1;
                else if (o1.lowerBound > o2.lowerBound) return 1;
                
                double o1Sum = 0.0;
                double o2Sum = 0.0;
                int prev;
                for (int c = 1; c < o1.tour.length; c++) {
                    prev = c - 1;
                    o1Sum += weight(o1.tour[prev], o1.tour[c]);
                    o2Sum += weight(o2.tour[prev], o2.tour[c]);
                }
                if (o1Sum < o2Sum) return -1;
                else if (o1Sum > o2Sum) return 1;
                return 0;
            }
        });
        
        //this will store new paths created by expanding existing partial
        //tours
        int[] aPath;
        
        //initialize first node comprising just vertex 0
        //after that, this will store existing nodes removed from the queue
        Node node = new Node(new int[]{0});
        
        //this will store newly created nodes
        Node newNode;
        
        //add the initial node onto the queue
        q.add(node);
        
        //while the queue is not empty
        while (!q.isEmpty()) {
            
            //remove a node from the front of the queue
            node = q.first();
            q.remove(node);
            
            //if its lower bound is worse than the current best, move onto the
            //next node on the queue
            if (node.lowerBound >= bestWeight) continue;
            
            //for every city on the graph
            for (int c = 1; c < ADJ_MATRIX.length; c++) {
                
                //as long as the city is not already included in the partial
                //tour
                if (node.skip[c]) continue;
                
                //create a new path by appending this city to the existing
                //partial tour in the current node
                aPath = add(node.tour, c);
                
                //create a new node with this path
                newNode = new Node(aPath);
                
                //if this new node has a lower bound less than the best current 
                //full tour cost, this means it can potentially become the next 
                //optimal tour (the lower bound value grows as cities are added
                //onto the tour) so add it onto the queue
                if (newNode.lowerBound < bestWeight) q.add(newNode);
                //don't need to check for full paths, this condition will
                //always fail on a full path, the lowerBound can be equal to
                //but never less than the best weight
            }
        }
    }
    
    private double weight (int city1, int city2) {
        return ADJ_MATRIX[city1][city2];
    }
    
    private int[] add (int[] array, int newE) {
        int[] results = Arrays.copyOf(array, array.length + 1);
        results[array.length] = newE;
        return results;
    }
    
    public int[] getTour () {
        return bestPath;
    }
    
    public double getCost () {
        return bestWeight;
    }
    
    /**
     * Represents a partial tour of the graph and the lowest bound of the tour.
     */
    private class Node {
        
        //array of vertex indexes making up this partial tour, in traversal 
        //order.
        private final int[] tour;
        
        //level at which the node is contained within the tree
        private final int level;
        
        //computed lower bound for this tour
        private final double lowerBound;
        
        private final boolean[] skip;
        
        /**
         * Create a node representing a partial tour comprised of the
         * given vertices and compute its lower bound.
         *
         * @param partialTour array of vertex indexes making up this partial
         * tour, in traversal order.
         */
        public Node (int[] partialTour) {
            
            //tour length represents this node's depth in the tree, with only
            //1 vertex (the beginning vertex) being the root
            level = partialTour.length - 1;
            
            //save the tour
            tour = partialTour;
            
            //initialize lower bound sum
            double bound = 0;
            
            //array of vertices which will not be searched for optimal cost
            //of leaving because they have already been exited from in the 
            //partial tour
            skip = new boolean[ADJ_MATRIX.length];
            
            //if this is not the root of the tree
            if (level != 0) {
                
                //for every vertex except the last
                int limit = tour.length - 1;
                for (int c = 0; c < limit; c++) {
                    
                    //add weight from this vertex to next vertex to the 
                    //lower bound sum
                    bound += weight(tour[c], tour[c + 1]);
                    
                    //the current vertex has already been travelled from
                    skip[tour[c]] = true;
                }
            }
            
            //if this represents a full tour
            if (level == ADJ_MATRIX.length - 1) {
                
                //add the weight from the last vertex to vertex 0
                bound += weight(tour[tour.length - 1], 0);
                
                //if this is found to be the new best bound
                if (bound < bestWeight) {
                    
                    //save it
                    bestWeight = bound;
                    
                    //create array for new best path
                    int[] temp = new int[ADJ_MATRIX.length + 1];
                    
                    //copy the tour into it
                    System.arraycopy(tour, 0, temp, 0, tour.length);
                    
                    //add vertex 0 to the end
                    temp[temp.length - 1] = 0;
                    
                    //save the best path
                    bestPath = temp;
                    
                    //big question on my mind - Will two full tour nodes ever
                    //be compared? And if so, should the lowerBound of those
                    //include the weight of the return back to vertex 0 or not?
                    
                }
                    
                //this represents a partial tour with less vertices than
                //then the full amount of cities
            } else {
            
                //will contain lowest found cost of leaving a vertex
                double best;
            
                //for all vertices c1 of the graph
                for (int c1 = 0; c1 < ADJ_MATRIX.length; c1++) {

                    //if vertex c1 has been exited from do not find its lowest
                    //cost of leaving, the weight of leaving is already known 
                    //and included in the lower bound sum
                    if (skip[c1]) continue;

                    //start of with a value for best that ensures any cost is 
                    //better than it
                    best = Double.POSITIVE_INFINITY;

                    //once again, for all vertices c2 of the graph
                    for (int c2 = 0; c2 < ADJ_MATRIX.length; c2++) {

                        //if c2 is already in the tour or same as c1
                        //do not consider weight from c1 to c2
                        if (skip[c2] || c1 == c2) continue;

                        //else save the lower (current best or weight from c1 to
                        //c2)
                        best = Math.min(best, weight(c1,c2));
                    }

                    //after optimal cost of leaving for vertex c1 has been 
                    //found, add it to the lower bound sum
                    bound += best;
                }
            }
            //lower bound has been found
            lowerBound = bound;
            
            //after finding the lower bound mark the last vertex on this partial
            //tour to be skipped (the skip array will be reused to check which
            //vertices already exist in a given node's path)
            skip[tour[tour.length - 1]] = true;
        }
    }
}

