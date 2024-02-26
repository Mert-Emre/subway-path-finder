// Program reads a file to obtain information about metro lines and their stations.
// It uses a recursive way to find the path between two given stations.
// Function starts from source station and tries to go both forward and backward in the same metro line.
// When it comes to a breakpoint, first, it transfers to the adjacent stations and checks them.
// If it can't find the destination in these metro lines, it turns to the breakpoint and continue from the same line.
// Detailed explanations are given in the relevant parts of the code.

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.awt.Color;
import java.awt.Font;

public class Main {
    public static void main(String[] args) {
        try {
            File metroFile = new File("input.txt");
            Scanner scanner = new Scanner(metroFile);
            String[] metroLines = new String[10];
            int[][] metroColors = new int[10][3];
            String[][] stationNames = new String[10][];
            double[][][] stationCoords = new double[10][][];
            String[][] breakpoints = new String[7][];
            boolean[][] stationsVisited = new boolean[10][];

            // Using counters to differentiate breakpoints and metro lines
            int metroCounter = 0;
            int bpCounter = 0;
            while (scanner.hasNext()) {
                // If there are still metro lines that haven't been read, read them into
                // metroLines
                if (metroCounter < 10) {
                    String line = scanner.nextLine();
                    String[] lineSplit = line.split(" ");
                    metroLines[metroCounter] = lineSplit[0];
                    String[] colorArray = lineSplit[1].split(",");
                    // r,g,b values of a specific metro line
                    metroColors[metroCounter][0] = Integer.parseInt(colorArray[0]);
                    metroColors[metroCounter][1] = Integer.parseInt(colorArray[1]);
                    metroColors[metroCounter][2] = Integer.parseInt(colorArray[2]);
                    String[] stationsLine = scanner.nextLine().split(" ");
                    // Creating temporary arrays in order to use ragged arrays to prevent redundant
                    // null items
                    String[] tempStations = new String[stationsLine.length / 2];
                    boolean[] tempVisited = new boolean[stationsLine.length / 2];
                    double[][] tempCoords = new double[stationsLine.length / 2][2];
                    for (int i = 0; i < stationsLine.length; i += 2) {
                        // i corresponds to station name and i+1 corresponds to the coordinates of this
                        // station
                        tempStations[i / 2] = stationsLine[i];
                        tempVisited[i / 2] = false;
                        String[] coordsSplit = stationsLine[i + 1].split(",");
                        tempCoords[i / 2][0] = Double.parseDouble(coordsSplit[0]);
                        tempCoords[i / 2][1] = Double.parseDouble(coordsSplit[1]);
                    }
                    stationNames[metroCounter] = tempStations;
                    stationsVisited[metroCounter] = tempVisited;
                    stationCoords[metroCounter] = tempCoords;
                    metroCounter++;
                } else if (bpCounter < 7) {
                    // All metro lines have been read now it is time to read breakpoints
                    String line = scanner.nextLine();
                    String[] lineSplit = line.split(" ");
                    String[] tempBreakPoints = new String[lineSplit.length];
                    for (int i = 0; i < lineSplit.length; i++) {
                        tempBreakPoints[i] = lineSplit[i];
                    }
                    breakpoints[bpCounter] = tempBreakPoints;
                    bpCounter++;
                }
            }
            scanner.close();

            // Getting inputs fromm user
            Scanner userInput = new Scanner(System.in);
            String firstStation = userInput.next();
            String lastStation = userInput.next();
            userInput.close();

            // Searching the user inputs in the arrays of station names if it can't be
            // found, the program prints a message to the user and finishes
            // fs: first station, ls: last station
            int fsLineIndex = -1;
            int fsStationIndex = -1;
            int lsLineIndex = -1;
            for (int i = 0; i < stationNames.length; i++) {
                for (int j = 0; j < stationNames[i].length; j++) {
                    if (stationNames[i][j].equals(firstStation) || stationNames[i][j].equals("*" + firstStation)) {
                        fsLineIndex = i;
                        fsStationIndex = j;
                    }
                    if (stationNames[i][j].equals(lastStation) || stationNames[i][j].equals("*" + lastStation)) {
                        lsLineIndex = i;
                    }
                }
            }

            if (fsLineIndex == -1 || lsLineIndex == -1) {
                System.out.println("The station names provided are not present in this map.");
                return;
            }

            // Try to find a path if it doesn't exist print a message to the user and finish
            // the program
            String path = findPath(metroLines, stationNames, breakpoints, lastStation, fsLineIndex,
                    fsStationIndex,
                    0,
                    "", null);
            if (path == null) {
                System.out.println("These two stations are not connected");
                return;
            }
            // If a path exists, print this path to the user
            String[] pathArray = path.split(" ");
            for (int i = 0; i < pathArray.length; i++) {
                System.out.println(pathArray[i].split("/")[0]);
            }

            // Setting up a canvas for animation of the path
            int canvasWidth = 1024;
            int canvasHeight = 482;
            StdDraw.setCanvasSize(canvasWidth, canvasHeight);
            StdDraw.setXscale(0, 1);
            StdDraw.setYscale(0, 1);
            StdDraw.enableDoubleBuffering();
            drawAnimation(pathArray, stationCoords, metroColors, stationNames, stationsVisited, canvasWidth,
                    canvasHeight);

        } catch (FileNotFoundException e) {
            System.out.println("File is not found.");
        }
    }

