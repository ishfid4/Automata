package pl.wieloskalowe.controls;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import pl.wieloskalowe.*;
import pl.wieloskalowe.Controllers.CellDrawer;

/**
 * Created by ishfi on 21.05.2017.
 */
public class MCanvas extends Canvas {
    private double cellWidth, cellHeight;
    private String automatonType;

    public void setCellWidth(double cellWidth) {
        this.cellWidth = cellWidth;
    }

    public void setCellHeight(double cellHeight) {
        this.cellHeight = cellHeight;
    }

    public void setAutomatonType(String automatonType) {
        this.automatonType = automatonType;
    }

    private final CellDrawer redrawTask = new CellDrawer(this) {
        @Override
        protected void redraw(GraphicsContext graphicsContext, Board2D board2D, String automatonType,
                              double cellWidth, double cellHeight, double canvasHeight, double canvasWidth) {
            graphicsContext.clearRect(0,0,canvasWidth, canvasHeight);

            if (automatonType.equals("GameOfLife")) {
                for (CellCoordinates cellCoordinates : board2D.getAllCoordinates()) {
                    Cell cell = board2D.getCell(cellCoordinates);
                    drawBinaryCell(graphicsContext, cellWidth, cellHeight, cellCoordinates.getX(),
                            cellCoordinates.getY(), cell.isAlive());
                }
            }

            if (automatonType.equals("NaiveGrainGrow")) {
                for (CellCoordinates cellCoordinates : board2D.getAllCoordinates()) {
                    CellGrain cell = (CellGrain) board2D.getCell(cellCoordinates);
                    drawGrainCell(graphicsContext, cellWidth, cellHeight, cellCoordinates.getX(),
                            cellCoordinates.getY(), cell.isAlive(), cell.getColor());
                }
            }
        }
    };

    private void drawBinaryCell(GraphicsContext graphicsContext, double cellWidth, double cellHeight,
                                int x, int y, boolean alive) {
        if (alive) {
            graphicsContext.fillRect(x * cellWidth, y * cellHeight, cellWidth, cellHeight);
        } else
            graphicsContext.strokeRect(x * cellWidth,y * cellHeight,cellWidth,cellHeight);
    }

    private void drawGrainCell(GraphicsContext graphicsContext, double cellWidth, double cellHeight,
                              int x, int y, boolean alive, Color color) {
        if (alive) {
            graphicsContext.setFill(color);
            graphicsContext.fillRect(x * cellWidth, y * cellHeight, cellWidth, cellHeight);
        } else
            graphicsContext.strokeRect(x * cellWidth,y * cellHeight,cellWidth,cellHeight);
    }

    public void onDataRecived(Board2D board2D) {
        redrawTask.setUpData(cellWidth,cellHeight,automatonType);
        redrawTask.requestRedraw(board2D);
    }

}