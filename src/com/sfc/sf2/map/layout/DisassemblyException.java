/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sfc.sf2.map.layout;

/**
 *
 * @author troyp
 */
public class DisassemblyException extends Exception {
    public DisassemblyException(String message) {
        super(message);
    }
    public DisassemblyException(Throwable cause) {
        super(cause);
    }
    public DisassemblyException(String message, Throwable cause) {
        super(message, cause);
    }
}
