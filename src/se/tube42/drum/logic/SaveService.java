
package se.tube42.drum.logic;

import java.io.*;

import se.tube42.lib.service.*;

import se.tube42.drum.audio.*;
import se.tube42.drum.data.*;

import static se.tube42.drum.data.Constants.*;


public final class SaveService
{
    private final static String 
          HEADER = "drum-save-v0.2",
          TAIL = "drum-done",
          SAVE_NAME = "PREF-" + HEADER
          ;
    
    
    // -----------------------------------------------------------
    // storageAccess
    // -----------------------------------------------------------
    
    // ------------------------------------------------    
    //  helper functions
    
    public static String getSave(int num)
    {
        return StorageService.load(SAVE_NAME + "." + num, (String)null);
    }
    
    public static void setSave(int num, String data)
    {
        if(data != null) {
            StorageService.save(SAVE_NAME + "." + num, data);
            StorageService.flush();
        }
    }
    
    
    // -----------------------------------------------------------
    // serial / deserial
    // -----------------------------------------------------------
    
    
    // serialize current program
    public static String currentToString() 
    {
        try {
            final Program p = World.prog;
            final EffectChain fx = World.mixer.getEffectChain();           
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            
            dos.writeUTF(HEADER);
            
            // serialize p
            dos.writeInt(p.getRawBanks());
            dos.writeShort(p.getTempo());
            dos.writeByte(p.getTempoMultiplier());
            dos.writeByte(p.getVoice());
            for(int i = 0; i < 3; i++) dos.writeInt(0); // pad
            
            
            final int [] data = p.getRawPads();
            for(int i = 0; i < data.length; i++) {
                dos.writeInt( data[i]);
                dos.writeByte( p.getSampleVariant(i));                
                dos.writeFloat( p.getVolume(i));
                dos.writeShort( p.getVolumeVariation(i));
            }
            
            // serialize fx
            dos.writeShort(fx.getEnabledRaw());
            dos.writeShort(0); // reserved
            
            Effect comp = fx.getEffect(FX_COMP);
            dos.writeFloat( comp.getConfig(0));
            dos.writeFloat( comp.getConfig(1));
            
            for(int i = 0; i < 4; i++) dos.writeFloat( 0); // reserved
            
            
            dos.writeUTF(TAIL);
            
            
            // encode and save serialized data
            dos.flush();            
            final String str = EncodingService.encode( baos.toByteArray() );
            return str;
        } catch(Exception e) {
            System.err.println("SAVE ERROR: " + e);
            return null;
        }        
    }
    
    // load current program from a string
    public static boolean stringToCurrent(String str)
    {
        if(str == null)
            return false;
                        
        try {
            
            final Program p = World.prog;
            final EffectChain fx = World.mixer.getEffectChain();
            
            byte [] bytes = EncodingService.decode(str);
            if(bytes == null)  {
                System.err.println("ERROR: could not decode string: " + str);
                return false;
            }
            
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            DataInputStream dis = new DataInputStream(bais);
            
            
            if(!HEADER.equals(dis.readUTF())) {
                System.err.println("Internal error: bad save header");
                return false;
            }
            
            // deserialze to p 
            p.setRawBanks( dis.readInt());            
            p.setTempo( dis.readShort() );
            p.setTempoMultiplier( dis.readByte() );
            p.setVoice( dis.readByte() );
            for(int i = 0; i < 3; i++) dis.readInt(); // pad
            
            
            final int [] data = p.getRawPads();
            for(int i = 0; i < data.length; i++) {
                data[i] = dis.readInt();
                p.setSampleVariant(i, dis.readByte());
                p.setVolume(i, dis.readFloat());
                p.setVolumeVariation(i, dis.readShort());
            }
            
            
            // deserialize fx
            fx.setEnabledRaw( dis.readShort());
            dis.readShort(); // reserved
            
            Effect comp = fx.getEffect(FX_COMP);
            comp.setConfig(0, dis.readFloat() );
            comp.setConfig(1, dis.readFloat() );
            
            for(int i = 0; i < 4; i++) dis.readFloat(); // reserved
            
            if(!TAIL.equals(dis.readUTF())) {
                System.err.println("Internal error: bad save tail");
                return false;
            }
                                  
            return true;
        } catch(Exception e) {
            System.err.println("LOAD ERROR: " + e);
            return false;
        }        
    }
    
    
    // -----------------------------------------------------------
    // load / save
    // -----------------------------------------------------------
    
    // save this        
    public static boolean save(int num)                    
    {        
        final String data = currentToString();
        if(data != null) {
            setSave(num, data);
            return true;
        } else {
            return false;
        }
    }
    
    
    // load from a save
    public static boolean load(int num)
    {
        final String str = getSave(num);        
        return str == null ? false : stringToCurrent(str);
    }
}