/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.controllers;

import mvg.auxilary.*;
import mvg.managers.ScreenManager;
import mvg.managers.SessionManager;
import mvg.model.Screens;
import java.io.IOException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * views Controller class
 *
 * @author ghost
 */
public class HomescreenController extends ScreenController implements Initializable
{
    @FXML
    private Button btnCreateAccount;
    private ColorAdjust colorAdjust = new ColorAdjust();

    @Override
    public void refreshView()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading homescreen view..");
    }

    @Override
    public void refreshModel()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading homescreen data model..");
    }
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) 
    {
    }
    
    private Rectangle createTile()
    {
        Rectangle rectangle = new Rectangle(160, 100);
        Random rand = new Random();
        double r = rand.nextDouble();
        double g = rand.nextDouble();
        double b = rand.nextDouble();
        rectangle.setStroke(Color.WHITE);
        rectangle.setFill(new Color(r,g,b,0.5));

        return rectangle;
    }

    @FXML
    public void showSettings()
    {
        try
        {
            if(ScreenManager.getInstance().loadScreen(Screens.SETTINGS.getScreen(),getClass().getResource("../views/"+Screens.SETTINGS.getScreen())))
                ScreenManager.getInstance().setScreen(Screens.SETTINGS.getScreen());
            else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load settings screen.");
        } catch (IOException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        }
    }
}
