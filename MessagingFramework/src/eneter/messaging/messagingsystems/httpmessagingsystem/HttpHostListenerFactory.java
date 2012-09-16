package eneter.messaging.messagingsystems.httpmessagingsystem;

import java.net.InetSocketAddress;

import eneter.messaging.messagingsystems.tcpmessagingsystem.IServerSecurityFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.pathlisteningbase.HostListenerBase;
import eneter.messaging.messagingsystems.tcpmessagingsystem.pathlisteningbase.IHostListenerFactory;


class HttpHostListenerFactory implements IHostListenerFactory
{

    @Override
    public Class<?> getListenerType()
    {
        return HttpHostListener.class;
    }

    @Override
    public HostListenerBase CreateHostListener(InetSocketAddress address,
            IServerSecurityFactory securityFactory)
    {
        HttpHostListener aHostListener = new HttpHostListener(address,  securityFactory);
        return aHostListener;
    }

}
