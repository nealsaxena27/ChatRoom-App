package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ClientHandler implements Runnable{
    public static HashSet<ClientHandler> clientHandlers = new HashSet<>();
    public static HashMap<String, HashSet<ClientHandler>> roomHandlers = new HashMap<>();
    private Socket clientSocket;
    private BufferedReader in;
    private BufferedWriter out;
    private String clientUsername;
    private String currentRoom;

    public ClientHandler(Socket clientSocket){
        try{
            this.clientSocket = clientSocket;
            this.currentRoom = null;
            this.out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.clientUsername = in.readLine();
            clientHandlers.add(this);
            joinRoom("root");
        } catch(IOException e){
            closeEverything();
        }
    }

    public void run(){
        String messageFromClient;

        while(clientSocket.isConnected()){
            try{
                messageFromClient = in.readLine();
                if(messageFromClient.equals("/quit")){
                    closeEverything();
                    break;
                }
                else if(messageFromClient.startsWith("/nick")){
                    String newClientUsername = messageFromClient
                        .replaceFirst("^/nick", "")
                        .stripLeading();
                    broadcastMessage("SERVER: " + clientUsername + " has changed username to " + newClientUsername);
                    clientUsername = newClientUsername;
                }
                else if(messageFromClient.startsWith("/join")){
                    String newRoom = messageFromClient
                        .replaceFirst("^/join", "")
                        .stripLeading();
                    joinRoom(newRoom);
                }
                else if(messageFromClient.equals("/leave")){
                    leaveRoom();
                }
                else if(messageFromClient.equals("/users")){
                    displayUsers();
                }
                else if(messageFromClient.equals("/rooms")){
                    displayRooms();
                }
                else broadcastMessage(clientUsername + ": " + messageFromClient);
            } catch(IOException e){
                closeEverything();
                break;
            }
        }
    }

    public void broadcastMessage(String messageToSend){
        HashSet<ClientHandler> room = roomHandlers.get(currentRoom);
        if(room == null) return;
        for(ClientHandler clientHandler: room){
            try{
                if(!clientHandler.equals(this)){
                    clientHandler.out.write(messageToSend);
                    clientHandler.out.newLine();
                    clientHandler.out.flush();
                }
            } catch(IOException e){
                closeEverything();
            }
        }
    }

    public void directMessage(String messageToSend){
        try{
            out.write(messageToSend);
            out.newLine();
            out.flush();
        } catch(IOException e){
            closeEverything();
        }
    }

    public void displayUsers(){
        HashSet<ClientHandler> room = roomHandlers.get(currentRoom);
        if(room != null){
            directMessage("SERVER: List of users in your current room " + currentRoom + ":");
            for(ClientHandler clientHandler: room){
                directMessage(clientHandler.clientUsername);
            }
        }
    }

    public void displayRooms(){
        Set<String> rooms = roomHandlers.keySet();
        if(!rooms.isEmpty()){
            directMessage("SERVER: List of active rooms on the server: ");
            for(String room: rooms){
                directMessage(room);
            }
        }
    }

    public void joinRoom(String newRoom){
        leaveRoom();
        currentRoom = newRoom;
        roomHandlers.putIfAbsent(currentRoom, new HashSet<>());
        roomHandlers.get(currentRoom).add(this);
        broadcastMessage("SERVER: " + clientUsername + " has entered the room " + currentRoom);
    }

    public void leaveRoom(){
        HashSet<ClientHandler> room = roomHandlers.get(currentRoom);
        if(room != null){
            broadcastMessage("SERVER: " + clientUsername + " has left the room " + currentRoom);
            room.remove(this);
            if(room.isEmpty()){
                roomHandlers.remove(currentRoom);
            }
            currentRoom = null;
        }
    }

    public void removeClientHandler(){
        leaveRoom();
        clientHandlers.remove(this);
    }

    public void closeEverything(){
        removeClientHandler();
        try{
            if(in != null) in.close();
            if(out != null) out.close();
            if(clientSocket != null) clientSocket.close();
        } catch(IOException e){
            e.printStackTrace();
        }
    }
}
