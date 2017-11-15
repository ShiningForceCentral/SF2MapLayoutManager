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
    
    private static final int COMMAND_LEFTMAP = 0;
    private static final int COMMAND_UPPERMAP = 1;
    private static final int COMMAND_CUSTOMVALUE = 2;
    
    private byte[] inputData;
    private short inputWord = 0;
    private int inputCursor = -2;
    private int inputBitCursor = 16;
    private short[] outputData = null;
    private Tile[] outputTiles = null;
    private StringBuilder debugSb = null;
    
    private int blocksetCursor;
    private int blockCursor;

    MapBlock[][] leftHistoryMap = null;
    MapBlock[][] upperHistoryMap = null;    
    
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
        leftHistoryMap = new MapBlock[blockSet.length][4];
        upperHistoryMap = new MapBlock[blockSet.length][4];
        blocksetCursor = 2;
        blockCursor = 0;
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
                        block.setIndex(blockSet[blocksetCursor].getIndex());
                        applyFlags(block);
                        blocks[blockCursor] = block; 
                        if(blockCursor>0){
                            saveBlockToLeftStackMap(blocks[blockCursor-1].getIndex(), block);
                        }else{
                            saveBlockToLeftStackMap(0, block);
                        }
                        if(blockCursor>63){
                            saveBlockToUpperStackMap(blocks[blockCursor-64].getIndex(), block);
                        }else{
                            saveBlockToUpperStackMap(0, block);
                        }
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
                        int result = value + (1<<count);
                        System.out.println("count="+count+", value="+value+", result="+result);
                        int offset = (getNextBit()==1)?1:64;
                        for(int i=0;i<result;i++){
                            if(blockCursor<64*64){
                                blocks[blockCursor] = blocks[blockCursor-offset];
                                blockCursor++;
                            }
                        }
                        //System.out.println(debugSb.substring(debugSb.length()-1-2));
                        //System.out.println("outputData="+outputData);
                    }
                }else{
                    /* 1... check if left block history stack available */
                    int commandType;
                    int leftBlockCursor = (blockCursor>0)?blocks[blockCursor-1].getIndex():0;
                    int upperBlockCursor = (blockCursor>63)?blocks[blockCursor-64].getIndex():0;
                    if(leftHistoryMap[leftBlockCursor][0]!=null){
                        /* 1... left block stack available, check next bit */
                        if(getNextBit()==0){
                            /* 10 : Get block from left block history map */
                            System.out.println("Block=$" + Integer.toHexString(blockCursor)+" - 10 : Get block from left block history map.");
                            commandType = COMMAND_LEFTMAP;
                        }else{
                            /* 11... check if upper block history stack available */
                            if(upperHistoryMap[upperBlockCursor][0]!=null){
                                /* 11... check next bit */
                                if(getNextBit()==0){
                                    /* 110 : Get block from upper block history map */
                                    System.out.println("Block=$" + Integer.toHexString(blockCursor)+" - 110 : Get block from upper block history map.");
                                    commandType = COMMAND_UPPERMAP;
                                }else{
                                    /* 111 : custom value */
                                    System.out.println("Block=$" + Integer.toHexString(blockCursor)+" - 111 : Custom value.");
                                    commandType = COMMAND_CUSTOMVALUE;
                                }
                            }else{
                                /* 11 with no upper stack : Custom value */
                                System.out.println("Block=$" + Integer.toHexString(blockCursor)+" - 11 with no upper stack : Custom value.");
                                commandType = COMMAND_CUSTOMVALUE;
                            }
                        }
                    }else{
                        /* 1... check if upper block history stack available */
                        if(upperHistoryMap[upperBlockCursor][0]!=null){
                            /* 1... check next bit */
                            if(getNextBit()==0){
                                /* 10 with no left stack : Get block from upper block history map */
                                System.out.println("Block=$" + Integer.toHexString(blockCursor)+" - 10 with no left stack : Get block from upper block history map.");
                                commandType = COMMAND_UPPERMAP;
                            }else{
                                /* 11 with no left stack : custom value */
                                System.out.println("Block=$" + Integer.toHexString(blockCursor)+" - 11 with no left stack : Custom value.");
                                commandType = COMMAND_CUSTOMVALUE;
                            }
                        }else{
                            /* 1 with no left and upper stack : Custom value*/
                            System.out.println("Block=$" + Integer.toHexString(blockCursor)+" - 1 with no left or upper stack : Custom value.");
                            commandType = COMMAND_CUSTOMVALUE;
                        }
                    }
                    int stackSize = 0;
                    int stackTarget = 0;
                    MapBlock targetBlock = null;
                    switch(commandType){
                        
                        case COMMAND_LEFTMAP :
                            if(leftHistoryMap[leftBlockCursor][1]==null){
                                targetBlock = leftHistoryMap[leftBlockCursor][0];
                                System.out.println("Stack contains only one entry : get entry 0.");
                            }else{
                                for(int i=0;i<4;i++){
                                    if(leftHistoryMap[leftBlockCursor][i]!=null){
                                        stackSize++;
                                    }
                                }
                                while(stackSize>1){
                                    stackSize--;
                                    if(getNextBit()==0){
                                        stackTarget++;
                                    }else{
                                        break;
                                    }
                                }
                                System.out.println("Get stack entry "+stackTarget);
                                targetBlock = leftHistoryMap[leftBlockCursor][stackTarget];
                            }
                            block.setTiles(targetBlock.getTiles());
                            block.setIndex(targetBlock.getIndex());
                            break;
                        
                        case COMMAND_UPPERMAP :
                            if(upperHistoryMap[upperBlockCursor][1]==null){
                                targetBlock = upperHistoryMap[upperBlockCursor][0];
                                System.out.println("Stack contains only one entry : get entry 0.");
                            }else{
                                for(int i=0;i<4;i++){
                                    if(upperHistoryMap[upperBlockCursor][i]!=null){
                                        stackSize++;
                                    }
                                }
                                while(stackSize>1){
                                    stackSize--;
                                    if(getNextBit()==0){
                                        stackTarget++;
                                    }else{
                                        break;
                                    }
                                }
                                System.out.println("Get stack entry "+stackTarget);
                                targetBlock = upperHistoryMap[upperBlockCursor][stackTarget];
                            }
                            block.setTiles(targetBlock.getTiles());
                            block.setIndex(targetBlock.getIndex());
                            break;
                            
                        case COMMAND_CUSTOMVALUE :
                            int length = Integer.toString(blocksetCursor,2).length();
                            int value = 0;
                            while(length>0){
                                value = value * 2 + getNextBit();
                                length--;
                            }
                            targetBlock = blockSet[value];
                            block.setTiles(targetBlock.getTiles());
                            block.setIndex(targetBlock.getIndex());
                            applyFlags(block);
                            break;
                    }
                    
                    
                    blocks[blockCursor] = block;

                    if(blockCursor>0){
                        saveBlockToLeftStackMap(blocks[blockCursor-1].getIndex(), block);
                    }else{
                        saveBlockToLeftStackMap(0, block);
                    }
                    if(blockCursor>63){
                        saveBlockToUpperStackMap(blocks[blockCursor-64].getIndex(), block);
                    }else{
                        saveBlockToUpperStackMap(0, block);
                    }                    
                    
                    blockCursor++;
                    
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
                flags = (short)0x8000;
            }
        }else{
            if(getNextBit()==0){
                if(getNextBit()==0){
                    /* 100 : $4000 */
                    flags = (short)0x4000;
                }else{
                    /* 101 : $8000 */
                    flags = (short)0xC000;
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
    
    private void saveBlockToLeftStackMap(int leftBlockIndex, MapBlock block){
        MapBlock[] currentStack = leftHistoryMap[leftBlockIndex];
        
        if(!block.equals(currentStack[0])){
            MapBlock[] newStack = new MapBlock[4];
            leftHistoryMap[leftBlockIndex] = newStack;
            newStack[0] = block;
            int currentStackCursor = 0;
            int newStackCursor = 1;
            while(newStackCursor<4){
                if(currentStack[currentStackCursor]!=null){
                    if(!block.equals(currentStack[currentStackCursor])){
                        newStack[newStackCursor] = currentStack[currentStackCursor];
                        currentStackCursor++;
                        newStackCursor++;
                    }else{
                        currentStackCursor++;
                    }
                }else{
                    return;
                }
            }
        }
        
    }
    
    private void saveBlockToUpperStackMap(int upperBlockIndex, MapBlock block){
        MapBlock[] currentStack = upperHistoryMap[upperBlockIndex];
        
        if(!block.equals(currentStack[0])){
            MapBlock[] newStack = new MapBlock[4];
            upperHistoryMap[upperBlockIndex] = newStack;
            newStack[0] = block;
            int currentStackCursor = 0;
            int newStackCursor = 1;
            while(newStackCursor<4){
                if(currentStack[currentStackCursor]!=null){
                    if(!block.equals(currentStack[currentStackCursor])){
                        newStack[newStackCursor] = currentStack[currentStackCursor];
                        currentStackCursor++;
                        newStackCursor++;
                    }else{
                        currentStackCursor++;
                    }
                }else{
                    return;
                }
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
