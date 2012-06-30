package Chat;

public class ChatMessage
{
   
    public String To;
    
    public String Serialize()
    {
        return "aa";
    }
    
    public static ChatMessage Deserialize(Object object)
    {
        return new ChatMessage();
    }
}
