package eneter.messaging.endpoints.rpc;

import java.lang.reflect.*;
import java.util.HashSet;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.net.system.*;


class ServiceInterfaceChecker
{
    public static <T> void check(Class<T> clazz)
    {
        // It must be an interface.
        if (!clazz.isInterface())
        {
            String anErrorMessage = "The type '" + clazz.getSimpleName() + "' is not an interface.";
            EneterTrace.error(anErrorMessage);
            throw new IllegalStateException(anErrorMessage);
        }
        
        // Generic service type is not supported.
        if (clazz.getTypeParameters().length > 0)
        {
            String anErrorMessage = "The service interface '" + clazz.getSimpleName() + "' cannot be generic.";
            EneterTrace.error(anErrorMessage);
            throw new IllegalStateException(anErrorMessage);
        }
        
        HashSet<String> aUsedNames = new HashSet<String>();
        
        // Check declared methods and arguments of all public methods.
        for (Method aMethodInfo : clazz.getMethods())
        {
            // Overloading is not allowed.
            String aMethodName = aMethodInfo.getName();
            if (aUsedNames.contains(aMethodName))
            {
                String anErrorMessage = "The interface already contains method or event with the name '" + aMethodInfo.getName() + "'.";
                EneterTrace.error(anErrorMessage);
                throw new IllegalStateException(anErrorMessage);
            }
            
            aUsedNames.add(aMethodName);
            
            // If it is an event.
            // Event<MyEventArgs> somethingIsDone()
            Type aGenericReturnType = aMethodInfo.getGenericReturnType();
            Class<?> aReturnType = aMethodInfo.getReturnType();
            if (aReturnType == Event.class && aMethodInfo.getParameterTypes().length == 0)
            {
                // Get type of event args. 
                /*if (aGenericReturnType instanceof ParameterizedType)
                {
                    ParameterizedType aGenericParameter = (ParameterizedType) aGenericReturnType;
                    Class<?> anEventArgsType = (Class<?>) aGenericParameter.getActualTypeArguments()[0];
                    
                    // If the event argument is not derived from EventArgs.
                    if (!EventArgs.class.isAssignableFrom(anEventArgsType))
                    {
                        String anErrorMessage = "The event '" + aMethodInfo.getName() + "' has argument '" + anEventArgsType.getSimpleName() + "' which is not derived from EventArgs.";
                        EneterTrace.error(anErrorMessage);
                        throw new IllegalStateException(anErrorMessage);
                    }
                }*/
            }
            // If it is a method.
            else
            {
                // Generic return type is not supported because of generic erasure effect in Java.
                if (aGenericReturnType instanceof ParameterizedType)
                {
                    String anErrorMessage = "The return parameter of method '" + aMethodInfo.getName() + "' is generic.";
                    EneterTrace.error(anErrorMessage);
                    throw new IllegalStateException(anErrorMessage);
                }
                
                // Generic methods are not supported.
                TypeVariable<Method>[] aGenerics = aMethodInfo.getTypeParameters();
                if (aGenerics.length > 0)
                {
                    String anErrorMessage = "Method '" + aMethodInfo.getName() + "' is generic.";
                    EneterTrace.error(anErrorMessage);
                    throw new IllegalStateException(anErrorMessage);
                }
                
                // Generic arguments are not supported.
                Type[] anArguments = aMethodInfo.getGenericParameterTypes();
                for (int i = 0; i < anArguments.length; ++i)
                {
                    if (anArguments[i] instanceof ParameterizedType)
                    {
                        String anErrorMessage = "The " + Integer.toString(i + 1) + " argument of method '" + aMethodInfo.getName() + "' is generic.";
                        EneterTrace.error(anErrorMessage);
                        throw new IllegalStateException(anErrorMessage);
                    }
                }
            }
        }
        
    }
}
