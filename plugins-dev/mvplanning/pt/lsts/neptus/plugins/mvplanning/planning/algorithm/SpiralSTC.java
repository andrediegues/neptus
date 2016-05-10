/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: tsmarques
 * 6 May 2016
 */
package pt.lsts.neptus.plugins.mvplanning.planning.algorithm;

import java.util.ArrayList;
import java.util.List;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.plugins.mvplanning.interfaces.MapCell;
import pt.lsts.neptus.plugins.mvplanning.planning.GridCell;
import pt.lsts.neptus.plugins.mvplanning.planning.mapdecomposition.GridArea;
import pt.lsts.neptus.types.mission.GraphType;
import pt.lsts.neptus.types.mission.TransitionType;

/**
 * Implementation of the offline version of the
 * SpiralSTC's algorithm
 * @author tsmarques
 *
 */
public class SpiralSTC {
    private static final int UP = 1;
    private static final int DOWN = -1;
    private static final int LEFT = 2;
    private static final int RIGHT = -2;

    private GraphType graph;
    private MST minSpanningTree;

    public SpiralSTC(GridArea areaToCover) {
        this.minSpanningTree = new MST(areaToCover.getAreaCells().get(30));
        this.graph = generatePath(areaToCover);
    }

    public GraphType getGraph() {
        return graph;
    }

    private GraphType generatePath(GridArea areaToCover) {
        /* Build graph */
        GraphType planGraph = new GraphType();
        GridArea subCells = areaToCover.splitMegaCells();

        GridCell previousSubCell = null;
        GridCell previousCell = null;
        int previousDirection = 0;
        boolean firstNode = true;
        for(MapCell node : minSpanningTree.getNodeSequence()) {
            Goto newNode = new Goto();
            if(firstNode) {
                GridCell startSubCell = computeStartSubCell((GridCell) node, minSpanningTree, subCells);

                newNode.setId(startSubCell.id());
                newNode.setManeuverLocation(new ManeuverLocation(startSubCell.getLocation()));
                newNode.setInitialManeuver(true);
                planGraph.addManeuver(newNode);

                firstNode = false;
                previousSubCell = startSubCell;
                previousCell = (GridCell) node;
            }
            else {
                if(!node.id().equals(minSpanningTree.startCell().id())) {
                    /* Direction from previous mega-cell to the current one */
                    int nextDir = getNextDirection(previousCell, (GridCell) node);
                    /* Based on new direction compute the new subcell to move into */
                    GridCell nextSubCell = computeNewTransition(planGraph, nextDir, previousDirection, previousSubCell, previousCell, (GridCell) node, subCells);

                    if(planGraph.getManeuver(nextSubCell.id()) == null) {
                        newNode.setId(nextSubCell.id());
                        newNode.setManeuverLocation(new ManeuverLocation(nextSubCell.getLocation()));
                        planGraph.addManeuver(newNode);
                    }

                    planGraph.addTransition(new TransitionType(previousSubCell.id(), nextSubCell.id()));
                    previousDirection = nextDir;
                    previousSubCell = nextSubCell;
                    previousCell = (GridCell) node;
                }
            }
        }
        return planGraph;
    }

    /**
     * Given an initial mega-cell compute the sub-cell inside it
     * where the vehicle will start the plan
     * */
    private GridCell computeStartSubCell(GridCell startCell, MST minSpanningTree, GridArea subCells) {
        int i = startCell.getRow();
        int j = startCell.getColumn();

        GridCell nextCell = (GridCell) minSpanningTree.getNodeSequence().get(1);
        int movementDirection = getNextDirection(startCell, nextCell);

        int subCellRow;
        int subCellCol;
        /* move to the bottom-right of the start cell */
        if(movementDirection == UP) {
            subCellRow = 2*i + 1;
            subCellCol = 2*j + 1;
        }
        /* move to the top-right of the start cell */
        else if(movementDirection == LEFT) {
            subCellRow = 2*i;
            subCellCol = 2*j + 1;
        }
        /* move to the top-left of the start cell */
        else if(movementDirection == DOWN) {
            subCellRow = 2*i;
            subCellCol = 2*j;
        }
        /* move to the bottom-left of the start cell */
        else if(movementDirection == RIGHT) {
            subCellRow = 2*i + 1;
            subCellCol = 2*j;
        }
        else {
            NeptusLog.pub().error("Couldn't compute first sub-cell");
            return null;
        }

        return subCells.getAllCells()[subCellRow][subCellCol];
    }

