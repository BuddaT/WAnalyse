package net.buddat.wanalyse.gui;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import net.buddat.wanalyse.gui.undo.UndoManager;

public class MainWindow extends JFrame {

	private static final long serialVersionUID = -1945062747749826122L;

	private static final String WINDOW_TITLE = "WAnalyse - v";
	private static final double VERSION = 0.1;

	private static final int WIDTH = 800, HEIGHT = 600;

	private UndoManager undoManager;

	private final Map map;
	private final GraphicPanel graphicPanel;

	public MainWindow() {
		super(WINDOW_TITLE + VERSION);

		this.setSize(WIDTH, HEIGHT);
		this.setResizable(false);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setLocationRelativeTo(null);

		setUndoManager(new UndoManager());

		map = new Map("default", 24, 17);
		graphicPanel = new GraphicPanel(this, map);
		JScrollPane graphicScroll = new JScrollPane(graphicPanel,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		graphicScroll.getVerticalScrollBar().setUnitIncrement(10);

		this.add(graphicScroll);

		this.setVisible(true);
	}

	public UndoManager getUndoManager() {
		return undoManager;
	}

	public void setUndoManager(UndoManager undoManager) {
		this.undoManager = undoManager;
	}
}
