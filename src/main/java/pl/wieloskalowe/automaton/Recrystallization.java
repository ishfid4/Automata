package pl.wieloskalowe.automaton;

import javafx.scene.paint.Color;
import pl.wieloskalowe.Board2D;
import pl.wieloskalowe.CoordinatesWrapper;
import pl.wieloskalowe.cell.Cell;
import pl.wieloskalowe.cell.CellCoordinates;
import pl.wieloskalowe.cell.CellGrain;
import pl.wieloskalowe.neighborhoods.Neighborhood;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by ishfi on 22.05.2017.
 */
public class Recrystallization extends NaiveGrainGrow {
    private double iteration = 0;
    private double criticalIteration = 65; //for 300x300 board
    double criticalRo = roFunction(65) / (100 * 100);  //for 300x300 board
    private final double a = 86710969050178.5;
    private final double b = 9.41268203527779;
    //it should be: If bigger k -> faster new crystal grow
    private final double k = 100; //WTF coefficient

    public Recrystallization(Board2D board2D, Neighborhood neighborhood) {
        super(board2D, neighborhood);
    }

    public Recrystallization(Board2D board2D, Neighborhood neighborhood, CoordinatesWrapper coordinatesWrapper) {
        super(board2D, neighborhood, coordinatesWrapper);
    }

    private Cell getNextCellStateRec(Cell cell, Set<Cell> neighbours) {
        if (cell.copyGrain().isNewFromRecrystallization()) {
           return cell.copyGrain();
        } else {
            for (Cell c : neighbours) {
                CellGrain cellGrain = c.copyGrain();
                if (cellGrain.isNewFromRecrystallization()) {
                    cellGrain = new CellGrain(true, cellGrain.getColor());
                    cellGrain.setNewFromRecrystallization(true);
                    return cellGrain;
                }
            }
        }

        if (cell.isAlive()) {
            Color recrystalizedColor = cell.copyGrain().getColor();
            Color cellColor = cell.copyGrain().getColor();
            boolean onEdge = false;
            boolean recrystalized = false;
            for (Cell c : neighbours) {
                if (!cellColor.equals(c.copyGrain().getColor()) && !c.copyGrain().getColor().equals(Color.color(1,1,1)))
                    onEdge = true;
                if (c.copyGrain().isNewFromRecrystallization()) {
                    recrystalizedColor = c.copyGrain().getColor();
                    recrystalized = true;
                }
            }
            if (recrystalized){
                CellGrain cellGrain = new CellGrain(true,recrystalizedColor);
                cellGrain.setOnEdge(onEdge);
                cellGrain.setNewFromRecrystallization(true);
                return cellGrain;
            } else {
                CellGrain cellGrain = cell.copyGrain();
                cellGrain.setOnEdge(onEdge);
                return cellGrain;
            }
        }
        return cell.copyGrain();
    }

    private synchronized void oneIteretionRecryst() {
        Set<CellCoordinates> coordinatesSet = board2D.getAllCoordinates();
        Board2D nextBoard = new Board2D(board2D);

        for (CellCoordinates cellCoordinates : coordinatesSet) {
            Cell currentCell = super.board2D.getCell(cellCoordinates);

            Set<CellCoordinates> coordinatesNeighbours =  super.neighborhood.cellNeighbors(cellCoordinates);

            if (super.coordinatesWrapper != null)
                coordinatesNeighbours = super.coordinatesWrapper.wrapCellCoordinates(coordinatesNeighbours);

            Set<Cell> neighbours = coordinatesNeighbours.stream()
                    .map(cord -> super.board2D.getCell(cord)).collect(Collectors.toSet());

            nextBoard.setCell(cellCoordinates, getNextCellStateRec(currentCell, neighbours));
        }

        super.board2D = nextBoard;
    }

    private synchronized boolean isAnyCellDead(){
        Set<CellCoordinates> coordinatesSet = super.board2D.getAllCoordinates();
        boolean anyDead = false;
        for (CellCoordinates cellCoordinates : coordinatesSet) {
            CellGrain currentCell = super.board2D.getCell(cellCoordinates).copyGrain();
            if (!currentCell.isAlive())
                anyDead = true;
        }
        return anyDead;
    }

    //TODO: Its probably bad way of implementing this
    @Override
    public synchronized void oneIteration() {
        boolean anyDead = isAnyCellDead();

        if (!anyDead) {
            oneIteretionRecryst();
            //Searching for alive -> incrementing cell iterator var
            //Also calc for each cell ro and sum leftovers
            double sumOfLeftoversFromRos = propagateRoValuesAndReturnSumOfLeftovers();

            // randomly sum of leftovers add? to ro in cells on edge
            //Searching for edges ->if newFromRecryst -> set it false and reset ro and iterator
            // if ro > critical -> set newFromRecryst true
            sumOfLeftoversFromRos = sumOfLeftoversFromRos / k;
            randomlySpreadLeftoversAndHandleCellsIntendentToRecrystalization(sumOfLeftoversFromRos);
        }else
            super.oneIteration();
    }

    private synchronized void randomlySpreadLeftoversAndHandleCellsIntendentToRecrystalization(double sumDevidedByK) {
        Random random = new Random();
        Set<CellCoordinates> coordinatesSet = super.board2D.getAllCoordinates();
        Board2D nextBoard = new Board2D(super.board2D);

        for (CellCoordinates cellCoordinates : coordinatesSet) {
            CellGrain currentCell = super.board2D.getCell(cellCoordinates).copyGrain();

            if (currentCell.isOnEdge()) {
                //TODO: This random should be implemented in different way
                if (random.nextInt(1000) % (int)k == 0){
                    double currentRo = currentCell.getRo();
                    currentCell.setRo(currentRo + sumDevidedByK);
                }

                if (currentCell.getRo() > criticalRo){
                    currentCell = new CellGrain();
                    currentCell.nextState();
                    currentCell.setNewFromRecrystallization(true);
                } else {
                    currentCell.setNewFromRecrystallization(false);
                }
            }

            nextBoard.setCell(cellCoordinates, currentCell);
        }

        super.board2D = nextBoard;
    }

    private synchronized double propagateRoValuesAndReturnSumOfLeftovers() {
        double sumOfLeftoversFromRos = 0.0;
        Set<CellCoordinates> coordinatesSet = super.board2D.getAllCoordinates();
        Board2D nextBoard = new Board2D(super.board2D);

        for (CellCoordinates cellCoordinates : coordinatesSet) {
            CellGrain currentCell = super.board2D.getCell(cellCoordinates).copyGrain();

            if (currentCell.isAlive()) {
                double currentIteration = currentCell.getIteration();
                currentCell.setIteration(currentIteration + 1);

                double cellsRo = roFunction(currentIteration) - roFunction(currentIteration - 1);
                cellsRo = cellsRo / (100 * 100);

                if (currentCell.isOnEdge()) {
                    currentCell.setRo(0.8 * cellsRo);
                    sumOfLeftoversFromRos += 0.2 * cellsRo;
                } else {
                    currentCell.setRo(0.2 * cellsRo);
                    sumOfLeftoversFromRos += 0.8 * cellsRo;
                }
            }

            nextBoard.setCell(cellCoordinates, currentCell);
        }

        super.board2D = nextBoard;
        return sumOfLeftoversFromRos;
    }

    private double roFunction(double it){
        return (a / b) + (1 - (a / b)) * Math.pow(Math.E, -b * (it/1000));
    }
}
