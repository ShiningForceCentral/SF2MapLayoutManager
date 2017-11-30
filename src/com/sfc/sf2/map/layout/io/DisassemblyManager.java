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
import static com.sfc.sf2.graphics.compressed.StackGraphicsEncoder.bytesToHex;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author wiz
 */
public class DisassemblyManager {
    
    private static final int COMMAND_LEFTMAP = 0;
    private static final int COMMAND_UPPERMAP = 1;
    private static final int COMMAND_CUSTOMVALUE = 2;
    
    com.sfc.sf2.map.block.io.DisassemblyManager blockManager = new com.sfc.sf2.map.block.io.DisassemblyManager();
    
    private byte[] inputData;
    private short inputWord = 0;
    private int inputCursor = -2;
    private int inputBitCursor = 16;
    private StringBuilder debugSb = null;
    
    MapBlock[] blockset = null;
    private int blocksetCursor;
    private int blockCursor;

    MapBlock[][] leftHistoryMap = null;
    MapBlock[][] upperHistoryMap = null;    
    
    Color[] palette = null;
    Tile[] tileset = new Tile[128*5];
    
    public MapLayout importDisassembly(String palettesPath, String tilesetsPath, String mapPath){
        //System.out.println("com.sfc.sf2.maplayout.io.DisassemblyManager.importDisassembly() - Importing disassembly ...");
        MapLayout layout = null;
        try{
            
            String tilesetsFilePath = mapPath + "00-tilesets.bin";
            String blocksPath = mapPath + "0-blocks.bin";
            String layoutPath = mapPath + "1-layout.bin";
            
            layout = importDisassembly(palettesPath, tilesetsPath, tilesetsFilePath , blocksPath, layoutPath);

        }catch(Exception e){
             System.err.println("com.sfc.sf2.maplayout.io.PngManager.importPng() - Error while parsing graphics data : "+e);
             e.printStackTrace();
        }         
                
        //System.out.println("com.sfc.sf2.maplayout.io.DisassemblyManager.importDisassembly() - Disassembly imported.");
        return layout;
    }      
    