    // This function takes 10 arguments first one is the array of lineNames and it
    // is something like ["M1","M2",...]

    // Second argument is multi dimensional array of station names, i.e.
    // [["Yenikapi, Gebze",...],["Levent","Uskudar",...],...]

    // Third argument is the coordinates of a station corresponding to an entity
    // with same indexes in station names array

    // Fourth argument is the array of breakpoints

    // Fifth argument is the destination given by the user

    // Sixth argument represents the index of the current line for our imaginary
    // train

    // Seventh argument represents the index of the current station for our
    // imaginary train

    // Eighth argument represents the direction of the train, if it is positive,
    // train will visit the station with index number stInd + 1. If it is negative,
    // train will visit the station with index number stInd - 1. This argument
    // prevents infinite loop and stack overflow by avoiding to visit same stations
    // again and again.

    // Ninth argument is a string that is used in breakpoint stations. If train
    // enters a new line in a breakpoint but can't reach to destination, by using
    // this argument, train avoids entering the same line again. Its form is like
    // "384" and it means that train has visited the lines with index 3,8 and 4.

    // Tenth argument is a string and it avoids repeating stations in the result.
    // Since breakpoint stations are entities of multiple metro lines, if path
    // includes transfers, breakpoint stations appears twice in the console output.
    // This argument is required to prevent duplicate station names at breakpoints.
    public static String findPath(String[] lineNames, String[][] sNames, String[][] bp,
                                  String dest, int lineInd,
                                  int stInd, int direction, String formerLine, String formerStation) {

        // If train passed the terminal stations (first or last), it means that this
        // line
        // doesn't include destination station so return null
        if (lineInd < 0 || stInd < 0 || lineInd >= sNames.length || stInd >= sNames[lineInd].length) {
            return null;
        }
        // Even null values are not expected in station names, it is an extra check
        if (sNames[lineInd][stInd] == null) {
            return null;
        }

        // Current station corresponds to a station like "Yenikapi"
        // Take current station name out from station names array, if it starts with an
        // "*" remove it, because it is just for drawing purposes
        String currentStation = null;
        String currentIndexes = lineInd + "/" + stInd;
        if (sNames[lineInd][stInd].charAt(0) == '*') {
            currentStation = sNames[lineInd][stInd].substring(1);
        } else {
            currentStation = sNames[lineInd][stInd];
        }

        // If current station is the same with destination, we reached our goal and
        // return the name of this station and stop recursion
        if (dest.equals(currentStation) || ("*" + dest).equals(currentStation)) {
            return currentStation + "/" + currentIndexes;
        }

        // Check for if the current station is a breakpoint.
        // An example of an element of breakpoint array is ["Otogar","M1A","M1B"]
        for (int i = 0; i < bp.length; i++) {
            if (currentStation.equals(bp[i][0]) || currentStation.equals("*" + bp[i][0])) {
                // If it is a breakpoint
                for (int j = 1; j < bp[i].length; j++) {
                    for (int k = 0; k < lineNames.length; k++) {
                        // bp[i][j] is a name of a line, i.e. "M1A"
                        // lineNames[k] is also a name of a line, i.e "M1A"

                        // First condition gives us the line indexes of the lines that pass through the
                        // current breakpoint
                        // Second condition ensures that the train is not going to try to transfer to
                        // the current line
                        // Third condition ensures that the train is not going to visit a line which is
                        // visited before
                        // formerLine is something like "384" and it means that train has visited the
                        // lines with index 3,8 and 4.
                        if (bp[i][j].equals(lineNames[k]) && k != lineInd
                                && formerLine.indexOf(Integer.toString(k)) == -1) {
                            // If we passed this check, this means we found a line that passes through
                            // the breakpoint and is not visited before
                            int otherLineInd = 0;
                            // With this for loop we find the station index of the breakpoint station in
                            // this new unvisited line. Since a station can be the first station of one line
                            // and seventh station of another line we need that information
                            for (int z = 0; z < sNames[k].length; z++) {
                                if (bp[i][0].equals(sNames[k][z]) || ("*" + bp[i][0]).equals(sNames[k][z])) {
                                    otherLineInd = z;
                                    break;
                                }
                            }
                            // We will transfer the train to a line which is not visited before, and we
                            // adjust the line and station indexes for new line
                            // When the train is transferred to a new station, at first call it will not
                            // have a direction and it will try to go both forward and backward in new line.
                            String otherLine = findPath(lineNames, sNames, bp, dest, k, otherLineInd, 0,
                                    formerLine + lineInd, sNames[lineInd][stInd]);
                            if (otherLine != null) {
                                return otherLine;
                            }
                            // If we can't reach to the destination by visiting other lines passes through
                            // breakpoint, we continue to our way in the same line(which made us reach the
                            // breakpoint) and with the same direction that we had before.
                            String sameLine = findPath(lineNames, sNames, bp, dest, lineInd,
                                    stInd + direction,
                                    direction, formerLine + k, sNames[lineInd][stInd]);
                            // If the destination is found by continuing to the same line return the path
                            // but prevent station name duplicates caused by breakpoints
                            if (sameLine != null) {
                                if (formerStation.equals(currentStation)
                                        || formerStation.equals("*" + currentStation)) {
                                    return sameLine;
                                }
                                return currentStation + "/" + currentIndexes + " " + sameLine;
                            }
                            // If the destination is not found return null
                            return null;
                        }
                    }
                }
            }
        }

        // If it is the first time the function is called or the train transferred to a
        // new
        // line, then at the beginning it will not have a direction and will try to send
        // one train to forward(+1 index the same line) and one train to negative(-1
        // index in the same line) direction
        // from breakpoint
        if (direction == 0) {
            String goBack = findPath(lineNames, sNames,bp, dest, lineInd, stInd - 1, -1, formerLine,
                    currentStation);

            String goForward = findPath(lineNames, sNames, bp, dest, lineInd, stInd + 1, 1, formerLine,
                    currentStation);

            // If one of the trains found the destination, then return the current station
            // with the returned path from recursive function
            if (goBack != null) {
                return currentStation + "/" + currentIndexes + " " + goBack;
            }
            if (goForward != null) {
                return currentStation + "/" + currentIndexes + " " + goForward;
            }
            return null;
        }

        // If the function is called before and the train already has a direction move
        // to the next station in this direction
        String move = findPath(lineNames, sNames,bp, dest, lineInd, stInd + direction, direction, formerLine,
                currentStation);
        if (move != null) {
            return currentStation + "/" + currentIndexes + " " + move;
        }
        // If none of the trains were able to find a path to the destination, return
        // null
        return null;
    }

