import java.awt.Color;

public class Board {
    public static final int COLS = 10;
    public static final int ROWS = 20;
    public static final int CELL = 32;

    private Color[][] grid = new Color[ROWS][COLS];

    /** True if (col,row) is a solid wall, floor, or settled block. */
    public boolean isBlocked(int col, int row) {
        if (col < 0 || col >= COLS) return true;
        if (row >= ROWS) return true;   // floor
        if (row < 0)    return false;   // above board = open
        return grid[row][col] != null;
    }

    public Color getColor(int col, int row) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return null;
        return grid[row][col];
    }

    public void setCell(int col, int row, Color c) {
        if (row >= 0 && row < ROWS && col >= 0 && col < COLS)
            grid[row][col] = c;
    }

    /** Clear all full rows, shift above rows down. Returns number cleared. */
    public int clearLines() {
        int cleared = 0;
        for (int r = ROWS - 1; r >= 0; ) {
            if (isRowFull(r)) {
                shiftDown(r);
                cleared++;
                // don't decrement r; new content is now at row r
            } else {
                r--;
            }
        }
        return cleared;
    }

    private boolean isRowFull(int row) {
        for (int c = 0; c < COLS; c++) if (grid[row][c] == null) return false;
        return true;
    }

    private void shiftDown(int clearedRow) {
        for (int r = clearedRow; r > 0; r--)
            grid[r] = grid[r - 1].clone();
        grid[0] = new Color[COLS];
    }
}