    public MapLayout importDisassembly(String palettesPath, String tilesetsPath, String tilesetsFilePath, String blocksPath, String layoutPath){
        //System.out.println("com.sfc.sf2.maplayout.io.DisassemblyManager.importDisassembly() - Importing disassembly ...");
        MapLayout layout = null;
        String palettePath = "";
        String[] tilesetPaths = {"","","","",""};
        try{
            int[] indexes = parseTilesetsFile(tilesetsFilePath);
            int paletteIndex = indexes[0];
            
            List<String> paletteFilenames = new ArrayList<String>();
            String pDir = palettesPath.substring(0, palettesPath.lastIndexOf(System.getProperty("file.separator")));
            String pPattern = palettesPath.substring(palettesPath.lastIndexOf(System.getProperty("file.separator"))+1);
            File pDirectory = new File(pDir);
            File[] pFiles = pDirectory.listFiles();
            System.out.println("Listing palette files with pattern : "+palettesPath+"*.bin");
            for(File f : pFiles){
                if(f.getName().startsWith(pPattern) && f.getName().endsWith(".bin")){
                    paletteFilenames.add(f.getName());
                    //System.out.println("Adding : "+f.getName());
                }
            }
            if(paletteFilenames.isEmpty()){
                System.err.println("com.sfc.sf2.maplayout.io.DisassemblyManager.importDisassembly() - ERROR : no palette file imported. Wrong path or filename prefix ?");
            } else if(paletteIndex > paletteFilenames.size()){
                System.err.println("com.sfc.sf2.maplayout.io.DisassemblyManager.importDisassembly() - ERROR : Index "+paletteIndex+" id superior to unmber of palette files found : "+paletteFilenames.size());
            } else {
                palettePath = pDir + System.getProperty("file.separator") + paletteFilenames.get(paletteIndex);
                System.out.println("com.sfc.sf2.maplayout.io.DisassemblyManager.importDisassembly() - Selected palette file : "+palettePath);
            }


            
            List<String> tilesetFilenames = new ArrayList<String>();
            String tDir = tilesetsPath.substring(0, tilesetsPath.lastIndexOf(System.getProperty("file.separator")));
            String tPattern = tilesetsPath.substring(tilesetsPath.lastIndexOf(System.getProperty("file.separator"))+1);
            File tDirectory = new File(tDir);
            File[] tFiles = tDirectory.listFiles();
            System.out.println("Listing tileset files with pattern : "+tilesetsPath+"*.bin");
            for(File f : tFiles){
                if(f.getName().startsWith(tPattern) && f.getName().endsWith(".bin")){
                    tilesetFilenames.add(f.getName());
                    //System.out.println("Adding : "+f.getName());
                }
            }
            if(tilesetFilenames.isEmpty()){
                System.err.println("com.sfc.sf2.maplayout.io.DisassemblyManager.importDisassembly() - ERROR : no tileset file imported. Wrong path or filename prefix ?");
            } else {
                for(int i=0;i<tilesetPaths.length;i++){
                    if(indexes[i+1] > tilesetFilenames.size()){
                        System.err.println("com.sfc.sf2.maplayout.io.DisassemblyManager.importDisassembly() - ERROR for tileset "+(i+1)+" : Index "+indexes[i+1]+" id superior to unmber of tileset files found : "+tilesetFilenames.size());
                    } else if(indexes[i+1]!=-1){
                        tilesetPaths[i] = tDirectory + System.getProperty("file.separator") + tilesetFilenames.get(indexes[i+1]);
                        System.out.println("com.sfc.sf2.maplayout.io.DisassemblyManager.importDisassembly() - Selected tileset "+(i+1)+" : "+tilesetPaths[i]);
                    } else{
                        System.err.println("com.sfc.sf2.maplayout.io.DisassemblyManager.importDisassembly() - Tileset "+(i+1)+" is declared empty.");
                    }
                }               
                
            }              
            
            layout = importDisassembly(palettePath, tilesetPaths[0], tilesetPaths[1], tilesetPaths[2], tilesetPaths[3], tilesetPaths[4], blocksPath, layoutPath);

        }catch(Exception e){
             System.err.println("com.sfc.sf2.maplayout.io.PngManager.importPng() - Error while parsing graphics data : "+e);
             e.printStackTrace();
        }         
                
        //System.out.println("com.sfc.sf2.maplayout.io.DisassemblyManager.importDisassembly() - Disassembly imported.");
        return layout;
    }  
    