    // First argument is the array of station coordinates
    // Second argument is the array that contains the r,g,b values of a metro line
    // Third argument is the array of station names
    // Fourth argument is used for coloring the stations in the path
    public static void drawMap(double[][][] stationCoords, int[][] metroColors, String[][] stationNames,
                               boolean[][] stationVisited, int canvasWidth, int canvasHeight) {
        StdDraw.clear();
        StdDraw.picture(0.5, 0.5, "background.jpg");
        StdDraw.setFont(new Font("Helvatica", Font.BOLD, 8));
        // Iterate through the coordinates of evey station
        for (int lineIndex = 0; lineIndex < stationCoords.length; lineIndex++) {
            for (int stationIndex = 0; stationIndex < stationCoords[lineIndex].length; stationIndex++) {
                // Extracting the station coordinates for drawing
                double x1 = stationCoords[lineIndex][stationIndex][0] / canvasWidth;
                double y1 = stationCoords[lineIndex][stationIndex][1] / canvasHeight;
                // If there is a station in front of the current station, then there should be a
                // line in specified color between these two stations
                if ((stationIndex + 1 < stationCoords[lineIndex].length)) {
                    // Extracting the destination coordinates and the color of line;
                    double x2 = stationCoords[lineIndex][stationIndex + 1][0] / canvasWidth;
                    double y2 = stationCoords[lineIndex][stationIndex + 1][1] / canvasHeight;
                    int lineR = metroColors[lineIndex][0];
                    int lineG = metroColors[lineIndex][1];
                    int lineB = metroColors[lineIndex][2];
                    StdDraw.setPenColor(new Color(lineR, lineG, lineB));
                    StdDraw.setPenRadius(0.012);
                    StdDraw.line(x1, y1, x2, y2);
                }

                // Draw the circle of the current station and if the station name contains an
                // "*" also write the name of it
                StdDraw.setPenColor(StdDraw.WHITE);
                StdDraw.setPenRadius(0.01);
                StdDraw.point(x1, y1);
                StdDraw.setPenColor(StdDraw.BLACK);
                if (stationNames[lineIndex][stationIndex].charAt(0) == '*') {
                    StdDraw.text(x1, y1 + 5.0 / canvasHeight, stationNames[lineIndex][stationIndex].split("\\*")[1]);
                }
            }
        }

        // Color the stations which are in the path and visited in the animation in
        // previous frames
        StdDraw.setPenColor(StdDraw.PRINCETON_ORANGE);
        StdDraw.setPenRadius(0.01);
        for (int i = 0; i < stationVisited.length; i++) {
            for (int j = 0; j < stationVisited[i].length; j++) {
                if (stationVisited[i][j]) {
                    double x = stationCoords[i][j][0] / canvasWidth;
                    double y = stationCoords[i][j][1] / canvasHeight;
                    StdDraw.point(x, y);
                }
            }
        }
    }

