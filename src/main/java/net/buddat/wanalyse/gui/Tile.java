package net.buddat.wanalyse.gui;


public class Tile {
	
	private short posX, posY;
	
	private byte terrainType;
	
	public Tile(int x, int y) {
		setX((short) x);
		setY((short) y);
	}
	
	public short getX() {
		return posX;
	}

	public void setX(short x) {
		this.posX = x;
	}

	public short getY() {
		return posY;
	}

	public void setY(short y) {
		this.posY = y;
	}

	public byte getTerrainType(boolean cave) {
		return terrainType;
	}
}
