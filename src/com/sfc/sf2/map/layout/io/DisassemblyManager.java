/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sfc.sf2.map.layout.io;

import com.sfc.sf2.graphics.Tile;
import com.sfc.sf2.map.layout.MapLayout;
import com.sfc.sf2.map.block.MapBlock;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author wiz
 */
public class DisassemblyManager {

    private static final int MAPLAYOUT_TILES_LENGTH = 128*5;
    
    private byte[] inputData;
    private short inputWord = 0;
    private int inputCursor = -2;
    private int inputBitCursor = 16;
    private List<Short> outputData = null;
    private Tile[] outputTiles = null;
    private StringBuilder debugSb = null;
    
    Color[] palette = null;
    Tile[] tileset = new Tile[128*5];
    private Short[] rightTileHistory = new Short[0x800];
    private Short[] bottomTileHistory = new Short[0x800];
    
    public MapLayout importDisassembly(String palettePath, String tileset1Path, String tileset2Path, String tileset3Path, String tileset4Path, String tileset5Path, String blocksPath, String layoutPath){
        System.out.println("com.sfc.sf2.maplayout.io.DisassemblyManager.importDisassembly() - Importing disassembly ...");
        MapLayout layout = new MapLayout();
        try{
            com.sfc.sf2.map.block.io.DisassemblyManager blockManager = new com.sfc.sf2.map.block.io.DisassemblyManager(); 
            MapBlock[] blocks = blockManager.importDisassembly(palettePath, tileset1Path, tileset2Path, tileset3Path, tileset4Path, tileset5Path, blocksPath);

            if(blocks!=null){
                layout = parseLayoutData();
            }
        }catch(Exception e){
             System.err.println("com.sfc.sf2.maplayout.io.PngManager.importPng() - Error while parsing graphics data : "+e);
             e.printStackTrace();
        }         
                
        System.out.println("com.sfc.sf2.maplayout.io.DisassemblyManager.importDisassembly() - Disassembly imported.");
        return layout;
    }
    
    private MapLayout parseLayoutData(){
        MapLayout layout = null;
        MapBlock[] blocks = null;
        try{

                      
        }catch(Exception e){
             System.err.println("com.sfc.sf2.maplayout.io.DisassemblyManager.parseGraphics() - Error while parsing block data : "+e);
             e.printStackTrace();
        } 
        return layout;
    }   
    
    private static short getNextWord(byte[] data, int cursor){
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.put(data[cursor+1]);
        bb.put(data[cursor]);
        short s = bb.getShort(0);
        return s;
    }    
    
    private int getNextBit(){
        int bit = 0;
        if(inputBitCursor>=16){
            inputBitCursor = 0;
            inputCursor+=2;
            inputWord = getNextWord(inputData, inputCursor);
        } 
        bit = (inputWord>>(15-inputBitCursor)) & 1;
        inputBitCursor++;
        //debugSb.append(bit);
        return bit;
    }

    public void exportDisassembly(MapBlock[] blocks, String blocksFilePath, MapLayout layout, String layoutFilePath){
        System.out.println("com.sfc.sf2.maplayout.io.DisassemblyManager.exportDisassembly() - Exporting disassembly ...");
        try {
            com.sfc.sf2.map.block.io.DisassemblyManager blockManager = new com.sfc.sf2.map.block.io.DisassemblyManager(); 
            byte[] blockBytes = produceLayoutBytes(layout);
            Path graphicsFilePath = Paths.get(layoutFilePath);
            Files.write(graphicsFilePath,blockBytes);
            System.out.println(blockBytes.length + " bytes into " + graphicsFilePath);
        } catch (Exception ex) {
            Logger.getLogger(DisassemblyManager.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            System.out.println(ex);
        }            
        System.out.println("com.sfc.sf2.maplayout.io.DisassemblyManager.exportDisassembly() - Disassembly exported.");        
    }     
    
    private byte[] produceLayoutBytes(MapLayout layout){
        StringBuilder outputSb = new StringBuilder();
        byte[] output = null;

        
        return output;
    }
    
}
