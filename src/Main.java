package Main;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {
    
    //how many cities the graph should contain
    private static int CITY_AMOUNT;
    
    //read weights from the adjacency matrix instead of the vertices array
    private static boolean useMatrix;
            
    //adjacency matrix
    private static double[][] adjMatrix;
    
    //vertices array
    private static Point2D.Double[] vertices;
    
    //saved cost of best path found
    private static double bestResult;
    
    public static void main (String[] args) {
        runProcedure();
    }
    
    /**
     * Sample program testing procedure.
     */
    static void runProcedure () {
        System.out.println("Welcome to the TSP solver");
        System.out.println("Which method to use?\n0 - brute force (compare all"
                + " possible paths and find shortest, inefficient but finds "
                + "optimal\n\tpath)\n1 - branch-and-bound (efficient but still"
                + " has limitations, finds\n\toptimal path)\n2 - nearest "
                + "neighbor (starting at any city always travel to closest "
                + "one, fastest but not\n\tlikely to give optimal solution)");
        int method = 3;
        while (method < 0 || method > 2) method = getInteger("Type 0 1 or 2:");
        System.out.println("How many cities to use?\nBrute force method "
                + "can handle only up to 11 before Java runs out of memory.\n"
                + "Branch-and-bound can find a solution within reasonable time"
                + " only up to 18 cities.\nNearest neighbor method doesn't "
                + "have a practical limit.");
        CITY_AMOUNT = 0;
        while (CITY_AMOUNT < 1) 
            CITY_AMOUNT = getInteger("Type the city amount:");
        System.out.println("Read from existing file 'cities' "
                + "(no file extension)?");
        System.out.println("If not, a new file will be generated.");
        boolean read = yesNo("Read cities file?");
        if (read) {
            try {
                vertices = readCities(CITY_AMOUNT);
            } catch (Exception x) {
                System.out.println("Something went wrong, creating a new file"
                        + " instead.");
                read = false;
            }
        }
        if (!read) {
            makeCityFile(CITY_AMOUNT);
            try {
                vertices = readCities(CITY_AMOUNT);
            } catch (Exception x) {
            }
        }
        System.out.println("The cities are:");
        int c = 0;
        for (Point2D.Double p : vertices) {
            System.out.println(c + " at " + p);
            c++;
        }
        long time1 = 0;
        long time2;
        int results[] = null;
        switch (method) {
            case 0:
                time1 = System.currentTimeMillis();
                results = bruteForce();
                break;
            case 1:
                convertToMatrix();
                time1 = System.currentTimeMillis();
                results = branchNBound();
                break;
            case 2:
                time1 = System.currentTimeMillis();
                results = nearestNeighbor();
        }
        time2 = System.currentTimeMillis();
        double time = (time2 - time1) / 1000.0;
        System.out.println("Calculated path " + Arrays.toString(results)
                + " with cost " + bestResult + " in " + time + " seconds.");
        if (yesNo("Go again?")) runProcedure();
    }
    
    static int getInteger (String message) {
        System.out.println(message);
        Scanner scan = new Scanner(System.in);
        int result;
        try {
            result = scan.nextInt();
        } catch (NoSuchElementException x) {
            return getInteger("Please type an integer:");
        }
        return result;
    }
    
    /**
     * Meant to obtain a yes/no answer from the user. This method will get input
     * until the user enters a word starting with either 'y' or 'n'
     *
     * @param prompt a string to print right before pausing to get input
     * @return true if user input begins with 'y', or false if it begins with
     * 'n'
     */
    static boolean yesNo (String prompt) {
        System.out.println(prompt);
        Scanner inScan = new Scanner(System.in);
        String result = inScan.nextLine();
        char firstChar = result.charAt(0);
        if (firstChar == 'y') return true;
        else if (firstChar == 'n') return false;
        else return yesNo("YES or NO ?");
    }
    
    /**
     * Can handle up to 11 cities with worst time 5 seconds. 12 Cities and
     * higher need more space than Java is allocated.
     */
    static int[] bruteForce () {
        LinkedList<int[]> permutations = new LinkedList<>();
        int[] vertices = new int[CITY_AMOUNT - 1];
        for (int c = 0; c < vertices.length; c++) vertices[c] = c + 1;
        int[] bestP = null;
        double bestW = Double.POSITIVE_INFINITY;
        double current;
        for (int[] path : generateAllPermutations(vertices)) {
            current = weight(0, path[0]);
            for (int c = 1; c < path.length; c++) 
                current += weight(path[c - 1], path[c]);
            current += weight(path[path.length - 1], 0);
            if (current < bestW) {
                bestW = current;
                bestP = path;
            }
        }
        int[] results = new int[CITY_AMOUNT + 1];
        results[0] = 0;
        System.arraycopy(bestP, 0, results, 1, bestP.length);
        results[results.length - 1] = 0;
        bestResult = bestW;
        return results;
    }
    
    static int[] branchNBound () {
        if (!useMatrix) {
            convertToMatrix();
        }
        BranchNBoundTSP solver = new BranchNBoundTSP(adjMatrix);
        solver.go();
        bestResult = solver.getCost();
        return solver.getTour();
    }
    
    /**
     * No limit, O(N) efficiency.
     */
    static int[] nearestNeighbor () {
        int start = 0;
        
        int[] path = new int[CITY_AMOUNT + 1];
        boolean[] out = new boolean[CITY_AMOUNT];
        out[start] = true;
        path[0] = start;
        int current = start;
        double best;
        int bestNext = start;
        double sum = 0;
        double weight;
        for (int c1 = 1; c1 < CITY_AMOUNT; c1++) {
            best = Double.POSITIVE_INFINITY;
            for (int c2 = 0; c2 < CITY_AMOUNT; c2++) {
                if (out[c2]) continue;
                weight = weight(current, c2);
                if (best > weight) {
                    best = weight;
                    bestNext = c2;
                }
            }
            sum += best;
            current = bestNext;
            out[current] = true;
            path[c1] = current;
        }
        sum += weight(current, start);
        path[path.length - 1] = start;
        bestResult = sum;
        return path;
    }
    
    static void convertToMatrix () {
        adjMatrix = new double[CITY_AMOUNT][CITY_AMOUNT];
        for (int c1 = 0; c1 < CITY_AMOUNT; c1++) {
            for (int c2 = 0; c2 < CITY_AMOUNT; c2++)
                adjMatrix[c1][c2] = vertices[c1].distance(vertices[c2]);

        }
        useMatrix = true;
    }
    
    static double weight (int v1, int v2) {
        double result;
        if (useMatrix) {
            result = adjMatrix[v1][v2];
        } else {
            result = vertices[v1].distance(vertices[v2]);
        }
        return result;
    }
    
    static List<int[]> generateAllPermutations (int[] elements) {
        List<int[]> results = new LinkedList<>();
        if (elements.length == 1) {
            results.add(elements);
        } else { 
            int[] rest;
            int[] perm;
            for (int c = 0; c < elements.length; c++) {
                rest = new int[elements.length - 1];
                System.arraycopy(elements, 0, rest, 0, c);
                System.arraycopy(elements, c + 1, rest, c, rest.length - c);
                for (int[] r : generateAllPermutations(rest)) {
                    perm = new int[elements.length];
                    perm[0] = elements[c];
                    System.arraycopy(r, 0, perm, 1, r.length);
                    results.add(perm);
                }
            }
        }
       return results;
    }
    
    /**
     * Write a random city data file for 'amount' cities.
     * 
     * @param amount amount of cities to generate
     */
    static void makeCityFile (int amount) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("cities"))) {
            double randomX;
            double randomY;
            while (amount > 0) {
                randomX = randomDouble(-1_000, 1_000, 2);
                randomY = randomDouble(-1_000, 1_000, 2);
                writer.print(randomX + "," + randomY + "\n");
                amount--;
            }
        } catch (IOException x) {
        }
        useMatrix = false;
    }
    
    /**
     * Read cities from file and return as an array of Point2D objects with
     * double precision.
     * 
     * @param amount amount of cities to read, must not be greater than than
     * how many can be found in the file "cities" (no file extension) in the
     * code/jar/program directory
     * 
     * @return all read cities data as an array of 2D points holding doubles
     * data
     */
    static Point2D.Double[] readCities (int amount) throws FileNotFoundException 
    {
        
        Point2D.Double[] results = new Point2D.Double[amount];
        File f = new File("cities");
        Scanner scan = new Scanner(f).useDelimiter("[,\\s]");
        double x;
        double y;
        for (int c = 0; c < amount; c++) {
            x = scan.nextDouble();
            y = scan.nextDouble();
            results[c] = new Point2D.Double(x, y);
        }
        return results;
    }
    
    static double randomDouble (double min, double max, 
            int decimal_places) 
    {
        
        double num = Math.random() * (max - min) + min;
        
        double power = Math.pow(10, decimal_places);
        
        num *= power;
        num = Math.round(num);
        num /= power;
        
        if (num > max) num = Math.floor(max * power) / power;            
        else if (num < min) num = Math.ceil(min * power) / power;
        
        return num;
    }
}
