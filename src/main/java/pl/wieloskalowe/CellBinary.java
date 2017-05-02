package pl.wieloskalowe;

/**
 * Created by ishfi on 02.05.2017.
 */
public class CellBinary {
    private boolean state;

    public CellBinary() {
        this.state = false;
    }

    public CellBinary(CellBinary cell) {
        this.state = cell.state;
    }

    public CellBinary(boolean state) {
        this.state = state;
    }

    public boolean isAlive() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }
}
