package ca.group8.gameservice.splendorgame.controller.splendorlogic;

import ca.group8.gameservice.splendorgame.model.splendormodel.CityBoard;
import ca.group8.gameservice.splendorgame.model.splendormodel.CityCard;
import ca.group8.gameservice.splendorgame.model.splendormodel.Extension;
import ca.group8.gameservice.splendorgame.model.splendormodel.PlayerInGame;
import ca.group8.gameservice.splendorgame.model.splendormodel.TableTop;
import java.util.HashMap;

public class ClaimCityAction extends Action {

  private final CityCard cityCard;

  public ClaimCityAction(CityCard cityCard) {
    super.type = this.getClass().getSimpleName();
    this.cityCard = cityCard;
  }
  @Override
  void execute(TableTop curTableTop, PlayerInGame playerInGame, ActionGenerator actionGenerator,
               ActionInterpreter actionInterpreter) {
    CityBoard cityBoard = (CityBoard) curTableTop.getBoard(Extension.CITY);
    cityBoard.assignCityCard(playerInGame.getName(),cityCard);
    // after assigning, terminate
    actionGenerator.getPlayerActionMaps().put(playerInGame.getName(), new HashMap<>());
  }

  public CityCard getCityCard() {
    return cityCard;
  }

}
