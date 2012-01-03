/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright � 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.dataprocessing.wrapping;

import java.io.Serializable;

/**
 * The data structure representing the wrapped data.
 * @author Ondrej Uzovic
 *
 */
public class WrappedData implements Serializable
{
    /**
     * Default constructor used for the deserialization.
     */
    public WrappedData()
    {
    }
    
    /**
     * Constructs wrapped data from input parameters.
     * @param addedData new data added to the original data
     * @param originalData original data
     */
    public WrappedData(Object addedData, Object originalData)
    {
        myAddedData = addedData;
        myOriginalData = originalData;
    }
    
    /**
     * Newly added data.
     */
    public Object myAddedData;
    
    /**
     * Original (wrapped) data.
     */
    public Object myOriginalData;
    
    private static final long serialVersionUID = -8325844480504249827L;
}
