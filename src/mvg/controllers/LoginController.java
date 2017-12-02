/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.controllers;

import mvg.MVG;
import mvg.auxilary.IO;
import mvg.auxilary.RemoteComms;
import mvg.managers.*;
import mvg.auxilary.Session;
import mvg.exceptions.LoginException;
import mvg.model.Screens;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;

import javax.swing.JOptionPane;

/**
 * views Controller class
 *
 * @author ghost
 */
public class LoginController extends ScreenController implements Initializable
{
    @FXML
    private TextField txtUsr;
    @FXML
    private TextField txtPwd;

    @Override
    public void refreshView()
    {
        //TODO: remove this
        txtUsr.setText("ghost");
        txtPwd.setText("12345678");
    }

    @Override
    public void refreshModel()
    {
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) 
    {
        refreshView();
    }

    @FXML
    public void resetPassword()
    {
        try
        {
            if(MVG.getScreenManager().loadScreen(Screens.RESET_PWD.getScreen(),getClass().getResource("../views/"+Screens.RESET_PWD.getScreen())))
                MVG.getScreenManager().setScreen(Screens.RESET_PWD.getScreen());
            else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load password reset screen.");
        } catch (IOException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        }
    }

    @FXML
    public void login()
    {
        final ScreenManager screenManager = MVG.getScreenManager();
        MVG.getScreenManager().showLoadingScreen(param ->
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        String usr = txtUsr.getText(), pwd=txtPwd.getText();
                        if(usr!=null && pwd!=null)
                        {
                            Session session = RemoteComms.auth(usr, pwd);
                            SessionManager ssn_mgr = SessionManager.getInstance();
                            ssn_mgr.addSession(session);

                            //load User data to memory
                            UserManager.getInstance().loadDataFromServer();

                            if (screenManager.loadScreen(Screens.HOME.getScreen(), MVG.class.getResource("views/" + Screens.HOME.getScreen())))
                            {
                                screenManager.setScreen(Screens.HOME.getScreen());
                            } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load home screen.");
                        }else{
                            JOptionPane.showMessageDialog(null, "Invalid entry.", "Login failure", JOptionPane.ERROR_MESSAGE);
                            IO.log(getClass().getName(), IO.TAG_ERROR, "invalid input.");
                        }
                    }catch(ConnectException ex)
                    {
                        JOptionPane.showMessageDialog(null, ex.getMessage() + ", \nis the server up? are you connected to the network?", "Login failure", JOptionPane.ERROR_MESSAGE);
                        IO.log(getClass().getName(), IO.TAG_ERROR, ex.getMessage() + ", \nis the server up? are you connected to the network?");
                        ex.printStackTrace();
                    } catch (LoginException ex)
                    {
                        JOptionPane.showMessageDialog(null, ex.getMessage(), "Login failure", JOptionPane.ERROR_MESSAGE);
                        IO.log(getClass().getName(), IO.TAG_ERROR, ex.getMessage());
                        ex.printStackTrace();
                    } catch (IOException e)
                    {
                        IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                        e.printStackTrace();
                    }
                }
            }).start();
            return null;
        });
    }
}
