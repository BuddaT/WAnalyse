package net.buddat.wanalyse.gui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.event.MouseInputAdapter;

public class GraphicPanel extends JPanel {

	private static final long serialVersionUID = 5938186762187399440L;
	
	public enum EditState {
		TERRAIN_PENCIL, TERRAIN_BRUSH, TERRAIN_ERASER, TERRAIN_FILL, TERRAIN_PICKER,
		OBJECT_PENCIL, OBJECT_ERASER, OBJECT_PICKER,
		FENCE_PENCIL, FENCE_LINE, FENCE_ERASER, FENCE_PICKER,
		OVERLAY_PENCIL, OVERLAY_BRUSH, OVERLAY_ERASER, OVERLAY_FILL, OVERLAY_PICKER, 
		LABEL
	}
	
	private final MainWindow mainWindow;
	private final Map map;
	
	private int mouseX, mouseY;
	private final Color highlightColor = new Color(Color.YELLOW.getRed(), Color.YELLOW.getGreen(), Color.YELLOW.getBlue(), 100);
	
	public static final int TILE_MAX_SIZE = 128, TILE_MIN_SIZE = 4, TILE_SIZE_STEP = 4;
	private int tileSize = 32;
	
	private EditState currentState = EditState.TERRAIN_PENCIL;
	
	private boolean caveLayer;
	
	private boolean saveToImage = false;

	public GraphicPanel(MainWindow main, Map m) {
		super();
		
		this.map = m;
		this.mainWindow = main;
		
		setupMouseDrag();
		revalidateScroll();
	}
	
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		JViewport jv = (JViewport) this.getParent();
		Point jvp = jv.getViewPosition();
		
		int xStart = (int) jvp.getX();
		xStart /= tileSize;
		if (xStart - 1 >= 0)
			xStart -= 1;
		
		int yStart = (int) jvp.getY();
		yStart /= tileSize;
		if (yStart - 1 >= 0)
			yStart -= 1;
		
		int xEnd = (int) (jvp.getX() + jv.getWidth());
		xEnd /= tileSize;
		if (xEnd + 1 < map.getMapWidth())
			xEnd += 1;
		else
			xEnd = map.getMapWidth();
		
		int yEnd = (int) (jvp.getY() + jv.getHeight());
		yEnd /= tileSize;
		if (yEnd + 1 < map.getMapHeight())
			yEnd += 1;
		else
			yEnd = map.getMapHeight();
		
		if (saveToImage) {
			xStart = 0;
			yStart = 0;
			xEnd = map.getMapWidth();
			yEnd = map.getMapHeight();
		}
		
		int halfTileSize = tileSize / 2;
		FontMetrics fm = g.getFontMetrics();
		