    /**
     * Computes the next subcell(s) the vehicle is going to move into
     * */
    public GridCell computeNewTransition(GraphType path, int nextDir, int prevDir, GridCell currSubCell, GridCell sourceMegaCell, GridCell destMegaCell, GridArea subCells) {
        if(nextDir == 0) {
            NeptusLog.pub().error("Couldn't compute direction from " + sourceMegaCell.id() + " to " + destMegaCell.id());

            return null;
        }
        else {
            /* Going back to the previous node */
            if(nextDir == -prevDir)
               return goAroundNode(path, nextDir, currSubCell, subCells);
            else {
                int i = destMegaCell.getRow();
                int j = destMegaCell.getColumn();

                int subCellRow;
                int subCellCol;
                /* move to the bottom-right of new subcell*/
                if(nextDir == UP) {
                    subCellRow = 2*i + 1;
                    subCellCol = 2*j + 1;
                }
                /* move to the top-right of the new subcell */
                else if(nextDir == LEFT) {
                    subCellRow = 2*i;
                    subCellCol = 2*j + 1;
                }
                /* move to the top-left of the new subcell */
                else if(nextDir == DOWN) {
                    subCellRow = 2*i;
                    subCellCol = 2*j;
                }
                /* move to the bottom-left of the new subcell */
                else if(nextDir == RIGHT) {
                    subCellRow = 2*i + 1;
                    subCellCol = 2*j;
                }
                else
                    return null;

                return (GridCell) subCells.getAllCells()[subCellRow][subCellCol];
            }
        }
    }


    /**
     * Generates a path, i.e. sequence of transitions,
     * that goes around a spanning tree's leaf node and
     * returns the last sub cell the vehicle will be in
     * after this move.
     * */
    private GridCell goAroundNode(GraphType path, int nextDir, GridCell currSubCell, GridArea subCells) {
        List<GridCell> nodesSequence;
        int currRow = currSubCell.getRow();
        int currCol = currSubCell.getColumn();

        if(nextDir == UP)
            nodesSequence = goAroundDown(currRow, currCol, subCells);
        else if(nextDir == DOWN)
            nodesSequence = goAroundUp(currRow, currCol, subCells);
        else if(nextDir == LEFT)
            nodesSequence = goAroundRight(currRow, currCol, subCells);
        else if(nextDir == RIGHT)
            nodesSequence = goAroundLeft(currRow, currCol, subCells);
        else {
            NeptusLog.pub().error("Can't go around leaf node from " + currSubCell.id() + " sub-cell");
            return null;
        }

        /* Add paths between computed nodes */
        for(int i = 1; i < nodesSequence.size(); i++) {
            MapCell source = nodesSequence.get(i-1);
            MapCell dest = nodesSequence.get(i);

            if(path.getManeuver(source.id()) == null) {
                Goto newNode = new Goto();
                newNode.setId(source.id());
                newNode.setManeuverLocation(new ManeuverLocation(source.getLocation()));
                path.addManeuver(newNode);
            }

            if(path.getManeuver(dest.id()) == null) {
                Goto newNode = new Goto();
                newNode.setId(dest.id());
                newNode.setManeuverLocation(new ManeuverLocation(dest.getLocation()));
                path.addManeuver(newNode);
            }

            path.addTransition(new TransitionType(source.id(), dest.id()));
        }

        /* return last cell the vehicles's move into */
        return nodesSequence.get(nodesSequence.size()-1);
    }

