package rpcservice;

public class Calculator implements ICalculator
{
    @Override
    public double Sum(double a, double b)
    {
        return a + b;
    }

    @Override
    public double Subtract(double a, double b)
    {
        return a - b;
    }

    @Override
    public double Multiplay(double a, double b)
    {
        return a * b;
    }

    @Override
    public double Divide(double a, double b)
    {
        return a / b;
    }
    
}