    // First argument is the path found by findPath function
    // Second argument is the array of station coordinates
    // Third argument is the array that contains the r,g,b values of a metro line
    // Fourth argument is the array of station names
    // Fifth argument is used for coloring the stations in the path
    public static void drawAnimation(String[] path, double[][][] stationCoords, int[][] metroColors,
                                     String[][] stationNames, boolean[][] stationVisited, int canvasWidth, int canvasHeight) {
        for (String station : path) {
            // First we draw the map, after that we can draw big orange circle for animation
            drawMap(stationCoords, metroColors, stationNames, stationVisited, canvasWidth, canvasHeight);
            StdDraw.setPenColor(StdDraw.PRINCETON_ORANGE);
            StdDraw.setPenRadius(0.02);
            // Stations are in the form of stationName/lineIndex/stationIndex
            // i.e. Yenikapi/3/0
            String[] stationSplit = station.split("/");
            int lineIndex = Integer.parseInt(stationSplit[1]);
            int stationIndex = Integer.parseInt(stationSplit[2]);
            double x = stationCoords[lineIndex][stationIndex][0] / canvasWidth;
            double y = stationCoords[lineIndex][stationIndex][1] / canvasHeight;
            // Draw a bigger circle in the current station of animation.
            StdDraw.point(x, y);
            stationVisited[lineIndex][stationIndex] = true;
            StdDraw.show();
            StdDraw.pause(300);
        }
    }

}
