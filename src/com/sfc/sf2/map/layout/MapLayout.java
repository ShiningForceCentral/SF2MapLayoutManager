/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sfc.sf2.map.layout;

import com.sfc.sf2.map.block.MapBlock;
import com.sfc.sf2.map.block.Tileset;
import com.sfc.sf2.palette.Palette;
import java.awt.image.IndexColorModel;

/**
 *
 * @author wiz
 */
public class MapLayout {
    
    public static final int BLOCK_WIDTH = 64;
    public static final int BLOCK_HEIGHT = 64;
    
    private int index;
     
    private Tileset[] tilesets;
    private MapBlock[] blocks;
    
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Tileset[] getTilesets() {
        return tilesets;
    }

    public void setTilesets(Tileset[] tilesets) {
        this.tilesets = tilesets;
    }

    public MapBlock[] getBlocks() {
        return blocks;
    }

    public void setBlocks(MapBlock[] blocks) {
        this.blocks = blocks;
    }

    public Palette getPalette() {
        if (blocks == null) {
            return null;
        } else {
            return blocks[0].getPalette();
        }
    }

    public IndexColorModel getIcm() {
        Palette palette = getPalette();
        if (palette == null) {
            return null;
        } else {
            return palette.getIcm();
        }
    }
}
