import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;

public class GamePanel extends JPanel {

    // ── Layout ───────────────────────────────────────────────────
    private static final int BW   = Board.COLS * Board.CELL;   // board width  320
    private static final int BH   = Board.ROWS * Board.CELL;   // board height 640
    private static final int SW   = 190;                        // side-panel width
    public  static final int TOTAL_W = BW + SW;
    public  static final int TOTAL_H = BH;

    // ── Game state ───────────────────────────────────────────────
    private enum State { READY, PLAYING, GAME_OVER }

    private State        state   = State.READY;
    private Board        board;
    private Tetromino    current, next;
    private GameCharacter chara;
    private int          flagCol, flagRow;
    private int          score;
    private float        timeLeft;
    private String       deathMsg = "";

    // ── Timers ───────────────────────────────────────────────────
    private Timer gameTimer;          // ~60 fps physics + render
    private Timer dropTimer;          // piece gravity
    private int   dropMs  = 800;
    private long  lastTick;

    // ── Input ────────────────────────────────────────────────────
    private final Set<Integer> keys = new HashSet<>();

    // ── Particle flash for flag grab ─────────────────────────────
    private int flashFrames = 0;

    // ─────────────────────────────────────────────────────────────
    public GamePanel() {
        setPreferredSize(new Dimension(TOTAL_W, TOTAL_H));
        setBackground(Color.BLACK);
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                keys.add(k);
                onKeyPress(k);
            }
            @Override public void keyReleased(KeyEvent e) {
                keys.remove(e.getKeyCode());
            }
        });

        gameTimer = new Timer(16, e -> gameTick());
        dropTimer = new Timer(dropMs, e -> dropTick());
    }

    // ── Game lifecycle ───────────────────────────────────────────
    public void startGame() {
        board    = new Board();
        chara    = new GameCharacter(0, Board.ROWS - 1);
        current  = Tetromino.random();
        next     = Tetromino.random();
        score    = 0;
        timeLeft = 90f;
        dropMs   = 800;
        deathMsg = "";
        state    = State.PLAYING;
        lastTick = System.currentTimeMillis();
        placeFlag();
        dropTimer.setDelay(dropMs);
        gameTimer.start();
        dropTimer.start();
        requestFocusInWindow();
    }

    private void endGame(String msg) {
        state    = State.GAME_OVER;
        deathMsg = msg;
        gameTimer.stop();
        dropTimer.stop();
        repaint();
    }

    // ── Flag placement ───────────────────────────────────────────
    /**
     * Place flag in the upper portion of the board on an empty cell
     * that has a solid surface below it (or is otherwise accessible).
     * Avoids completely sealed positions.
     */
    private void placeFlag() {
        // Try rows 1..9 (upper area), random columns
        for (int attempt = 0; attempt < 200; attempt++) {
            int r = 1 + (int)(Math.random() * 9);           // rows 1-9
            int c = (int)(Math.random() * Board.COLS);
            if (board.isBlocked(c, r)) continue;
            // Prefer positions that have open space around them (not buried)
            int openNeighbors = 0;
            if (!board.isBlocked(c - 1, r)) openNeighbors++;
            if (!board.isBlocked(c + 1, r)) openNeighbors++;
            if (!board.isBlocked(c, r - 1)) openNeighbors++;
            if (openNeighbors >= 1) {
                flagCol = c;
                flagRow = r;
                return;
            }
        }
        // Fallback: any empty cell in rows 1-9
        for (int r = 1; r < 10; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                if (!board.isBlocked(c, r)) { flagCol = c; flagRow = r; return; }
            }
        }
        // Last resort: row 0
        for (int c = 0; c < Board.COLS; c++) {
            if (!board.isBlocked(c, 0)) { flagCol = c; flagRow = 0; return; }
        }
    }

    // ── Main game tick (60fps) ───────────────────────────────────
    private void gameTick() {
        if (state != State.PLAYING) return;

        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastTick) / 1000f, 0.05f);
        lastTick = now;

        // Character input
        chara.left  = keys.contains(KeyEvent.VK_A);
        chara.right = keys.contains(KeyEvent.VK_D);
        chara.jump  = keys.contains(KeyEvent.VK_W);
        chara.update(dt, board);

        // Character crushed by falling piece?
        if (current != null && chara.hitByPiece(current)) {
            endGame("캐릭터가 낙하 블록에 깔렸습니다!");
            return;
        }

        // Timer countdown
        timeLeft -= dt;
        if (timeLeft <= 0) {
            endGame("시간 초과! 깃발에 도달하지 못했습니다.");
            return;
        }

        // Flag reached?
        if (chara.touchesFlag(flagCol, flagRow)) {
            score += 1000 + (int)(timeLeft * 10);
            flashFrames = 30;
            placeFlag();
            timeLeft = Math.min(timeLeft + 20, 90f); // time bonus
        }

        if (flashFrames > 0) flashFrames--;

        // Speed ramp: every 20 s elapsed, drop interval -80 ms (floor 150 ms)
        int elapsed = (int)(90 - timeLeft);
        int target  = Math.max(150, 800 - (elapsed / 20) * 80);
        if (target != dropMs) {
            dropMs = target;
            dropTimer.setDelay(dropMs);
        }

        repaint();
    }

    // ── Piece drop tick ──────────────────────────────────────────
    private void dropTick() {
        if (state != State.PLAYING || current == null) return;
        Tetromino moved = current.moved(1, 0);
        if (moved.fits(board)) {
            current = moved;
        } else {
            settlePiece();
        }
        repaint();
    }

    private void settlePiece() {
        current.place(board);

        // Settled on character?
        if (chara.hitByPiece(current)) {
            endGame("캐릭터가 블록에 깔렸습니다!");
            return;
        }

        int cleared = board.clearLines();
        if (cleared > 0) score += scoreForLines(cleared);

        // If flag buried, relocate
        if (board.isBlocked(flagCol, flagRow)) placeFlag();

        current = next;
        next    = Tetromino.random();

        // Board full?
        if (!current.fits(board)) {
            endGame("보드가 가득 찼습니다!");
        }
    }

    private int scoreForLines(int n) {
        if (n == 1) return 100;
        if (n == 2) return 300;
        if (n == 3) return 500;
        return 800;
    }

    // ── Key press handler (block player controls + restart) ──────
    private void onKeyPress(int k) {
        if (state != State.PLAYING) {
            if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) startGame();
            return;
        }
        if (current == null) return;

        if      (k == KeyEvent.VK_LEFT)  tryMove(current.moved(0, -1));
        else if (k == KeyEvent.VK_RIGHT) tryMove(current.moved(0,  1));
        else if (k == KeyEvent.VK_UP)    tryMove(current.rotated());
        else if (k == KeyEvent.VK_DOWN)  tryMove(current.moved(1,  0));
        else if (k == KeyEvent.VK_SPACE) hardDrop();
        repaint();
    }

    private void tryMove(Tetromino candidate) {
        if (candidate.fits(board)) current = candidate;
    }

    private void hardDrop() {
        while (true) {
            Tetromino down = current.moved(1, 0);
            if (down.fits(board)) {
                current = down;
                // Check character hit at each step so hard-drop kills properly
                if (chara.hitByPiece(current)) break;
            } else break;
        }
        settlePiece();
    }

    // ── Painting ─────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBoard(g2);
        if (state == State.PLAYING || state == State.GAME_OVER) {
            drawGhost(g2);
            drawPiece(g2, current);
            drawFlag(g2);
            drawCharacter(g2);
        }
        drawSidePanel(g2);

        if (state == State.READY)     drawOverlay(g2, "협동 테트리스", "Enter 또는 Space 로 시작");
        if (state == State.GAME_OVER) drawOverlay(g2, "게임 오버", deathMsg + "   |   Enter로 재시작");

        g2.dispose();
    }

    // Board background + settled blocks
    private void drawBoard(Graphics2D g) {
        g.setColor(new Color(18, 18, 30));
        g.fillRect(0, 0, BW, BH);

        // Grid lines
        g.setColor(new Color(40, 40, 60));
        for (int r = 0; r <= Board.ROWS; r++) g.drawLine(0, r * Board.CELL, BW, r * Board.CELL);
        for (int c = 0; c <= Board.COLS; c++) g.drawLine(c * Board.CELL, 0, c * Board.CELL, BH);

        // Settled cells
        if (board != null) {
            for (int r = 0; r < Board.ROWS; r++)
                for (int c = 0; c < Board.COLS; c++) {
                    Color col = board.getColor(c, r);
                    if (col != null) fillCell(g, c, r, col);
                }
        }

        // Border
        g.setColor(new Color(100, 100, 140));
        g.setStroke(new BasicStroke(2));
        g.drawRect(0, 0, BW - 1, BH - 1);
        g.setStroke(new BasicStroke(1));
    }

    // Ghost piece (shadow)
    private void drawGhost(Graphics2D g) {
        if (current == null) return;
        Tetromino ghost = current;
        while (true) {
            Tetromino d = ghost.moved(1, 0);
            if (d.fits(board)) ghost = d; else break;
        }
        Color base = current.color();
        Color gc = new Color(base.getRed(), base.getGreen(), base.getBlue(), 55);
        for (int[] cell : ghost.cells()) {
            if (cell[0] >= 0) {
                int px = cell[1] * Board.CELL + 2, py = cell[0] * Board.CELL + 2;
                int sz = Board.CELL - 4;
                g.setColor(gc);
                g.fillRect(px, py, sz, sz);
                g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 90));
                g.drawRect(px, py, sz - 1, sz - 1);
            }
        }
    }

    private void drawPiece(Graphics2D g, Tetromino piece) {
        if (piece == null) return;
        for (int[] cell : piece.cells())
            if (cell[0] >= 0) fillCell(g, cell[1], cell[0], piece.color());
    }

    private void fillCell(Graphics2D g, int col, int row, Color base) {
        int x = col * Board.CELL, y = row * Board.CELL;
        // Main fill
        g.setColor(base);
        g.fillRect(x + 1, y + 1, Board.CELL - 2, Board.CELL - 2);
        // Top/left highlight
        g.setColor(base.brighter().brighter());
        g.drawLine(x + 1, y + 1, x + Board.CELL - 2, y + 1);
        g.drawLine(x + 1, y + 1, x + 1, y + Board.CELL - 2);
        // Bottom/right shadow
        g.setColor(base.darker());
        g.drawLine(x + 1, y + Board.CELL - 2, x + Board.CELL - 2, y + Board.CELL - 2);
        g.drawLine(x + Board.CELL - 2, y + 1, x + Board.CELL - 2, y + Board.CELL - 2);
    }

    private void drawFlag(Graphics2D g) {
        int px = flagCol * Board.CELL;
        int py = flagRow * Board.CELL;
        int cs = Board.CELL;

        // Glow / flash effect when flag grabbed
        if (flashFrames > 0) {
            int alpha = (int)(flashFrames * 8);
            g.setColor(new Color(255, 255, 100, Math.min(200, alpha)));
            g.fillOval(px - cs/2, py - cs/2, cs * 2, cs * 2);
        }

        // Pole
        g.setColor(new Color(200, 200, 200));
        g.fillRect(px + cs / 2 - 1, py, 3, cs);

        // Flag banner
        int[] px_ = { px + cs/2 + 2, px + cs - 3, px + cs/2 + 2 };
        int[] py_ = { py + 3, py + cs/2 - 3, py + cs - 7 };
        g.setColor(new Color(255, 200, 0));
        g.fillPolygon(px_, py_, 3);
        g.setColor(new Color(255, 160, 0));
        g.drawPolygon(px_, py_, 3);

        // Star on flag
        g.setColor(Color.WHITE);
        g.fillOval(px + cs/2 + 4, py + cs/2 - 8, 6, 6);
    }

    private void drawCharacter(Graphics2D g) {
        if (chara == null || !chara.alive) return;
        int x = (int) chara.x;
        int y = (int) chara.y;
        int w = (int) GameCharacter.W;
        int h = (int) GameCharacter.H;
        int pad = 2;

        // Body
        GradientPaint gp = new GradientPaint(x + pad, y, new Color(80, 220, 80),
                                              x + pad, y + h, new Color(30, 140, 30));
        g.setPaint(gp);
        g.fillRoundRect(x + pad, y, w - pad * 2, h - 6, 10, 10);

        // Outline
        g.setColor(new Color(20, 100, 20));
        g.drawRoundRect(x + pad, y, w - pad * 2, h - 6, 10, 10);

        // Eyes
        g.setColor(Color.WHITE);
        g.fillOval(x + 5, y + 3, 8, 8);
        g.fillOval(x + 17, y + 3, 8, 8);
        g.setColor(new Color(30, 50, 200));
        g.fillOval(x + 7, y + 5, 4, 4);
        g.fillOval(x + 19, y + 5, 4, 4);
        // Shine
        g.setColor(Color.WHITE);
        g.fillOval(x + 8, y + 5, 2, 2);
        g.fillOval(x + 20, y + 5, 2, 2);

        // Smile
        g.setColor(new Color(20, 80, 20));
        g.drawArc(x + 8, y + 14, 14, 8, 200, 140);

        // Feet
        g.setColor(new Color(160, 100, 30));
        g.fillRoundRect(x + 3, y + h - 6, 11, 6, 4, 4);
        g.fillRoundRect(x + w - 14, y + h - 6, 11, 6, 4, 4);
    }

    // ── Side panel ───────────────────────────────────────────────
    private void drawSidePanel(Graphics2D g) {
        int sx = BW + 10;

        // Background
        g.setColor(new Color(25, 25, 40));
        g.fillRect(BW, 0, SW, BH);
        g.setColor(new Color(80, 80, 120));
        g.drawLine(BW, 0, BW, BH);

        // ── Score ──
        drawLabel(g, "SCORE", sx, 30);
        g.setFont(new Font("Consolas", Font.BOLD, 24));
        g.setColor(new Color(255, 220, 0));
        g.drawString(String.valueOf(score), sx, 58);

        // ── Timer ──
        drawLabel(g, "남은 시간", sx, 90);
        float t = (state == State.PLAYING) ? Math.max(0, timeLeft) : 0;
        g.setFont(new Font("Consolas", Font.BOLD, 26));
        g.setColor(t < 15 ? Color.RED : t < 30 ? Color.ORANGE : new Color(0, 210, 255));
        g.drawString(String.format("%5.1f s", t), sx, 120);

        // Timer bar
        int barW = SW - 20;
        g.setColor(new Color(50, 50, 70));
        g.fillRoundRect(sx, 128, barW, 8, 4, 4);
        float ratio = (state == State.PLAYING) ? Math.max(0, timeLeft / 90f) : 0;
        Color barColor = ratio < 0.2f ? Color.RED : ratio < 0.4f ? Color.ORANGE : new Color(0, 200, 80);
        g.setColor(barColor);
        g.fillRoundRect(sx, 128, (int)(barW * ratio), 8, 4, 4);

        // ── Next piece preview ──
        drawLabel(g, "NEXT", sx, 158);
        drawPreview(g, sx, 165);

        // ── Controls ──
        int cy = 295;
        drawLabel(g, "블록 플레이어", sx, cy);    cy += 5;
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(200, 200, 200));
        String[] blockControls = { "← →  블록 이동", "↑      회전", "↓      천천히 내리기", "Space  빠른 낙하" };
        for (String s : blockControls) { cy += 16; g.drawString(s, sx, cy); }

        cy += 22;
        drawLabel(g, "캐릭터 플레이어", sx, cy);  cy += 5;
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(200, 200, 200));
        String[] charControls = { "A D   좌우 이동", "W      점프 (최소 2칸)" };
        for (String s : charControls) { cy += 16; g.drawString(s, sx, cy); }

        cy += 26;
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(new Color(255, 220, 80));
        g.drawString("깃발에 도달하세요!", sx, cy);

        cy += 16;
        g.setColor(new Color(255, 120, 120));
        g.drawString("블록에 맞으면 게임 오버!", sx, cy);

        // Speed indicator
        cy += 26;
        drawLabel(g, "현재 속도", sx, cy); cy += 3;
        g.setFont(new Font("Consolas", Font.PLAIN, 11));
        g.setColor(new Color(150, 200, 255));
        g.drawString(dropMs + " ms/칸", sx, cy + 14);
    }

    private void drawLabel(Graphics2D g, String text, int x, int y) {
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(new Color(140, 160, 220));
        g.drawString(text, x, y);
    }

    private void drawPreview(Graphics2D g, int sx, int sy) {
        if (next == null) return;
        int cell = Board.CELL / 2 - 1;   // 15px mini-cell
        int boxW = 4 * cell + 12, boxH = 4 * cell + 12;

        g.setColor(new Color(35, 35, 55));
        g.fillRoundRect(sx, sy, boxW, boxH, 6, 6);
        g.setColor(new Color(70, 70, 100));
        g.drawRoundRect(sx, sy, boxW, boxH, 6, 6);

        for (int[] c : next.preview().cells()) {
            int px = sx + 6 + c[1] * cell;
            int py = sy + 6 + c[0] * cell;
            g.setColor(next.color());
            g.fillRect(px + 1, py + 1, cell - 2, cell - 2);
            g.setColor(next.color().brighter());
            g.drawRect(px + 1, py + 1, cell - 3, cell - 3);
        }
    }

    private void drawOverlay(Graphics2D g, String title, String subtitle) {
        // Dim the board
        g.setColor(new Color(0, 0, 0, 185));
        g.fillRect(0, 0, BW, BH);

        // Title box
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(title);
        int cx = (BW - tw) / 2;
        int cy = BH / 2 - 30;

        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(cx - 20, cy - fm.getAscent() - 10, tw + 40, fm.getHeight() + 20, 16, 16);
        g.setColor(new Color(255, 220, 60));
        g.drawString(title, cx, cy);

        // Subtitle
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        fm = g.getFontMetrics();
        // Multi-line support (split on |)
        String[] lines = subtitle.split("   \\|   ");
        int lineY = cy + 40;
        for (String line : lines) {
            int lw = fm.stringWidth(line);
            g.setColor(new Color(200, 200, 200));
            g.drawString(line, (BW - lw) / 2, lineY);
            lineY += 20;
        }
    }
}
