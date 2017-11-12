package mvg.controllers;

import javafx.scene.image.ImageView;
import mvg.auxilary.IO;
import mvg.managers.ScreenManager;
import mvg.managers.SessionManager;
import mvg.model.Employee;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class NavController extends ScreenController implements Initializable
{
    @FXML
    private Label lblScreen;
    @FXML
    private ImageView btnBack,btnNext,btnHome;

    @Override
    public void refreshView()
    {
        try
        {
            //Render default profile image
            Image image = SwingFXUtils.toFXImage(ImageIO.read(new File("images/profile.png")), null);
            super.getProfileImageView().setImage(image);

            /*//Render forward nav icon
            image = SwingFXUtils.toFXImage(ImageIO.read(new File("images/chevron_right_black.png")), null);
            btnNext.setImage(image);

            //Render previous nav icon
            image = SwingFXUtils.toFXImage(ImageIO.read(new File("images/chevron_left_black.png")), null);
            btnBack.setImage(image);

            //Render home nav icon
            image = SwingFXUtils.toFXImage(ImageIO.read(new File("images/home_black.png")), null);
            btnHome.setImage(image);*/
        } catch (IOException e)
        {
            e.printStackTrace();
            IO.log(getClass().getName(), IO.TAG_ERROR, "Could not load default profile image.");
        }

        if (SessionManager.getInstance().getActive() != null)
        {
            if (!SessionManager.getInstance().getActive().isExpired())
            {
                //Render user name
                Employee e = SessionManager.getInstance().getActiveEmployee();
                if(e!=null)
                    this.getUserNameLabel().setText(e.toString());
                else IO.log(getClass().getName(), IO.TAG_ERROR, "No active sessions.");
            }else
            {
                IO.log(getClass().getName(), IO.TAG_ERROR, "No active sessions were found!");
                return;
            }
        } else {
            IO.log(getClass().getName(), IO.TAG_ERROR, "No valid sessions were found!");
            return;
        }
        //Render current screen name
        lblScreen.setText(ScreenManager.getInstance().peekScreenControllers().getKey());
    }

    @Override
    public void refreshModel()
    {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        ScreenManager.getInstance().setLblScreenName(lblScreen);
        refreshView();
    }
}
