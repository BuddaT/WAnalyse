package net.buddat.wanalyse;

import net.buddat.wanalyse.gui.MainWindow;

public class WAnalyse {
	
	private MainWindow main;

	public WAnalyse(String[] args) {
		loadSettings();
		
		setMainWindow(new MainWindow());
	}

	private void loadSettings() {
		
	}

	public MainWindow getMainWindow() {
		return main;
	}

	public void setMainWindow(MainWindow main) {
		this.main = main;
	}

	public static void main(String[] args) {
		new WAnalyse(args);
	}

}
