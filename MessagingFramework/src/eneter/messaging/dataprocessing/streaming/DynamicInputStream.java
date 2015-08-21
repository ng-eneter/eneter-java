/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2015 Ondrej Uzovic
 * 
 */

package eneter.messaging.dataprocessing.streaming;

import java.io.*;

import eneter.messaging.dataprocessing.messagequeueing.MessageQueue;
import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.internal.ThreadLock;

/**
 * Stream which can be written and read at the same time.
 * The dynamic stream supports writing of data by one thread and reading by another.
 * The reading operation is blocked until the requested amount of data is not available.
 *
 */
public class DynamicInputStream extends InputStream
{
    /**
     * Gets the blocking mode. If blocking mode then Read method blocks until data is available.
     * @param isBlocking true if the reading shall be blocking until data is available.
     */
    public void setBlockingMode(boolean isBlocking)
    {
        if (isBlocking == true)
        {
            myMessageQueue.blockProcessingThreads();
        }
        else
        {
            myMessageQueue.unblockProcessingThreads();
        }
    }
    
    /**
     * Gets the blocking mode. If blocking mode then Read method blocks until data is available.
     * @return true if the reading is blocked until required amount of data is available.
     */
    public boolean getBlockingMode()
    {
        return myMessageQueue.isBlockingMode();
    }
    
    /**
     * Reads one byte from the stream.
     * 
     * If the requested byte is not available the thread is blocked until byte is put to the stream = from another thread.
     * 
     */
    @Override
    public int read() throws IOException
    {
        myLockDuringReading.lock();
        try
        {
            byte[] aByte = new byte[1];
            if (read(aByte, 0, aByte.length) > 0)
            {
                return aByte[0];
            }
            
            return -1;
        }
        finally
        {
            myLockDuringReading.unlock();
        }
    }
    
    /**
     * Reads data from the stream to the specified buffer.
     * 
     * If the requested amount of data is not available the thread is blocked until required amount of data
     * is available - until data is written by another thread.
     */
    @Override
    public int read(byte[] buffer, int offset, int count) throws IOException
    {
        //EneterTrace aTrace = EneterTrace.entering();
        //try
        //{
            if (myIsClosed)
            {
                return 0;
            }
            
            // Only one thread can be reading.
            // The thread waits until all data is available.
            // Note: If more threads would try to read in parallel then it would be bad because the reading removes the data from the queue.
            myLockDuringReading.lock();
            try
            {
                int aReadSize = 0;

                // If there is a memory stream that we already started to read but not finished. (i.e.: requested data ended at the middle
                // of the memory stream.
                if (myDataInProgress != null)
                {
                    // Read the data from the memory stream, and store the really read size.
                    aReadSize = myDataInProgress.read(buffer, offset, count);

                    // If we are at the end of the memory stream then close it.
                    if (myDataInProgress.available() == 0)
                    {
                        myDataInProgress.close();
                        myDataInProgress = null;
                    }
                }

                // While stream is not closed and we do not read required amount of data.
                while (!myIsClosed && aReadSize < count)
                {
                    try
                    {
                        // Remove the sequence of bytes from the queue.
                        myDataInProgress = myMessageQueue.dequeueMessage();
                    }
                    catch (Exception err)
                    {
                        // Note: Android 2.1 does not support inner exception in the constructor.
                        //       Therefore it is set via initCause(..) method.
                        IOException aNewErr = new IOException("Removing bytes from the queue failed.");
                        aNewErr.initCause(err);
                        throw aNewErr;
                    }

                    if (myDataInProgress != null)
                    {
                        // Try to read the remaining amount of bytes from the memory stream.
                        aReadSize += myDataInProgress.read(buffer, offset + aReadSize, count - aReadSize);

                        // If we are at the end of the memory stream then close it.
                        if (myDataInProgress.available() == 0)
                        {
                            myDataInProgress.close();
                            myDataInProgress = null;
                        }
                    }

                    // If we are in unblocking mode then do not loop if the queue is empty.
                    if (myMessageQueue.isBlockingMode() == false && myMessageQueue.getCount() == 0)
                    {
                        break;
                    }
                }

                return aReadSize;
            }
            finally
            {
                myLockDuringReading.unlock();
            }
        //}
        //finally
        //{
        //    EneterTrace.leaving(aTrace);
        //}
    }
    
    /**
     * Writes the data to the stream.
     * 
     * @param buffer Buffer to be written to the stream
     * @param offset Starting podition in the buffer from where data will be read.
     * @param count Amount of data to be read from the buffer and written to the stream.
     */
    public void write(byte[] buffer, int offset, int count)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myLockDuringWriting.lock();
            try
            {
                if (myIsClosed)
                {
                    return;
                }
                
                byte[] aData = new byte[count];
                System.arraycopy(buffer, offset, aData, 0, count);
                
                ByteArrayInputStream aChunkStream = new ByteArrayInputStream(aData, 0, count);
                myMessageQueue.enqueueMessage(aChunkStream);
            }
            finally
            {
                myLockDuringWriting.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Writes data to the stream the way that it just stores the reference to the input data.
     * 
     * It does not copy the incoming data to the stream but instead of that it just stores the reference.
     * This approach is very fast but the input byte[] array should not be modified after calling this method.
     * 
     * @param data data to be written to the stream.
     * @param offset Starting position in the buffer from where data will be read.
     * @param count Amount of data to be read from the buffer and written to the stream.
     */
    public void writeWithoutCopying(byte[] data, int offset, int count)
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myLockDuringWriting.lock();
            try
            {
                if (myIsClosed)
                {
                    return;
                }
                
                ByteArrayInputStream aChunkStream = new ByteArrayInputStream(data, offset, count);
                myMessageQueue.enqueueMessage(aChunkStream);
            }
            finally
            {
                myLockDuringWriting.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }
    
    /**
     * Closes the stream and releases the reading thread waiting for data.
     */
    @Override
    public void close()
    {
        EneterTrace aTrace = EneterTrace.entering();
        try
        {
            myLockDuringWriting.lock();
            try
            {
                myMessageQueue.unblockProcessingThreads();
                myIsClosed = true;
            }
            finally
            {
                myLockDuringWriting.unlock();
            }
        }
        finally
        {
            EneterTrace.leaving(aTrace);
        }
    }

    // The writing puts the byte sequences to the queue as they come.
    // The reading removes the sequences of bytes from the queue.
    private MessageQueue<ByteArrayInputStream> myMessageQueue = new MessageQueue<ByteArrayInputStream>();
    private ThreadLock myLockDuringReading = new ThreadLock();
    private ThreadLock myLockDuringWriting = new ThreadLock();
    
    private volatile boolean myIsClosed = false;
    private ByteArrayInputStream myDataInProgress;
}
