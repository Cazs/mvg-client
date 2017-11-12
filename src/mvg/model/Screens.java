/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.model;

/**
 *
 * @author ghost
 */
public enum Screens 
{
    HOME("Homescreen.fxml"),
    LOGIN("Login.fxml"),
    SETTINGS("Settings.fxml"),
    CREATE_ACCOUNT("Create_account.fxml"),
    RESET_PWD("ResetPassword.fxml");

    private String screen;
    
    Screens(String screen){
        this.screen = screen;
    }
    
    public String getScreen()
    {
        return screen;
    }
}