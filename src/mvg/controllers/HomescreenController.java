/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import mvg.MVG;
import mvg.auxilary.*;
import mvg.managers.ScreenManager;
import mvg.managers.SlideshowManager;
import mvg.model.Screens;
import java.io.File;
import java.io.IOException;
import javafx.fxml.FXML;
import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import javax.imageio.ImageIO;

/**
 * views Controller class
 *
 * @author ghost
 */

public class HomescreenController extends ScreenController implements Initializable
{
    @FXML
    private Button btnCreateAccount;
    @FXML
    private ImageView imgSlide;
    @FXML
    private HBox hboxSliderNav;
    @FXML
    private VBox enquiryForm;
    @FXML
    private TextField txtEnquiry;
    private ColorAdjust colorAdjust = new ColorAdjust();
    //@FXML
    //private ScrollPane slideshowScrollPane;

    @Override
    public void refreshView()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading homescreen view..");

        try
        {
            if(SlideshowManager.getInstance().getImagePaths()!=null)
            {
                //Setup first slide
                Image image = SwingFXUtils.toFXImage(ImageIO
                        .read(new File("images/slider/" + SlideshowManager.getInstance()
                                .getImagePaths()[SlideshowManager.current_index])), null);
                imgSlide.setImage(image);
                imgSlide.setSmooth(true);
            } else IO.log(getClass().getName(), IO.TAG_ERROR, "slider image paths are null.");

            /*//Setup second slide
            image = SwingFXUtils.toFXImage(ImageIO.read(new File("images/slider/2.jpg")), null);
            imgSlide2.setImage(image);
            imgSlide2.setSmooth(true);*/

            //Set ImageView sizes
            if(MVG.getScreenManager()!=null)
            {
                imgSlide.setFitHeight(MVG.getScreenManager().getStage().getHeight());
                imgSlide.setFitWidth(MVG.getScreenManager().getStage().getWidth());

                //imgSlide2.setFitHeight(MVG.getScreenManager().getStage().getHeight());
                //imgSlide2.setFitWidth(MVG.getScreenManager().getStage().getWidth());
            }else
            {
                IO.log(getClass().getName(), IO.TAG_ERROR, "MVG ScreenManager is null.");
            }

            hboxSliderNav.getChildren().setAll(new Node[]{});
            //Setup slider nav
            for(int i=0;i<SlideshowManager.getInstance().getImagePaths().length;i++)
            {
                Circle circle = new Circle();
                circle.setRadius(10);
                if(i==SlideshowManager.current_index)
                    circle.setFill(Color.color(1.0,.35f,0).brighter());
                else circle.setFill(Color.DARKGREY);
                hboxSliderNav.getChildren().add(circle);
            }

            final DoubleProperty opacity =  MVG.getScreenManager().opacityProperty();
            Timeline fade = new Timeline(new KeyFrame(Duration.ONE, new KeyValue(opacity, 0.0)),
                    new KeyFrame(Duration.millis(500),new KeyValue(opacity, 1.0)));
            fade.play();
        } catch (IOException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        }
    }

    @Override
    public void refreshModel()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading homescreen data model..");
        SlideshowManager.getInstance().initialize();
    }
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) 
    {
        if(MVG.getScreenManager()!=null)
        {
            SlideshowManager.getInstance().initialize();
            //imgSlide.setFitWidth(Double.parseDouble(String.valueOf(MVG.getScreenManager().getStage().getWidth())));
            //imgSlide.setFitHeight(Double.parseDouble(String.valueOf(MVG.getScreenManager().getStage().getHeight())));

            MVG.getScreenManager().getStage().widthProperty().addListener((observable, oldValue, newValue) ->
            {
                imgSlide.setFitWidth(Double.parseDouble(String.valueOf(newValue)));
                //imgSlide2.setFitWidth(Double.parseDouble(String.valueOf(newValue)));
            });
            MVG.getScreenManager().getStage().heightProperty().addListener((observable, oldValue, newValue) ->
            {
                imgSlide.setFitHeight(Double.parseDouble(String.valueOf(newValue)));
                //imgSlide2.setFitHeight(Double.parseDouble(String.valueOf(newValue)));
            });

            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    while (true)
                    {
                        try
                        {
                            Platform.runLater(() -> nextSlide());
                            Thread.sleep(MVG.DELAY);
                        } catch (InterruptedException e)
                        {
                            e.printStackTrace();
                            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                        }
                    }
                }
            }).start();
        }
    }

    @FXML
    public void transportClick()
    {
        txtEnquiry.setText("Transport");
        enquiryForm.setVisible(!enquiryForm.isVisible());
    }

    @FXML
    public void accomodationClick()
    {
        txtEnquiry.setText("Accomodation");
        enquiryForm.setVisible(!enquiryForm.isVisible());
    }

    @FXML
    public void experienceClick()
    {
        txtEnquiry.setText("Experience");
        enquiryForm.setVisible(!enquiryForm.isVisible());
    }

    @FXML
    public void submitEnquiry()
    {

    }

    @FXML
    public void showSettings()
    {
        try
        {
            if(MVG.getScreenManager().loadScreen(Screens.SETTINGS.getScreen(),getClass().getResource("../views/"+Screens.SETTINGS.getScreen())))
                MVG.getScreenManager().setScreen(Screens.SETTINGS.getScreen());
            else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load settings screen.");
        } catch (IOException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        }
    }

    @FXML
    public void nextSlide()
    {
        //slideshowScrollPane.setHvalue(slideshowScrollPane.getHmax());
        if(SlideshowManager.getInstance().getImagePaths()!=null)
        {
            if(SlideshowManager.current_index+1 < SlideshowManager.getInstance().getImagePaths().length)
                SlideshowManager.current_index++;
            else SlideshowManager.current_index = 0;

            refreshView();
            /*try
            {
                Image image = SwingFXUtils.toFXImage(ImageIO.read(new File("images/slider/" + SlideshowManager.getInstance()
                        .getImagePaths()[SlideshowManager.current_index])), null);
                imgSlide.setImage(image);
                imgSlide.setSmooth(true);
            } catch (IOException e)
            {
                e.printStackTrace();
                IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
            }*/
        } else IO.log(getClass().getName(), IO.TAG_ERROR, "No slider images found.");
    }

    @FXML
    public void previousSlide()
    {
        //slideshowScrollPane.setHvalue(slideshowScrollPane.getHmin());
        if(SlideshowManager.getInstance().getImagePaths()!=null)
        {
            if(SlideshowManager.current_index-1 >= 0)
                SlideshowManager.current_index--;
            else SlideshowManager.current_index = SlideshowManager.getInstance().getImagePaths().length-1;

            refreshView();
            /*try
            {
                Image image = SwingFXUtils.toFXImage(ImageIO.read(new File("images/slider/" + SlideshowManager.getInstance()
                        .getImagePaths()[SlideshowManager.current_index])), null);
                imgSlide.setImage(image);
                imgSlide.setSmooth(true);
            } catch (IOException e)
            {
                e.printStackTrace();
                IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
            }*/
        } else IO.log(getClass().getName(), IO.TAG_ERROR, "No slider images found.");
    }
}