    private int[] parseTilesetsFile(String tilesetsFilePath){
        int[] indexes = new int[6];
        try {
            Path tilesetspath = Paths.get(tilesetsFilePath);
            if(!tilesetspath.toFile().exists()){
                 System.err.println("ERROR - File not found : "+tilesetsFilePath);
            }else{
                byte[] data = Files.readAllBytes(tilesetspath);
                indexes[0] = (int)(data[0]);
                indexes[1] = (int)(data[1]);
                indexes[2] = (int)(data[2]);
                indexes[3] = (int)(data[3]);
                indexes[4] = (int)(data[4]);
                indexes[5] = (int)(data[5]);                
            }        
        } catch (IOException ex) {
            Logger.getLogger(DisassemblyManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return indexes;
    }
    
    public MapLayout importDisassembly(String palettePath, String tileset1Path, String tileset2Path, String tileset3Path, String tileset4Path, String tileset5Path, String blocksPath, String layoutPath){
        System.out.println("com.sfc.sf2.maplayout.io.DisassemblyManager.importDisassembly() - Importing disassembly ...");
        MapLayout layout = new MapLayout();
        try{
            blockset = blockManager.importDisassembly(palettePath, tileset1Path, tileset2Path, tileset3Path, tileset4Path, tileset5Path, blocksPath);

            if(blockset!=null){
                layout = parseLayoutData(blockset, layoutPath);
            }
        }catch(Exception e){
             System.err.println("com.sfc.sf2.maplayout.io.PngManager.importPng() - Error while parsing graphics data : "+e);
             e.printStackTrace();
        }         
                
        //System.out.println("debugSb="+debugSb.toString());
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
        debugSb = new StringBuilder();
        try{
            /*for(int i=0;i<blockSet.length;i++){
                blocks[i] = blockSet[i];
            }*/

            
            Path layoutpath = Paths.get(layoutPath);
            if(!layoutpath.toFile().exists()){
                 System.err.println("ERROR - File not found : "+layoutPath);
            }        
            inputData = Files.readAllBytes(layoutpath);
            while(blockCursor<64*64){
                
                MapBlock block = new MapBlock();
                
                debugSb.append(" ");
                
                if(getNextBit()==0){
                    if(getNextBit()==0){
                        /* 00 */
                        //System.out.println("Block=$" + Integer.toHexString(blockCursor)+" - 00 : Output next block from block set.");
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
                        //System.out.println("Block=$" + Integer.toHexString(blockCursor)+" - 01 : Copy section.");
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
                        //System.out.println(" count="+count+", value="+value+", result="+result);
                        int offset = (getNextBit()==1)?1:64;
                        for(int i=0;i<result;i++){
                            if(blockCursor<64*64){
                                MapBlock copy = new MapBlock();
                                MapBlock source = blocks[blockCursor-offset];
                                copy.setTiles(source.getTiles());
                                copy.setIndex(source.getIndex());
                                copy.setFlags(source.getFlags());
                                blocks[blockCursor] = copy;
                                //System.out.println(" Copy of block=$" + Integer.toHexString(blocks[blockCursor].getIndex())+" / "+blocks[blockCursor].getIndex());
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
                            //System.out.println("Block=$" + Integer.toHexString(blockCursor)+" - 10 : Get block from left block history map.");
                            commandType = COMMAND_LEFTMAP;
                        }else{
                            /* 11... check if upper block history stack available */
                            if(upperHistoryMap[upperBlockCursor][0]!=null){
                                /* 11... check next bit */
                                if(getNextBit()==0){
                                    /* 110 : Get block from upper block history map */
                                    //System.out.println("Block=$" + Integer.toHexString(blockCursor)+" - 110 : Get block from upper block history map.");
                                    commandType = COMMAND_UPPERMAP;
                                }else{
                                    /* 111 : custom value */
                                    //System.out.println("Block=$" + Integer.toHexString(blockCursor)+" - 111 : Custom value.");
                                    commandType = COMMAND_CUSTOMVALUE;
                                }
                            }else{
                                /* 11 with no upper stack : Custom value */
                                //System.out.println("Block=$" + Integer.toHexString(blockCursor)+" - 11 with no upper stack : Custom value.");
                                commandType = COMMAND_CUSTOMVALUE;
                            }
                        }
                    }else{
                        /* 1... check if upper block history stack available */
                        if(upperHistoryMap[upperBlockCursor][0]!=null){
                            /* 1... check next bit */
                            if(getNextBit()==0){
                                /* 10 with no left stack : Get block from upper block history map */
                                //System.out.println("Block=$" + Integer.toHexString(blockCursor)+" - 10 with no left stack : Get block from upper block history map.");
                                commandType = COMMAND_UPPERMAP;
                            }else{
                                /* 11 with no left stack : custom value */
                                //System.out.println("Block=$" + Integer.toHexString(blockCursor)+" - 11 with no left stack : Custom value.");
                                commandType = COMMAND_CUSTOMVALUE;
                            }
                        }else{
                            /* 1 with no left and upper stack : Custom value*/
                            //System.out.println("Block=$" + Integer.toHexString(blockCursor)+" - 1 with no left or upper stack : Custom value.");
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
                                //System.out.println(" Stack contains only one entry : get entry 0.");
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
                                //System.out.println(" Get stack entry "+stackTarget);
                                targetBlock = leftHistoryMap[leftBlockCursor][stackTarget];
                            }
                            block.setTiles(targetBlock.getTiles());
                            block.setIndex(targetBlock.getIndex());
                            block.setFlags(targetBlock.getFlags());
                            break;
                        
                        case COMMAND_UPPERMAP :
                            if(upperHistoryMap[upperBlockCursor][1]==null){
                                targetBlock = upperHistoryMap[upperBlockCursor][0];
                                //System.out.println(" Stack contains only one entry : get entry 0.");
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
                                //System.out.println(" Get stack entry "+stackTarget);
                                targetBlock = upperHistoryMap[upperBlockCursor][stackTarget];
                            }
                            block.setTiles(targetBlock.getTiles());
                            block.setIndex(targetBlock.getIndex());
                            block.setFlags(targetBlock.getFlags());
                            break;
                            
                        case COMMAND_CUSTOMVALUE :
                            int length = Integer.toString(blocksetCursor,2).length();
                            int value = 0;
                            while(length>0){
                                value = value * 2 + getNextBit();
                                length--;
                            }
                            //System.out.println(" blocksetCursor=="+blocksetCursor+" / "+Integer.toString(blocksetCursor,2)+", length="+Integer.toString(blocksetCursor,2).length()+", Value="+value);
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
                
                //System.out.println(" Output block = $" + Integer.toHexString(block.getIndex())+" / "+block.getIndex());
                //System.out.println(debugSb.substring(debugSb.lastIndexOf(" ")));  
                
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
        block.setFlags(flags&0xFFFF);
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
        //System.out.println("Next input word = $"+Integer.toString(s, 16)+" / "+Integer.toString(s, 2));
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
            byte[] layoutBytes = produceLayoutBytes(layout);
            blockManager.exportDisassembly(blockset, blocksFilePath);
            Path layoutFilepath = Paths.get(layoutFilePath);
            Files.write(layoutFilepath,layoutBytes);
            System.out.println(layoutBytes.length + " bytes into " + layoutFilepath);
        } catch (Exception ex) {
            Logger.getLogger(DisassemblyManager.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            System.out.println(ex);
        }            
        System.out.println("com.sfc.sf2.maplayout.io.DisassemblyManager.exportDisassembly() - Disassembly exported.");        
    }     
    
    private byte[] produceLayoutBytes(MapLayout layout){
        
        blockset = produceNewBlockset(blockset, layout);
        
        leftHistoryMap = new MapBlock[blockset.length][4];
        upperHistoryMap = new MapBlock[blockset.length][4];
        
        StringBuilder outputSb = new StringBuilder();
        outputSb.append(" ");
        byte[] output;        
        blocksetCursor = 3;
        blockCursor = 0;        
        
        String leftCopyCandidate;
        int leftCopyLength;
        String upperCopyCandidate;
        int upperCopyLength;
        String leftHistoryCandidate;
        String upperHistoryCandidate;
        String nextBlockCandidate;
        String customBlockCandidate;
        
        MapBlock[] blocks = layout.getBlocks();
        
        while(blockCursor<64*64){

            leftCopyCandidate = null;
            leftCopyLength = 0;
            upperCopyCandidate = null;
            upperCopyLength = 0;
            leftHistoryCandidate = null;
            upperHistoryCandidate = null;
            nextBlockCandidate = null;
            customBlockCandidate = null;
            
            
            MapBlock block = blocks[blockCursor];
            //System.out.println("Block $"+Integer.toString(block.getIndex(),16)+" / $"+Integer.toString(block.getFlags(),16));
            MapBlock leftBlock = null;
            int leftHistoryCursor = 0;
            if(blockCursor>0){
                leftBlock = blocks[blockCursor-1];
                leftHistoryCursor = leftBlock.getIndex();
            }
            MapBlock upperBlock = null;
            int upperHistoryCursor = 0;
            if(blockCursor>63){
                upperBlock = blocks[blockCursor-64];
                upperHistoryCursor = upperBlock.getIndex();
            }
            
            /* Produce candidate commands */ 
            
            if(block.equals(leftBlock)){
                /* Produce leftCopyCandidate with length */
                leftCopyLength = 1;
                while(blockCursor+leftCopyLength<blocks.length && blocks[blockCursor+leftCopyLength].equals(blocks[blockCursor-1+leftCopyLength])){
                    leftCopyLength++;
                }
                int powerOfTwo = 0;
                while((1<<powerOfTwo)<=leftCopyLength){
                    powerOfTwo++;
                }
                powerOfTwo--;
                int rest = leftCopyLength - (1<<powerOfTwo);
                StringBuilder commandSb = new StringBuilder();
                commandSb.append("01");
                int zeros = powerOfTwo;
                while(zeros>0){
                    commandSb.append("0");
                    zeros--;
                }
                commandSb.append("1");
                if(powerOfTwo>0){
                    String restString = Integer.toString(rest,2);
                    while(restString.length()<powerOfTwo){
                        restString = "0" + restString;
                    }
                    commandSb.append(restString);
                    
                }
                commandSb.append("1");
                leftCopyCandidate = commandSb.toString();
                //System.out.println(" leftCopyCandidate="+leftCopyCandidate+" - "+leftCopyLength+" blocks");
            }
            
            if(block.equals(upperBlock)){
                /* Produce upperCopyCandidate with length */
                upperCopyLength = 1;
                while(blockCursor+upperCopyLength<blocks.length && blocks[blockCursor+upperCopyLength].equals(blocks[blockCursor-64+upperCopyLength])){
                    upperCopyLength++;
                }
                int powerOfTwo = 0;
                while((1<<powerOfTwo)<=upperCopyLength){
                    powerOfTwo++;
                }
                powerOfTwo--;
                int rest = upperCopyLength - (1<<powerOfTwo);
                StringBuilder commandSb = new StringBuilder();
                commandSb.append("01");
                int zeros = powerOfTwo;
                while(zeros>0){
                    commandSb.append("0");
                    zeros--;
                }
                commandSb.append("1");
                if(powerOfTwo>0){
                    String restString = Integer.toString(rest,2);
                    while(restString.length()<powerOfTwo){
                        restString = "0" + restString;
                    }
                    commandSb.append(restString);
                }
                commandSb.append("0");
                upperCopyCandidate = commandSb.toString();
                //System.out.println(" upperCopyCandidate="+upperCopyCandidate+" - "+upperCopyLength+" blocks");
            }
            
            int leftBlockHistoryIndex = getLeftHistoryIndex(leftHistoryCursor, block);
            if(leftBlockHistoryIndex>=0){
                /* Produce leftHistoryCandidate*/
                StringBuilder commandSb = new StringBuilder();
                commandSb.append("10");
                MapBlock[] stack = leftHistoryMap[leftHistoryCursor];
                if(stack[1]==null){
                    /* No index to add */
                }else{
                    int stackSize = 0;
                    for(int i=0;i<4;i++){
                        if(stack[i]!=null){
                            stackSize++;
                        }
                    }  
                    for(int i=0;i<=stackSize;i++){
                        if(block.equals(stack[i])){
                            if(i<stackSize-1){
                                commandSb.append("1");
                            }
                            break;
                        }else{
                            commandSb.append("0");
                        }
                    }
                }
                leftHistoryCandidate = commandSb.toString();
                //System.out.println(" leftHistoryCandidate="+leftHistoryCandidate);
            }
            
            int upperBlockHistoryIndex = getUpperHistoryIndex(upperHistoryCursor, block);
            if(upperBlockHistoryIndex>=0){
                /* Produce upperHistoryCandidate*/
                StringBuilder commandSb = new StringBuilder();
                commandSb.append("10");
                MapBlock[] stack = upperHistoryMap[upperHistoryCursor];
                if(stack[1]==null){
                    /* No index to add */
                }else{
                    int stackSize = 0;
                    for(int i=0;i<4;i++){
                        if(stack[i]!=null){
                            stackSize++;
                        }
                    }  
                    for(int i=0;i<=stackSize;i++){
                        if(block.equals(stack[i])){
                            if(i<stackSize-1){
                                commandSb.append("1");
                            }
                            break;
                        }else{
                            commandSb.append("0");
                        }
                    }
                }
                if(leftHistoryMap[leftHistoryCursor][0]!=null){
                    commandSb.insert(0, "1");
                }
                upperHistoryCandidate = commandSb.toString();
                //System.out.println(" upperHistoryCandidate="+upperHistoryCandidate);
            }
            
            if(leftCopyCandidate==null && upperCopyCandidate==null && leftHistoryCandidate==null && upperHistoryCandidate==null){
                if(blocksetCursor<blockset.length && block.getIndex()==blockset[blocksetCursor].getIndex()){
                    /* Produce nextBlockCandidate */
                    nextBlockCandidate = "00" + produceFlagBits(block.getFlags());
                    //System.out.println(" nextBlockCandidate="+nextBlockCandidate);
                }
                
                if(nextBlockCandidate==null){
                    /* Produce customBlockCandidate */
                    StringBuilder commandSb = new StringBuilder();
                    commandSb.append("1");
                    int length = Integer.toString(blocksetCursor-1,2).length();
                    //System.out.println(" blocksetCursor="+(blocksetCursor-1)+" / "+Integer.toString(blocksetCursor-1,2)+", length="+length);
                    String value = Integer.toString(block.getIndex(),2);
                    while(value.length()<length){
                        value = "0" + value;
                    }
                    commandSb.append(value);
                    commandSb.append(produceFlagBits(block.getFlags()));
                    if(leftHistoryMap[leftHistoryCursor][0]!=null && upperHistoryMap[upperHistoryCursor][0]!=null){
                        commandSb.insert(0, "11");
                    }else if(leftHistoryMap[leftHistoryCursor][0]!=null || upperHistoryMap[upperHistoryCursor][0]!=null){
                        commandSb.insert(0, "1");
                    }
                    customBlockCandidate = commandSb.toString();
                    //System.out.println(" customBlockCandidate="+customBlockCandidate);
                }
                
            }
            
            /* Select command to output */
            if(leftCopyLength>1 || upperCopyLength>1){
                if(leftCopyLength>upperCopyLength){
                    outputSb.append(leftCopyCandidate);
                    blockCursor+=leftCopyLength;
                }else{
                    outputSb.append(upperCopyCandidate);
                    blockCursor+=upperCopyLength;
                }
            }else{
                if(nextBlockCandidate!=null){
                    outputSb.append(nextBlockCandidate);
                    savehistoryMaps(leftBlock, upperBlock, blockCursor, block);
                    blockCursor++;
                    blocksetCursor++;
                }else if(upperCopyCandidate!=null){
                    outputSb.append(upperCopyCandidate);
                    blockCursor+=upperCopyLength;
                }else if(leftCopyCandidate!=null){
                    outputSb.append(leftCopyCandidate);
                    blockCursor+=leftCopyLength;
                }else if(leftHistoryCandidate!=null){
                    outputSb.append(leftHistoryCandidate);
                    savehistoryMaps(leftBlock, upperBlock, blockCursor, block);
                    blockCursor++;
                }else if(upperHistoryCandidate!=null){
                    outputSb.append(upperHistoryCandidate);
                    savehistoryMaps(leftBlock, upperBlock, blockCursor, block);
                    blockCursor++;
                }else if(customBlockCandidate!=null){
                    outputSb.append(customBlockCandidate);
                    savehistoryMaps(leftBlock, upperBlock, blockCursor, block);
                    blockCursor++;
                }else{
                    System.out.println("ERROR : NO CANDIDATE COMMAND FOUND FOR BLOCK.");
                }
                
            }
            
            //System.out.println(" Selected command ="+outputSb.substring(outputSb.lastIndexOf(" ")));     
            outputSb.append(" ");
            
        }
        
        
        
        //System.out.println("output = " + outputSb.toString());
        outputSb = new StringBuilder(outputSb.toString().replace(" ",""));
        
        while(outputSb.length()%16 != 0){
            outputSb.append("1");
        }
        /* Byte array conversion */
        output = new byte[outputSb.length()/8];
        for(int i=0;i<output.length;i++){
            Byte b = (byte)(Integer.valueOf(outputSb.substring(i*8, i*8+8),2)&0xFF);
            output[i] = b;
        }
        System.out.println("output bytes length = " + output.length);
        //System.out.println("output = " + bytesToHex(output));
        return output;
    }
    
    private void savehistoryMaps(MapBlock leftBlock, MapBlock upperBlock, int blockCursor, MapBlock block){                
        if(blockCursor>0){
            saveBlockToLeftStackMap(leftBlock.getIndex(), block);
        }else{
            saveBlockToLeftStackMap(0, block);
        }
        if(blockCursor>63){
            saveBlockToUpperStackMap(upperBlock.getIndex(), block);
        }else{
            saveBlockToUpperStackMap(0, block);
        }
    }
    
    private MapBlock[] produceNewBlockset(MapBlock[] blockSet, MapLayout layout){
        List<Integer> newBlocksetValues = new ArrayList<>();
        MapBlock[] newBlockset;
        MapBlock[] blocks = layout.getBlocks();
        /* Add base blocks : empty, closed chest and open chest */
        newBlocksetValues.add(blockSet[0].getIndex());
        newBlocksetValues.add(blockSet[1].getIndex());
        newBlocksetValues.add(blockSet[2].getIndex());
        /* Add blocks in layout's appearing order */
        for(int i=0;i<blocks.length;i++){
            if(!newBlocksetValues.contains(blocks[i].getIndex())){
                newBlocksetValues.add(blocks[i].getIndex());
            }
        }
        /* Add remaining unused blocks */
        for(int i=0;i<blockSet.length;i++){
            if(!newBlocksetValues.contains(blockSet[i].getIndex())){
                newBlocksetValues.add(blockSet[i].getIndex());
            }
        }
        newBlockset = new MapBlock[newBlocksetValues.size()];
        for(int i=0;i<newBlockset.length;i++){
            newBlockset[i] = blockSet[newBlocksetValues.get(i)];
        }
        for(int i=0;i<blocks.length;i++){
            MapBlock block = blocks[i];
            for(int j=0;j<newBlockset.length;j++){
                if(block.getIndex()==newBlockset[j].getIndex()){
                    block.setIndex(j);
                    break;
                }
            }
        }
        for(int i=0;i<newBlockset.length;i++){
            newBlockset[i].setIndex(i);
        }
        return newBlockset;
    }
    
    private int getLeftHistoryIndex(int leftHistoryCursor, MapBlock block){
        int index = -1;
        MapBlock[] stack = leftHistoryMap[leftHistoryCursor];
        if(stack[0]==null){
            return index;
        }else{
            for(int i=0;i<4;i++){
                if(block.equals(stack[i])){
                    index = i;
                    break;
                }
            }
        }
        return index;
    }
    
    private int getUpperHistoryIndex(int upperHistoryCursor, MapBlock block){
        int index = -1;
        MapBlock[] stack = upperHistoryMap[upperHistoryCursor];
        if(stack[0]==null){
            return index;
        }else{
            for(int i=0;i<4;i++){
                if(block.equals(stack[i])){
                    index = i;
                    break;
                }
            }
        }
        return index;
    }
    
    private String produceFlagBits(int flags){
        String flagBits = null;
        switch(flags){
            case 0 :
                flagBits = "00";
                break;
            case 0xC000 :
                flagBits = "01";
                break;
            case 0x4000 :
                flagBits = "100";
                break;
            case 0x8000 :
                flagBits = "101";
                break;
            default :
                StringBuilder sb = new StringBuilder();
                sb.append("11");
                for(int i=0;i<6;i++){
                    if(((flags>>(15-i))&1)==0){
                        sb.append("0");
                    }else{
                        sb.append("1");
                    }
                }
                flagBits = sb.toString();
                break;
        }
        return flagBits;
    }

    public MapBlock[] getBlockset() {
        return blockset;
    }

    public void setBlockset(MapBlock[] blockset) {
        this.blockset = blockset;
    }
    
    
    
    
}
