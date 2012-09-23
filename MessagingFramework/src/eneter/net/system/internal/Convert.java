package eneter.net.system.internal;

public class Convert
{
    public static String toBase64String(byte[] bytes)
    {
        StringBuilder aStringBuilder = new StringBuilder();
        toBase64String(bytes, aStringBuilder);
        
        return aStringBuilder.toString();
    }
    
    /**
     * Converts an array of bytes to its equivalent string representation
     * that is encoded with base-64 digits. 
     * @param bytes bytes to be converted.
     * @param result string builder that will append the result. 
     */
    public static void toBase64String(byte[] bytes, StringBuilder result)
    {
        int oDataLen = (bytes.length * 4 + 2) / 3; // output length without padding
       
        int ip = 0;
        int iEnd = bytes.length ;
        int op = 0;
        while (ip < iEnd)
        {
            int i0 = bytes[ip++] & 0xff;
            int i1 = ip < iEnd ? bytes[ip++] & 0xff : 0;
            int i2 = ip < iEnd ? bytes[ip++] & 0xff : 0;
            int o0 = i0 >>> 2;
            int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
            int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
            int o3 = i2 & 0x3F;
            
            result.append(map1[o0]);
            ++op;
            
            result.append(map1[o1]);
            ++op;
            
            result.append(op < oDataLen ? map1[o2] : '=');
            ++op;
            
            result.append(op < oDataLen ? map1[o3] : '=');
            ++op;
        }
    }
    
    /**
     * Decodes the specified string, which encodes binary data as base-64 digits,
     * to a byte array.
     * @param s decoded string
     * @return array of bytes
     */
    public static byte[] fromBase64String(String s)
    {
        return fromBase64String(s, 0, s.length());
    }
    
    /**
     * Decodes the specified string, which encodes binary data as base-64 digits,
     * to a byte array.
     * @param s decoded string
     * @param startPosition start position
     * @param length length
     * @return
     */
    public static byte[] fromBase64String(String s, int startPosition, int length)
    {
        int iLen = length;
        
        if (iLen % 4 != 0)
        {
            throw new IllegalArgumentException ("Length of Base64 encoded input string is not a multiple of 4.");
        }
        
        while (iLen > 0 && s.charAt(startPosition + iLen-1) == '=')
        {
            iLen--;
        }
        
        int oLen = (iLen * 3) / 4;
        byte[] out = new byte[oLen];
        
        int ip = startPosition;
        int iEnd = startPosition + iLen;
        int op = 0;
        while (ip < iEnd)
        {
            int i0 = s.charAt(ip++);
            int i1 = s.charAt(ip++);
            int i2 = ip < iEnd ? s.charAt(ip++) : 'A';
            int i3 = ip < iEnd ? s.charAt(ip++) : 'A';
            
            if (i0 > 127 || i1 > 127 || i2 > 127 || i3 > 127)
            {
                throw new IllegalArgumentException ("Illegal character in Base64 encoded data.");
            }
            
            int b0 = map2[i0];
            int b1 = map2[i1];
            int b2 = map2[i2];
            int b3 = map2[i3];
            
            if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0)
            {
                throw new IllegalArgumentException ("Illegal character in Base64 encoded data.");
            }
            
            int o0 = ( b0 <<2) | (b1>>>4);
            int o1 = ((b1 & 0xf)<<4) | (b2>>>2);
            int o2 = ((b2 & 3)<<6) | b3;
            out[op++] = (byte)o0;
            
            if (op<oLen)
            {
                out[op++] = (byte)o1;
            }
            
            if (op<oLen)
            {
                out[op++] = (byte)o2;
            }
        }
        
        return out;
    }
    
    
    // Mapping table from 6-bit nibbles to Base64 characters.
    private static final char[] map1 = new char[64];
  
    // Mapping table from Base64 characters to 6-bit nibbles.
    private static final byte[] map2 = new byte[128];
    
    // Static constructor.
    static
    {
        int i=0;
        for (char c='A'; c<='Z'; c++) map1[i++] = c;
        for (char c='a'; c<='z'; c++) map1[i++] = c;
        for (char c='0'; c<='9'; c++) map1[i++] = c;
        map1[i++] = '+'; map1[i++] = '/';
        
        for (i=0; i<map2.length; i++) map2[i] = -1;
        for (i=0; i<64; i++) map2[map1[i]] = (byte)i;
    }
}
