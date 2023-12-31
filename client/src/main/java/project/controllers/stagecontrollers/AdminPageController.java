package project.controllers.stagecontrollers;

import com.mashape.unirest.http.exceptions.UnirestException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;
import project.App;
import project.config.GameBoardLayoutConfig;
import project.connection.LobbyRequestSender;
import project.controllers.popupcontrollers.lobbypopup.LobbyWarnPopUpController;
import project.view.lobby.PlayerLobbyGui;
import project.view.lobby.RegisteredGameGui;
import project.view.lobby.communication.GameParameters;
import project.view.lobby.communication.Player;
import project.view.lobby.communication.Role;

/**
 * AdminPageController.
 */
public class AdminPageController extends AbstractLobbyController {

  private final GameBoardLayoutConfig config = App.getGuiLayouts();
  private List<Player> allRegisteredPlayers = new ArrayList<>();
  private final List<GameParameters> allRegisteredGameServices = new ArrayList<>();
  // registered game service part
  @FXML
  private ScrollPane allRegisteredGameScrollPane;
  @FXML
  private VBox allRegisteredGameVbox;
  // app user related fields
  @FXML
  private ScrollPane allPlayersScrollPane;
  @FXML
  private VBox allPlayersVbox;
  @FXML
  private Button settingButton;
  @FXML
  private Button lobbyPageButton;
  // new user related fields
  @FXML
  private ChoiceBox<String> rolesChoiceBox;
  @FXML
  private ColorPicker newUserColourPicker;
  @FXML
  private TextField userName;
  @FXML
  private PasswordField userPassword;
  @FXML
  private Button addUserButton;

  /**
   * Constructor of AdminPageController.
   */
  public AdminPageController() {
  }


  private void pageSpecificActionBind() {
    settingButton.setOnAction(event -> {
      App.loadNewSceneToPrimaryStage("setting_page.fxml", new SettingPageController());
    });
    // jump back to lobby page
    lobbyPageButton.setOnAction(event -> {
      App.loadNewSceneToPrimaryStage("lobby_page.fxml", new LobbyController());
    });
  }

  private void setUpInitialUsers() {
    LobbyRequestSender sender = App.getLobbyServiceRequestSender();
    // set up the user gui in the scroll pane region
    String adminToken = App.getUser().getAccessToken();
    allRegisteredPlayers = Arrays.asList(sender.getPlayers(adminToken));
    for (Player player : allRegisteredPlayers) {
      PlayerLobbyGui playerLobbyGui = new PlayerLobbyGui(player);
      playerLobbyGui.setBorderColour();
      allPlayersVbox.getChildren().add(playerLobbyGui);
    }
    allPlayersScrollPane.setContent(allPlayersVbox);
  }

  private void setUpInitialRegisteredGames() {
    LobbyRequestSender sender = App.getLobbyServiceRequestSender();
    allRegisteredGameServices.addAll(sender.sendAllGamesRequest());
    if (!allRegisteredGameServices.isEmpty()) {
      for (GameParameters gameParameters : allRegisteredGameServices) {
        allRegisteredGameVbox.getChildren().add(new RegisteredGameGui(gameParameters));
      }
      allRegisteredGameScrollPane.setContent(allRegisteredGameVbox);
    }
  }

  private void setUpCreateUser() {
    //add choices for rolesChoiceBox (player, service, admin)
    rolesChoiceBox.getItems().add("Player");
    rolesChoiceBox.getItems().add("Admin");
    rolesChoiceBox.getItems().add("Service");
    // by default, creating players
    rolesChoiceBox.setValue("Player");

    addUserButton.setOnAction(event -> {
      String msg;
      String title;
      String username = userName.getText();
      String password = userPassword.getText();
      Color prefColour = newUserColourPicker.getValue();
      // Convert the color to a 16-byte encoded string
      String colorString = String.format("%02X%02X%02X%02X",
          (int) (prefColour.getRed() * 255),
          (int) (prefColour.getGreen() * 255),
          (int) (prefColour.getBlue() * 255),
          (int) (prefColour.getOpacity() * 255));

      Role role = Role.valueOf("ROLE_" + rolesChoiceBox.getValue().toUpperCase(Locale.ROOT));
      Player newPlayer = new Player(username, colorString, password, role);
      //passing in name of player who we are adding

      try {
        App.getLobbyServiceRequestSender().putOneNewPlayer(App.getUser().getAccessToken(),
            username, newPlayer);
        //refresh the page
        App.loadNewSceneToPrimaryStage("admin_zone.fxml", new AdminPageController());
        title = "Add New Player Confirmation";
        msg = "Player was added to the Lobby Service database!";
        App.loadPopUpWithController("lobby_warn.fxml",
            new LobbyWarnPopUpController(msg, title),
            config.getSmallPopUpWidth(),
            config.getSmallPopUpHeight(),
            StageStyle.UTILITY);
      } catch (UnirestException e) {
        title = "Add New Player Error";
        msg = "Player could not be added to the Lobby Service database";
        App.loadPopUpWithController("lobby_warn.fxml",
            new LobbyWarnPopUpController(msg, title),
            config.getSmallPopUpWidth(),
            config.getSmallPopUpHeight(),
            StageStyle.UTILITY);
      }
    });
  }


  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    super.initialize(url, resourceBundle);
    // for admin page, bind action to setting and lobby button
    pageSpecificActionBind();

    // all users
    setUpInitialUsers();

    // set up the services part that can be forced unregistered
    setUpInitialRegisteredGames();

    // set up logic for creating one user
    setUpCreateUser();

  }
}
