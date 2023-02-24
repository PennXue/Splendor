package ca.group8.gameservice.splendorgame.controller.splendorlogic;

import ca.group8.gameservice.splendorgame.controller.SplendorJsonHelper;
import ca.group8.gameservice.splendorgame.model.splendormodel.Board;
import ca.group8.gameservice.splendorgame.model.splendormodel.Card;
import ca.group8.gameservice.splendorgame.model.splendormodel.CityCard;
import ca.group8.gameservice.splendorgame.model.splendormodel.Colour;
import ca.group8.gameservice.splendorgame.model.splendormodel.DevelopmentCard;
import ca.group8.gameservice.splendorgame.model.splendormodel.DoubleGoldPower;
import ca.group8.gameservice.splendorgame.model.splendormodel.Extension;
import ca.group8.gameservice.splendorgame.model.splendormodel.FivePointsPower;
import ca.group8.gameservice.splendorgame.model.splendormodel.GameInfo;
import ca.group8.gameservice.splendorgame.model.splendormodel.NobleCard;
import ca.group8.gameservice.splendorgame.model.splendormodel.PlayerStates;
import ca.group8.gameservice.splendorgame.model.splendormodel.Position;
import ca.group8.gameservice.splendorgame.model.splendormodel.Power;
import ca.group8.gameservice.splendorgame.model.splendormodel.TableTop;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.isharipov.gson.adapters.PolymorphDeserializer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import com.google.gson.reflect.TypeToken;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Aim to test all abstract classes in the whole backend (make sure we can parse them correctly).
 */
public class TestSplendorJsonHelper {
  List<String> names = new ArrayList<>(Arrays.asList("Bob", "Marry"));
  List<Extension> extensions = new ArrayList<>(Arrays.asList(Extension.BASE, Extension.ORIENT));
  PlayerStates playerStates = new PlayerStates(names);
  GameInfo gameInfo = new GameInfo(extensions, names, names.get(0));
  EnumMap<Colour, Integer> price = new EnumMap<Colour,Integer>(Colour.class){{
    put(Colour.BLUE, 3);
    put(Colour.RED, 0);
    put(Colour.BLACK, 0);
    put(Colour.GREEN, 0);
    put(Colour.WHITE, 0);
  }};
  DevelopmentCard devCard = new DevelopmentCard(3,
      price, "c2", 1, Colour.BLACK, 1, new ArrayList<>());

  ActionInterpreter actionInterpreter = new ActionInterpreter(gameInfo, playerStates);

  @Test
  void testSerializeCard() {
    List<Card> testCards = new ArrayList<>();
    CityCard cityCard = new CityCard(15, price, "c1", 1);
    NobleCard nobleCard = new NobleCard(4, price, "c3");
    testCards.add(cityCard);
    testCards.add(devCard);
    testCards.add(nobleCard);

    // use this when you want to serialize/deserialize List<?> or Map<?>
    Type listOfCardType = new TypeToken<List<Card>>(){}.getType();

    // rather than getting new Gson(), we use our customized
    // Gson object: SplendorJsonHelper.getInstance().getGson()
    Gson curGson = SplendorJsonHelper.getInstance().getGson();
    // serialize classes
    String listCardsJson = curGson.toJson(testCards, listOfCardType);
    // parse classes: fromJson(serialized string, type(List or Map you defined))
    List<Card> parsedResult = curGson.fromJson(listCardsJson, listOfCardType);

    for (Card c : parsedResult) {
      if (c instanceof NobleCard) {
        assertEquals(nobleCard, c);
      } else if (c instanceof DevelopmentCard) {
        assertEquals(devCard, c);
      } else if (c instanceof CityCard) {
        assertEquals(cityCard, c);
      }
    }

  }


  @Test
  void testSerializedMapOfAction() {
    Position cardPosition = new Position(1,1);
    PurchaseAction pa = new PurchaseAction(cardPosition,devCard,2);
    ReserveAction ra = new ReserveAction(cardPosition, devCard);
    TakeTokenAction ta = new TakeTokenAction(price);
    Map<String, Action> actionMap = new HashMap<>();
    // convert the pa (PurchaseAction) object to json string, and then hash it with md5
    String paHashed = DigestUtils.md5Hex(SplendorJsonHelper.getInstance().getGson().toJson(pa).toUpperCase());
    String raHashed = DigestUtils.md5Hex(SplendorJsonHelper.getInstance().getGson().toJson(ra).toUpperCase());
    String taHashed = DigestUtils.md5Hex(SplendorJsonHelper.getInstance().getGson().toJson(ta).toUpperCase());
    actionMap.put(paHashed, pa);
    actionMap.put(raHashed, ra);
    actionMap.put(taHashed, ta);
    // now serialize this whole map, we first register a Type Map<String,Action>
    Type actionMapType = new TypeToken<Map<String,Action>>(){}.getType();
    // our magical gson object that handles serializing/deserializing everything
    Gson gson = SplendorJsonHelper.getInstance().getGson();
    String serializedMap = gson.toJson(actionMap, actionMapType);
    // now try to deserialize things
    Map<String, Action> newActionMap = gson.fromJson(serializedMap, actionMapType);

    // the content should identical besides their addresses
    // we did not override the equals yet, so no worry for that for now
    System.out.println(actionMap);
    System.out.println(newActionMap);
    assertEquals(actionMap.keySet(), newActionMap.keySet());
  }

  @Test
  void testSerializeArrayOfPower() {
    DoubleGoldPower dp = new DoubleGoldPower();
    FivePointsPower fp = new FivePointsPower();
    Power[] powers = new Power[] {dp, fp};
    Gson gson = SplendorJsonHelper.getInstance().getGson();
    String powersJson = gson.toJson(powers, Power[].class);
    Power[] newPowers = gson.fromJson(powersJson, Power[].class);
    assertArrayEquals(powers, newPowers);
  }


  @Test
  void testSerializeBoards() {
    TableTop tt = new TableTop(names, extensions);
    Map<Extension,Board> boards = tt.getGameBoards();
    Type boardMapType = new TypeToken<Map<Extension,Board>>(){}.getType();
    Gson gson = SplendorJsonHelper.getInstance().getGson();
    String boardsJson = gson.toJson(boards, boardMapType);
    Map<Extension,Board> newBoards = gson.fromJson(boardsJson, boardMapType);

    assertEquals(boards.keySet(),newBoards.keySet());
  }
}
