package eneter.messaging.endpoints.typedmessages;

import java.io.Serializable;

public class Fake_TypedMessage implements Serializable
{
    public Fake_TypedMessage()
    {
    }

    public Fake_TypedMessage(String firstName, String secondName)
    {
        FirstName = firstName;
        SecondName = secondName;
    }

    public String FirstName;
    public String SecondName;
    
    private static final long serialVersionUID = -9000473394942426528L;
}
