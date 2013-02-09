/**
 * Project: Eneter.Messaging.Framework
 * Author: Ondrej Uzovic
 * 
 * Copyright © 2012 Ondrej Uzovic
 * 
 */

package eneter.messaging.nodes.channelwrapper;

import java.io.Serializable;

/**
 * The data structure representing the wrapped data.
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
        AddedData = addedData;
        OriginalData = originalData;
    }
    
    /**
     * Newly added data.
     */
    public Object AddedData;
    
    /**
     * Original (wrapped) data.
     */
    public Object OriginalData;
    
    private static final long serialVersionUID = -8325844480504249827L;
}
