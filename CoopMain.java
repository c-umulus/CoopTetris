package test;

import javax.swing.SwingUtilities;

/** 협동 테트리스 진입점 */
public class CoopMain {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CoopTetrisFrame frame = new CoopTetrisFrame();
            frame.setVisible(true);
        });
    }
}