    private List<GridCell> goAroundUp(int currRow, int currCol, GridArea subCells) {
        GridCell[][] cells = subCells.getAllCells();
        List<GridCell> nodesSequence = new ArrayList<>();
        /* Move one sub-cell to the up */
        currRow--;
        nodesSequence.add(cells[currRow][currCol]);

        /* Then, move one sub-cell left */
        currCol--;
        nodesSequence.add(cells[currRow][currCol]);

        /* Then, move 2 sub-cells down */
        currRow++;
        nodesSequence.add(cells[currRow][currCol]);
        currRow++;
        nodesSequence.add(cells[currRow][currCol]);

        return nodesSequence;
    }

    private List<GridCell> goAroundDown(int currRow, int currCol, GridArea subCells) {
        GridCell[][] cells = subCells.getAllCells();
        List<GridCell> nodesSequence = new ArrayList<>();
        /* Move one sub-cell to the down */
        currRow++;
        nodesSequence.add(cells[currRow][currCol]);

        /* Then, move one sub-cell right */
        currCol++;
        nodesSequence.add(cells[currRow][currCol]);

        /* Then, move 2 sub-cells up */
        currRow--;
        nodesSequence.add(cells[currRow][currCol]);
        currRow--;
        nodesSequence.add(cells[currRow][currCol]);

        return nodesSequence;
    }

    private List<GridCell> goAroundLeft(int currRow, int currCol, GridArea subCells) {
        GridCell[][] cells = subCells.getAllCells();
        List<GridCell> nodesSequence = new ArrayList<>();
        /* Move one sub-cell to the left */
        currCol--;
        nodesSequence.add(cells[currRow][currCol]);

        /* Then, move one sub-cell down */
        currRow++;
        nodesSequence.add(cells[currRow][currCol]);

        /* Then, move 2 sub-cells right */
        currCol++;
        nodesSequence.add(cells[currRow][currCol]);
        currCol++;
        nodesSequence.add(cells[currRow][currCol]);

        return nodesSequence;
    }

    private List<GridCell> goAroundRight(int currRow, int currCol, GridArea subCells) {
        GridCell[][] cells = subCells.getAllCells();
        List<GridCell> nodesSequence = new ArrayList<>();

        /* Move one sub-cell to the right */
        currCol++;
        nodesSequence.add(cells[currRow][currCol]);

        /* Then, move one sub-cell up */
        currRow--;
        nodesSequence.add(cells[currRow][currCol]);

        /* Then, move 2 sub-cells left */
        currCol--;
        nodesSequence.add(cells[currRow][currCol]);

        currCol--;
        nodesSequence.add(cells[currRow][currCol]);

        return nodesSequence;
    }

    /**
     * Given 2 cells, the start, A, and goal, B, returns in
     * which direction the vehicle is going to travel in order to
     * go from A to B.
     * 1 is Up
     * -1 is Down
     * 2 is Left
     * -2 is Right
     * */
    private int getNextDirection(GridCell currentMegaCell, GridCell nextMegaCell) {
        int currentRow = currentMegaCell.getRow();
        int nextRow = nextMegaCell.getRow();
        int currentCol = currentMegaCell.getColumn();
        int nextCol = nextMegaCell.getColumn();

        /* Going up */
        if(nextRow == (currentRow - 1) && nextCol == currentCol)
            return UP;

        /* Going down */
        else if(nextRow == (currentRow + 1) && nextCol == currentCol)
            return DOWN;

        /* Going left */
        else if(nextCol == (currentCol - 1) && nextRow == currentRow)
            return LEFT;

        /* Going right */
        else if(nextCol == (currentCol + 1) && nextRow == currentRow)
            return RIGHT;
        else
            return 0;
    }
}