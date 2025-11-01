package presenter;

import view.ProcessSimulatorGUI;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ProcessSimulatorGUI simulator = new ProcessSimulatorGUI();
                simulator.setVisible(true);
            }
        });
    }
}