		/*
		 * Terrain and grid.
		 */
		g.setColor(Color.BLACK);
		for (int i = xStart; i < xEnd; i++) {
			for (int j = yStart; j < yEnd; j++) {
				Tile t = map.getTile(i, j, false);
				
				g.drawRect(i * tileSize, j * tileSize, tileSize, tileSize);
			}
		}
	}

	public boolean isCaveLayer() {
		return caveLayer;
	}

	public void setCaveLayer(boolean caveLayer) {
		this.caveLayer = caveLayer;
		repaint();
	}
	
	public EditState getEditState() {
		return currentState;
	}
	
	public void setEditState(EditState e) {
		currentState = e;
	}
	
	public void zoomIn() {
		if (tileSize + TILE_SIZE_STEP > TILE_MAX_SIZE)
			return;
		
		int oldSize = tileSize;
		Container c = GraphicPanel.this.getParent();
		int newX, newY;
		
		tileSize += TILE_SIZE_STEP;
		
		revalidateScroll();
		
		if (c instanceof JViewport) {
			JViewport jv = (JViewport) c;
			Point pos = jv.getViewPosition();
			
			newX = pos.x + ((pos.x / oldSize) * (TILE_SIZE_STEP + 1));
			newY = pos.y + ((pos.y / oldSize) * (TILE_SIZE_STEP + 1));
			
			jv.setViewPosition(new Point(newX, newY));
		}
		
		repaint();
	}
	
	public void zoomOut() {
		if (tileSize - TILE_SIZE_STEP < TILE_MIN_SIZE)
			return;
		
		int oldSize = tileSize;
		Container c = GraphicPanel.this.getParent();
		int newX, newY;
		
		tileSize -= TILE_SIZE_STEP;
		
		revalidateScroll();
		
		if (c instanceof JViewport) {
			JViewport jv = (JViewport) c;
			Point pos = jv.getViewPosition();
			
			newX = pos.x - ((pos.x / oldSize) * (TILE_SIZE_STEP + 1));
			newY = pos.y - ((pos.y / oldSize) * (TILE_SIZE_STEP + 1));
			
			jv.setViewPosition(new Point(newX, newY));
		}
		
		repaint();
	}

	public void resizeMap(int[] newSize) {
		if (newSize == null)
			return;
		
		int northAdj = newSize[0];
		int eastAdj = newSize[1];
		int southAdj = newSize[2];
		int westAdj = newSize[3];
		
		int newWidth = map.getMapWidth() + eastAdj + westAdj;
		int newHeight = map.getMapHeight() + northAdj + southAdj;
		
		map.resizeMap(newWidth, newHeight, westAdj, northAdj);
		
		revalidateScroll();
		repaint();
	}

	public BufferedImage getMapImage() {
		BufferedImage mapImg = new BufferedImage(map.getMapWidth() * tileSize, map.getMapHeight() * tileSize, BufferedImage.TYPE_INT_ARGB);
		Graphics g = mapImg.getGraphics();
		saveToImage = true;
		
		paint(g);
		
		return mapImg;
	}
	
	public void revalidateScroll() {
		this.setPreferredSize(new Dimension(map.getMapWidth() * this.tileSize, map.getMapHeight() * this.tileSize));
		this.revalidate();
	}
	
	private void setupMouseDrag() {
		MouseInputAdapter mia = new MouseInputAdapter() {
			int m_XDifference, m_YDifference;
			boolean m2or3_dragging, m1_dragging;
			Container c;

			@Override
			public void mouseDragged(MouseEvent e) {
				if (m2or3_dragging) {
					c = GraphicPanel.this.getParent();
					if (c instanceof JViewport) {
						JViewport jv = (JViewport) c;
						Point p = jv.getViewPosition();
						int newX = p.x - (e.getX() - m_XDifference);
						int newY = p.y - (e.getY() - m_YDifference);
	
						int maxX = GraphicPanel.this.getWidth() - jv.getWidth();
						int maxY = GraphicPanel.this.getHeight() - jv.getHeight();
						if (newX < 0)
							newX = 0;
						if (newX > maxX)
							newX = maxX;
						if (newY < 0)
							newY = 0;
						if (newY > maxY)
							newY = maxY;
	
						jv.setViewPosition(new Point(newX, newY));
					}
				}
				if (m1_dragging)
					draggedMouse(e.getPoint());
				
				mouseMoved(e);
			}
			
			@Override
			public void mouseMoved(MouseEvent e) {
				mouseX = e.getX();
				mouseY = e.getY();
				repaint();
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3 || e.getButton() == MouseEvent.BUTTON2) {
					setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
					m_XDifference = e.getX();
					m_YDifference = e.getY();
					m2or3_dragging = true;
				} else if (e.getButton() == MouseEvent.BUTTON1) {
					m1_dragging = true;
				}
			}
	
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3 || e.getButton() == MouseEvent.BUTTON2) {
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					m2or3_dragging = false;
				} else if (e.getButton() == MouseEvent.BUTTON1) {
					m1_dragging = false;
				}
			}   
			
			@Override
			public void mouseClicked(MouseEvent e) { 
				if (e.getButton() == MouseEvent.BUTTON1)
					clickedMouse(e.getPoint());
				else if (e.getButton() == MouseEvent.BUTTON3)
					clickedRightMouse(e.getPoint());
			}
		};
		
		addMouseMotionListener(mia);
		addMouseListener(mia);
	}
	
	public void draggedMouse(Point p) {
		int x = (int) (p.getX() / tileSize);
		int y = (int) (p.getY() / tileSize);
		
		int objLocX = (int) ((p.getX() - (x * tileSize)) / (tileSize / 3));
		int objLocY = (int) ((p.getY() - (y * tileSize)) / (tileSize / 3));
		int objLoc = (objLocY * 3) + objLocX;
		
		int fenceLocX = (int) ((p.getX() - (x * tileSize)) / (tileSize / 4));
		int fenceLocY = (int) ((p.getY() - (y * tileSize)) / (tileSize / 4));
		Tile rightTile = map.getTile(x + 1, y, false);
		Tile downTile = map.getTile(x, y + 1, false);
		
		if (downTile == null)
			if (y + 1 < map.getMapHeight()) {
				downTile = new Tile(x, y + 1);
				map.addTile(x, y + 1, downTile);
			}
		if (rightTile == null) 
			if (x + 1 < map.getMapWidth()) {
				rightTile = new Tile(x + 1, y);
				map.addTile(x + 1, y, rightTile);
			}
		
		/*int relevantType;
		Color c = mainWindow.getOverlayColor();
		
		Tile t = map.getTile(x, y, true);
		if (t == null) {
			map.addTile(x, y, new Tile(x, y));
			t = map.getTile(x, y, true);
		}
		
		if (x < map.getMapWidth() && x >= 0 && y >= 0 && y < map.getMapHeight()) {
			switch (currentState) {
				case TERRAIN_PENCIL:
					relevantType = mainWindow.getSelectedTerrain();
					if (t.getTerrainType(caveLayer) != relevantType)
						currentDragAction.addAction(new TileChange(t, (byte) relevantType, caveLayer, mainWindow.isCaveEntrance(relevantType)));
					break;
				case TERRAIN_BRUSH:
					relevantType = mainWindow.getSelectedTerrain();
					currentDragAction.addAction(new BrushTileChange(map, x, y, mainWindow.getBrushSize(), (byte) relevantType, caveLayer));
					break;
				case TERRAIN_FILL:
					relevantType = mainWindow.getSelectedTerrain();
					if (currentDragAction.isEmpty())
						if (t.getTerrainType(caveLayer) != relevantType)
							currentDragAction.addAction(new TileFill(map, x, y, (byte) relevantType, caveLayer));
					break;
				case TERRAIN_ERASER:
					relevantType = 0;
					currentDragAction.addAction(new TileChange(t, (byte) relevantType, caveLayer, false));
					break;
				case TERRAIN_PICKER:
					if (mainWindow.getSelectedTerrain() != t.getTerrainType(caveLayer))
						mainWindow.forceSelection(ImageType.TERRAIN, t.getTerrainType(caveLayer));
					break;
				case OBJECT_PENCIL:
					relevantType = mainWindow.getSelectedObject();
					if (t.getObjectType(caveLayer, objLoc) != relevantType)
						currentDragAction.addAction(new ObjectChange(t, (byte) relevantType, objLoc, caveLayer));
					break;
				case OBJECT_ERASER:
					relevantType = 0;
					currentDragAction.addAction(new ObjectChange(t, (byte) relevantType, objLoc, caveLayer));
					break;
				case OBJECT_PICKER:
					if (mainWindow.getSelectedObject() != t.getObjectType(caveLayer, objLoc))
						mainWindow.forceSelection(ImageType.OBJECT, t.getObjectType(caveLayer, objLoc));
					break;
				case FENCE_PENCIL:
				case FENCE_ERASER:
					relevantType = (currentState == EditState.FENCE_ERASER ? 0 : mainWindow.getSelectedFence());

					switch (fenceLocX) {
						case 0:
							if (fenceLocY == 1 || fenceLocY == 2)
								if (t.getFenceType(caveLayer, Tile.LEFT_FENCE) != relevantType + 1)
									currentDragAction.addAction(new FenceChange(t, (byte) (relevantType + 1), Tile.LEFT_FENCE, caveLayer));
							break;
						case 1:
						case 2:
							if (fenceLocY == 0) {
								if (t.getFenceType(caveLayer, Tile.TOP_FENCE) != relevantType)
									currentDragAction.addAction(new FenceChange(t, (byte) relevantType, Tile.TOP_FENCE, caveLayer));
							} else if (fenceLocY == 3) {
								if (downTile != null)
									if (downTile.getFenceType(caveLayer, Tile.TOP_FENCE) != relevantType)
										currentDragAction.addAction(new FenceChange(downTile, (byte) relevantType, Tile.TOP_FENCE, caveLayer));
							}
							break;
						case 3:
							if (fenceLocY == 1 || fenceLocY == 2) {								
								if (rightTile != null)
									if (rightTile.getFenceType(caveLayer, Tile.LEFT_FENCE) != relevantType + 1)
										currentDragAction.addAction(new FenceChange(rightTile, (byte) (relevantType + 1), Tile.LEFT_FENCE, caveLayer));
							}
							break;
					}
					break;
				case FENCE_LINE:
					relevantType = mainWindow.getSelectedFence();
					
					if (fenceLineChange == null) {
						switch (fenceLocX) {
							case 0:
								if (fenceLocY == 1 || fenceLocY == 2)
									fenceLineChange = new FenceLine(map, x, y, (byte) relevantType, Tile.LEFT_FENCE, caveLayer);
								break;
							case 1:
							case 2:
								if (fenceLocY == 0) {
									fenceLineChange = new FenceLine(map, x, y, (byte) relevantType, Tile.TOP_FENCE, caveLayer);
								} else if (fenceLocY == 3) {
									fenceLineChange = new FenceLine(map, x, y + 1, (byte) relevantType, Tile.TOP_FENCE, caveLayer);
								}
								break;
							case 3:
								if (fenceLocY == 1 || fenceLocY == 2) {								
									fenceLineChange = new FenceLine(map, x + 1, y, (byte) relevantType, Tile.LEFT_FENCE, caveLayer);
								}
								break;
						}
					}
					
					if (fenceLineChange != null)
						fenceLineChange.update(x, y);
					break;
				case FENCE_PICKER:
					relevantType = mainWindow.getSelectedFence();
					
					switch (fenceLocX) {
						case 0:
							if (fenceLocY == 1 || fenceLocY == 2)
								relevantType = t.getFenceType(caveLayer, Tile.LEFT_FENCE) - 1;
							break;
						case 1:
						case 2:
							if (fenceLocY == 0) {
								relevantType = t.getFenceType(caveLayer, Tile.TOP_FENCE);
							} else if (fenceLocY == 3) {
								relevantType = downTile.getFenceType(caveLayer, Tile.TOP_FENCE);
							}
							break;
						case 3:
							if (fenceLocY == 1 || fenceLocY == 2) {								
								relevantType = rightTile.getFenceType(caveLayer, Tile.LEFT_FENCE) - 1;
							}
							break;
					}
					
					if (relevantType < 0)
						relevantType = 0;
					
					if (mainWindow.getSelectedFence() != relevantType)
						mainWindow.forceSelection(ImageType.FENCE, relevantType);
					break;
				case OVERLAY_PENCIL:
					if (t.getOverlayColor(caveLayer) != c)
						currentDragAction.addAction(new OverlayChange(t, c, caveLayer));
					break;
				case OVERLAY_BRUSH:
					currentDragAction.addAction(new BrushOverlayChange(map, x, y, mainWindow.getBrushSize(), c, caveLayer));
					break;
				case OVERLAY_ERASER:
					c = null;
					if (t.getOverlayColor(caveLayer) != c)
						currentDragAction.addAction(new OverlayChange(t, c, caveLayer));
					break;
				case OVERLAY_PICKER:
					mainWindow.setOverlayColor(t.getOverlayColor(caveLayer));
					break;
				default:
					break;
			}
		}*/
		
		repaint();
	}

	public void clickedMouse(Point p) {		
		int x = (int) (p.getX() / tileSize);
		int y = (int) (p.getY() / tileSize);
		
		int objLocX = (int) ((p.getX() - (x * tileSize)) / (tileSize / 3));
		int objLocY = (int) ((p.getY() - (y * tileSize)) / (tileSize / 3));
		int objLoc = (objLocY * 3) + objLocX;
		
		int fenceLocX = (int) ((p.getX() - (x * tileSize)) / (tileSize / 4));
		int fenceLocY = (int) ((p.getY() - (y * tileSize)) / (tileSize / 4));
		Tile rightTile = map.getTile(x + 1, y, false);
		Tile downTile = map.getTile(x, y + 1, false);
		
		if (downTile == null)
			if (y + 1 < map.getMapHeight()) {
				downTile = new Tile(x, y + 1);
				map.addTile(x, y + 1, downTile);
			}
		if (rightTile == null) 
			if (x + 1 < map.getMapWidth()) {
				rightTile = new Tile(x + 1, y);
				map.addTile(x + 1, y, rightTile);
			}
		
		/*int relevantType;
		Color c = mainWindow.getOverlayColor();
		
		Tile t = map.getTile(x, y, true);
		if (t == null) {
			map.addTile(x, y, new Tile(x, y));
			t = map.getTile(x, y, true);
		}
		
		if (x < map.getMapWidth() && x >= 0 && y >= 0 && y < map.getMapHeight()) {
			switch (currentState) {
				case TERRAIN_PENCIL:
					relevantType = mainWindow.getSelectedTerrain();
					if (t.getTerrainType(caveLayer) != relevantType)
						undoManager.addAction(new TileChange(t, (byte) relevantType, caveLayer, mainWindow.isCaveEntrance(relevantType)));
					break;
				case TERRAIN_BRUSH:
					relevantType = mainWindow.getSelectedTerrain();
					
					undoManager.addAction(new BrushTileChange(map, x, y, mainWindow.getBrushSize(), (byte) relevantType, caveLayer));
					break;
				case TERRAIN_FILL:
					relevantType = mainWindow.getSelectedTerrain();
					if (t.getTerrainType(caveLayer) != relevantType)
						undoManager.addAction(new TileFill(map, x, y, (byte) relevantType, caveLayer));
					break;
				case TERRAIN_ERASER:
					relevantType = 0;
					if (t.getTerrainType(caveLayer) != relevantType)
						undoManager.addAction(new TileChange(t, (byte) relevantType, caveLayer, false));
					break;
				case TERRAIN_PICKER:
					if (mainWindow.getSelectedTerrain() != t.getTerrainType(caveLayer));
						mainWindow.forceSelection(ImageType.TERRAIN, t.getTerrainType(caveLayer));
					break;
				case OBJECT_PENCIL:
					relevantType = mainWindow.getSelectedObject();
					if (t.getObjectType(caveLayer, objLoc) != relevantType)
						undoManager.addAction(new ObjectChange(t, (byte) relevantType, objLoc, caveLayer));
					break;
				case OBJECT_ERASER:
					relevantType = 0;
					undoManager.addAction(new ObjectChange(t, (byte) relevantType, objLoc, caveLayer));
					break;
				case OBJECT_PICKER:
					if (mainWindow.getSelectedObject() != t.getObjectType(caveLayer, objLoc))
						mainWindow.forceSelection(ImageType.OBJECT, t.getObjectType(caveLayer, objLoc));
					break;
				case FENCE_PENCIL:
				case FENCE_LINE:
				case FENCE_ERASER:
					relevantType = (currentState == EditState.FENCE_ERASER ? 0 : mainWindow.getSelectedFence());

					switch (fenceLocX) {
						case 0:
							if (fenceLocY == 1 || fenceLocY == 2)
								if (t.getFenceType(caveLayer, Tile.LEFT_FENCE) != relevantType + 1)
									undoManager.addAction(new FenceChange(t, (byte) (relevantType + 1), Tile.LEFT_FENCE, caveLayer));
							break;
						case 1:
						case 2:
							if (fenceLocY == 0) {
								if (t.getFenceType(caveLayer, Tile.TOP_FENCE) != relevantType)
									undoManager.addAction(new FenceChange(t, (byte) relevantType, Tile.TOP_FENCE, caveLayer));
							} else if (fenceLocY == 3) {
								if (downTile != null)
									if (downTile.getFenceType(caveLayer, Tile.TOP_FENCE) != relevantType)
										undoManager.addAction(new FenceChange(downTile, (byte) relevantType, Tile.TOP_FENCE, caveLayer));
							}
							break;
						case 3:
							if (fenceLocY == 1 || fenceLocY == 2) {								
								if (rightTile != null)
									if (rightTile.getFenceType(caveLayer, Tile.LEFT_FENCE) != relevantType + 1)
										undoManager.addAction(new FenceChange(rightTile, (byte) (relevantType + 1), Tile.LEFT_FENCE, caveLayer));
							}
							break;
					}
					break;
				case FENCE_PICKER:	
					relevantType = mainWindow.getSelectedFence();
					
					switch (fenceLocX) {
						case 0:
							if (fenceLocY == 1 || fenceLocY == 2)
								relevantType = t.getFenceType(caveLayer, Tile.LEFT_FENCE) - 1;
							break;
						case 1:
						case 2:
							if (fenceLocY == 0) {
								relevantType = t.getFenceType(caveLayer, Tile.TOP_FENCE);
							} else if (fenceLocY == 3) {
								relevantType = downTile.getFenceType(caveLayer, Tile.TOP_FENCE);
							}
							break;
						case 3:
							if (fenceLocY == 1 || fenceLocY == 2) {								
								relevantType = rightTile.getFenceType(caveLayer, Tile.LEFT_FENCE) - 1;
							}
							break;
					}
					
					if (mainWindow.getSelectedFence() != relevantType)
						mainWindow.forceSelection(ImageType.FENCE, relevantType);
					break;
				case OVERLAY_PENCIL:
					if (t.getOverlayColor(caveLayer) != c)
						undoManager.addAction(new OverlayChange(t, c, caveLayer));
					break;
				case OVERLAY_BRUSH:
					undoManager.addAction(new BrushOverlayChange(map, x, y, mainWindow.getBrushSize(), c, caveLayer));
					break;
				case OVERLAY_ERASER:
					c = null;
					if (t.getOverlayColor(caveLayer) != c)
						undoManager.addAction(new OverlayChange(t, c, caveLayer));
					break;
				case OVERLAY_PICKER:
					mainWindow.setOverlayColor(t.getOverlayColor(caveLayer));
					break;
				case LABEL:
					LabelDialog d = new LabelDialog();
					undoManager.addAction(new LabelChange(t, d.getLabel(), mainWindow.getLabelColor(), caveLayer));
					d.dispose();
					break;
				default:
					Logger.log("Unhandled State: " + currentState);
					break;
			}
		}*/
		
		repaint();
	}
	
	public void clickedRightMouse(Point p) {
		int x = (int) (p.getX() / tileSize);
		int y = (int) (p.getY() / tileSize);
		int locX = (int) ((p.getX() - (x * tileSize)) / (tileSize / 3));
		int locY = (int) ((p.getY() - (y * tileSize)) / (tileSize / 3));
		
		switch(currentState) {
			case OBJECT_ERASER:
			case OBJECT_PENCIL:
				if (x < map.getMapWidth() && x >= 0 && y >= 0 && y < map.getMapHeight()) {
					Tile t = map.getTile(x, y, true);
					int loc = (locY * 3) + locX;
				}
				break;
			default:
				break;
		}
		
		repaint();
	}	
}
