package messagebusechoservice;

// Simple echo service.
class EchoService implements IEcho
{
    @Override
    public String hello(String text)
    {
        return text;
    }
    
}
