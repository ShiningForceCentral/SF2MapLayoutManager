/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sfc.sf2.map.layout;

import com.sfc.sf2.graphics.GraphicsManager;
import com.sfc.sf2.graphics.Tile;
import com.sfc.sf2.map.block.MapBlock;
import com.sfc.sf2.map.block.MapBlockManager;
import com.sfc.sf2.map.layout.io.DisassemblyManager;
import com.sfc.sf2.map.layout.io.PngManager;
import com.sfc.sf2.palette.PaletteManager;
import java.awt.Color;

/**
 *
 * @author wiz
 */
public class MapLayoutManager {
       
    private PaletteManager paletteManager = new PaletteManager();
    private GraphicsManager graphicsManager = new GraphicsManager();
    private DisassemblyManager disassemblyManager = null;
    private MapBlock[] blocks;
    private MapLayout layout;
    private MapBlock[] blockset;
       
    public void importDisassembly(String palettePath, String tileset1Path, String tileset2Path, String tileset3Path, String tileset4Path, String tileset5Path, String blocksPath, String layoutPath){
        System.out.println("com.sfc.sf2.maplayout.MapLayoutManager.importDisassembly() - Importing disassembly ...");
        disassemblyManager = new DisassemblyManager();
        layout = disassemblyManager.importDisassembly(palettePath, tileset1Path, tileset2Path, tileset3Path, tileset4Path, tileset5Path, blocksPath, layoutPath);
        blockset = disassemblyManager.getBlockset();
        System.out.println("com.sfc.sf2.maplayout.MapLayoutManager.importDisassembly() - Disassembly imported.");
    }
    
    public void importDisassembly(String palettesPath, String tilesetsPath, String tilesetsFilePath, String blocksPath, String layoutPath){
        System.out.println("com.sfc.sf2.maplayout.MapLayoutManager.importDisassembly() - Importing disassembly ...");
        disassemblyManager = new DisassemblyManager();
        layout = disassemblyManager.importDisassembly(palettesPath, tilesetsPath, tilesetsFilePath, blocksPath, layoutPath);
        blockset = disassemblyManager.getBlockset();
        System.out.println("com.sfc.sf2.maplayout.MapLayoutManager.importDisassembly() - Disassembly imported.");
    }
    
    public void exportDisassembly(String blocksPath, String layoutPath){
        System.out.println("com.sfc.sf2.maplayout.MapLayoutManager.importDisassembly() - Exporting disassembly ...");
        disassemblyManager.exportDisassembly(blocks, blocksPath, layout, layoutPath);
        System.out.println("com.sfc.sf2.maplayout.MapLayoutManager.importDisassembly() - Disassembly exported.");        
    }   
    
    public void importRom(String romFilePath, String paletteOffset, String paletteLength, String graphicsOffset, String graphicsLength){
        System.out.println("com.sfc.sf2.maplayout.MapLayoutManager.importOriginalRom() - Importing original ROM ...");
        graphicsManager.importRom(romFilePath, paletteOffset, paletteLength, graphicsOffset, graphicsLength,GraphicsManager.COMPRESSION_BASIC);
        System.out.println("com.sfc.sf2.maplayout.MapLayoutManager.importOriginalRom() - Original ROM imported.");
    }
    
    public void exportRom(String originalRomFilePath, String graphicsOffset){
        System.out.println("com.sfc.sf2.maplayout.MapLayoutManager.exportOriginalRom() - Exporting original ROM ...");
        graphicsManager.exportRom(originalRomFilePath, graphicsOffset, GraphicsManager.COMPRESSION_BASIC);
        System.out.println("com.sfc.sf2.maplayout.MapLayoutManager.exportOriginalRom() - Original ROM exported.");        
    }      
    
    public void exportPng(String filepath){
        System.out.println("com.sfc.sf2.maplayout.MapLayoutManager.exportPng() - Exporting PNG ...");
        PngManager.exportPng(layout, filepath);
        System.out.println("com.sfc.sf2.maplayout.MapLayoutManager.exportPng() - PNG exported.");       
    }

    public MapLayout getLayout() {
        return layout;
    }

    public void setLayout(MapLayout layout) {
        this.layout = layout;
    }

    public MapBlock[] getBlockset() {
        return blockset;
    }

    public void setBlockset(MapBlock[] blockset) {
        this.blockset = blockset;
    }
    
    
}
