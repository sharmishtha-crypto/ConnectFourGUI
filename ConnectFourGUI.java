import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;


enum PlayerColor {
    RED("Red", Color.RED),
    YELLOW("Yellow", Color.YELLOW);

    private final String name;
    private final Color color;

    PlayerColor(String name, Color color) {
        this.name = name;
        this.color = color;
    }

    public String getName() { return name; }
    public Color getColor() { return color; }

    public PlayerColor next() {
        return this == RED ? YELLOW : RED;
    }
}


class ColumnFullException extends Exception {
    public ColumnFullException(String message) {
        super(message);
    }
}


class GameHistory<T> {
    private List<T> history = new ArrayList<>();
    public void record(T entry) { history.add(entry); }
    public List<T> getHistory() { return history; }
}


abstract class GameBoard {
    protected int rows;
    protected int cols;

    public GameBoard(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
    }

    public abstract boolean isFull();
}

class ConnectFourBoard extends GameBoard {
    private PlayerColor[][] grid;

    public ConnectFourBoard(int rows, int cols) {
        super(rows, cols);
        grid = new PlayerColor[rows][cols];
    }

    public PlayerColor getCell(int r, int c) {
        return grid[r][c];
    }

    public int dropDisc(int col, PlayerColor player) throws ColumnFullException {
        if (col < 0 || col >= cols) return -1;
        for (int r = rows - 1; r >= 0; r--) {
            if (grid[r][col] == null) {
                grid[r][col] = player;
                return r;
            }
        }
        throw new ColumnFullException("Column " + (col + 1) + " is already full!");
    }

    @Override
    public boolean isFull() {
        for (int c = 0; c < cols; c++) {
            if (grid[0][c] == null) return false;
        }
        return true;
    }


    public boolean checkWin(int r, int c, PlayerColor player) {
        return checkDirection(r, c, 1, 0, player) || // Vertical
               checkDirection(r, c, 0, 1, player) || // Horizontal
               checkDirection(r, c, 1, 1, player) || // Diagonal \
               checkDirection(r, c, 1, -1, player);   // Diagonal /
    }

    private boolean checkDirection(int r, int c, int rDir, int cDir, PlayerColor player) {
        int count = 1;
        count += countMatches(r, c, rDir, cDir, player);
        count += countMatches(r, c, -rDir, -cDir, player);
        return count >= 4;
    }

    private int countMatches(int r, int c, int rDir, int cDir, PlayerColor player) {
        int count = 0;
        int currR = r + rDir;
        int currC = c + cDir;
        while (currR >= 0 && currR < rows && currC >= 0 && currC < cols && grid[currR][currC] == player) {
            count++;
            currR += rDir;
            currC += cDir;
        }
        return count;
    }
}


public class ConnectFourGUI extends JFrame {
    private static final int ROWS = 6;
    private static final int COLS = 7;
    private ConnectFourBoard board;
    private PlayerColor currentPlayer;
    private JButton[] colButtons;
    private JPanel gridPanel;
    private JLabel statusLabel;
    private GameHistory<String> history;
    
    private static int totalGamesPlayed = 0; // Static Tracker

    public ConnectFourGUI() {
        setTitle("Connect Four - DCST Strategy Game");
        setSize(700, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        board = new ConnectFourBoard(ROWS, COLS);
        currentPlayer = PlayerColor.RED;
        history = new GameHistory<>();
        totalGamesPlayed++;

        setupTopPanel();
        setupGridPanel();
    }

    private void setupTopPanel() {
        JPanel topPanel = new JPanel(new GridLayout(1, COLS));
        colButtons = new JButton[COLS];

        for (int c = 0; c < COLS; c++) {
            final int colIdx = c;
            JButton btn = new JButton("↓ Drop");
            btn.setFont(new Font("Arial", Font.BOLD, 12));
            btn.setFocusPainted(false);
            btn.addActionListener(e -> handleColumnClick(colIdx));
            colButtons[c] = btn;
            topPanel.add(btn);
        }

        JPanel headerPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Current Turn: " + currentPlayer.getName() + " | Games Played: " + totalGamesPlayed, SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        headerPanel.add(topPanel, BorderLayout.NORTH);
        headerPanel.add(statusLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);
    }

    private void setupGridPanel() {
        gridPanel = new JPanel(new GridLayout(ROWS, COLS));
        gridPanel.setBackground(new Color(20, 20, 150));
        drawBoard();
        add(gridPanel, BorderLayout.CENTER);
    }

private void drawBoard() {
        gridPanel.removeAll();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                final int row = r; // Effectively final copy for the inner class
                final int col = c; // Effectively final copy for the inner class

                JPanel cell = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        g.setColor(new Color(20, 20, 150));
                        g.fillRect(0, 0, getWidth(), getHeight());
                        
                        // Use the final variables here instead of r and c
                        PlayerColor p = board.getCell(row, col);
                        if (p == null) {
                            g.setColor(Color.WHITE);
                        } else {
                            g.setColor(p.getColor());
                        }
                        // Draw circular disc slots
                        g.fillOval(8, 8, getWidth() - 16, getHeight() - 16);
                    }
                };
                cell.setBorder(BorderFactory.createLineBorder(new Color(10, 10, 100), 2));
                gridPanel.add(cell);
            }
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void handleColumnClick(int col) {
        try {
            int row = board.dropDisc(col, currentPlayer);
            history.record(currentPlayer.getName() + " dropped disc in column " + (col + 1));


            if (board.checkWin(row, col, currentPlayer)) {
                JOptionPane.showMessageDialog(this, currentPlayer.getName() + " wins the game!", "Victory", JOptionPane.INFORMATION_MESSAGE);
                resetGame();
                return;
            }


            if (board.isFull()) {
                JOptionPane.showMessageDialog(this, "The game is a draw!", "Draw", JOptionPane.INFORMATION_MESSAGE);
                resetGame();
                return;
            }

            // Switch Player Turn
            currentPlayer = currentPlayer.next();
            statusLabel.setText("Current Turn: " + currentPlayer.getName() + " | Games Played: " + totalGamesPlayed);
            drawBoard();

        } catch (ColumnFullException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Column Full Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetGame() {
        board = new ConnectFourBoard(ROWS, COLS);
        currentPlayer = PlayerColor.RED;
        statusLabel.setText("Current Turn: " + currentPlayer.getName() + " | Games Played: " + totalGamesPlayed);
        drawBoard();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ConnectFourGUI().setVisible(true);
        });
    }
}