/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sfc.sf2.map.layout.layout;

import com.sfc.sf2.graphics.Tile;
import com.sfc.sf2.map.block.MapBlock;
import com.sfc.sf2.map.block.layout.MapBlockLayout;
import com.sfc.sf2.map.layout.MapLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 *
 * @author wiz
 */
public class MapLayoutLayout extends JPanel implements MouseListener, MouseMotionListener {
    
    private static final int DEFAULT_TILES_PER_ROW = 64*3;
    
    private static final int ACTION_CHANGE_BLOCK_VALUE = 0;
    private static final int ACTION_CHANGE_BLOCK_FLAGS = 1;
    
    private MapBlock selectedBlock0;
    
    private List<int[]> actions = new ArrayList<int[]>();
    
    private int tilesPerRow = DEFAULT_TILES_PER_ROW;
    private MapLayout layout;
    private MapBlock[] blockset;
    private int currentDisplaySize = 1;
    
    private BufferedImage currentImage;
    private boolean redraw = true;
    private int renderCounter = 0;
    private boolean drawFlags = false;
    private boolean drawGrid = false;
    
    private BufferedImage gridImage;
    private BufferedImage obstructedImage;
    private BufferedImage leftUpstairsImage;
    private BufferedImage rightUpstairsImage;

    public MapLayoutLayout() {
       addMouseListener(this);
       addMouseMotionListener(this);
    }
   
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);   
        g.drawImage(buildImage(), 0, 0, this);       
    }
    
    public BufferedImage buildImage(){
        if(redraw){
            currentImage = buildImage(this.layout,this.tilesPerRow, false);
            setSize(currentImage.getWidth(), currentImage.getHeight());
        }
        return currentImage;
    }
    
    public BufferedImage buildImage(MapLayout layout, int tilesPerRow, boolean pngExport){
        renderCounter++;
        System.out.println("Map render "+renderCounter);
        this.layout = layout;
        if(redraw){
            MapBlock[] blocks = layout.getBlocks();
            int imageHeight = 64*3*8;
            Color[] palette = blocks[0].getTiles()[0].getPalette();
            palette[0] = new Color(255, 255, 255, 0);
            IndexColorModel icm = buildIndexColorModel(palette);
            currentImage = new BufferedImage(tilesPerRow*8, imageHeight , BufferedImage.TYPE_BYTE_INDEXED, icm);
            Graphics graphics = currentImage.getGraphics();            
            for(int y=0;y<64;y++){
                for(int x=0;x<64;x++){
                    MapBlock block = blocks[y*64+x];
                    BufferedImage blockImage = block.getImage();
                    BufferedImage flagImage = block.getFlagImage();
                    if(blockImage==null){
                        blockImage = new BufferedImage(3*8, 3*8 , BufferedImage.TYPE_BYTE_INDEXED, icm);
                        Graphics blockGraphics = blockImage.getGraphics();                    
                        blockGraphics.drawImage(block.getTiles()[0].getImage(), 0*8, 0*8, null);
                        blockGraphics.drawImage(block.getTiles()[1].getImage(), 1*8, 0*8, null);
                        blockGraphics.drawImage(block.getTiles()[2].getImage(), 2*8, 0*8, null);
                        blockGraphics.drawImage(block.getTiles()[3].getImage(), 0*8, 1*8, null);
                        blockGraphics.drawImage(block.getTiles()[4].getImage(), 1*8, 1*8, null);
                        blockGraphics.drawImage(block.getTiles()[5].getImage(), 2*8, 1*8, null);
                        blockGraphics.drawImage(block.getTiles()[6].getImage(), 0*8, 2*8, null);
                        blockGraphics.drawImage(block.getTiles()[7].getImage(), 1*8, 2*8, null);
                        blockGraphics.drawImage(block.getTiles()[8].getImage(), 2*8, 2*8, null);
                        block.setImage(blockImage);
                    }
                    graphics.drawImage(blockImage, x*3*8, y*3*8, null);
                    if(drawFlags){
                        if(flagImage==null){
                            flagImage = new BufferedImage(3*8, 3*8, BufferedImage.TYPE_INT_ARGB);
                            Graphics2D g2 = (Graphics2D) flagImage.getGraphics(); 
                            if((block.getFlags()&0xC000)==0xC000){
                                g2.drawImage(getObstructedImage(), 0, 0, null);
                            }else if((block.getFlags()&0x8000)==0x8000){
                                g2.drawImage(getRightUpstairs(), 0, 0, null);
                            }else if((block.getFlags()&0x4000)==0x4000){
                                g2.drawImage(getLeftUpstairs(), 0, 0, null);
                            }
                            block.setFlagImage(flagImage);
                        }
                        graphics.drawImage(flagImage, x*3*8, y*3*8, null);
                    }
                }
            } 
            if(drawGrid){
                graphics.drawImage(getGridImage(), 0, 0, null);
            }            
            redraw = false;
            currentImage = resize(currentImage);
        }
                  
        return currentImage;
    }
    
    private BufferedImage getGridImage(){
        if(gridImage==null){
            gridImage = new BufferedImage(3*8*64, 3*8*64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = (Graphics2D) gridImage.getGraphics(); 
            g2.setColor(Color.BLACK);
            for(int i=0;i<64;i++){
                g2.drawLine(3*8+i*3*8, 0, 3*8+i*3*8, 3*8*64-1);
                g2.drawLine(0, 3*8+i*3*8, 3*8*64-1, 3*8+i*3*8);
            }
        }
        return gridImage;
    }
    
    private BufferedImage getObstructedImage(){
        if(obstructedImage==null){
            obstructedImage = new BufferedImage(3*8, 3*8, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = (Graphics2D) obstructedImage.getGraphics();  
            g2.setColor(Color.WHITE);
            Line2D line1 = new Line2D.Double(6, 6, 18, 18);
            g2.draw(line1);
            Line2D line2 = new Line2D.Double(6, 18, 18, 6);
            g2.draw(line2);
        }
        return obstructedImage;
    }
    
    private BufferedImage getLeftUpstairs(){
        if(leftUpstairsImage==null){
            leftUpstairsImage = new BufferedImage(3*8, 3*8, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = (Graphics2D) leftUpstairsImage.getGraphics();  
            g2.setColor(Color.BLUE);
            Line2D line0 = new Line2D.Double(3-1, 3, 21-1, 21);
            Line2D line1 = new Line2D.Double(3, 3, 21, 21);
            Line2D line2 = new Line2D.Double(3+1, 3, 21+1, 21);
            g2.draw(line0);
            g2.draw(line1);
            g2.draw(line2);
        }
        return leftUpstairsImage;
    }   
    
    private BufferedImage getRightUpstairs(){
        if(rightUpstairsImage==null){
            rightUpstairsImage = new BufferedImage(3*8, 3*8, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = (Graphics2D) rightUpstairsImage.getGraphics();  
            g2.setColor(Color.BLUE);
            Line2D line0 = new Line2D.Double(3-1, 21, 21-1, 3);
            Line2D line1 = new Line2D.Double(3, 21, 21, 3);
            Line2D line2 = new Line2D.Double(3+1, 21, 21+1, 3);
            g2.draw(line0);
            g2.draw(line1);
            g2.draw(line2);
        }
        return rightUpstairsImage;
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
    
    public void resize(int size){
        this.currentDisplaySize = size;
        currentImage = resize(currentImage);
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
        redraw = true;
    }

    public MapLayout getMapLayout() {
        return layout;
    }

    public void setMapLayout(MapLayout layout) {
        this.layout = layout;
    }


    @Override
    public void mouseClicked(MouseEvent e) {
    }
    @Override
    public void mouseEntered(MouseEvent e) {

    }
    @Override
    public void mouseExited(MouseEvent e) {

    }
    @Override
    public void mousePressed(MouseEvent e) {
        int x = e.getX() / (currentDisplaySize * 3*8);
        int y = e.getY() / (currentDisplaySize * 3*8);
        
        switch (e.getButton()) {
            case MouseEvent.BUTTON1:
                setBlockValue(x, y, MapBlockLayout.selectedBlockIndex0);
                if(selectedBlock0!=null && selectedBlock0.getIndex()==MapBlockLayout.selectedBlockIndex0){
                    setFlagValue(x, y, selectedBlock0.getFlags());
                }
                break;
            case MouseEvent.BUTTON2:
                selectedBlock0 = layout.getBlocks()[y*64+x];
                MapBlockLayout.selectedBlockIndex0 = selectedBlock0.getIndex();
                break;
            case MouseEvent.BUTTON3:
                setBlockValue(x, y, MapBlockLayout.selectedBlockIndex1);
                break;
            default:
                break;
        }
        this.repaint();
        System.out.println("Blockset press "+e.getButton()+" "+x+" - "+y);
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        
    }
    
    public void setBlockValue(int x, int y, int value){
        MapBlock[] blocks = layout.getBlocks();
        MapBlock block = blocks[y*64+x];
        if(block.getIndex()!=value){
            int[] action = new int[3];
            action[0] = ACTION_CHANGE_BLOCK_VALUE;
            action[1] = y*64+x;
            action[2] = block.getIndex();
            block.setIndex(value);
            block.setImage(null);
            block.setTiles(blockset[block.getIndex()].getTiles());
            actions.add(action);
            redraw = true;
        }
    }
    
    public void setFlagValue(int x, int y, int value){
        MapBlock[] blocks = layout.getBlocks();
        MapBlock block = blocks[y*64+x];
        if(block.getFlags()!=value){
            int[] action = new int[3];
            action[0] = ACTION_CHANGE_BLOCK_FLAGS;
            action[1] = y*64+x;
            action[2] = block.getFlags();
            block.setFlags(value);
            block.setFlagImage(null);
            actions.add(action);
            redraw = true;
        }
    }
    
    public void revertLastAction(){
        if(actions.size()>0){
            int[] action = actions.get(actions.size()-1);
            if(action[0]==ACTION_CHANGE_BLOCK_VALUE){
                MapBlock block = layout.getBlocks()[action[1]];
                block.setIndex(action[2]);
                block.setImage(null);
                block.setTiles(blockset[block.getIndex()].getTiles());
                actions.remove(actions.size()-1);
                redraw = true;
                this.repaint();
            }else if(action[0]==ACTION_CHANGE_BLOCK_FLAGS){
                MapBlock block = layout.getBlocks()[action[1]];
                block.setFlags(action[2]);
                block.setFlagImage(null);
                actions.remove(actions.size()-1);
                redraw = true;
                this.repaint();
            }
        }
    }

    public MapBlock[] getBlockset() {
        return blockset;
    }

    public void setBlockset(MapBlock[] blockset) {
        this.blockset = blockset;
    }

    public boolean isDrawFlags() {
        return drawFlags;
    }

    public void setDrawFlags(boolean drawFlags) {
        this.drawFlags = drawFlags;
        this.redraw = true;
    }

    public MapBlock getSelectedBlock0() {
        return selectedBlock0;
    }

    public void setSelectedBlock0(MapBlock selectedBlock0) {
        this.selectedBlock0 = selectedBlock0;
    }

    public List<int[]> getActions() {
        return actions;
    }

    public void setActions(List<int[]> actions) {
        this.actions = actions;
    }

    public boolean isRedraw() {
        return redraw;
    }

    public void setRedraw(boolean redraw) {
        this.redraw = redraw;
    }

    public boolean isDrawGrid() {
        return drawGrid;
    }

    public void setDrawGrid(boolean drawGrid) {
        this.drawGrid = drawGrid;
        this.redraw = true;
    }
    
    
    
    
}
