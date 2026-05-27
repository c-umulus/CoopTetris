public class GameCharacter {

    public static final float W = Board.CELL;          // 1 cell wide
    public static final float H = Board.CELL * 2;      // 2 cells tall

    private static final float GRAVITY  =  950f;  // px/s²
    private static final float JUMP_VEL = -530f;  // px/s  (≈4.5 cells high)
    private static final float SPEED    =  160f;  // px/s horizontal

    public float x, y;      // pixel top-left
    public float vx, vy;
    public boolean onGround;
    public boolean alive = true;

    // Input (set externally each frame)
    public boolean left, right, jump;
    private boolean jumpConsumed;

    public GameCharacter(int startCol, int startRow) {
        x = startCol * Board.CELL;
        y = (startRow - 1) * (float) Board.CELL; // feet on startRow
    }

    public void update(float dt, Board board) {
        if (!alive) return;

        // Horizontal velocity
        vx = 0;
        if (left  && !right) vx = -SPEED;
        if (right && !left)  vx =  SPEED;

        // Jump
        if (jump && onGround && !jumpConsumed) {
            vy = JUMP_VEL;
            onGround = false;
            jumpConsumed = true;
        }
        if (!jump) jumpConsumed = false;

        // Gravity
        vy += GRAVITY * dt;
        vy = Math.min(vy, 950f); // terminal velocity cap

        // Move X then resolve, then move Y then resolve
        x += vx * dt;
        resolveX(board);

        y += vy * dt;
        resolveY(board);
    }

    private void resolveX(Board board) {
        // Board walls
        if (x < 0) { x = 0; return; }
        if (x + W > Board.COLS * Board.CELL) { x = Board.COLS * Board.CELL - W; return; }

        int topRow = Math.max(0, (int)(y / Board.CELL));
        int botRow = Math.min(Board.ROWS - 1, (int)((y + H - 1) / Board.CELL));

        if (vx > 0) {
            int rightCol = (int)((x + W - 1) / Board.CELL);
            for (int r = topRow; r <= botRow; r++) {
                if (board.isBlocked(rightCol, r)) {
                    x = rightCol * Board.CELL - W;
                    return;
                }
            }
        } else if (vx < 0) {
            int leftCol = (int)(x / Board.CELL);
            for (int r = topRow; r <= botRow; r++) {
                if (board.isBlocked(leftCol, r)) {
                    x = (leftCol + 1) * Board.CELL;
                    return;
                }
            }
        }
    }

    private void resolveY(Board board) {
        int leftCol  = Math.max(0, Math.min(Board.COLS - 1, (int)(x / Board.CELL)));
        int rightCol = Math.max(0, Math.min(Board.COLS - 1, (int)((x + W - 1) / Board.CELL)));

        if (vy >= 0) { // falling or still
            int feetRow = (int)((y + H) / Board.CELL);
            if (feetRow >= Board.ROWS
                    || board.isBlocked(leftCol,  feetRow)
                    || board.isBlocked(rightCol, feetRow)) {
                y = feetRow * Board.CELL - H;
                vy = 0;
                onGround = true;
            } else {
                onGround = false;
            }
            // hard floor clamp
            if (y + H > Board.ROWS * Board.CELL) {
                y = Board.ROWS * Board.CELL - H;
                vy = 0;
                onGround = true;
            }
        } else { // rising
            if (y < 0) {
                y = 0;
                vy = 0;
            } else {
                int headRow = (int)(y / Board.CELL);
                if (board.isBlocked(leftCol, headRow) || board.isBlocked(rightCol, headRow)) {
                    y = (headRow + 1) * Board.CELL;
                    vy = 0;
                }
            }
        }
    }

    /** Grid column of character center. */
    public int centerCol() {
        return (int)((x + W / 2) / Board.CELL);
    }

    /** Top grid row (head). */
    public int topRow() {
        return Math.max(0, (int)(y / Board.CELL));
    }

    /** Bottom grid row (feet). */
    public int botRow() {
        return Math.min(Board.ROWS - 1, (int)((y + H - 1) / Board.CELL));
    }

    /** True if any cell of the falling piece is inside the character's body. */
    public boolean hitByPiece(Tetromino piece) {
        int cc = centerCol();
        int rt = topRow();
        int rb = botRow();
        for (int[] cell : piece.cells()) {
            int pr = cell[0], pc = cell[1];
            if (pc == cc && pr >= rt && pr <= rb) return true;
        }
        return false;
    }

    /** True if character overlaps the flag cell. */
    public boolean touchesFlag(int flagCol, int flagRow) {
        return centerCol() == flagCol && flagRow >= topRow() && flagRow <= botRow();
    }
}
