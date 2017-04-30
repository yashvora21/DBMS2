package simpledb.buffer;

import simpledb.file.*;
import java.util.*;

/**
 * Manages the pinning and unpinning of buffers to blocks.
 * @author Edward Sciore
 *
 */
class BasicBufferMgr {
//   private Buffer[] bufferpool;
   private int numAvailable;
      
   private int totalBuffs;   
   private Map <Block, Buffer> bufferPoolMap; 
   
   /**
    * Creates a buffer manager having the specified number 
    * of buffer slots.
    * This constructor depends on both the {@link FileMgr} and
    * {@link simpledb.log.LogMgr LogMgr} objects 
    * that it gets from the class
    * {@link simpledb.server.SimpleDB}.
    * Those objects are created during system initialization.
    * Thus this constructor cannot be called until 
    * {@link simpledb.server.SimpleDB#initFileAndLogMgr(String)} or
    * is called first.
    * @param numbuffs the number of buffer slots to allocate
    */
   BasicBufferMgr(int numbuffs) {
//      bufferpool = new Buffer[numbuffs];
//      numAvailable = numbuffs;
//      for (int i=0; i<numbuffs; i++)
//         bufferpool[i] = new Buffer();
      
      bufferPoolMap = new HashMap<Block, Buffer>();
      numAvailable = numbuffs;
      totalBuffs = numbuffs;
   }
   
   /**
    * Flushes the dirty buffers modified by the specified transaction.
    * @param txnum the transaction's id number
    */
   synchronized void flushAll(int txnum) {
//      for (Buffer buff : bufferpool)
//         if (buff.isModifiedBy(txnum))
//         buff.flush();
      
	   Iterator it = bufferPoolMap.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        Buffer buff = (Buffer) pair.getValue();
	        if (buff.isModifiedBy(txnum)) {
	        	buff.flush();
	        }
	        
	        // remove block?
	        //bufferPoolMap.remove(pair.getKey());
	        
	        //System.out.println(pair.getKey() + " = " + pair.getValue());
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	   
   }
   
   /**
    * Pins a buffer to the specified block. 
    * If there is already a buffer assigned to that block
    * then that buffer is used;  
    * otherwise, an unpinned buffer from the pool is chosen.
    * Returns a null value if there are no available buffers.
    * @param blk a reference to a disk block
    * @return the pinned buffer
    */
   synchronized Buffer pin(Block blk) {
      Buffer buff = findExistingBuffer(blk);
      if (buff == null) {
         buff = chooseUnpinnedBuffer();
         if (buff == null)
            return null;
         
         // remove mapping?
         //bufferPoolMap.remove(blk);
         
         // adding new blk-buff to the map          
         bufferPoolMap.put(blk, buff);
         
         buff.assignToBlock(blk);
         
      }
      if (!buff.isPinned())
         numAvailable--;
      buff.pin();
      
      System.out.println("\nPin-> Number of buffers available: " + numAvailable);
      
      return buff;
   }
   
   /**
    * Allocates a new block in the specified file, and
    * pins a buffer to it. 
    * Returns null (without allocating the block) if 
    * there are no available buffers.
    * @param filename the name of the file
    * @param fmtr a pageformatter object, used to format the new block
    * @return the pinned buffer
    */
   synchronized Buffer pinNew(String filename, PageFormatter fmtr) {
      Buffer buff = chooseUnpinnedBuffer();
      if (buff == null)
         return null;
      
      
      buff.assignToNew(filename, fmtr);

      // remove mapping?
      //bufferPoolMap.remove(buff.block());
      
      // is this ok?
      bufferPoolMap.put(buff.block(), buff);
      
      
      numAvailable--;
      buff.pin();
      
      System.out.println("\nPin-> Number of buffers available: " + numAvailable);
      
      return buff;
   }
   
   /**
    * Unpins the specified buffer.
    * @param buff the buffer to be unpinned
    */
   synchronized void unpin(Buffer buff) {
	   System.out.println("\ninside unpin(): with buff = " + buff + " and block = " + buff.block());
	   
	   
      buff.unpin();
      if (!buff.isPinned()) {
         numAvailable++;
         // remove mapping?
         //bufferPoolMap.remove(buff.block());
      }
      
      System.out.println("Unpin-> Number of buffers available: " + numAvailable);
      
   }
   
   /**
    * Returns the number of available (i.e. unpinned) buffers.
    * @return the number of available buffers
    */
   int available() {
      return numAvailable;
   }
   
   private Buffer findExistingBuffer(Block blk) {
	   return bufferPoolMap.get(blk);
	   
//      for (Buffer buff : bufferpool) {
//         Block b = buff.block();
//         if (b != null && b.equals(blk))
//            return buff;
//      }
//      return null;
   }
   
    private Buffer chooseUnpinnedBuffer() {
    	
    	System.out.println("\ninside chooseUnpinnedBuffer(): with numAvailable = " + numAvailable);
    	
//    	if (bufferPoolMap.size() < totalBuffs) {
//    		return new Buffer();
//    	}
    	if (numAvailable > 0) {
    		return new Buffer();
    	}
    	
    	System.out.println("Using MRM replacement policy!");
    	
    	// go through all the used buffers and see if there is any !isPinned buffer
    	// select buffer with highest LSN and !isPinned
    	
	    int maxLSN = Integer.MIN_VALUE;
	    Block b = null;
	    Iterator it = bufferPoolMap.entrySet().iterator();
	    while (it.hasNext()) {
		    Map.Entry pair = (Map.Entry)it.next();
	        Block blk = (Block) pair.getKey();
	        Buffer buff = (Buffer) pair.getValue();
	        if (!buff.isPinned()) {
	    	    if (maxLSN < buff.getLogSequenceNumber())
	    		    b = blk;
	    	   	    maxLSN = buff.getLogSequenceNumber();
	        }
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	    
	    Buffer buff = (Buffer) bufferPoolMap.get(b);
	    
	    // remove block?
        bufferPoolMap.remove(b);
	    
	    // return buff?
	    return buff;
	   
//      for (Buffer buff : bufferpool)
//         if (!buff.isPinned())
//         return buff;
//      return null;
   }

	public Map<Block, Buffer> getBufferStatistics() {
		return this.bufferPoolMap;
	}
}
