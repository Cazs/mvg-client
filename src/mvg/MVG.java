/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg;

import com.sun.javafx.application.LauncherImpl;
import mvg.auxilary.Globals;
import mvg.auxilary.IO;
import mvg.controllers.ScreenController;
import mvg.managers.ScreenManager;
import mvg.model.Screens;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author ghost
 */
public class MVG extends Application
{
    @Override
    public void start(Stage stage) throws Exception 
    {
        stage.setOnCloseRequest(event ->
        {
            int result = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit?");
            if(result==JOptionPane.OK_OPTION)
            {
                stage.close();
                System.exit(0);
            }
            else  event.consume();
        });
        //grid = new GridDisplay(2, 4);
        ScreenManager screen_mgr = ScreenManager.getInstance();//new ScreenManager();
        IO.getInstance().init(screen_mgr);

        if(screen_mgr.loadScreen(Screens.LOGIN.getScreen(),getClass().getResource("views/"+Screens.LOGIN.getScreen())))
        {
            screen_mgr.setScreen(Screens.LOGIN.getScreen());
            HBox root = new HBox();
            HBox.setHgrow(screen_mgr, Priority.ALWAYS);

            root.getChildren().addAll(screen_mgr);

            Scene scene = new Scene(root);
            stage.setTitle(Globals.APP_NAME.getValue());
            stage.setScene(scene);

            stage.setMinHeight(600);
            stage.setHeight(700);
            stage.setMinWidth(600);

            if(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth()>=1200)
                stage.setWidth(900);
            stage.show();
        }else
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, "login screen was not successfully loaded.");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        LauncherImpl.launchApplication(mvg.MVG.class, MVGPreloader.class, args);
    }
    
}
