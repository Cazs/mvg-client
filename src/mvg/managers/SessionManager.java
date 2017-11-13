/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mvg.auxilary.IO;
import mvg.auxilary.RemoteComms;
import mvg.auxilary.Session;
import mvg.model.User;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author ghost
 */
public class SessionManager 
{
    private static final SessionManager sess_mgr = new SessionManager();
    private HashMap<String, Session> sessions = new HashMap<>();
    private Session active;
    private User active_user;
    public static final String TAG = "SessionManager";
    
    private SessionManager(){};
    
    public static SessionManager getInstance()
    {
        return sess_mgr;
    }

    /**
     * Method to add a Session to the HashMap of sessions
     * @param session <pre>Session</pre> object to be added.
     */
    public void addSession(Session session)
    {
        if(session==null)
            return;
        //check if session being added exists in list of sessions
        Session s = getUserSession(session.getUsername());
        if(s!=null)
        {
            //if it exists in the list,update the date, session_id & ttl
            s.setDate(session.getDate());
            s.setSessionId(session.getSessionId());
            s.setTtl(session.getTtl());
            setActive(s);
        }
        else
        {
            //if it doesn't exist, add it to the list and set it as active
            sessions.put(session.getSessionId(), session);
            setActive(session);
        }
        try 
        {
            Session active_sess = getActive();
            if(active_sess!=null)
            {
                ArrayList<AbstractMap.SimpleEntry<String,String>> headers = new ArrayList<>();
                headers.add(new AbstractMap.SimpleEntry<>("Cookie", active_sess.getSessionId()));
                String user_json = RemoteComms.sendGetRequest("/api/user/" + active_sess.getUsername(), headers);
                if(user_json!=null)
                {
                    if(!user_json.equals("[]") && !user_json.equals("null"))
                    {
                        Gson gson = new GsonBuilder().create();
                        User user = gson.fromJson(user_json, User.class);
                        setActiveUser(user);
                    }else{
                        IO.logAndAlert("Invalid Credentials", "No user was found that matches the given credentials.", IO.TAG_ERROR);
                    }
                }else{
                    IO.logAndAlert("Invalid Credentials", "No user was found that matches the given credentials.", IO.TAG_ERROR);
                }
            }else{
                IO.logAndAlert("Session Error", "No active sessions.", IO.TAG_ERROR);
            }
        } catch (IOException ex) 
        {
            IO.log(TAG, IO.TAG_ERROR, ex.getMessage());
        }
    }
    
    public void setActiveUser(User empl)
    {
        this.active_user =empl;
    }
    
    public User getActiveUser()
    {
        return this.active_user;
    }
    
    public HashMap<String, Session> getSessions()
    {
        return sessions;
    }
    
    public Session getUserSession(String usr)
    {
        return sessions.get(usr);
    }
    
    public void setActive(Session session)
    {
        this.active = session;
    }
    
    public Session getActive()
    {
        return active;
    }
}
