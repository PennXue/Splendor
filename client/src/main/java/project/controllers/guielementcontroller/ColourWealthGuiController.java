package project.controllers.guielementcontroller;

import ca.mcgill.comp361.splendormodel.model.Colour;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import project.App;

public class ColourWealthGuiController implements Initializable {

  @FXML
  private Text gemCountText;

  @FXML
  private Text tokenCountText;

  @FXML
  private Text separationText;

  @FXML
  private Rectangle backgroundRectangle;

  private final Colour colour;

  public ColourWealthGuiController(Colour colour) {
    this.colour = colour;
  }


  public Text getGemCountText() {
    return gemCountText;
  }

  public Text getTokenCountText() {
    return tokenCountText;
  }



  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    String colourString = App.getColourStringMap().get(colour);
    String tipInfo =
        "Number on left is the number of cards of this corresponding colour in hand\n" +
            "Number on right is the number of tokens of this corresponding colour in hand";
    if (colour == Colour.GOLD) {
      tipInfo += "\nNote that gold card can not be paired!";
    }
    App.bindToolTip(tipInfo,15,backgroundRectangle, 20);
    backgroundRectangle.setFill(Color.web(colourString));
    if (colour == Colour.BLACK) {
      gemCountText.setFill(Color.WHITE);
      separationText.setFill(Color.WHITE);
      tokenCountText.setFill(Color.WHITE);
    }
  }
}
