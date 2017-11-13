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
    private short[] outputData = null;
    private Tile[] outputTiles = null;
    private StringBuilder debugSb = null;
    
    private int blocksetCursor = 2;
    private int blockCursor = 0;
    
    Color[] palette = null;
    Tile[] tileset = new Tile[128*5];
    private Short[] rightTileHistory = new Short[0x800];
    private Short[] bottomTileHistory = new Short[0x800];
    
    public MapLayout importDisassembly(String palettePath, String tileset1Path, String tileset2Path, String tileset3Path, String tileset4Path, String tileset5Path, String blocksPath, String layoutPath){
        System.out.println("com.sfc.sf2.maplayout.io.DisassemblyManager.importDisassembly() - Importing disassembly ...");
        MapLayout layout = new MapLayout();
        try{
            com.sfc.sf2.map.block.io.DisassemblyManager blockManager = new com.sfc.sf2.map.block.io.DisassemblyManager(); 
            MapBlock[] blockSet = blockManager.importDisassembly(palettePath, tileset1Path, tileset2Path, tileset3Path, tileset4Path, tileset5Path, blocksPath);

            if(blockSet!=null){
                layout = parseLayoutData(blockSet, layoutPath);
            }
        }catch(Exception e){
             System.err.println("com.sfc.sf2.maplayout.io.PngManager.importPng() - Error while parsing graphics data : "+e);
             e.printStackTrace();
        }         
                
        System.out.println("com.sfc.sf2.maplayout.io.DisassemblyManager.importDisassembly() - Disassembly imported.");
        return layout;
    }
    
    private MapLayout parseLayoutData(MapBlock[] blockSet, String layoutPath){
        MapLayout layout = new MapLayout();
        MapBlock[] blocks = new MapBlock[64*64];
        try{
            /*for(int i=0;i<blockSet.length;i++){
                blocks[i] = blockSet[i];
            }*/
            Path layoutpath = Paths.get(layoutPath);
            inputData = Files.readAllBytes(layoutpath);
            while(blockCursor<64*64){
                
                MapBlock block = new MapBlock();
                
                
                if(getNextBit()==0){
                    if(getNextBit()==0){
                        /* 00 */
                        System.out.println("Block=$" + Integer.toHexString(blockCursor)+" - Output next block from block set.");
                        blocksetCursor++;
                        block.setTiles(blockSet[blocksetCursor].getTiles());
                        applyFlags(block);
                        blocks[blockCursor] = block;                        
                        blockCursor++;
                    }else{
                        /* 01 */
                        System.out.println("Block=$" + Integer.toHexString(blockCursor)+" - Copy section.");
                        int count = 0;
                        while(getNextBit()==0){
                            count++;
                        }
                        int cursor = count;
                        int value = 0;
                        while(cursor>0){
                            value = value * 2 + getNextBit();
                            cursor--;
                        }
                        int result = value + (2<<count-1);
                        System.out.println("count="+count+", value="+value+", result="+result);
                        int offset = (getNextBit()==1)?1:64;
                        for(int i=0;i<result;i++){
                            blocks[blockCursor] = blocks[blockCursor-offset];
                            blockCursor++;
                        }
                        //System.out.println(debugSb.substring(debugSb.length()-1-2));
                        //System.out.println("outputData="+outputData);
                    }
                }else{
                    if(getNextBit()==0){
                        if(getNextBit()==0){
                            /* 100 */
                            //System.out.println("commandNumber=$" + Integer.toHexString(initialCommandNumber-remainingCommandNumber)+" - outputRightTileFromHistory");
                            //outputRightTileFromHistory();
                        }else{
                            /* 101 */
                            //System.out.println("commandNumber=$" + Integer.toHexString(initialCommandNumber-remainingCommandNumber)+" - outputBottomTileFromHistory");
                            //outputBottomTileFromHistory();
                        }
                    }else{
                        if(getNextBit()==0){
                            /* 110 */
                            //System.out.println("commandNumber=$" + Integer.toHexString(initialCommandNumber-remainingCommandNumber)+" - outputNextTileWithSameFlags");
                            //outputNextTileWithSameFlags();
                        }else{
                            /* 111 */
                            //System.out.println("commandNumber=$" + Integer.toHexString(initialCommandNumber-remainingCommandNumber)+" - outputNextTileWithNewFlags");
                            //outputNextTileWithNewFlags();
                        }
                    }
                    break;
                }                
                
                

                
            }
        }catch(Exception e){
             System.err.println("com.sfc.sf2.maplayout.io.DisassemblyManager.parseGraphics() - Error while parsing block data : "+e);
             e.printStackTrace();
        }             
        for(int i=0;i<blocks.length;i++){
            if(blocks[i]==null){
                blocks[i] = blockSet[0];
            }
        }  
        layout.setBlocks(blocks);
        return layout;
    }   
    
    private void applyFlags(MapBlock block){
        short flags = 0;
        if(getNextBit()==0){
            if(getNextBit()==0){
                /* 00 : no flag set */
            }else{
                /* 01 : $C000*/
                flags = (short)0xC000;
            }
        }else{
            if(getNextBit()==0){
                if(getNextBit()==0){
                    /* 100 : $4000 */
                    flags = (short)0x4000;
                }else{
                    /* 101 : $8000 */
                    flags = (short)0x8000;
                }
            }else{
                /* 11 : next 6 bits = flag mask XXXX XX00 0000 0000 */
                flags = (short)(getNextBit() * 0x8000
                        + getNextBit() * 0x4000
                        + getNextBit() * 0x2000
                        + getNextBit() * 0x1000
                        + getNextBit() * 0x0800
                        + getNextBit() * 0x0400);
            }
        }   
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
