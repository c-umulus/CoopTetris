import java.awt.Color;
import java.util.Random;

public class Tetromino {

    // 7 types × 4 rotations × 4 cells: each cell is {rowOffset, colOffset}
    private static final int[][][][] SHAPES = {
        // I  (4×4 bounding box)
        { {{1,0},{1,1},{1,2},{1,3}}, {{0,2},{1,2},{2,2},{3,2}},
          {{2,0},{2,1},{2,2},{2,3}}, {{0,1},{1,1},{2,1},{3,1}} },
        // O
        { {{0,1},{0,2},{1,1},{1,2}}, {{0,1},{0,2},{1,1},{1,2}},
          {{0,1},{0,2},{1,1},{1,2}}, {{0,1},{0,2},{1,1},{1,2}} },
        // T  (3×3 in 4×4)
        { {{0,1},{1,0},{1,1},{1,2}}, {{0,1},{1,1},{1,2},{2,1}},
          {{1,0},{1,1},{1,2},{2,1}}, {{0,1},{1,0},{1,1},{2,1}} },
        // S
        { {{0,1},{0,2},{1,0},{1,1}}, {{0,1},{1,1},{1,2},{2,2}},
          {{1,1},{1,2},{2,0},{2,1}}, {{0,0},{1,0},{1,1},{2,1}} },
        // Z
        { {{0,0},{0,1},{1,1},{1,2}}, {{0,2},{1,1},{1,2},{2,1}},
          {{1,0},{1,1},{2,1},{2,2}}, {{0,1},{1,0},{1,1},{2,0}} },
        // J
        { {{0,0},{1,0},{1,1},{1,2}}, {{0,1},{0,2},{1,1},{2,1}},
          {{1,0},{1,1},{1,2},{2,2}}, {{0,1},{1,1},{2,0},{2,1}} },
        // L
        { {{0,2},{1,0},{1,1},{1,2}}, {{0,1},{1,1},{2,1},{2,2}},
          {{1,0},{1,1},{1,2},{2,0}}, {{0,0},{0,1},{1,1},{2,1}} }
    };

    private static final Color[] COLORS = {
        new Color(0,   240, 240), // I – cyan
        new Color(240, 240,  0 ), // O – yellow
        new Color(160,  0,  240), // T – purple
        new Color(0,   240,  0 ), // S – green
        new Color(240,  0,   0 ), // Z – red
        new Color(0,    0,  240), // J – blue
        new Color(240, 160,  0 )  // L – orange
    };

    private static final Random RNG = new Random();

    int type;       // 0–6
    int rotation;   // 0–3
    int row, col;   // board position of bounding-box top-left

    public Tetromino(int type) {
        this.type     = type;
        this.rotation = 0;
        this.row      = 0;
        this.col      = 3; // spawn centered
    }

    public static Tetromino random() { return new Tetromino(RNG.nextInt(7)); }

    /** Absolute {boardRow, boardCol} of each of the 4 cells. */
    public int[][] cells() {
        int[][] off = SHAPES[type][rotation];
        int[][] abs = new int[4][2];
        for (int i = 0; i < 4; i++) {
            abs[i][0] = row + off[i][0];
            abs[i][1] = col + off[i][1];
        }
        return abs;
    }

    public Color color() { return COLORS[type]; }

    public Tetromino moved(int dr, int dc) {
        Tetromino t = copy();
        t.row += dr;
        t.col += dc;
        return t;
    }

    public Tetromino rotated() {
        Tetromino t = copy();
        t.rotation = (rotation + 1) % 4;
        return t;
    }

    public boolean fits(Board board) {
        for (int[] c : cells())
            if (board.isBlocked(c[1], c[0])) return false;
        return true;
    }

    public void place(Board board) {
        for (int[] c : cells())
            board.setCell(c[1], c[0], COLORS[type]);
    }

    /** Copy with row=0, col=0 for preview rendering. */
    public Tetromino preview() {
        Tetromino t = new Tetromino(type);
        t.rotation = 0;
        t.row = 0;
        t.col = 0;
        return t;
    }

    private Tetromino copy() {
        Tetromino t = new Tetromino(type);
        t.rotation = rotation;
        t.row = row;
        t.col = col;
        return t;
    }
}
