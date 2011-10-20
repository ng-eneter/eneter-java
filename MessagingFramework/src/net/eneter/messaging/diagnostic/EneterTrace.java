/**
 * 
 */
package net.eneter.messaging.diagnostic;

import android.util.Log;

/**
 * The class provides the functionality to trace info, warning and error messages.
 * The trace displays the time, thread id and the message for every trace entry.
 * The messages can be observed on the debug port.
 * 
 * @author vachix
 *
 */
public class EneterTrace {
	/**
	 * 
	 * @param message
	 * @param details
	 */
    public static void Info(String message, String details)
    {
    	Log.i("Eneter", getMessage(message, details));
    }
    
    private static String getMessage(String message, String messageDetails)
    {
        String aMessage;

        if (messageDetails == "" || messageDetails == null)
        {
            aMessage = message;
        }
        else
        {
            aMessage = message + " Details: " + messageDetails;
        }

        return aMessage;
    }
}
