/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sfc.sf2.map.layout.layout;

import com.sfc.sf2.graphics.Tile;
import com.sfc.sf2.map.block.MapBlock;
import com.sfc.sf2.map.layout.MapLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import javax.swing.JPanel;

/**
 *
 * @author wiz
 */
public class MapLayoutLayout extends JPanel {
    
    private static final int DEFAULT_TILES_PER_ROW = 3;
    
    private int tilesPerRow = DEFAULT_TILES_PER_ROW;
    private MapLayout layout;
    private int currentDisplaySize = 1;
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);   
        g.drawImage(buildImage(), 0, 0, this);       
    }
    
    public BufferedImage buildImage(){
        BufferedImage image = buildImage(this.layout,this.tilesPerRow, false);
        setSize(image.getWidth(), image.getHeight());
        return image;
    }
    
    public BufferedImage buildImage(MapLayout layout, int tilesPerRow, boolean pngExport){
        MapBlock[] blocks = layout.getBlocks();
        int imageHeight = blocks.length*3*8;
        BufferedImage image;
        IndexColorModel icm = buildIndexColorModel(blocks[0].getTiles()[0].getPalette());
        image = new BufferedImage(tilesPerRow*8, imageHeight , BufferedImage.TYPE_BYTE_BINARY, icm);
        Graphics graphics = image.getGraphics();        

        for(int i=0;i<blocks.length;i++){
            graphics.drawImage(blocks[i].getTiles()[0].getImage(), 0*8, i*3*8 + 0*8, null);
            graphics.drawImage(blocks[i].getTiles()[1].getImage(), 1*8, i*3*8 + 0*8, null);
            graphics.drawImage(blocks[i].getTiles()[2].getImage(), 2*8, i*3*8 + 0*8, null);
            graphics.drawImage(blocks[i].getTiles()[3].getImage(), 0*8, i*3*8 + 1*8, null);
            graphics.drawImage(blocks[i].getTiles()[4].getImage(), 1*8, i*3*8 + 1*8, null);
            graphics.drawImage(blocks[i].getTiles()[5].getImage(), 2*8, i*3*8 + 1*8, null);
            graphics.drawImage(blocks[i].getTiles()[6].getImage(), 0*8, i*3*8 + 2*8, null);
            graphics.drawImage(blocks[i].getTiles()[7].getImage(), 1*8, i*3*8 + 2*8, null);
            graphics.drawImage(blocks[i].getTiles()[8].getImage(), 2*8, i*3*8 + 2*8, null);
        }                  
        return resize(image);
    }
    
    private IndexColorModel buildIndexColorModel(Color[] colors){
        byte[] reds = new byte[16];
        byte[] greens = new byte[16];
        byte[] blues = new byte[16];
        byte[] alphas = new byte[16];
        reds[0] = (byte)0xFF;
        greens[0] = (byte)0xFF;
        blues[0] = (byte)0xFF;
        alphas[0] = 0;
        for(int i=1;i<16;i++){
            reds[i] = (byte)colors[i].getRed();
            greens[i] = (byte)colors[i].getGreen();
            blues[i] = (byte)colors[i].getBlue();
            alphas[i] = (byte)0xFF;
        }
        IndexColorModel icm = new IndexColorModel(4,16,reds,greens,blues,alphas);
        return icm;
    }    
    
    private BufferedImage resize(BufferedImage image){
        BufferedImage newImage = new BufferedImage(image.getWidth()*currentDisplaySize, image.getHeight()*currentDisplaySize, BufferedImage.TYPE_INT_ARGB);
        Graphics g = newImage.getGraphics();
        g.drawImage(image, 0, 0, image.getWidth()*currentDisplaySize, image.getHeight()*currentDisplaySize, null);
        g.dispose();
        return newImage;
    }    
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(getWidth(), getHeight());
    }
    
    public int getTilesPerRow() {
        return tilesPerRow;
    }

    public void setTilesPerRow(int tilesPerRow) {
        this.tilesPerRow = tilesPerRow;
    }

    public int getCurrentDisplaySize() {
        return currentDisplaySize;
    }

    public void setCurrentDisplaySize(int currentDisplaySize) {
        this.currentDisplaySize = currentDisplaySize;
    }

    public MapLayout getMapLayout() {
        return layout;
    }

    public void setMapLayout(MapLayout layout) {
        this.layout = layout;
    }
    
    
}
