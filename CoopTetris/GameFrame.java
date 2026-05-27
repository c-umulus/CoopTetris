import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame {

    public GameFrame() {
        setTitle("협동 테트리스 — 블록 조작 + 캐릭터 이동");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel gp = new GamePanel();
        add(gp);
        pack();

        // Center on screen
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - getWidth()) / 2, (screen.height - getHeight()) / 2);

        gp.requestFocusInWindow();
    }
}
