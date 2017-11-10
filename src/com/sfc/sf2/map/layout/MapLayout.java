/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sfc.sf2.map.layout;

import com.sfc.sf2.map.block.MapBlock;

/**
 *
 * @author wiz
 */
public class MapLayout {
    
    private int index;
    
    private MapBlock[] blocks; 
    
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }  

    public MapBlock[] getBlocks() {
        return blocks;
    }

    public void setBlocks(MapBlock[] blocks) {
        this.blocks = blocks;
    }

    
}
