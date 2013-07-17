package net.bashtech.geobot;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class WSServer extends WebSocketServer {

    private Set<WebSocket> adminFeed = new HashSet<WebSocket>();

    public WSServer( int port ) throws UnknownHostException {
        super( new InetSocketAddress( port ) );
    }

    public WSServer( InetSocketAddress address ) {
        super( address );
    }

    @Override
    public void onOpen( WebSocket conn, ClientHandshake handshake ) {
        System.out.println( conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected." );
    }

    @Override
    public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
        System.out.println( conn.getRemoteSocketAddress().getAddress().getHostAddress() + " disconnected." );
        synchronized (adminFeed){
            adminFeed.remove(conn);
        }
    }

    @Override
    public void onMessage( WebSocket conn, String message ) {
        String log = "";

        if(message.length() < 1)
            return;

        if(message.trim().equalsIgnoreCase(BotManager.getInstance().wsAdminPassword)){
            log = "WS: " + conn.getRemoteSocketAddress().getAddress().getHostAddress() + " logged in as Admin";
            System.out.println(log);
            synchronized (adminFeed){
                adminFeed.add(conn);
            }
        }

//        Channel ch = BotManager.getInstance().getChannel(message);
//        if(ch != null){
//            log = "WS: " + conn.getRemoteSocketAddress().getAddress().getHostAddress() + " logged in as " + ch.getTwitchName();
//            synchronized (ch.wsSubscribers){
//                ch.wsSubscribers.add(conn);
//            }
//            System.out.println("DEBUG: " + ch.wsSubscribers.size());
//        }

        conn.send(log);
        sendToAdmin(log);
        System.out.println( conn + ": " + message );
    }

    @Override
    public void onError( WebSocket conn, Exception ex ) {
        ex.printStackTrace();
        if( conn != null ) {
            // some errors like port binding failed may not be assignable to a specific websocket
        }
    }

    /**
     * Sends <var>text</var> to all currently connected WebSocket clients.
     *
     * @param text
     *            The String to send across the network.
     * @throws InterruptedException
     *             When socket related I/O errors occur.
     */
    public void sendToAll( String text ) {
        Collection<WebSocket> con = connections();
        synchronized ( con ) {
            for( WebSocket c : con ) {
                c.send( text );
            }
        }
    }

    public void sendToAdmin( String text ) {
        synchronized ( adminFeed ) {
            for( WebSocket c : adminFeed ) {
                if(c.isOpen())
                    try{
                        c.send( text );
                    }catch (Exception e){
                        e.printStackTrace();
                    }
            }
        }
    }

    public void sendToSubscribers(String text, Channel channelInfo) {
        synchronized ( channelInfo.wsSubscribers ) {
            Iterator<WebSocket> iterator = channelInfo.wsSubscribers.iterator();
            while (iterator.hasNext()) {
                WebSocket element = iterator.next();

                if(element.isOpen()){
                    try{
                        element.send( text );
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else{
                    iterator.remove();
                }


            }

        }
    }
}
