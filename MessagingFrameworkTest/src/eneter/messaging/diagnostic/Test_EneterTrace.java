package eneter.messaging.diagnostic;

import java.util.ArrayList;

import org.junit.Test;

public class Test_EneterTrace
{

	@Test
	public void TraceMessages()
	{
		EneterTrace.info("This is info.");
		EneterTrace.warning("This is warning.");
		EneterTrace.error("This is error.");
		EneterTrace.error("This is error.", "detail error info");

		// Trace exception
		try
		{
			try
			{
				try
				{
					TestMethod1();
				}
				catch (Exception err)
				{
					throw new Exception("2nd Inner Exception.", err);
				}
			}
			catch (Exception err)
			{
				throw new Exception("3th Inner Exception.", err);
			}
		}
		catch (Exception err)
		{
			EneterTrace.info("Info with exception", err);
			EneterTrace.warning("Warning with exception", err);
			EneterTrace.error("Error with exception", err);
		}

		EneterTrace.error("This is the error with Null", (Exception)null);

		try
		{
			EneterTrace.setTraceLog(System.out);

			EneterTrace.info("Info also to console.");
			EneterTrace.warning("Warning also to console.");
			EneterTrace.error("Error also to console.");
		}
		finally
		{
			EneterTrace.setTraceLog(null);
		}
	}
	
	@Test
	public void EneterExitMethodTrace() 
			throws InterruptedException
	{
		//EneterTrace.TraceLog = Console.Out;
		EneterTrace.setDetailLevel(EneterTrace.EDetailLevel.Debug);
		
		AutoCloseable aTrace = EneterTrace.entering();
		try
		{
			//EneterTrace.Info("Hello");
			Thread.sleep(1000);
		}
		finally
		{
			aTrace.close();
		}

		EneterTrace.setDetailLevel(EneterTrace.EDetailLevel.Short);
		EneterTrace.setTraceLog(null);
	}

	
	@Test
	public void TraceMessagesMultithread() 
			throws InterruptedException
	{
        // Create 10 competing threads
        final ArrayList<Thread> aThreads = new ArrayList<Thread>();
        
        for (int t = 0; t < 10; ++t)
        {
            Thread aThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                	TraceMessages();
    				//Thread.sleep(1);
                }
            }); 
                    
            aThreads.add(aThread);
        }

        // Start sending from threads
        for (Thread t : aThreads)
        {
            t.start();
        }
        
        // Start sending from threads
        for (Thread t : aThreads)
        {
        	t.join(300);
        	// TODO : What test?
        	//assertFalse(??);
        }
	}

	
	/*
	@Test
	public void FilterTest()
	{
		EneterTrace.DetailLevel = EneterTrace.EDetailLevel.Debug;
		
		// Write traces to the string.
		EneterTrace.TraceLog = new StringWriter();
		
		try
		{
			// Eneter trace.
			EneterTrace.NameSpaceFilter = new Regex("^Eneter");
			EneterTrace.Debug("This message shall be traced.");
			Thread.Sleep(10);
			string aMessage = EneterTrace.TraceLog.ToString();
			Assert.IsTrue(aMessage.Contains("This message shall be traced."));
			
			
			// Create the new "log".
			EneterTrace.TraceLog = new StringWriter();
			
			// Eneter trace shall be filtered out.
			EneterTrace.NameSpaceFilter = new Regex(@"^(?!\bEneter\b)");
			EneterTrace.Debug("This message shall not be traced.");
			Thread.Sleep(10);
			Assert.AreEqual("", EneterTrace.TraceLog.ToString());
		}
		finally
		{
			EneterTrace.TraceLog = null;
			EneterTrace.NameSpaceFilter = null;
			EneterTrace.DetailLevel = EneterTrace.EDetailLevel.Short;
		}
	}
	*/

	private void TestMethod1()
	{
		TestMethod2();
	}

	private void TestMethod2()
	{
		TestMethod3();
	}

	private void TestMethod3()
	{
		throw new UnsupportedOperationException("1st Inner Exception.");
	}
}
