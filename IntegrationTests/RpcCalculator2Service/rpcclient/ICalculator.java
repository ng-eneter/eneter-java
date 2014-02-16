package rpcclient;

import eneter.net.system.Event;
import eneter.net.system.EventArgs;

public interface ICalculator
{
    Event<EventArgs> somethingHappened();
    
    double Sum(double a, double b);
    double Subtract(double a, double b);
    double Multiplay(double a, double b);
    double Divide(double a, double b);
}
